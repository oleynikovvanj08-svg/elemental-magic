package com.example.elementalmagic;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = MagicMod.MODID)
public final class ModExtras {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MagicMod.MODID);

    public static final DeferredItem<AbsorptionSwordItem> ABSORPTION_SWORD = ITEMS.register("absorption_sword",
            () -> new AbsorptionSwordItem(AbsorptionSwordItem.properties()));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    /** Кладём меч в основную вкладку мода. */
    @SubscribeEvent
    public static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> main = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, "main"));
        if (event.getTabKey().equals(main)) {
            event.accept(ABSORPTION_SWORD.get());
        }
    }

    private ModExtras() {}
}
