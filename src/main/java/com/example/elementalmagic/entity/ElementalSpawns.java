package com.example.elementalmagic.entity;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/** Добавляет мобов стихий в биомы. Включается файлом biome_modifier/elemental_mobs.json. */
public record ElementalSpawns() implements BiomeModifier {
    public static final MapCodec<ElementalSpawns> CODEC = MapCodec.unit(new ElementalSpawns());

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD) return;
        for (ModEntities.MobEntry m : ModEntities.ALL) {
            if (!spawnsIn(m, biome)) continue;
            int weight = weight(m);
            int max = m.kind() == ElementalMob.Kind.WISP ? 3 : 2;
            builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(m.type().get(), weight, 1, max));
        }
    }

    /** Частота спавна. Для сравнения: у зомби 95. */
    private static int weight(ModEntities.MobEntry m) {
        int w = m.kind() == ElementalMob.Kind.WISP ? 15 : 10;
        // Стихийные мобы спавнятся во всём Верхнем мире, поэтому реже
        return m.element() == com.example.elementalmagic.magic.Element.ELEMENTAL ? Math.max(1, w / 2) : w;
    }

    private static boolean spawnsIn(ModEntities.MobEntry m, Holder<Biome> b) {
        return switch (m.element()) {
            case LIGHTNING -> b.is(BiomeTags.IS_MOUNTAIN);
            case LIGHT -> any(b, Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW);
            case DARKNESS -> any(b, Biomes.DARK_FOREST);
            case WATER -> any(b, Biomes.BEACH, Biomes.RIVER, Biomes.SWAMP);
            case EARTH -> any(b, Biomes.BADLANDS, Biomes.WOODED_BADLANDS, Biomes.ERODED_BADLANDS,
                    Biomes.SAVANNA_PLATEAU);
            case AIR -> any(b, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST,
                    Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_SAVANNA);
            case FIRE -> any(b, Biomes.NETHER_WASTES, Biomes.DESERT);
            case NATURE -> b.is(BiomeTags.IS_FOREST);
            case ASH -> any(b, Biomes.BASALT_DELTAS, Biomes.SOUL_SAND_VALLEY, Biomes.SAVANNA);
            case EXPLOSION -> any(b, Biomes.CRIMSON_FOREST, Biomes.BADLANDS);
            case ELEMENTAL -> b.is(BiomeTags.IS_OVERWORLD);
            case TIME -> any(b, Biomes.DRIPSTONE_CAVES, Biomes.LUSH_CAVES);
            case STARS -> any(b, Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS, Biomes.SMALL_END_ISLANDS,
                    Biomes.END_BARRENS);
            case DARK_NATURE -> any(b, Biomes.MANGROVE_SWAMP, Biomes.WARPED_FOREST);
            case SHADOW -> any(b, Biomes.DEEP_DARK, Biomes.DARK_FOREST);
        };
    }

    @SafeVarargs
    private static boolean any(Holder<Biome> b, ResourceKey<Biome>... keys) {
        for (ResourceKey<Biome> k : keys) {
            if (b.is(k)) return true;
        }
        return false;
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
