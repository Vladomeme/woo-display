package net.woo.main;

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
        HudRenderCallback.EVENT.register((context, delta) -> {
            if (WOOConfig.INSTANCE.enabled) SituationalDisplay.render(context);
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> SituationalDisplay.init(client));
    }
}
