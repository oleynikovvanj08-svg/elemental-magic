package com.example.elementalmagic.boss.client;

import com.example.elementalmagic.MagicMod;
import com.example.elementalmagic.boss.ModBosses;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Привязка отрисовщиков боссов. Работает только на клиенте. */
@EventBusSubscriber(modid = MagicMod.MODID, value = Dist.CLIENT)
public final class ClientBossRenderers {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModBosses.TIME_KEEPER.get(), TimeKeeperRenderer::new);
    }

    private ClientBossRenderers() {}
}
