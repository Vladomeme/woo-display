package net.woo.main.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.woo.main.SituationalDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	@Inject(method = "onDamaged", at = @At(value = "HEAD"))
	private void onDamaged(DamageSource source, CallbackInfo ci) {
		if (((LivingEntity) (Object) this) instanceof ClientPlayerEntity)
			SituationalDisplay.onDamage(source);
	}
}
