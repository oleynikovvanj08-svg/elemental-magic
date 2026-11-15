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

    /** Владыка магмы не горит ни в огне, ни в лаве. */
    public static final DeferredHolder<EntityType<?>, EntityType<MagmaLordEntity>> MAGMA_LORD =
            ENTITIES.register("magma_lord", () -> EntityType.Builder
                    .of(MagmaLordEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 3.0f)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("magma_lord"));

    public static final DeferredHolder<EntityType<?>, EntityType<StarSentinelEntity>> STAR_SENTINEL =
            ENTITIES.register("star_sentinel", () -> EntityType.Builder
                    .of(StarSentinelEntity::new, MobCategory.MONSTER)
                    .sized(0.9f, 2.9f)
                    .clientTrackingRange(10)
                    .build("star_sentinel"));

    public static final DeferredHolder<EntityType<?>, EntityType<BlightLordEntity>> BLIGHT_LORD =
            ENTITIES.register("blight_lord", () -> EntityType.Builder
                    .of(BlightLordEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 3.0f)
                    .clientTrackingRange(10)
                    .build("blight_lord"));

    public static final DeferredHolder<EntityType<?>, EntityType<ShadowMasterEntity>> SHADOW_MASTER =
            ENTITIES.register("shadow_master", () -> EntityType.Builder
                    .of(ShadowMasterEntity::new, MobCategory.MONSTER)
                    .sized(0.8f, 2.7f)
                    .clientTrackingRange(10)
                    .build("shadow_master"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TIME_KEEPER.get(), TimeKeeperEntity.createAttributes().build());
        event.put(MAGMA_LORD.get(), MagmaLordEntity.createAttributes().build());
        event.put(STAR_SENTINEL.get(), StarSentinelEntity.createAttributes().build());
        event.put(BLIGHT_LORD.get(), BlightLordEntity.createAttributes().build());
        event.put(SHADOW_MASTER.get(), ShadowMasterEntity.createAttributes().build());
    }

    private ModBosses() {}
}
