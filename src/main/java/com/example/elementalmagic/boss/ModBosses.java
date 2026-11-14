package com.example.elementalmagic.boss;

import com.example.elementalmagic.MagicMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Регистрация боссов. Вызови ModBosses.register(modBus) в конструкторе MagicMod. */
@EventBusSubscriber(modid = MagicMod.MODID)
public final class ModBosses {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MagicMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<TimeKeeperEntity>> TIME_KEEPER =
            ENTITIES.register("time_keeper", () -> EntityType.Builder
                    .of(TimeKeeperEntity::new, MobCategory.MONSTER)
                    .sized(0.9f, 2.9f)
                    .clientTrackingRange(10)
                    .build("time_keeper"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TIME_KEEPER.get(), TimeKeeperEntity.createAttributes().build());
    }

    private ModBosses() {}
}
