package com.cooptweaks.discord;

import com.cooptweaks.Configuration;
import com.cooptweaks.Dimension;
import com.cooptweaks.Main;
import com.cooptweaks.discord.commands.Status;
import com.cooptweaks.types.ConfigMap;
import com.cooptweaks.types.Result;
import com.cooptweaks.utils.TimeSince;
import com.mojang.datafixers.util.Pair;
import discord4j.common.retry.ReconnectOptions;
import discord4j.common.store.Store;
import discord4j.common.store.impl.LocalStoreLayout;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.RestClient;
import discord4j.rest.util.Color;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 Handles the Discord bot and its interactions with the server.
 <p>
 The bot is responsible for sending messages to the Discord channel and the server chat.
 */
public final class Discord {
	/** Whether the bot has finished setting up all necessary components. */
	private static final AtomicBoolean BOT_READY = new AtomicBoolean(false);

	private static MinecraftServer SERVER;
	private static GatewayDiscordClient GATEWAY;

	private static Snowflake BOT_USER_ID;
	private static Snowflake CHANNEL_ID;

	private static MessageChannel CHANNEL;

	private static long LAST_PRESENCE_UPDATE = 0;
	private static boolean PRESENCE_CYCLE = true;

	/** Slash commands. */
	private static final HashMap<String, SlashCommand> COMMANDS = new HashMap<>(Map.of(
			"status", new Status()
	));

	/** Queue of events to be processed after the bot is ready. */
	private static final List<Runnable> QUEUE = new ArrayList<>(2);

	public void QueueEvent(Runnable event) {
		if (BOT_READY.get()) {
			event.run();
		} else {
			QUEUE.add(event);
		}
	}

	private static void ProcessQueue() {
		QUEUE.forEach(Runnable::run);
		QUEUE.clear();
	}

	public void Start() {
		ConfigMap config = new ConfigMap(Configuration.DISCORD_PATH);
		String token = config.get("token").toString();
		String channelId = config.get("channel_id").toString();
		long applicationId = config.get("application_id").toLong();

		if (token.isEmpty() || channelId.isEmpty() || applicationId == 0) {
			Main.LOGGER.error("Some Discord configuration fields are missing, skipping Discord bot setup.");
			return;
		}

		List<ApplicationCommandRequest> commands = new ArrayList<>(COMMANDS.size());

		for (SlashCommand command : COMMANDS.values()) {
			ApplicationCommandRequest cmd = command.build();
			Main.LOGGER.info("Found command '{}'.", command.getName());
			commands.add(cmd);
		}

		DiscordClient.create(token)
				.gateway()
				.setStore(Store.fromLayout(LocalStoreLayout.create()))
				.setEnabledIntents(IntentSet.of(Intent.GUILD_MESSAGES, Intent.MESSAGE_CONTENT, Intent.GUILD_MEMBERS))
				.setInitialPresence(presence -> ClientPresence.idle(ClientActivity.watching("the server start")))
				.setReconnectOptions(ReconnectOptions.builder().setMaxRetries(5).setJitterFactor(0.5).build())
				.login()
				.doOnNext(gateway -> {
					RestClient rest = gateway.getRestClient();

					Main.LOGGER.info("Overwriting global application commands.");
					rest.getApplicationService()
							.bulkOverwriteGlobalApplicationCommand(applicationId, commands)
							.doOnError(err -> Main.LOGGER.error("Failed to overwrite global application commands. Error: {}", err.getMessage()))
							.subscribe();

					GATEWAY = gateway;

					gateway.on(ReadyEvent.class).subscribe(this::onReady);
					gateway.on(MessageCreateEvent.class).subscribe(this::onMessage);
					gateway.on(ChatInputInteractionEvent.class).subscribe(this::onInteraction);
				})
				.flatMap(gateway -> gateway.getChannelById(Snowflake.of(channelId))
						.ofType(MessageChannel.class)
						.doOnNext(channel -> {
							CHANNEL = channel;
							CHANNEL_ID = channel.getId();
							BOT_READY.set(true);

							// Process queued events now that the bot is ready.
							ProcessQueue();

							Main.LOGGER.info("Discord bot online and ready.");
						}))
				.subscribe();
	}

	public void Stop() {
		if (BOT_READY.get()) {
			SendEmbed("Server stopping.", Color.RED);
			Main.LOGGER.info("Logging out of Discord.");
			GATEWAY.logout().block();
		}
	}

	private void onReady(ReadyEvent ready) {
		GATEWAY.updatePresence(ClientPresence.online(ClientActivity.playing("Minecraft"))).subscribe();
		User self = ready.getSelf();
		BOT_USER_ID = self.getId();
	}

	private void onInteraction(ChatInputInteractionEvent event) {
		if (!BOT_READY.get()) {
			return;
		}

		String cmd = event.getCommandName();

		if (COMMANDS.containsKey(cmd)) {
			SlashCommand command = COMMANDS.get(cmd);
			Result<EmbedCreateSpec> embed = command.execute(SERVER);

			if (embed.isSuccess()) {
				event.reply().withEmbeds(embed.getValue()).subscribe();
			} else {
				String err = embed.getError();
				Main.LOGGER.error("Command '{}' failed to execute. Error: {}", cmd, err);
				event.reply().withContent(String.format("Command failed to execute. Error: %s", err)).subscribe();
			}

			return;
		}

		Main.LOGGER.warn("Unknown command '{}'", cmd);
	}

	private void onMessage(MessageCreateEvent event) {
		if (!BOT_READY.get() || SERVER == null || SERVER.getCurrentPlayerCount() == 0) {
			return;
		}

		Message message = event.getMessage();

		// Ignore messages from other channels.
		if (!message.getChannelId().equals(CHANNEL_ID)) {
			return;
		}

		Optional<User> author = message.getAuthor();

		if (author.isEmpty()) {
			return;
		}

		Snowflake authorId = author.get().getId();

		if (authorId.equals(BOT_USER_ID)) {
			return;
		}

		message.getAuthorAsMember()
				.ofType(Member.class)
				.flatMap(member -> member.getColor().map(color -> Pair.of(color, member)))
				.doOnNext(pair -> {
					Color color = pair.getFirst();
					Member member = pair.getSecond();

					String content = message.getContent().trim();

					// The entire message that will be sent to the server.
					MutableText text = Text.empty();

					// Appending the Member display name with the same color as their highest role.
					text.append(Text.literal(member.getDisplayName()).styled(style -> style.withColor(color.getRGB())));
					text.append(Text.literal(" >> " + content));

					List<Attachment> attachments = message.getAttachments();

					// If the message has images, make them clickable.
					if (!attachments.isEmpty()) {
						if (!content.isEmpty()) {
							text.append(Text.literal(" "));
						}

						attachments.stream()
								.map(attachment -> {
									String name = attachment
											.getUrl()
											.substring(attachment.getUrl().lastIndexOf('/') + 1, attachment.getUrl().indexOf('?'));

									Style style = Style.EMPTY
											.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl()))
											.withItalic(true)
											.withUnderline(true)
											.withColor(Formatting.LIGHT_PURPLE);

									return Text.literal(name).setStyle(style);
								})
								.forEach(link -> {
									text.append(link);
									text.append(Text.literal(" "));
								});
					}

					SERVER.getPlayerManager().broadcast(text, false);
				}).subscribe();
	}

	public void SendEmbed(String message, Color color) {
		if (!BOT_READY.get()) {
			return;
		}

		EmbedCreateSpec embed = EmbedCreateSpec.builder()
				.color(color)
				.description(message)
				.build();

		CHANNEL.createMessage(embed).subscribe();
	}

	public void NotifyStarted(MinecraftServer server) {
		SERVER = server;
		LAST_PRESENCE_UPDATE = System.currentTimeMillis();
		QueueEvent(() -> SendEmbed("Server started!", Color.GREEN));
	}

	public void CyclePresence(List<ServerPlayerEntity> players) {
		if (!BOT_READY.get()) {
			return;
		}

		// Only update presence every 5 minutes.
		if (System.currentTimeMillis() - LAST_PRESENCE_UPDATE < 5 * 60 * 1000) {
			return;
		}

		LAST_PRESENCE_UPDATE = System.currentTimeMillis();

		if (PRESENCE_CYCLE) {
			String time = new TimeSince(Main.STARTUP).toString();
			if (time.contains(" and ")) {
				time = time.substring(0, time.indexOf(" and "));
			}

			GATEWAY.updatePresence(ClientPresence.online(ClientActivity.playing(String.format("for %s", time)))).subscribe();
		} else {
			if (players.isEmpty()) {
				GATEWAY.updatePresence(ClientPresence.online(ClientActivity.playing("Minecraft"))).subscribe();
			} else {
				GATEWAY.updatePresence(ClientPresence.online(ClientActivity.watching(String.format("%d players", players.size())))).subscribe();
			}
		}

		PRESENCE_CYCLE = !PRESENCE_CYCLE;
	}

	public void PlayerJoined(String name) {
		// In the case on an integrated server, this event might not be called, so queue it instead.
		QueueEvent(() -> SendEmbed(String.format("**%s** joined!", name), Color.GREEN));
	}

	public void PlayerLeft(ServerPlayerEntity player) {
		SendEmbed(String.format("**%s** left!", player.getName().getString()), Color.BLACK);
	}

	public void PlayerSentChatMessage(ServerPlayerEntity player, Text message) {
		if (!BOT_READY.get()) {
			return;
		}

		String name = player.getName().getString();
		String dimension = Dimension.getPlayerDimension(name);

		String text = String.format("`%s` **%s** >> %s", dimension, name, message.getString());
		CHANNEL.createMessage(text).subscribe();
	}

	public void PlayerChangedDimension(String name, String dimension) {
		String message = String.format("**%s** entered **%s**.", name, dimension);
		SendEmbed(message, Color.BLACK);
	}

	public void PlayerDied(String name, BlockPos pos, Text deathMessage) {
		String dimension = Dimension.getPlayerDimension(name);
		String text = deathMessage.getString().replace(name, String.format("**%s**", name));

		String message = String.format("%s%n*`%s` at %d, %d, %d*", text, dimension, pos.getX(), pos.getY(), pos.getZ());
		SendEmbed(message, Color.RED);
	}
}
