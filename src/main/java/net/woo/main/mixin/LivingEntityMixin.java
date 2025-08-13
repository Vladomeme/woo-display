package net.woo.main.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.world.World;
import net.woo.main.SituationalDisplay;
import net.woo.main.config.WOOConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Inject(method = "onDamaged", at = @At(value = "HEAD"))
	private void onDamaged(DamageSource source, CallbackInfo ci) {
		if (WOOConfig.INSTANCE.enabled) {
			if (((LivingEntity) (Object) this) instanceof ClientPlayerEntity) SituationalDisplay.onDamage(source);

			Entity attacker = source.getSource();
			if (attacker != null && attacker.getId() == Objects.requireNonNull(MinecraftClient.getInstance().player).getId())
				SituationalDisplay.updateReflexes();
		}
	}

	@Inject(method = "onDeath", at = @At(value = "HEAD"))
	private void onDeath(DamageSource source, CallbackInfo ci) {
		if (WOOConfig.INSTANCE.enabled) {
			Entity attacker = source.getSource();
			if (attacker == null) return;
			if (!attacker.equals(MinecraftClient.getInstance().player)) return;

			Text name = getCustomName();
			if (name == null) return;
			TextColor colour = name.getStyle().getColor();
			if (colour == null) return;

			if (colour.getRgb() == 16755200) SituationalDisplay.updateCloakedOnKill();
		}
	}

	public LivingEntityMixin(EntityType<?> type, World world) {
		super(type, world);
	}
}
