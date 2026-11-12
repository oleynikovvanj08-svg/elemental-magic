package com.example.elementalmagic.client;

import com.example.elementalmagic.MagicMod;
import com.example.elementalmagic.entity.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MagicMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientMobRenderers {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (ModEntities.MobEntry m : ModEntities.ALL) {
            String id = m.id();
            float scale = m.kind().scale;
            event.registerEntityRenderer(m.type().get(), ctx -> new ElementalMobRenderer(ctx, id, scale));
        }
    }

    private ClientMobRenderers() {}
}
