package com.cooptweaks.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.GlobalPos;

public interface PlayerDeathCallback {
	Event<PlayerDeathCallback> EVENT = EventFactory.createArrayBacked(PlayerDeathCallback.class,
			(listeners) -> (player, lastDeathPos, deathMessage) -> {
				for (PlayerDeathCallback listener : listeners) {
					listener.onDeath(player, lastDeathPos, deathMessage);
				}
			});

	void onDeath(ServerPlayerEntity player, GlobalPos lastDeathPos, Text deathMessage);
}
