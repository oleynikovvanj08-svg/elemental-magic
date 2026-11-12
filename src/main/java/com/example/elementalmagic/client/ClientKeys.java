package com.example.elementalmagic.client;

import com.example.elementalmagic.MagicMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MagicMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientKeys {
    public static final String CATEGORY = "key.categories.elementalmagic";

    public static final KeyMapping PREV_SPELL = new KeyMapping(
            "key.elementalmagic.prev_spell", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);

    public static final KeyMapping NEXT_SPELL = new KeyMapping(
            "key.elementalmagic.next_spell", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);

    public static final KeyMapping OPEN_BOOK = new KeyMapping(
            "key.elementalmagic.open_book", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(PREV_SPELL);
        event.register(NEXT_SPELL);
        event.register(OPEN_BOOK);
    }

    private ClientKeys() {}
}
