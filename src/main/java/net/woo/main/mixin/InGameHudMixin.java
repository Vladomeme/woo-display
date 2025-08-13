package net.woo.main.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import net.woo.main.SituationalDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

	@Inject(method = "setOverlayMessage", at = @At(value = "HEAD"))
	private void setOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
		if (message.getString().equals("Guard")) SituationalDisplay.updateGuardOnDamage();
	}
}
