package com.cooptweaks;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.discordjson.json.EmbedData;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import net.fabricmc.loader.api.FabricLoader;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public final class Bridge {
    private static MinecraftServer SERVER;
    private static GatewayDiscordClient GATEWAY;

    private static boolean ENABLED = false;

    private static String TOKEN;
    private static String CHANNEL_ID;
    private static String GUILD_ID;

    private static Snowflake BOT_ID;
    private static Guild GUILD;
    private static RestChannel CHANNEL;

    public static Map<String, String> CURRENT_DIMENSION = new HashMap<>();

    public static final Map<String, String> WORLDS = new HashMap<>(Map.of(
            "minecraft:overworld", "Overworld",
            "minecraft:the_nether", "Nether",
            "minecraft:the_end", "End"
    ));

    private static Bridge INSTANCE = null;

    public static Bridge getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Bridge();
        }
        return INSTANCE;
    }

    private Bridge() {
        Path DISCORD_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("cooptweaks/discord.toml");

        try {
            if (!Files.exists(DISCORD_CONFIG_PATH)) {
                String defaultConfig = "token = " + System.lineSeparator() + "channel_id = " + System.lineSeparator() + "guild_id = ";
                Files.writeString(DISCORD_CONFIG_PATH, defaultConfig, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Map<String, String> config = Config.Parse(DISCORD_CONFIG_PATH);
        TOKEN = config.get("token");
        CHANNEL_ID = config.get("channel_id");
        GUILD_ID = config.get("guild_id");
    }

    public void Start() {
        if (TOKEN.isEmpty() || CHANNEL_ID.isEmpty()) {
            Server.LOGGER.warn("Discord bot is not properly configured.");
            return;
        }

        GATEWAY = DiscordClient.create(TOKEN)
                .gateway()
                .setEnabledIntents(IntentSet.of(Intent.GUILD_MESSAGES, Intent.MESSAGE_CONTENT, Intent.GUILD_MEMBERS))
                .login()
                .block();

        if (GATEWAY == null) {
            Server.LOGGER.error("Failed to get Discord Gateway.");
        }

        GUILD = GATEWAY.getGuildById(Snowflake.of(GUILD_ID)).block();
        if (GUILD == null) {
            Server.LOGGER.error("Failed to get Guild.");
            return;
        }

        Channel channel = GATEWAY.getChannelById(Snowflake.of(CHANNEL_ID)).block();
        if (channel == null) {
            Server.LOGGER.error("Failed to get Channel from channel_id, does the Discord channel exist?");
            return;
        }

        CHANNEL = channel.getRestChannel();

        User user = GATEWAY.getSelf().block();
        if (user == null) {
            Server.LOGGER.error("Failed to get Bot User.");
            return;
        }

        BOT_ID = user.getId();
        Server.LOGGER.info("Logged in as {}", user.getUsername());
        ENABLED = true;

        SendEmbed("Server starting...", Color.BLACK);

        GATEWAY.on(MessageCreateEvent.class).subscribe(this::onMessage);
    }

    public void Stop() {
        SendEmbed("Server stopping.", Color.RED);

        if (ENABLED) {
            GATEWAY.logout().block();
        }
    }

    public void NotifyStarted(MinecraftServer server) {
        SERVER = server;
        SendEmbed("Server started!", Color.GREEN);
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

        if (authorId.equals(BOT_ID)) {
            return;
        }

        GUILD.getMemberById(authorId).doOnNext(member -> {
            String content = message.getContent();

            // The entire message that will be sent to the server.
            MutableText text = Text.empty();

            member.getColor().doOnNext(color -> {
                // Appending the GuildMember display name with the same color as the member's highest role.
                text.append(Text.literal(member.getDisplayName()).styled(style -> style.withColor(color.getRGB())));
                text.append(Text.literal(" > " + content.trim()));

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

                // This double subscribe doesn't seem right.
            }).subscribe();
        }).subscribe();
    }

    public void PlayerJoined(ServerPlayerEntity player) {
        String name = player.getName().getString();
        SendEmbed(String.format("**%s** joined!", name), Color.GREEN);

        // Update the player's current dimension.
        CURRENT_DIMENSION.put(name, player.getWorld().getRegistryKey().getValue().toString());
    }

    public void PlayerLeft(ServerPlayerEntity player) {
        SendEmbed(String.format("**%s** left!", player.getName().getString()), Color.BLACK);
    }

    public void PlayerSentChatMessage(ServerPlayerEntity player, SignedMessage message) {
        if (!ENABLED) {
            return;
        }

        String name = player.getName().getString();
        String dimensionId = CURRENT_DIMENSION.get(name);
        String dimension = getDimensionName(dimensionId);

        String text = String.format("`%s` **%s** >> %s", dimension, name, message.getContent().getString());
        CHANNEL.createMessage(text).subscribe();
    }

    public void PlayerChangedDimension(ServerPlayerEntity player, ServerWorld newWorld) {
        String dimension = getDimensionName(newWorld.getRegistryKey().getValue().toString());

        String name = player.getName().getString();
        CURRENT_DIMENSION.put(name, dimension);

        String message = String.format("**%s** entered **%s**.", name, dimension);
        SendEmbed(message, Color.BLACK);
    }

    public void PlayerDied(ServerPlayerEntity serverPlayerEntity, GlobalPos lastDeathPos, Text deathMessage) {
        String name = serverPlayerEntity.getName().getString();
        String dimensionId = CURRENT_DIMENSION.get(name);
        String dimension = getDimensionName(dimensionId);
        BlockPos pos = lastDeathPos.pos();

        String text = deathMessage.getString().replace(name, String.format("**%s**", name));

        String message = String.format("%s\n*`%s` at %d, %d, %d*", text, dimension, pos.getX(), pos.getY(), pos.getZ());
        SendEmbed(message, Color.RED);
    }

    private String getDimensionName(String dimensionId) {
        String dimension = WORLDS.get(dimensionId);
        if (dimension == null) {
            dimension = dimensionId;
        }
        return dimension;
    }
}
