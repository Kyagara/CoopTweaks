package com.cooptweaks.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.network.ServerPlayerEntity;

public interface GrantCriterionCallback {
	Event<GrantCriterionCallback> EVENT = EventFactory.createArrayBacked(GrantCriterionCallback.class,
			(listeners) -> (player, entry, criterionName, isDone) -> {
				for (GrantCriterionCallback listener : listeners) {
					listener.grantCriterion(player, entry, criterionName, isDone);
				}
			});

	void grantCriterion(ServerPlayerEntity player, AdvancementEntry entry, String criterionName, boolean isDone);
}
