package com.cooptweaks.discord;

import com.cooptweaks.Main;
import com.cooptweaks.Utils;
import com.cooptweaks.discord.commands.Status;
import com.cooptweaks.types.ConfigMap;
import com.cooptweaks.types.Result;
import com.mojang.datafixers.util.Pair;
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
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.ImmutableEmbedData;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.RestClient;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Discord {
	private Discord() {
	}

	private static Discord INSTANCE = null;

	public synchronized static Discord getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new Discord();
		}

		return INSTANCE;
	}

	private static MinecraftServer SERVER;
	private static GatewayDiscordClient GATEWAY;

	private static Snowflake BOT_USER_ID;
	private static Snowflake CHANNEL_ID;

	private static RestChannel CHANNEL;

	/** Slash commands. */
	// Not sure if a map is the best way to do this.
	private static final HashMap<String, SlashCommand> COMMANDS = new HashMap<>(Map.of(
			"status", new Status()
	));

	/** Whether the bot has finished setting up all necessary components. */
	private static final AtomicBoolean BOT_READY = new AtomicBoolean(false);

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

	public void Start(ConfigMap config) {
		String token = config.get("token").toString();
		String channelId = config.get("channel_id").toString();
		long applicationId = config.get("application_id").toLong();

		if (token.isEmpty() || channelId.isEmpty() || applicationId == 0) {
			Main.LOGGER.error("Discord bot is not properly configured.");
			return;
		}

		List<ApplicationCommandRequest> commands = new ArrayList<>(COMMANDS.size());

		for (SlashCommand command : COMMANDS.values()) {
			ApplicationCommandRequest cmd = command.build();
			Main.LOGGER.info("Found command '{}'", command.getName());
			commands.add(cmd);
		}

		DiscordClient.create(token)
				.gateway()
				.setStore(Store.fromLayout(LocalStoreLayout.create()))
				.setEnabledIntents(IntentSet.of(Intent.GUILD_MESSAGES, Intent.MESSAGE_CONTENT, Intent.GUILD_MEMBERS))
				.setInitialPresence(presence -> ClientPresence.idle(ClientActivity.watching("the server start")))
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
						.filter(Objects::nonNull)
						.map(Channel::getRestChannel)
						.filter(Objects::nonNull)
						.doOnNext(channel -> {
							CHANNEL = channel;
							CHANNEL_ID = channel.getId();
							BOT_READY.set(true);

							// Process queued events now that the bot is ready.
							ProcessQueue();

							Main.LOGGER.info("Discord bot ready.");
						}))
				.subscribe();
	}

	public void Stop() {
		SendEmbed("Server stopping.", Color.RED);

		if (BOT_READY.get()) {
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
			Main.LOGGER.info("Executing command '{}'", cmd);

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
				.filter(Objects::nonNull)
				.flatMap(member -> member.getColor().map(color -> Pair.of(color, member)))
				.doOnNext(pair -> {
					Color color = pair.getFirst();
					Member member = pair.getSecond();

					String content = message.getContent().trim();

					// The entire message that will be sent to the server.
					MutableText text = Text.empty();

					// Appending the GuildMember display name with the same color as the member's highest role.
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

		ImmutableEmbedData embed = EmbedData.builder()
				.color(color.getRGB())
				.description(message)
				.build();

		CHANNEL.createMessage(embed).subscribe();
	}

	public void NotifyStarted(MinecraftServer server) {
		SERVER = server;
		QueueEvent(() -> SendEmbed("Server started!", Color.GREEN));
	}

	public void PlayerJoined(String name) {
		SendEmbed(String.format("**%s** joined!", name), Color.GREEN);
	}

	public void PlayerLeft(ServerPlayerEntity player) {
		SendEmbed(String.format("**%s** left!", player.getName().getString()), Color.BLACK);
	}

	public void PlayerSentChatMessage(ServerPlayerEntity player, SignedMessage message) {
		if (!BOT_READY.get()) {
			return;
		}

		String name = player.getName().getString();
		String dimension = Utils.getPlayerDimension(name);

		String text = String.format("`%s` **%s** >> %s", dimension, name, message.getContent().getString());
		CHANNEL.createMessage(text).subscribe();
	}

	public void PlayerChangedDimension(String name, String dimension) {
		String message = String.format("**%s** entered **%s**.", name, dimension);
		SendEmbed(message, Color.BLACK);
	}

	public void PlayerDied(ServerPlayerEntity serverPlayerEntity, GlobalPos lastDeathPos, Text deathMessage) {
		String name = serverPlayerEntity.getName().getString();
		String dimension = Utils.getPlayerDimension(name);
		BlockPos pos = lastDeathPos.pos();

		String text = deathMessage.getString().replace(name, String.format("**%s**", name));

		String message = String.format("%s\n*`%s` at %d, %d, %d*", text, dimension, pos.getX(), pos.getY(), pos.getZ());
		SendEmbed(message, Color.RED);
	}
}
