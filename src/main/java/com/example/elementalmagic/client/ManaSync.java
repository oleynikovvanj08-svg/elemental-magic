package com.example.elementalmagic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = MagicMod.MODID)
public final class ManaSync {
    private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
                ManaPayload.TYPE,
                ManaPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    Player p = ctx.player();
                    // напрямую в data, без Mana.set, чтобы не зависеть от клиентского max()
                    p.setData(ModRegistry.MANA, payload.mana());
                })
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        int now = Mana.get(sp);
        Integer last = LAST_SENT.get(sp.getUUID());
        if (last == null || last != now) {
            LAST_SENT.put(sp.getUUID(), now);
            PacketDistributor.sendToPlayer(sp, new ManaPayload(now));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    private ManaSync() {}
}
