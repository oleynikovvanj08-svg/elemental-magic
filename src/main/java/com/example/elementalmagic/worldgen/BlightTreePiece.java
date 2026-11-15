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
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Гниющее древо: огромное тёмное дерево с полым стволом. Вокруг арена из мха и подзола,
 * корни расходятся по земле, наверху густая крона. В стволе вход и два сундука.
 */
public class BlightTreePiece extends ArenaPiece {
    private static final int W = 29;
    private static final int H = 26;
    private static final int D = 29;
    private static final int C = 14;

    public BlightTreePiece(RandomSource random, int x, int z) {
        super(ModStructures.BLIGHT_TREE_PIECE.get(), x, z, W, H, D, random);
    }

    public BlightTreePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.BLIGHT_TREE_PIECE.get(), tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        if (!updateAverageGroundHeight(level, box, 0)) return;

        BlockState podzol = Blocks.PODZOL.defaultBlockState();
        BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
        BlockState mud = Blocks.MUD.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState();
        BlockState roots = Blocks.MANGROVE_ROOTS.defaultBlockState();
        BlockState leaves = Blocks.DARK_OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockState shroomlight = Blocks.SHROOMLIGHT.defaultBlockState();

        // 1. Круглая арена из мха и подзола
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                if (Math.hypot(x - C, z - C) > 13.5) continue;
                placeBlock(level, pick(random, podzol, moss, mud), x, 0, z, box);
                fillColumnDown(level, dirt, x, -1, z, box);
            }
        }

        // 2. Расчистка
        generateAirBox(level, box, 0, 1, 0, W - 1, H - 1, D - 1);

        // 3. Ствол радиусом 3.4 высотой 17, внутри полость высотой 9
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                double d = Math.hypot(x - C, z - C);
                if (d > 3.4) continue;
                for (int y = 1; y <= 17; y++) {
                    if (d <= 1.6 && y <= 9) continue;
                    placeBlock(level, log, x, y, z, box);
                }
            }
        }

        // 4. Вход в ствол с южной стороны (3 блока шириной, 3 высотой) и сундуки внутри
        generateAirBox(level, box, C - 1, 1, 10, C + 1, 3, 12);
        chest(level, box, random, C, 1, C + 1, Direction.NORTH);
        chest(level, box, random, C + 1, 1, C, Direction.WEST);

        // 5. Корни расходятся по земле от ствола
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(k * 45);
            for (int r = 4; r <= 10; r++) {
                int rx = (int) Math.round(C + Math.cos(a) * r);
                int rz = (int) Math.round(C + Math.sin(a) * r);
                placeBlock(level, roots, rx, 1, rz, box);
                if (r <= 6) placeBlock(level, roots, rx, 2, rz, box);
            }
        }

        // 6. Крона: приплюснутый шар вокруг (C, 20, C)
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                for (int y = 14; y < H; y++) {
                    double nx = (x - C) / 8.0;
                    double nz = (z - C) / 8.0;
                    double ny = (y - 20) / 5.5;
                    if (nx * nx + nz * nz + ny * ny > 1) continue;
                    if (Math.hypot(x - C, z - C) <= 3.4 && y <= 17) continue;   // ствол
                    placeBlock(level, random.nextInt(14) == 0 && y <= 18 ? shroomlight : leaves, x, y, z, box);
                }
            }
        }

        // 7. Босс на арене перед входом в ствол
        spawnBoss(level, box, ModBosses.BLIGHT_LORD.get(), C, 1, 7);
    }
}
