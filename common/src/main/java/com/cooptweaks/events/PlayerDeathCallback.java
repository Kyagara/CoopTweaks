package com.cooptweaks.events;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.GlobalPos;

public interface PlayerDeathCallback {
	Event<PlayerDeathCallback> EVENT = EventFactory.createLoop(PlayerDeathCallback.class);

	void onDeath(ServerPlayerEntity player, GlobalPos lastDeathPos, Text deathMessage);
}
