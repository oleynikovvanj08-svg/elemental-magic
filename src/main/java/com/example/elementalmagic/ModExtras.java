package com.example.elementalmagic;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Дополнительные предметы, которые не вписываются в общий цикл по стихиям.
 * Пока пусто, так как Меч поглощения перенесён в ModRegistry.
 */
@EventBusSubscriber(modid = MagicMod.MODID)
public final class ModExtras {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MagicMod.MODID);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    @SubscribeEvent
    public static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        // Здесь можно будет добавлять будущие предметы
    }

    private ModExtras() {}
}
