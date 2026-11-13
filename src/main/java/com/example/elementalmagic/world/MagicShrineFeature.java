package com.example.elementalmagic.world;

import com.example.elementalmagic.ModRegistry;
import com.example.elementalmagic.ScrollItem;
import com.example.elementalmagic.magic.Element;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredItem;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/** Каменный алтарь с сундуком свитков. Ставится feature'ом в обычных биомах. */
public class MagicShrineFeature extends Feature<NoneFeatureConfiguration> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public MagicShrineFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        try {
            return placeSafe(ctx);
        } catch (Exception e) {
            LOGGER.error("elementalmagic: не удалось построить алтарь на {} — пропускаю", ctx.origin(), e);
            return false;
        }
    }

    private boolean placeSafe(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos base = ctx.origin();

        // Не строим в воде и лаве
        if (level.getBlockState(base.below()).liquid()) return false;

        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_COBBLESTONE.defaultBlockState();

        // Пол 5x5
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(base.offset(dx, -1, dz),
                        random.nextInt(3) == 0 ? mossy : cobble, 2);
            }
        }

        // Стены высотой 3, только в воздух
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

        // Крыша
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockState roof = random.nextInt(4) == 0
                        ? Blocks.STONE_BRICKS.defaultBlockState() : cobble;
                level.setBlock(base.offset(dx, 3, dz), roof, 2);
            }
        }

        level.setBlock(base.offset(1, 0, -1), Blocks.LANTERN.defaultBlockState(), 2);

        // Сундук: мана + 2-4 случайных свитка обычных школ
        BlockState chest = Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
        level.setBlock(base, chest, 2);
        if (level.getBlockEntity(base) instanceof Container container) {
            List<DeferredItem<ScrollItem>> pool = new ArrayList<>();
            for (Element e : Element.values()) {
                if (e.bossOnly) continue;
                pool.addAll(ModRegistry.SCROLLS.get(e));
            }
            container.setItem(0, new ItemStack(ModRegistry.MANA_ITEM.get(), 2 + random.nextInt(5)));
            int scrolls = Math.min(2 + random.nextInt(3), container.getContainerSize() - 1);
            for (int i = 1; i <= scrolls; i++) {
                container.setItem(i, new ItemStack(pool.get(random.nextInt(pool.size())).get()));
            }
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
