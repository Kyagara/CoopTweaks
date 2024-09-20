package com.cooptweaks;

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
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.EmbedData;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.RestClient;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.*;

public final class Discord {
	private static MinecraftServer SERVER;
	private static GatewayDiscordClient GATEWAY;

	private static boolean ENABLED = false;

	private static Snowflake BOT_USER_ID;

	/** Used to send messages to the channel */
	private static RestChannel CHANNEL;

	/** Maps player names to their current dimension. */
	public static Map<String, String> CURRENT_DIMENSION_ID = new HashMap<>();

	/** Maps dimension IDs to their names. */
	public static final Map<String, String> WORLDS = new HashMap<>(Map.of(
			"minecraft:overworld", "Overworld",
			"minecraft:the_nether", "Nether",
			"minecraft:the_end", "End"
	));

	private static Discord INSTANCE = null;

	public static Discord getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new Discord();
		}
		return INSTANCE;
	}

	private Discord() {
	}

	public void Start() {
		Map<String, String> config = Config.Parse(Config.DISCORD);
		String token = config.get("token");
		String channelId = config.get("channel_id");

		if (token.isEmpty() || channelId.isEmpty()) {
			Server.LOGGER.warn("Discord bot is not properly configured.");
			return;
		}

		// For now, blocking just once to make sure the gateway is ready.
		GATEWAY = DiscordClient.create(token)
				.gateway()
				.setStore(Store.fromLayout(LocalStoreLayout.create()))
				.setEnabledIntents(IntentSet.of(Intent.GUILD_MESSAGES, Intent.MESSAGE_CONTENT, Intent.GUILD_MEMBERS))
				.setInitialPresence(presence -> ClientPresence.idle(ClientActivity.watching("the server start")))
				.login()
				.block();

		registerInteractions();

		GATEWAY.on(ReadyEvent.class).subscribe(this::onReady);
		GATEWAY.on(MessageCreateEvent.class).subscribe(this::onMessage);
		GATEWAY.on(ChatInputInteractionEvent.class).subscribe(this::onInteraction);

		GATEWAY.getChannelById(Snowflake.of(channelId))
				.filter(Objects::nonNull)
				.map(Channel::getRestChannel)
				.filter(Objects::nonNull)
				.doOnNext(channel -> {
					CHANNEL = channel;
					ENABLED = true;

					SendEmbed("Server starting...", Color.BLACK);
				})
				.subscribe();
	}

	private void registerInteractions() {
		RestClient rest = GATEWAY.getRestClient();

		// Map of functions to be called when the command is executed.
		// There's only a few commands, so there's no need for a more complex system.
		List<String> list = List.of(
				"status:Shows information about the server."
		);

		List<ApplicationCommandRequest> commands = new ArrayList<>(list.size());

		for (String key : list) {
			// Get the command name and description from the key.
			String[] parts = key.split(":");
			String name = parts[0];
			String description = parts[1];

			ApplicationCommandRequest cmd = ApplicationCommandRequest.builder()
					.name(name)
					.description(description)
					.build();

			commands.add(cmd);
		}

		rest.getApplicationService()
				.bulkOverwriteGlobalApplicationCommand(1L, commands)
				.subscribe();
	}

	public void Stop() {
		SendEmbed("Server stopping.", Color.RED);

		if (ENABLED) {
			GATEWAY.logout().block();
		}
	}

	private void onReady(ReadyEvent ready) {
		User self = ready.getSelf();
		BOT_USER_ID = self.getId();
		Server.LOGGER.info("Logged in as {}", self.getUsername());
	}

	private void onInteraction(ChatInputInteractionEvent event) {
		if (!ENABLED) {
			return;
		}

		String cmd = event.getCommandName();

		if (cmd.equals("status")) {
			if (SERVER == null) {
				event.reply("Server has not started yet.").subscribe();
			}

			String motd = SERVER.getServerMotd();
			String address = SERVER.getServerIp() + ":" + SERVER.getServerPort();
			String version = SERVER.getVersion();
			String players = String.format("%d/%d", SERVER.getCurrentPlayerCount(), SERVER.getMaxPlayerCount());
			String advancements = String.format("%d/%d", Advancements.COMPLETED_ADVANCEMENTS.size(), Advancements.ALL_ADVANCEMENTS.size());

			// String.Format everything to a single string.
			String message = String.format("`MOTD`: %s\n`Address`: %s\n`Version`: %s\n`Players`: %s\n`Advancements`: %s",
					motd, address, version, players, advancements);

			event.reply(message).subscribe();
		}
	}

	private void onMessage(MessageCreateEvent event) {
		if (!ENABLED || SERVER == null || SERVER.getCurrentPlayerCount() == 0) {
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

					String content = message.getContent();

					// The entire message that will be sent to the server.
					MutableText text = Text.empty();

					// Appending the GuildMember display name with the same color as the member's highest role.
					text.append(Text.literal(member.getDisplayName()).styled(style -> style.withColor(color.getRGB())));
					text.append(Text.literal(" >> " + content.trim()));

					List<Attachment> attachments = message.getAttachments();

					// If the message has images, make them clickable.
					if (!attachments.isEmpty()) {
						List<Text> links = new ArrayList<>();

						for (Attachment attachment : message.getAttachments()) {
							// Get the file name from the url, stopping at the extension.
							String name = attachment.getUrl().substring(attachment.getUrl().lastIndexOf('/') + 1, attachment.getUrl().indexOf('?'));

							Style style = Style.EMPTY
									.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl()))
									.withItalic(true)
									.withUnderline(true)
									.withColor(Formatting.LIGHT_PURPLE);

							links.add(Text.literal(name).setStyle(style));
						}

						if (!content.isEmpty()) {
							text.append(Text.literal(" "));
						}

						links.forEach(link -> {
							text.append(link);
							text.append(Text.literal(" "));
						});
					}

					SERVER.getPlayerManager().broadcast(text, false);
				}).subscribe();
	}

	public void SendEmbed(String message, Color color) {
		if (!ENABLED) {
			return;
		}

		var embed = EmbedData.builder()
				.color(color.getRGB())
				.description(message)
				.build();

		CHANNEL.createMessage(embed).subscribe();
	}

	public void NotifyStarted(MinecraftServer server) {
		SERVER = server;
		GATEWAY.updatePresence(ClientPresence.online(ClientActivity.playing("Minecraft"))).subscribe();
		SendEmbed("Server started!", Color.GREEN);
	}

	public void PlayerJoined(ServerPlayerEntity player) {
		String name = player.getName().getString();
		SendEmbed(String.format("**%s** joined!", name), Color.GREEN);

		// Update the player's current dimension.
		CURRENT_DIMENSION_ID.put(name, player.getWorld().getRegistryKey().getValue().toString());
	}

	public void PlayerLeft(ServerPlayerEntity player) {
		SendEmbed(String.format("**%s** left!", player.getName().getString()), Color.BLACK);
	}

	public void PlayerSentChatMessage(ServerPlayerEntity player, SignedMessage message) {
		if (!ENABLED) {
			return;
		}

		String name = player.getName().getString();
		String dimensionId = CURRENT_DIMENSION_ID.get(name);
		String dimension = getDimensionName(dimensionId);

		String text = String.format("`%s` **%s** >> %s", dimension, name, message.getContent().getString());
		CHANNEL.createMessage(text).subscribe();
	}

	public void PlayerChangedDimension(ServerPlayerEntity player, ServerWorld newWorld) {
		String dimension = getDimensionName(newWorld.getRegistryKey().getValue().toString());

		String name = player.getName().getString();
		CURRENT_DIMENSION_ID.put(name, dimension);

		String message = String.format("**%s** entered **%s**.", name, dimension);
		SendEmbed(message, Color.BLACK);
	}

	public void PlayerDied(ServerPlayerEntity serverPlayerEntity, GlobalPos lastDeathPos, Text deathMessage) {
		String name = serverPlayerEntity.getName().getString();
		String dimension = getDimensionName(CURRENT_DIMENSION_ID.get(name));
		BlockPos pos = lastDeathPos.pos();

		String text = deathMessage.getString().replace(name, String.format("**%s**", name));

		String message = String.format("%s\n*`%s` at %d, %d, %d*", text, dimension, pos.getX(), pos.getY(), pos.getZ());
		SendEmbed(message, Color.RED);
	}

	/**
	 Gets the name of a dimension from its ID.

	 @param dimensionId The dimension ID. Example: "minecraft:overworld".
	 @return The name of the dimension. Example: "Overworld".
	 */
	private String getDimensionName(String dimensionId) {
		String dimension = WORLDS.get(dimensionId);
		if (dimension == null) {
			dimension = dimensionId;
		}
		return dimension;
	}
}
