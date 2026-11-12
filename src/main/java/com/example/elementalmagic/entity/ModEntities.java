package com.example.elementalmagic.entity;

import com.example.elementalmagic.MagicMod;
import com.example.elementalmagic.magic.Element;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = MagicMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MagicMod.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MagicMod.MODID);

    /** Моб, материал с него и яйцо призыва. id моба вида fire_golem. */
    public record MobEntry(Element element, ElementalMob.Kind kind, String id,
                           DeferredHolder<EntityType<?>, EntityType<ElementalMob>> type,
                           DeferredItem<Item> material,
                           DeferredItem<DeferredSpawnEggItem> egg) {}

    public static final List<MobEntry> ALL = new ArrayList<>();

    static {
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

                DeferredItem<Item> material = ITEMS.registerSimpleItem(e.id + "_" + k.material,
                        new Item.Properties().rarity(e.bossOnly ? Rarity.RARE : Rarity.UNCOMMON));

                DeferredItem<DeferredSpawnEggItem> egg = ITEMS.register(id + "_spawn_egg",
                        () -> new DeferredSpawnEggItem(type, e.color, k.eggColor, new Item.Properties()));

                ALL.add(new MobEntry(e, k, id, type, material, egg));
            }
        }
    }

    private static boolean fireImmune(Element e) {
        return e == Element.FIRE || e == Element.ASH || e == Element.EXPLOSION;
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        for (MobEntry m : ALL) {
            event.put(m.type().get(), ElementalMob.createAttributes(m.element(), m.kind()).build());
        }
    }

    /** Спавн как у обычных монстров: на земле, в темноте. Биомы задаются в biome_modifier. */
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
    }

    private ModEntities() {}
}
