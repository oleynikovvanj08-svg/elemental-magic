package com.example.elementalmagic.boss.client;

import com.example.elementalmagic.MagicMod;
import com.example.elementalmagic.boss.BlightLordEntity;
import com.example.elementalmagic.boss.MagmaLordEntity;
import com.example.elementalmagic.boss.ModBosses;
import com.example.elementalmagic.boss.ShadowMasterEntity;
import com.example.elementalmagic.boss.StarSentinelEntity;
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
        event.registerEntityRenderer(ModBosses.MAGMA_LORD.get(),
                ctx -> new HumanoidBossRenderer<MagmaLordEntity>(ctx, "magma_lord", 1.5f));
        event.registerEntityRenderer(ModBosses.STAR_SENTINEL.get(),
                ctx -> new HumanoidBossRenderer<StarSentinelEntity>(ctx, "star_sentinel", 1.4f));
        event.registerEntityRenderer(ModBosses.BLIGHT_LORD.get(),
                ctx -> new HumanoidBossRenderer<BlightLordEntity>(ctx, "blight_lord", 1.5f));
        event.registerEntityRenderer(ModBosses.SHADOW_MASTER.get(),
                ctx -> new HumanoidBossRenderer<ShadowMasterEntity>(ctx, "shadow_master", 1.3f));
    }

    private ClientBossRenderers() {}
}
