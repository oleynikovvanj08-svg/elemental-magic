package com.example.elementalmagic.client;

import com.example.elementalmagic.MagicMod;
import com.example.elementalmagic.SpellbookItem;
import com.example.elementalmagic.StaffItem;
import com.example.elementalmagic.network.SwitchSpellPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
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
        while (ClientKeys.OPEN_BOOK.consumeClick()) openBook(mc, player);
    }

    private static InteractionHand carrierHand(LocalPlayer player) {
        Item main = player.getMainHandItem().getItem();
        Item off = player.getOffhandItem().getItem();
        if (main instanceof SpellbookItem || main instanceof StaffItem) return InteractionHand.MAIN_HAND;
        if (off instanceof SpellbookItem || off instanceof StaffItem) return InteractionHand.OFF_HAND;
        return null;
    }

    private static void send(LocalPlayer player, int dir) {
        if (carrierHand(player) != null) {
            PacketDistributor.sendToServer(new SwitchSpellPayload(dir));
        }
    }

    private static void openBook(Minecraft mc, LocalPlayer player) {
        InteractionHand hand = carrierHand(player);
        if (hand != null) mc.setScreen(new SpellbookScreen(hand));
    }

    private ClientEvents() {}
}
