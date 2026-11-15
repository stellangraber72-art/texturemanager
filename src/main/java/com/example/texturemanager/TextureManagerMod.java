package com.example.texturemanager;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextureManagerMod implements ClientModInitializer {
    public static final String MOD_ID = "texturemanager";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private static ModConfig config = new ModConfig();
    private static KeyMapping openKey;

    public static ModConfig config() { return config; }

    @Override
    public void onInitializeClient() {
        config = ModConfig.load();
        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.texturemanager.open", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_CONTROL, KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new TextureManagerScreen(null));
            }
        });
    }
}
