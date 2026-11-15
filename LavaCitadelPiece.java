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
 * Вулканическая цитадель 29x29: стены из чёрного камня, внутри арена. В центре платформа босса,
 * вокруг неё кольцо лавы, к платформе ведут четыре моста. Лава лежит вровень с полом
 * в каменных берегах, поэтому не растекается.
 */
public class LavaCitadelPiece extends ArenaPiece {
    private static final int W = 29;
    private static final int H = 14;
    private static final int D = 29;
    private static final int C = 14;

    public LavaCitadelPiece(RandomSource random, int x, int z) {
        super(ModStructures.LAVA_CITADEL_PIECE.get(), x, z, W, H, D, random);
    }

    public LavaCitadelPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.LAVA_CITADEL_PIECE.get(), tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        if (!updateAverageGroundHeight(level, box, 0)) return;

        BlockState stone = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState cracked = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState plain = Blocks.BLACKSTONE.defaultBlockState();
        BlockState magma = Blocks.MAGMA_BLOCK.defaultBlockState();
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState lava = Blocks.LAVA.defaultBlockState();
        BlockState lantern = Blocks.LANTERN.defaultBlockState();

        // 1. Пол и фундамент
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                placeBlock(level, pick(random, stone, cracked, plain), x, 0, z, box);
                fillColumnDown(level, plain, x, -1, z, box);
            }
        }

        // 2. Расчистка
        generateAirBox(level, box, 0, 1, 0, W - 1, H - 1, D - 1);

        // 3. Стены высотой 9, зубцы и магма на углах
        generateBox(level, box, 0, 1, 0, W - 1, 9, 0, stone, stone, false);
        generateBox(level, box, 0, 1, D - 1, W - 1, 9, D - 1, stone, stone, false);
        generateBox(level, box, 0, 1, 0, 0, 9, D - 1, stone, stone, false);
        generateBox(level, box, W - 1, 1, 0, W - 1, 9, D - 1, stone, stone, false);
        for (int i = 0; i < W; i += 2) {
            placeBlock(level, stone, i, 10, 0, box);
            placeBlock(level, stone, i, 10, D - 1, box);
            placeBlock(level, stone, 0, 10, i, box);
            placeBlock(level, stone, W - 1, 10, i, box);
        }
        for (int[] corner : new int[][]{{0, 0}, {W - 1, 0}, {0, D - 1}, {W - 1, D - 1}}) {
            for (int y = 1; y <= 11; y++) placeBlock(level, magma, corner[0], y, corner[1], box);
        }

        // 4. Ворота: проём 5x6 в стене z = 0
        generateAirBox(level, box, 12, 1, 0, 16, 6, 0);

        // 5. Центр: платформа босса, кольцо лавы и четыре моста
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                int dx = x - C;
                int dz = z - C;
                double d = Math.hypot(dx, dz);
                boolean bridge = Math.abs(dx) <= 1 || Math.abs(dz) <= 1;
                if (d <= 3.5) {
                    placeBlock(level, obsidian, x, 1, z, box);
                } else if (d <= 7.5 && !bridge) {
                    placeBlock(level, lava, x, 0, z, box);
                }
            }
        }

        // 6. Жаровни по углам арены
        for (int[] c : new int[][]{{3, 3}, {25, 3}, {3, 25}, {25, 25}}) {
            placeBlock(level, magma, c[0], 1, c[1], box);
            placeBlock(level, lantern, c[0], 2, c[1], box);
        }

        // 7. Сундуки у боковых стен и босс на платформе
        chest(level, box, random, 2, 1, C, Direction.EAST);
        chest(level, box, random, 26, 1, C, Direction.WEST);
        spawnBoss(level, box, ModBosses.MAGMA_LORD.get(), C, 2, C);
    }
}
