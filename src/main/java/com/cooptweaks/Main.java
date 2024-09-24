package com.cooptweaks;

import com.cooptweaks.advancements.Advancements;
import com.cooptweaks.discord.Discord;
import com.cooptweaks.event.GrantCriterionCallback;
import com.cooptweaks.event.PlayerDeathCallback;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class Main implements ModInitializer {
	public static final String MOD_ID = "cooptweaks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Configuration CONFIG = new Configuration();

	public static final Discord DISCORD = Discord.getInstance();
	public static final Advancements ADVANCEMENTS = Advancements.getInstance();

	/** Maps player names to their current dimension. */
	private static final ConcurrentHashMap<String, String> PLAYER_CURRENT_DIMENSION_ID = new ConcurrentHashMap<>();

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
			DISCORD.Start(CONFIG.getDiscordConfig());
		});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			DISCORD.NotifyStarted(server);

			// Requires the server to be started since the seed won't be available until then.
			// This might be changed if manually reading the level.dat, haven't seen any issue from doing it this way yet.
			ADVANCEMENTS.LoadAdvancements(server, CONFIG.getAdvancementsSavePath());
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			DISCORD.Stop();
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;
			String name = player.getName().getString();
			String dimensionId = player.getWorld().getRegistryKey().getValue().toString();

			updatePlayerCurrentDimension(name, dimensionId);
			DISCORD.PlayerJoined(name);
			ADVANCEMENTS.SyncPlayerOnJoin(player, name);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, sender) -> {
			ServerPlayerEntity player = handler.player;
			DISCORD.PlayerLeft(player);
		});

		ServerMessageEvents.CHAT_MESSAGE.register((message, player, parameters) -> {
			DISCORD.PlayerSentChatMessage(player, message);
		});

		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, oldWorld, newWorld) -> {
			String name = player.getName().getString();
			String dimensionId = newWorld.getRegistryKey().getValue().toString();

			updatePlayerCurrentDimension(name, dimensionId);
			DISCORD.PlayerChangedDimension(name, Utils.getPlayerDimension(name));
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			String name = newPlayer.getName().getString();
			String dimensionId = newPlayer.getWorld().getRegistryKey().getValue().toString();

			updatePlayerCurrentDimension(name, dimensionId);
		});

		PlayerDeathCallback.EVENT.register(DISCORD::PlayerDied);
		GrantCriterionCallback.EVENT.register(ADVANCEMENTS::OnCriterion);

		CommandRegistrationCallback.EVENT.register(ADVANCEMENTS::RegisterCommands);
	}

	/** Gets the current dimension ID of a player. */
	public static String getPlayerCurrentDimension(String name) {
		return PLAYER_CURRENT_DIMENSION_ID.get(name);
	}

	/** Updates the current dimension ID of a player. */
	public static void updatePlayerCurrentDimension(String name, String dimensionId) {
		PLAYER_CURRENT_DIMENSION_ID.put(name, dimensionId);
	}
}