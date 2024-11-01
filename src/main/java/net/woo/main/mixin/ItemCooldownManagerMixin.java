package net.woo.main.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.woo.main.SituationalDisplay;
import net.woo.main.config.WOOConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemCooldownManager.class)
public abstract class ItemCooldownManagerMixin {

	@SuppressWarnings({"ConstantValue", "RedundantCast"})
	@Inject(method = "set", at = @At(value = "HEAD"))
	private void set(Item item, int duration, CallbackInfo ci) {
		if (!WOOConfig.INSTANCE.enabled) return;

		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;
		if (item == Items.SHIELD
				&& ((ItemCooldownManager) (Object) this).equals(player.getItemCooldownManager())) SituationalDisplay.updateShielding();
	}
}
