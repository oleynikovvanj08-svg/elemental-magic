package com.example.elementalmagic.worldgen;

import com.example.elementalmagic.magic.Element;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import java.util.Arrays;

/** Основа для арен боссов: общие помощники для сундуков, босса и случайных блоков. */
public abstract class ArenaPiece extends ScatteredFeaturePiece {
    /** Сундуки арен: только обычные стихии, боссовые вещи падают с босса. */
    private static final Element[] CHEST_ELEMENTS = Arrays.stream(Element.values())
            .filter(e -> !e.bossOnly)
            .toArray(Element[]::new);

    protected ArenaPiece(StructurePieceType type, int x, int z, int width, int height, int depth,
                         RandomSource random) {
        super(type, x, 64, z, width, height, depth, Direction.Plane.HORIZONTAL.getRandomDirection(random));
    }

    protected ArenaPiece(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
    }

    /** Основной блок в 60% случаев, остальное поровну между двумя вариантами. */
    protected static BlockState pick(RandomSource random, BlockState main, BlockState alt1, BlockState alt2) {
        float r = random.nextFloat();
        return r < 0.6f ? main : (r < 0.8f ? alt1 : alt2);
    }

    /** Ставит сундук и наполняет его (один раз, в том чанке, где он лежит). */
    protected void chest(WorldGenLevel level, BoundingBox box, RandomSource random,
                         int x, int y, int z, Direction facing) {
        placeBlock(level, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), x, y, z, box);
        BlockPos pos = new BlockPos(getWorldX(x, z), getWorldY(y), getWorldZ(x, z));
        if (box.isInside(pos) && level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            ShrineLoot.fill(chest, CHEST_ELEMENTS[random.nextInt(CHEST_ELEMENTS.length)], random);
        }
    }

    /** Спавнит босса. Происходит один раз: в том чанке, где лежит выбранная точка. */
    protected void spawnBoss(WorldGenLevel level, BoundingBox box, EntityType<? extends Mob> type,
                             int x, int y, int z) {
        BlockPos pos = new BlockPos(getWorldX(x, z), getWorldY(y), getWorldZ(x, z));
        if (!box.isInside(pos)) return;
        Mob boss = type.create(level.getLevel());
        if (boss == null) return;
        boss.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        boss.setPersistenceRequired();
        level.addFreshEntityWithPassengers(boss);
    }
}
