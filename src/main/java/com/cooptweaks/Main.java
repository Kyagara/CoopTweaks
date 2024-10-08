package com.cooptweaks;

import com.cooptweaks.commands.advancements.Progress;
import com.cooptweaks.commands.misc.LinkCommand;
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

	public static final Discord DISCORD = new Discord();
	public static final Advancements ADVANCEMENTS = new Advancements();

	public static long STARTUP;

	public static void init() {
		Configuration.verify();

		// Start the Discord bot when the server is starting.
		LifecycleEvent.SERVER_BEFORE_START.register(server -> DISCORD.Start());

		LifecycleEvent.SERVER_STARTED.register(server -> {
			STARTUP = System.currentTimeMillis();

			ADVANCEMENTS.loadAdvancements(server);
			DISCORD.NotifyStarted(server);
		});

		LifecycleEvent.SERVER_LEVEL_SAVE.register(world -> DISCORD.CyclePresence(world.getPlayers()));

		LifecycleEvent.SERVER_STOPPING.register(server -> {
			DISCORD.Stop();
			ADVANCEMENTS.unload();
		});

		PlayerEvent.PLAYER_JOIN.register(player -> {
			String name = player.getName().getString();
			String dimensionId = player.getWorld().getRegistryKey().getValue().toString();

			Dimension.updatePlayerCurrentDimension(name, dimensionId);
			DISCORD.PlayerJoined(name);
			ADVANCEMENTS.SyncPlayerOnJoin(player, name);
		});

		PlayerEvent.PLAYER_QUIT.register(DISCORD::PlayerLeft);

		PlayerEvent.PLAYER_ADVANCEMENT.register(ADVANCEMENTS::OnCriterion);

		PlayerEvent.CHANGE_DIMENSION.register((player, oldWorld, newWorld) -> {
			String name = player.getName().getString();
			String dimensionId = newWorld.getValue().toString();

			Dimension.updatePlayerCurrentDimension(name, dimensionId);
			DISCORD.PlayerChangedDimension(name, Dimension.getPlayerDimension(name));
		});

		PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd, removalReason) -> {
			String name = newPlayer.getName().getString();
			String dimensionId = newPlayer.getWorld().getRegistryKey().getValue().toString();

			Dimension.updatePlayerCurrentDimension(name, dimensionId);
		});

		ChatEvent.RECEIVED.register((player, message) -> {
			DISCORD.PlayerSentChatMessage(player, message);
			return EventResult.pass();
		});

		EntityEvent.LIVING_DEATH.register((entity, source) -> {
			if (entity.isPlayer()) {
				String name = entity.getName().getString();
				String dimensionId = entity.getWorld().getRegistryKey().getValue().toString();
				BlockPos pos = entity.getBlockPos();

				Dimension.updatePlayerCurrentDimension(name, dimensionId);
				DISCORD.PlayerDied(name, pos, source.getDeathMessage(entity));
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
