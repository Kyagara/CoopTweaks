package com.cooptweaks;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.discordjson.json.EmbedData;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Bridge {
    // Discord config file, stores the bot token and channel id to send events and relay messages between the server and the channel.
    private static final Path DISCORD_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("cooptweaks/discord.toml");

    private static Bridge INSTANCE = null;

    private static MinecraftServer SERVER;
    private static GatewayDiscordClient GATEWAY;

    private static boolean ENABLED = false;

    private static String TOKEN;
    private static String CHANNEL_ID;

    private static Snowflake USER_ID;
    private static RestChannel CHANNEL;

    private Bridge() {
        try {
            if (!Files.exists(DISCORD_CONFIG_PATH)) {
                String defaultConfig = "token = " + System.lineSeparator() + "channel_id = ";
                Files.writeString(DISCORD_CONFIG_PATH, defaultConfig, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Map<String, String> config = Config.Parse(DISCORD_CONFIG_PATH);
        TOKEN = config.get("token");
        CHANNEL_ID = config.get("channel_id");
    }

    public static Bridge getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Bridge();
        }

        return INSTANCE;
    }

    public void Start() {
        if (TOKEN.isEmpty() || CHANNEL_ID.isEmpty()) {
            Server.LOGGER.warn("Discord bot is not configured, skipping...");
            return;
        }

        GATEWAY = DiscordClient.create(TOKEN)
                .gateway()
                .setEnabledIntents(IntentSet.of(Intent.GUILD_MESSAGES, Intent.MESSAGE_CONTENT, Intent.GUILD_MEMBERS))
                .login()
                .block();

        if (GATEWAY == null) {
            Server.LOGGER.error("Failed to login to Discord, skipping...");
            return;
        }

        USER_ID = GATEWAY.getSelfId();

        Channel channel = GATEWAY.getChannelById(Snowflake.of(CHANNEL_ID)).block();
        if (channel == null) {
            Server.LOGGER.error("Failed to get channel from channel_id.");
            return;
        }

        CHANNEL = channel.getRestChannel();
        ENABLED = true;

        // Send messages from the channel to the server.
        GATEWAY.on(MessageCreateEvent.class).subscribe(this::onMessage);

        SendEmbed("Server starting...", Color.BLACK);
    }

    private void onMessage(MessageCreateEvent event) {
        if (SERVER == null) {
            return;
        }

        if (event.getMessage().getAuthor().isEmpty() || event.getMessage().getAuthor().get().getId().equals(USER_ID)) {
            return;
        }

        Message message = event.getMessage();
        String sender = message.getAuthor().get().getUsername();
        String content = message.getContent();

        MutableText text = Text.literal(String.format("%s > ", sender));

        if (!content.isEmpty()) {
            text.append(content.trim());
        }

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
    }

    public void SendMessage(String text) {
        if (!ENABLED) {
            return;
        }
        CHANNEL.createMessage(text).block();
    }

    public void SendEmbed(String message, Color color) {
        if (!ENABLED) {
            return;
        }

        var embed = EmbedData.builder()
                .color(color.getRGB())
                .description(message)
                .build();

        CHANNEL.createMessage(embed).block();
    }

    public void NotifyStarted(MinecraftServer server) {
        if (!ENABLED) {
            return;
        }
        SERVER = server;
        SendEmbed("Server started!", Color.GREEN);
    }

    public void Stop() {
        if (!ENABLED) {
            return;
        }
        SendEmbed("Server stopping!", Color.RED);
        GATEWAY.logout().block();
    }
}
