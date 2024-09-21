package com.cooptweaks.discord;

import com.cooptweaks.Config;
import com.cooptweaks.Result;
import com.cooptweaks.Server;
import com.cooptweaks.Utils;
import com.cooptweaks.discord.commands.Status;
import com.ibm.icu.impl.Pair;
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

public final class Bridge {
	private static MinecraftServer SERVER;

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
		for (Runnable event : new ArrayList<>(QUEUE)) {
			event.run();
		}

		QUEUE.clear();
	}

	private static GatewayDiscordClient GATEWAY;

	/** Whether the bot is ready to bridge messages, set after finishing setup. */
	private static final AtomicBoolean BOT_READY = new AtomicBoolean(false);

	private static Snowflake BOT_USER_ID;

	/** Used to send messages to the channel */
	private static RestChannel CHANNEL;

	/** Slash commands. Not sure if a map is the best way to do this. */
	private static final Map<String, SlashCommand> COMMANDS = new HashMap<>(Map.of(
			"status", new Status()
	));

	private static Bridge INSTANCE = null;

	public static Bridge getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new Bridge();
		}
		return INSTANCE;
	}

	private Bridge() {
	}

	public void Start() {
		Map<String, String> config = Config.Parse(Config.DISCORD);
		String token = config.get("token");
		String channelId = config.get("channel_id");
		String applicationId = config.get("application_id");

		if (token.isEmpty() || channelId.isEmpty() || applicationId.isEmpty()) {
			Server.LOGGER.error("Discord bot is not properly configured.");
			return;
		}

		List<ApplicationCommandRequest> commands = new ArrayList<>(COMMANDS.size());

		for (String key : COMMANDS.keySet()) {
			SlashCommand command = COMMANDS.get(key);
			ApplicationCommandRequest cmd = command.Build();

			Server.LOGGER.info("Found command {}", command.getName());
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

					Server.LOGGER.info("Overwriting global application commands.");
					rest.getApplicationService()
							.bulkOverwriteGlobalApplicationCommand(Long.parseLong(applicationId), commands)
							.doOnError(err -> Server.LOGGER.error("Failed to overwrite global application commands. Error: {}", err.getMessage()))
							.subscribe();

					gateway.on(ReadyEvent.class).subscribe(this::onReady);
					gateway.on(MessageCreateEvent.class).subscribe(this::onMessage);
					gateway.on(ChatInputInteractionEvent.class).subscribe(this::onInteraction);

					GATEWAY = gateway;
				})
				.flatMap(gateway -> gateway.getChannelById(Snowflake.of(channelId))
						.filter(Objects::nonNull)
						.map(Channel::getRestChannel)
						.filter(Objects::nonNull)
						.doOnNext(channel -> {
							CHANNEL = channel;
							BOT_READY.set(true);

							// Process queued events now that the bot is ready.
							ProcessQueue();
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
		Server.LOGGER.info("Logged in as {}", self.getUsername());
	}

	private void onInteraction(ChatInputInteractionEvent event) {
		if (!BOT_READY.get()) {
			return;
		}

		String cmd = event.getCommandName();

		if (COMMANDS.containsKey(cmd)) {
			Server.LOGGER.info("Executing command {}", cmd);

			SlashCommand command = COMMANDS.get(cmd);
			Result<EmbedCreateSpec> embed = command.Execute(SERVER);

			if (embed.isSuccess()) {
				event.reply().withEmbeds(embed.getValue()).subscribe();
			} else {
				String err = embed.getError();
				Server.LOGGER.error("Command {} failed to execute. Error: {}", cmd, err);
				event.reply().withContent(String.format("Command failed to execute. Error: %s", err)).subscribe();
			}

			return;
		}

		Server.LOGGER.warn("Unknown command {}", cmd);
	}

	private void onMessage(MessageCreateEvent event) {
		if (!BOT_READY.get() || SERVER == null || SERVER.getCurrentPlayerCount() == 0) {
			return;
		}

		Message message = event.getMessage();
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
					Color color = pair.first;
					Member member = pair.second;

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
		String dimension = Utils.GetPlayerDimension(name);

		String text = String.format("`%s` **%s** >> %s", dimension, name, message.getContent().getString());
		CHANNEL.createMessage(text).subscribe();
	}

	public void PlayerChangedDimension(String name, String dimension) {
		String message = String.format("**%s** entered **%s**.", name, dimension);
		SendEmbed(message, Color.BLACK);
	}

	public void PlayerDied(ServerPlayerEntity serverPlayerEntity, GlobalPos lastDeathPos, Text deathMessage) {
		String name = serverPlayerEntity.getName().getString();
		String dimension = Utils.GetPlayerDimension(name);
		BlockPos pos = lastDeathPos.pos();

		String text = deathMessage.getString().replace(name, String.format("**%s**", name));

		String message = String.format("%s\n*`%s` at %d, %d, %d*", text, dimension, pos.getX(), pos.getY(), pos.getZ());
		SendEmbed(message, Color.RED);
	}
}
