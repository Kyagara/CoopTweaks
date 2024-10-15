package com.cooptweaks;

import com.cooptweaks.commands.advancements.Progress;
import com.cooptweaks.commands.misc.LinkCommand;
import com.cooptweaks.config.AdvancementsConfig;
import com.cooptweaks.config.Configuration;
import com.cooptweaks.config.DiscordConfig;
import com.cooptweaks.keybinds.misc.Link;
import com.cooptweaks.packets.LinkPacket;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.*;
import dev.architectury.networking.NetworkManager;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
	public static final String MOD_ID = "cooptweaks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** The time in milliseconds when the server was started. */
	public static long STARTUP;

	public static void init() {
		Configuration.verify();
		Configuration.load();

		LifecycleEvent.SERVER_BEFORE_START.register(server -> Discord.init());

		LifecycleEvent.SERVER_STARTED.register(server -> {
			STARTUP = System.currentTimeMillis();

			Advancements.start(server);
			Discord.serverStarted(server);
		});

		if (DiscordConfig.enabled()) {
			LifecycleEvent.SERVER_LEVEL_SAVE.register(world -> Discord.cyclePresence(world.getPlayers()));
		}

		LifecycleEvent.SERVER_STOPPING.register(server -> {
			Discord.serverStop();
			Advancements.unload();
		});

		PlayerEvent.PLAYER_JOIN.register(player -> {
			String name = player.getName().getString();
			String dimensionId = player.getWorld().getRegistryKey().getValue().toString();

			Dimension.updatePlayerCurrentDimension(name, dimensionId);

			Discord.playerJoin(name);
			Advancements.syncPlayer(player, name);
		});

		if (DiscordConfig.onLeave()) {
			PlayerEvent.PLAYER_QUIT.register(Discord::playerLeave);
		}

		if (AdvancementsConfig.enabled()) {
			PlayerEvent.PLAYER_ADVANCEMENT.register(Advancements::onCriterion);
		}

		PlayerEvent.CHANGE_DIMENSION.register((player, oldWorld, newWorld) -> {
			String name = player.getName().getString();
			String dimensionId = newWorld.getValue().toString();

			Dimension.updatePlayerCurrentDimension(name, dimensionId);

			Discord.playerChangedDimension(name, Dimension.getPlayerDimension(name));

		});

		PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd, removalReason) -> {
			String name = newPlayer.getName().getString();
			String dimensionId = newPlayer.getWorld().getRegistryKey().getValue().toString();

			Dimension.updatePlayerCurrentDimension(name, dimensionId);
		});

		if (DiscordConfig.onMessage()) {
			ChatEvent.RECEIVED.register((player, message) -> {
				Discord.playerMessage(player, message);
				return EventResult.pass();
			});
		}

		EntityEvent.LIVING_DEATH.register((entity, source) -> {
			if (entity.isPlayer()) {
				String name = entity.getName().getString();
				String dimensionId = entity.getWorld().getRegistryKey().getValue().toString();
				BlockPos pos = entity.getBlockPos();

				Dimension.updatePlayerCurrentDimension(name, dimensionId);
				Discord.playerDeath(name, pos, source.getDeathMessage(entity));
			}

			return EventResult.pass();
		});

		CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
			// Advancements
			new Progress().register(dispatcher, registryAccess, environment);

			// Misc
			new LinkCommand().register(dispatcher, registryAccess, environment);
		});

		// Handle the Link packet sent from a client.
		NetworkManager.registerReceiver(NetworkManager.Side.C2S, LinkPacket.PAYLOAD_ID, LinkPacket.CODEC, Link::handlePacket);
	}
}
