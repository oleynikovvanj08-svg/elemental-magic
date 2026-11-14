package com.example.elementalmagic.entity;

import com.example.elementalmagic.MagicMod;
import com.example.elementalmagic.boss.TimeKeeperEntity;
import com.example.elementalmagic.magic.Element;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@EventBusSubscriber(modid = MagicMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MagicMod.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MagicMod.MODID);
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MagicMod.MODID);

    /** Моб, материал с него и яйцо призыва. id моба вида fire_golem. */
    public record MobEntry(Element element, ElementalMob.Kind kind, String id,
                           DeferredHolder<EntityType<?>, EntityType<ElementalMob>> type,
                           DeferredItem<MaterialItem> material,
                           DeferredItem<NamedSpawnEgg> egg) {}

    public static final List<MobEntry> ALL = new ArrayList<>();

    // ===== Названия =====
    private static final Map<String, String> ADJ = Map.ofEntries(
            Map.entry("lightning", "Грозовой"), Map.entry("light", "Сияющий"),
            Map.entry("darkness", "Тёмный"), Map.entry("water", "Водный"),
            Map.entry("earth", "Земляной"), Map.entry("air", "Воздушный"),
            Map.entry("fire", "Огненный"), Map.entry("nature", "Лесной"),
            Map.entry("ash", "Пепельный"), Map.entry("explosion", "Взрывной"),
            Map.entry("elemental", "Стихийный"), Map.entry("time", "Временной"),
            Map.entry("stars", "Звёздный"), Map.entry("dark_nature", "Гнилостный"),
            Map.entry("shadow", "Теневой"));

    public static String mobName(Element e, ElementalMob.Kind k) {
        String kind = switch (k) {
            case GOLEM -> "голем";
            case MAGE -> "маг";
            case WISP -> "дух";
        };
        return ADJ.getOrDefault(e.id, e.id) + " " + kind;
    }

    public static String materialName(Element e, ElementalMob.Kind k) {
        String mat = switch (k) {
            case GOLEM -> "осколок";
            case MAGE -> "кристалл";
            case WISP -> "сгусток";
        };
        return ADJ.getOrDefault(e.id, e.id) + " " + mat;
    }

    /** Босс «Хранитель времени» — единственная сущность вне цикла стихий. */
    public static final DeferredHolder<EntityType<?>, EntityType<TimeKeeperEntity>> TIME_KEEPER =
            ENTITIES.register("time_keeper", () -> EntityType.Builder
                    .of(TimeKeeperEntity::new, MobCategory.MONSTER)
                    .sized(0.8f, 2.2f)
                    .clientTrackingRange(10)
                    .fireImmune()
                    .build("time_keeper"));

    static {
        BIOME_MODIFIERS.register("elemental_spawns", () -> ElementalSpawns.CODEC);

        for (Element e : Element.values()) {
            for (ElementalMob.Kind k : ElementalMob.Kind.values()) {
                String id = e.id + "_" + k.id;

                DeferredHolder<EntityType<?>, EntityType<ElementalMob>> type = ENTITIES.register(id, () -> {
                    EntityType.Builder<ElementalMob> builder = EntityType.Builder.<ElementalMob>of(
                                    (t, level) -> new ElementalMob(t, level, e, k), MobCategory.MONSTER)
                            .sized(k.width, k.height)
                            .clientTrackingRange(8);
                    if (fireImmune(e)) builder = builder.fireImmune();
                    return builder.build(id);
                });

                String matName = materialName(e, k);
                DeferredItem<MaterialItem> material = ITEMS.register(e.id + "_" + k.material,
                        () -> new MaterialItem(matName, new Item.Properties()
                                .rarity(e.bossOnly ? Rarity.RARE : Rarity.UNCOMMON)));

                String eggName = "Яйцо призыва: " + mobName(e, k);
                DeferredItem<NamedSpawnEgg> egg = ITEMS.register(id + "_spawn_egg",
                        () -> new NamedSpawnEgg(type, e.color, k.eggColor, eggName, new Item.Properties()));

                ALL.add(new MobEntry(e, k, id, type, material, egg));
            }
        }
    }

    /** Материал, который падает с моба этой стихии и вида. */
    public static Item material(Element e, ElementalMob.Kind k) {
        for (MobEntry m : ALL) {
            if (m.element() == e && m.kind() == k) return m.material().get();
        }
        return null;
    }

    private static boolean fireImmune(Element e) {
        return e == Element.FIRE || e == Element.ASH || e == Element.EXPLOSION;
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        for (MobEntry m : ALL) {
            event.put(m.type().get(), ElementalMob.createAttributes(m.element(), m.kind()).build());
        }
        event.put(TIME_KEEPER.get(), TimeKeeperEntity.createAttributes().build());
    }

    /** Спавн как у обычных монстров: на земле, в темноте. Биомы задаются в ElementalSpawns. */
    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        for (MobEntry m : ALL) {
            event.register(m.type().get(), SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        BIOME_MODIFIERS.register(bus);
    }

    /** Материал с русским названием без ru_ru.json. */
    public static final class MaterialItem extends Item {
        private final String fallbackName;

        public MaterialItem(String fallbackName, Properties props) {
            super(props);
            this.fallbackName = fallbackName;
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.translatableWithFallback(getDescriptionId(), fallbackName);
        }
    }

    /** Яйцо призыва с русским названием без ru_ru.json. */
    public static final class NamedSpawnEgg extends DeferredSpawnEggItem {
        private final String fallbackName;

        public NamedSpawnEgg(Supplier<? extends EntityType<? extends Mob>> type, int background, int highlight,
                             String fallbackName, Properties props) {
            super(type, background, highlight, props);
            this.fallbackName = fallbackName;
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.translatableWithFallback(getDescriptionId(), fallbackName);
        }
    }

    private ModEntities() {}
}
