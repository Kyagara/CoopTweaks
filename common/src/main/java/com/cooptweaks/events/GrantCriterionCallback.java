package com.cooptweaks.events;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.network.ServerPlayerEntity;

public interface GrantCriterionCallback {
	Event<GrantCriterionCallback> EVENT = EventFactory.createLoop(GrantCriterionCallback.class);

	void grantCriterion(ServerPlayerEntity player, AdvancementEntry entry, String criterionName, boolean isDone);
}
