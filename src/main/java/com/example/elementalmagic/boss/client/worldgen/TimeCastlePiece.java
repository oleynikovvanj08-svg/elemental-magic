package com.example.elementalmagic.worldgen;

import com.example.elementalmagic.boss.ModBosses;
import com.example.elementalmagic.boss.TimeKeeperEntity;
import com.example.elementalmagic.magic.Element;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.Arrays;

/**
 * Замок 25x25: стены с зубцами, ворота с одной стороны, четыре угловые башни
 * (в каждой сундук), во дворе «часы» на полу и Хранитель времени в центре.
 * Постройка симметрична, поэтому случайный поворот ей не мешает.
 */
public class TimeCastlePiece extends ScatteredFeaturePiece {
    private static final int W = 25;
    private static final int D = 25;
    private static final int H = 15;
    private static final int CENTER = 12;

    /** Сундуки башен: стихии только обычные, боссовые вещи падают с босса. */
    private static final Element[] CHEST_ELEMENTS = Arrays.stream(Element.values())
            .filter(e -> !e.bossOnly)
            .toArray(Element[]::new);

    public TimeCastlePiece(RandomSource random, int x, int z) {
        super(ModStructures.TIME_CASTLE_PIECE.get(), x, 64, z, W, H, D,
                Direction.Plane.HORIZONTAL.getRandomDirection(random));
    }

    public TimeCastlePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.TIME_CASTLE_PIECE.get(), tag);
    }

    /** Стены и пол: в основном целые камни, немного треснувших и замшелых. */
    private static final class WallSelector extends StructurePiece.BlockSelector {
        @Override
        public void next(RandomSource random, int x, int y, int z, boolean isEdge) {
            float r = random.nextFloat();
            if (r < 0.70f) this.next = Blocks.STONE_BRICKS.defaultBlockState();
            else if (r < 0.85f) this.next = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            else this.next = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        }
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        if (!updateAverageGroundHeight(level, box, 0)) return;

        BlockState brick = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState ring = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
        BlockState mark = Blocks.QUARTZ_BLOCK.defaultBlockState();
        BlockState glow = Blocks.GLOWSTONE.defaultBlockState();
        BlockState lantern = Blocks.LANTERN.defaultBlockState();
        WallSelector walls = new WallSelector();

        // 1. Пол на весь замок и фундамент вниз до земли
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                float r = random.nextFloat();
                BlockState floor = r < 0.6f ? brick
                        : (r < 0.8f ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                        : Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
                placeBlock(level, floor, x, 0, z, box);
                fillColumnDown(level, brick, x, -1, z, box);
            }
        }

        // 2. Расчищаем всё над полом
        generateAirBox(level, box, 0, 1, 0, W - 1, H - 1, D - 1);

        // 3. Внешние стены высотой 7 и зубцы поверху
        generateBox(level, box, 0, 1, 0, W - 1, 7, 0, false, random, walls);
        generateBox(level, box, 0, 1, D - 1, W - 1, 7, D - 1, false, random, walls);
        generateBox(level, box, 0, 1, 0, 0, 7, D - 1, false, random, walls);
        generateBox(level, box, W - 1, 1, 0, W - 1, 7, D - 1, false, random, walls);
        for (int i = 0; i < W; i += 2) {
            placeBlock(level, brick, i, 8, 0, box);
            placeBlock(level, brick, i, 8, D - 1, box);
            placeBlock(level, brick, 0, 8, i, box);
            placeBlock(level, brick, W - 1, 8, i, box);
        }

        // 4. Ворота в стене z = 0: проём 5 блоков шириной и 5 высотой
        generateAirBox(level, box, 10, 1, 0, 14, 5, 0);

        // 5. Четыре угловые башни: {x0, z0, x центра, z центра, z двери, направление сундука}
        int[][] towers = {
                {0, 0, 3, 3, 6, 0},
                {18, 0, 21, 3, 6, 0},
                {0, 18, 3, 21, 18, 1},
                {18, 18, 21, 21, 18, 1}
        };
        for (int[] t : towers) {
            int x0 = t[0], z0 = t[1], cx = t[2], cz = t[3], doorZ = t[4];
            // Пустая башня: стены, потолок на y = 12, внутри воздух
            generateBox(level, box, x0, 0, z0, x0 + 6, 12, z0 + 6, brick, air, false);
            // Крыша и зубцы
            generateBox(level, box, x0, 13, z0, x0 + 6, 13, z0 + 6, brick, brick, false);
            for (int i = 0; i <= 6; i += 2) {
                placeBlock(level, brick, x0 + i, 14, z0, box);
                placeBlock(level, brick, x0 + i, 14, z0 + 6, box);
                placeBlock(level, brick, x0, 14, z0 + i, box);
                placeBlock(level, brick, x0 + 6, 14, z0 + i, box);
            }
            // Дверь во двор
            generateAirBox(level, box, cx, 1, doorZ, cx, 2, doorZ);
            // Сундук лицом к двери и фонарь рядом
            Direction facing = t[5] == 0 ? Direction.SOUTH : Direction.NORTH;
            placeBlock(level, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing),
                    cx, 1, cz, box);
            placeBlock(level, lantern, cx + 1, 1, cz, box);

            BlockPos chestPos = new BlockPos(getWorldX(cx, cz), getWorldY(1), getWorldZ(cx, cz));
            if (box.isInside(chestPos) && level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                Element e = CHEST_ELEMENTS[random.nextInt(CHEST_ELEMENTS.length)];
                ShrineLoot.fill(chest, e, random);
            }
        }

        // 6. Колонны во дворе (укрытия в бою) с фонарями
        for (int px : new int[]{9, 15}) {
            for (int pz : new int[]{9, 15}) {
                for (int y = 1; y <= 5; y++) placeBlock(level, brick, px, y, pz, box);
                placeBlock(level, lantern, px, 6, pz, box);
            }
        }

        // 7. «Часы» на полу двора: светящийся центр, кольцо и 12 меток
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                double d = Math.hypot(x - CENTER, z - CENTER);
                if (d <= 2.5) placeBlock(level, glow, x, 0, z, box);
                else if (Math.abs(d - 7) < 0.6) placeBlock(level, ring, x, 0, z, box);
            }
        }
        for (int k = 0; k < 12; k++) {
            double a = Math.toRadians(k * 30);
            placeBlock(level, mark, (int) Math.round(CENTER + Math.cos(a) * 7), 0,
                    (int) Math.round(CENTER + Math.sin(a) * 7), box);
        }

        // 8. Босс в центре двора. Спавнится один раз: в том чанке, где лежит центр.
        BlockPos bossPos = new BlockPos(getWorldX(CENTER, CENTER), getWorldY(1), getWorldZ(CENTER, CENTER));
        if (box.isInside(bossPos)) {
            TimeKeeperEntity boss = ModBosses.TIME_KEEPER.get().create(level.getLevel());
            if (boss != null) {
                boss.moveTo(bossPos.getX() + 0.5, bossPos.getY(), bossPos.getZ() + 0.5, 0f, 0f);
                boss.finalizeSpawn(level, level.getCurrentDifficultyAt(bossPos), MobSpawnType.STRUCTURE, null);
                boss.setPersistenceRequired();
                level.addFreshEntityWithPassengers(boss);
            }
        }
    }
}
