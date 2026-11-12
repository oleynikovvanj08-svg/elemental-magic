package com.example.elementalmagic.network;

import com.example.elementalmagic.MagicMod;
import com.example.elementalmagic.SpellbookItem;
import com.example.elementalmagic.StaffItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = MagicMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SwitchSpellPayload.TYPE, SwitchSpellPayload.STREAM_CODEC, ModNetwork::handleSwitch);
    }

    private static boolean isCarrier(ItemStack stack) {
        return stack.getItem() instanceof SpellbookItem || stack.getItem() instanceof StaffItem;
    }

    private static void handleSwitch(SwitchSpellPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            int dir = msg.direction() < 0 ? -1 : 1;

            // Сначала основная рука, потом вторая, как на клиенте
            ItemStack stack = player.getMainHandItem();
            if (!isCarrier(stack)) stack = player.getOffhandItem();

            if (stack.getItem() instanceof SpellbookItem book) {
                book.cycle(player, stack, dir);
            } else if (stack.getItem() instanceof StaffItem staff) {
                staff.cycle(player, stack, dir);
            }
        });
    }

    private ModNetwork() {}
}
