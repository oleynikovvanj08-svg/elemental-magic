package com.example.elementalmagic.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Каменный алтарь с сундуком свитков. Ставится feature'ом в обычных биомах. */
public class MagicShrineFeature extends Feature<NoneFeatureConfiguration> {

    public MagicShrineFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos base = ctx.origin();

        // Не строим в воде и лаве
        if (level.getBlockState(base.below()).liquid()) return false;

        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_COBBLESTONE.defaultBlockState();

        // Пол 5x5, на одну клетку ниже точки (чтобы стоял на земле)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(base.offset(dx, -1, dz),
                        random.nextInt(3) == 0 ? mossy : cobble, 2);
            }
        }

        // Стены высотой 3, ставим только в воздух
        for (int dy = 0; dy <= 2; dy++) {
            for (int i = -2; i <= 2; i++) {
                wall(level, base, -2, dy, i, random, cobble, mossy);
                wall(level, base, 2, dy, i, random, cobble, mossy);
                wall(level, base, i, dy, -2, random, cobble, mossy);
                wall(level, base, i, dy, 2, random, cobble, mossy);
            }
        }

        // Проход с южной стороны
        level.removeBlock(base.offset(0, 0, 2), false);
        level.removeBlock(base.offset(0, 1, 2), false);

        // Крыша 5x5
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockState roof = random.nextInt(4) == 0
                        ? Blocks.STONE_BRICKS.defaultBlockState() : cobble;
                level.setBlock(base.offset(dx, 3, dz), roof, 2);
            }
        }

        // Фонарь в углу внутри
        level.setBlock(base.offset(1, 0, -1), Blocks.LANTERN.defaultBlockState(), 2);

        // Сундук со свитками
        BlockState chest = Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
        level.setBlock(base, chest, 2);
        if (level.getBlockEntity(base) instanceof BaseContainerBlockEntity container) {
            container.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath("elementalmagic", "chests/magic_shrine")),
                    random.nextLong());
        }
        return true;
    }

    private static void wall(WorldGenLevel level, BlockPos base, int x, int y, int z,
                             RandomSource r, BlockState cobble, BlockState mossy) {
        BlockPos p = base.offset(x, y, z);
        if (level.getBlockState(p).isAir()) {
            level.setBlock(p, r.nextInt(4) == 0 ? mossy : cobble, 2);
        }
    }
}
