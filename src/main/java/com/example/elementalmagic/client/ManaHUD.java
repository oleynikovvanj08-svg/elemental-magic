package com.example.elementalmagic.client;

import com.example.elementalmagic.Mana;
import com.example.elementalmagic.MagicMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = MagicMod.MODID, value = Dist.CLIENT)
public final class ManaHUD {

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, "mana"),
                new ManaLayer()
        );
    }

    private static class ManaLayer implements LayeredDraw.Layer {
        private static final int BAR_WIDTH = 100;
        private static final int BAR_HEIGHT = 10;
        private static final int MARGIN = 5;

        @Override
        public void render(GuiGraphics g, DeltaTracker delta) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null || mc.options.hideGui) return;

            int screenW = mc.getWindow().getGuiScaledWidth();
            int screenH = mc.getWindow().getGuiScaledHeight();

            int x = MARGIN;
            int y = screenH - 60;

            int current = Mana.get(player);
            int max = Mana.max(player);
            float fill = max > 0 ? (float) current / max : 0f;

            g.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xAA000000);
            int fillW = (int) (BAR_WIDTH * fill);
            g.fill(x, y, x + fillW, y + BAR_HEIGHT, 0xFF3FA9F5);
            g.renderOutline(x, y, BAR_WIDTH, BAR_HEIGHT, 0xFF000000);

            String text = current + " / " + max;
            int textX = x + BAR_WIDTH / 2 - mc.font.width(text) / 2;
            int textY = y + 1;
            g.drawString(mc.font, text, textX, textY, 0xFFFFFFFF, true);
        }
    }

    private ManaHUD() {}
}
