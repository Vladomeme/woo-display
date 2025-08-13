package net.woo.main;

import ch.njol.minecraft.uiframework.hud.Hud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.woo.main.config.WOOConfig;

public class WOO implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (WOOConfig.INSTANCE.enabled) SituationalDisplay.tick();
        });
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            if (WOOConfig.INSTANCE.enabled && SituationalDisplay.shouldRender())
                SituationalDisplay.INSTANCE.renderAbsolute(context, tickDelta);
        });
        Hud.INSTANCE.addElement(SituationalDisplay.INSTANCE);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> SituationalDisplay.init());
    }
}
