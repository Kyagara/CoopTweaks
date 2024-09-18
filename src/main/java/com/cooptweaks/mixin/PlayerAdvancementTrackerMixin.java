package com.cooptweaks.mixin;

import com.cooptweaks.events.GrantCriterionCallback;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {
    @Shadow
    private ServerPlayerEntity owner;

    @Shadow
    public abstract AdvancementProgress getProgress(AdvancementEntry advancement);

    @Inject(method = "grantCriterion", at = @At(value = "RETURN"))
    public void grantCriterion(AdvancementEntry entry, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        GrantCriterionCallback.EVENT.invoker().grantCriterion(owner, entry, criterionName, getProgress(entry).isDone());
    }

    @ModifyExpressionValue(method = "method_53637(Lnet/minecraft/advancement/AdvancementEntry;Lnet/minecraft/advancement/AdvancementDisplay;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancement/AdvancementDisplay;shouldAnnounceToChat()Z"))
    public boolean disableVanillaAnnounceAdvancements(boolean original) {
        return false;
    }
}