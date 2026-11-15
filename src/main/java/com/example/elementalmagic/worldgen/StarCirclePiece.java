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
 * Звёздный круг: открытая каменная площадка радиусом 14, по краю 12 колонн с огнями,
 * в центре аметистовое возвышение для босса, на полу «звёзды» из морских фонарей.
 */
public class StarCirclePiece extends ArenaPiece {
    private static final int W = 29;
    private static final int H = 16;
    private static final int D = 29;
    private static final int C = 14;

    public StarCirclePiece(RandomSource random, int x, int z) {
        super(ModStructures.STAR_CIRCLE_PIECE.get(), x, z, W, H, D, random);
    }

    public StarCirclePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.STAR_CIRCLE_PIECE.get(), tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        if (!updateAverageGroundHeight(level, box, 0)) return;

        BlockState polished = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState tiles = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState crackedTiles = Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
        BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState lantern = Blocks.SEA_LANTERN.defaultBlockState();

        // 1. Круглый пол с фундаментом
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                double d = Math.hypot(x - C, z - C);
                if (d > 14) continue;
                placeBlock(level, pick(random, polished, tiles, crackedTiles), x, 0, z, box);
                fillColumnDown(level, polished, x, -1, z, box);
            }
        }

        // 2. Расчистка
        generateAirBox(level, box, 0, 1, 0, W - 1, H - 1, D - 1);

        // 3. Возвышение в центре
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                if (Math.hypot(x - C, z - C) <= 3.5) placeBlock(level, amethyst, x, 1, z, box);
            }
        }

        // 4. «Звёзды» на полу
        for (int i = 0; i < 12; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 5 + random.nextDouble() * 8;
            placeBlock(level, lantern, (int) Math.round(C + Math.cos(angle) * radius), 0,
                    (int) Math.round(C + Math.sin(angle) * radius), box);
        }

        // 5. Двенадцать колонн по кругу с огнями наверху
        for (int k = 0; k < 12; k++) {
            double a = Math.toRadians(k * 30);
            int px = (int) Math.round(C + Math.cos(a) * 11.5);
            int pz = (int) Math.round(C + Math.sin(a) * 11.5);
            for (int y = 1; y <= 7; y++) placeBlock(level, polished, px, y, pz, box);
            placeBlock(level, Blocks.END_ROD.defaultBlockState(), px, 8, pz, box);
        }

        // 6. Сундуки и босс на возвышении
        chest(level, box, random, 23, 1, C, Direction.WEST);
        chest(level, box, random, 5, 1, C, Direction.EAST);
        spawnBoss(level, box, ModBosses.STAR_SENTINEL.get(), C, 2, C);
    }
}
