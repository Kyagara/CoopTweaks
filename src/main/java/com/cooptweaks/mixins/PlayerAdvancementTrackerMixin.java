package com.cooptweaks.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.advancement.PlayerAdvancementTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {
	@ModifyExpressionValue(method = "method_53637(Lnet/minecraft/advancement/AdvancementEntry;Lnet/minecraft/advancement/AdvancementDisplay;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancement/AdvancementDisplay;shouldAnnounceToChat()Z"))
	public boolean disableVanillaAnnounceAdvancements(boolean original) {
		return false;
	}
}