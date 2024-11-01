package net.woo.main.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.woo.main.SituationalDisplay;
import net.woo.main.config.WOOConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

	@Inject(method = "handleStatus", at = @At(value = "HEAD"))
	private void handleStatus(byte status, CallbackInfo ci) {
		if (WOOConfig.INSTANCE.enabled && status == 29) SituationalDisplay.updateGuard();
	}
}
