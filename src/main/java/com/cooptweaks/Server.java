package com.cooptweaks;

import com.cooptweaks.discord.Bridge;
import com.cooptweaks.events.GrantCriterionCallback;
import com.cooptweaks.events.PlayerDeathCallback;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Server implements ModInitializer {
	public static final String MOD_ID = "cooptweaks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Bridge BRIDGE = Bridge.getInstance();
	public static final Advancements ADVANCEMENTS = Advancements.getInstance();

	/** Maps player names to their current dimension. */
	public static Map<String, String> PLAYER_CURRENT_DIMENSION_ID = new HashMap<>();

	@Override
	public void onInitialize() {
		// Verify that the necessary config files exist.
		Config.Verify();

		ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
			BRIDGE.Start();
		});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			BRIDGE.NotifyStarted(server);
			// Requires the server to be started since the seed won't be available until then.
			// This might be changed if manually reading the level.dat, haven't seen any issue from doing it this way.
			ADVANCEMENTS.LoadAdvancements(server);
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			BRIDGE.Stop();
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;
			String name = player.getName().getString();
			String dimensionId = player.getWorld().getRegistryKey().getValue().toString();

			// Update the player's current dimension.
			PLAYER_CURRENT_DIMENSION_ID.put(name, dimensionId);

			BRIDGE.PlayerJoined(name);
			ADVANCEMENTS.SyncPlayerOnJoin(player, name);
		});

		ServerMessageEvents.CHAT_MESSAGE.register((message, player, parameters) -> {
			BRIDGE.PlayerSentChatMessage(player, message);
		});

		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, oldWorld, newWorld) -> {
			String name = player.getName().getString();
			String dimensionId = newWorld.getRegistryKey().getValue().toString();

			// Update the player's current dimension.
			PLAYER_CURRENT_DIMENSION_ID.put(name, dimensionId);

			BRIDGE.PlayerChangedDimension(name, Utils.GetPlayerDimension(name));
		});

		PlayerDeathCallback.EVENT.register(BRIDGE::PlayerDied);

		ServerPlayConnectionEvents.DISCONNECT.register((handler, sender) -> {
			ServerPlayerEntity player = handler.player;
			BRIDGE.PlayerLeft(player);
		});

		GrantCriterionCallback.EVENT.register(ADVANCEMENTS::OnCriterion);

		CommandRegistrationCallback.EVENT.register(ADVANCEMENTS::RegisterCommands);
	}
}