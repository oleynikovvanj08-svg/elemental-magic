package com.example.elementalmagic.worldgen;

import com.example.elementalmagic.boss.ModBosses;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Тёмная пирамида 27x27: ступенчатая, из глубинного сланца. Вход по туннелю с одной стороны,
 * внутри зал 9x9 с душевыми фонарями и пятнами скалка. Босс и два сундука внутри.
 */
public class ShadowPyramidPiece extends ArenaPiece {
    private static final int W = 27;
    private static final int H = 14;
    private static final int D = 27;
    private static final int C = 13;

    public ShadowPyramidPiece(RandomSource random, int x, int z) {
        super(ModStructures.SHADOW_PYRAMID_PIECE.get(), x, z, W, H, D, random);
    }

    public ShadowPyramidPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.SHADOW_PYRAMID_PIECE.get(), tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        if (!updateAverageGroundHeight(level, box, 0)) return;

        BlockState bricks = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState cracked = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        BlockState blackstone = Blocks.BLACKSTONE.defaultBlockState();
        BlockState polished = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState sculk = Blocks.SCULK.defaultBlockState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState soulLantern = Blocks.SOUL_LANTERN.defaultBlockState();

        // 1. Пол и фундамент
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                placeBlock(level, pick(random, bricks, cracked, blackstone), x, 0, z, box);
                fillColumnDown(level, bricks, x, -1, z, box);
            }
        }

        // 2. Расчистка
        generateAirBox(level, box, 0, 1, 0, W - 1, H - 1, D - 1);

        // 3. Ступенчатая пирамида: слой y занимает квадрат от y до 26 - y
        for (int y = 1; y < H; y++) {
            int lo = y;
            int hi = (W - 1) - y;
            if (lo > hi) break;
            generateBox(level, box, lo, y, lo, hi, y, hi, bricks, bricks, false);
            if (y % 3 == 0) {
                placeBlock(level, crying, lo, y, lo, box);
                placeBlock(level, crying, hi, y, lo, box);
                placeBlock(level, crying, lo, y, hi, box);
                placeBlock(level, crying, hi, y, hi, box);
            }
        }

        // 4. Зал внутри (9x9, высота 7) и туннель-вход 3x4 с южной стороны
        generateAirBox(level, box, C - 4, 1, C - 4, C + 4, 7, C + 4);
        generateAirBox(level, box, C - 1, 1, 0, C + 1, 4, C - 4);

        // 5. Пол зала из чёрного камня с пятнами скалка
        for (int x = C - 4; x <= C + 4; x++) {
            for (int z = C - 4; z <= C + 4; z++) {
                placeBlock(level, random.nextInt(10) < 3 ? sculk : polished, x, 0, z, box);
            }
        }

        // 6. Душевые фонари по углам зала
        for (int[] c : new int[][]{{C - 4, C - 4}, {C + 4, C - 4}, {C - 4, C + 4}, {C + 4, C + 4}}) {
            placeBlock(level, soulLantern, c[0], 1, c[1], box);
        }

        // 7. Сундуки у дальней стены и босс в центре зала
        chest(level, box, random, C - 2, 1, C + 4, Direction.NORTH);
        chest(level, box, random, C + 2, 1, C + 4, Direction.NORTH);
        spawnBoss(level, box, ModBosses.SHADOW_MASTER.get(), C, 1, C - 1);
    }
}
