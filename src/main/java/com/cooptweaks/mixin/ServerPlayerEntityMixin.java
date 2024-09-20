package com.cooptweaks.mixin;

import com.cooptweaks.events.PlayerDeathCallback;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.GlobalPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
	@Inject(method = "onDeath", at = @At(value = "RETURN"))
	public void onDeath(DamageSource damageSource, CallbackInfo ci) {
		ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
		PlayerEntityAccessor playerEntityAccessor = (PlayerEntityAccessor) this;
		Optional<GlobalPos> lastDeathPos = playerEntityAccessor.getLastDeathPos();
		PlayerDeathCallback.EVENT.invoker().onDeath(player, lastDeathPos.get(), damageSource.getDeathMessage(player));
	}
}
