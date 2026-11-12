package com.example.elementalmagic.client;

import com.example.elementalmagic.MagicMod;
import com.example.elementalmagic.SpellbookItem;
import com.example.elementalmagic.network.SwitchSpellPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = MagicMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        while (ClientKeys.PREV_SPELL.consumeClick()) send(player, -1);
        while (ClientKeys.NEXT_SPELL.consumeClick()) send(player, 1);
    }

    private static void send(LocalPlayer player, int dir) {
        boolean holdingBook = player.getMainHandItem().getItem() instanceof SpellbookItem
                || player.getOffhandItem().getItem() instanceof SpellbookItem;
        if (holdingBook) {
            PacketDistributor.sendToServer(new SwitchSpellPayload(dir));
        }
    }

    private ClientEvents() {}
}
