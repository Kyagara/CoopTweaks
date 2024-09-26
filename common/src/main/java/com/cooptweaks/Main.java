package com.cooptweaks;

import com.cooptweaks.advancements.Advancements;
import com.cooptweaks.discord.Discord;
import com.cooptweaks.events.GrantCriterionCallback;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.*;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public final class Main {
	public static final String MOD_ID = "cooptweaks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Configuration CONFIG = new Configuration();

	public static final Discord DISCORD = Discord.getInstance();
	public static final Advancements ADVANCEMENTS = Advancements.getInstance();

	/** Maps player names to their current dimension. */
	private static final ConcurrentHashMap<String, String> PLAYER_CURRENT_DIMENSION_ID = new ConcurrentHashMap<>();

	public static void init() {
		LifecycleEvent.SERVER_BEFORE_START.register(server -> {
			DISCORD.Start(CONFIG.getDiscordConfig());
		});

		LifecycleEvent.SERVER_STARTED.register(server -> {
			DISCORD.NotifyStarted(server);

			// Requires the server to be started since the seed won't be available until then.
			// This might be changed if manually reading the level.dat, haven't seen any issue from doing it this way yet.
			ADVANCEMENTS.LoadAdvancements(server, CONFIG.getAdvancementsSavePath());
		});

		LifecycleEvent.SERVER_STOPPING.register(server -> {
			DISCORD.Stop();
		});

		PlayerEvent.PLAYER_JOIN.register(player -> {
			String name = player.getName().getString();
			String dimensionId = player.getWorld().getRegistryKey().getValue().toString();

			updatePlayerCurrentDimension(name, dimensionId);
			DISCORD.PlayerJoined(name);
			ADVANCEMENTS.SyncPlayerOnJoin(player, name);
		});

		PlayerEvent.PLAYER_QUIT.register(DISCORD::PlayerLeft);

		PlayerEvent.CHANGE_DIMENSION.register((player, oldWorld, newWorld) -> {
			String name = player.getName().getString();
			String dimensionId = newWorld.getValue().toString();

			updatePlayerCurrentDimension(name, dimensionId);
			DISCORD.PlayerChangedDimension(name, Utils.getPlayerDimension(name));
		});

		// Maybe use DECORATE event instead?
		ChatEvent.RECEIVED.register((player, message) -> {
			DISCORD.PlayerSentChatMessage(player, message);
			return EventResult.pass();
		});

		EntityEvent.LIVING_DEATH.register((entity, source) -> {
			if (entity.isPlayer()) {
				String name = entity.getName().getString();
				String dimensionId = entity.getWorld().getRegistryKey().getValue().toString();
				BlockPos pos = entity.getBlockPos();

				updatePlayerCurrentDimension(name, dimensionId);
				DISCORD.PlayerDied(name, pos, source.getDeathMessage(entity));
			}

			return EventResult.pass();
		});

		GrantCriterionCallback.EVENT.register(ADVANCEMENTS::OnCriterion);
		CommandRegistrationEvent.EVENT.register(ADVANCEMENTS::RegisterCommands);
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
