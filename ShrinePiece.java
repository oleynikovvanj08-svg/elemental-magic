package com.example.elementalmagic.worldgen;

import com.example.elementalmagic.magic.Element;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Павильон 9x9: плита, четыре колонны, крыша, в центре постамент с сундуком.
 * Цвет акцентных блоков зависит от стихии. Постройка симметрична, поэтому поворот не важен.
 */
public class ShrinePiece extends ScatteredFeaturePiece {
    private static final int WIDTH = 9;
    private static final int HEIGHT = 7;
    private static final int DEPTH = 9;

    private final Element element;

    public ShrinePiece(RandomSource random, int x, int z, Element element) {
        super(ModStructures.SHRINE_PIECE.get(), x, 64, z, WIDTH, HEIGHT, DEPTH,
                Direction.Plane.HORIZONTAL.getRandomDirection(random));
        this.element = element;
    }

    public ShrinePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.SHRINE_PIECE.get(), tag);
        Element saved = Element.byId(tag.getString("Element"));
        this.element = saved != null ? saved : Element.ELEMENTAL;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Element", element.id);
    }

    /** Акцентный блок по стихии: колонны, кромка крыши, постамент. */
    private static Block accentOf(Element e) {
        return switch (e) {
            case LIGHTNING -> Blocks.COPPER_BLOCK;
            case LIGHT -> Blocks.GLOWSTONE;
            case DARKNESS -> Blocks.OBSIDIAN;
            case WATER -> Blocks.PRISMARINE_BRICKS;
            case EARTH -> Blocks.DRIPSTONE_BLOCK;
            case AIR -> Blocks.QUARTZ_BRICKS;
            case FIRE -> Blocks.MAGMA_BLOCK;
            case NATURE -> Blocks.MOSS_BLOCK;
            case ASH -> Blocks.BLACKSTONE;
            case EXPLOSION -> Blocks.RED_NETHER_BRICKS;
            case ELEMENTAL -> Blocks.AMETHYST_BLOCK;
            default -> Blocks.STONE_BRICKS;
        };
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // Подгоняем высоту под рельеф (один раз, по первому обработанному чанку)
        if (!updateAverageGroundHeight(level, box, 0)) return;

        BlockState brick = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState cracked = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState accent = accentOf(element).defaultBlockState();

        // Плита с «старинным» видом и фундаментом вниз до земли, чтобы ничего не висело
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                float r = random.nextFloat();
                BlockState floor = r < 0.6f ? brick : (r < 0.8f ? cracked : mossy);
                placeBlock(level, floor, x, 0, z, box);
                fillColumnDown(level, brick, x, -1, z, box);
            }
        }

        // Расчищаем место над плитой (деревья, трава)
        generateAirBox(level, box, 0, 1, 0, WIDTH - 1, HEIGHT - 1, DEPTH - 1);

        // Четыре колонны
        for (int px : new int[]{1, 7}) {
            for (int pz : new int[]{1, 7}) {
                for (int y = 1; y <= 3; y++) placeBlock(level, brick, px, y, pz, box);
                placeBlock(level, accent, px, 4, pz, box);
            }
        }

        // Крыша: кромка из акцентного блока, ступенчатый верх
        generateBox(level, box, 0, 5, 0, 8, 5, 8, accent, brick, false);
        generateBox(level, box, 2, 6, 2, 6, 6, 6, accent, brick, false);

        // Постамент, сундук и подвесной фонарь над ним
        placeBlock(level, accent, 4, 1, 4, box);
        placeBlock(level, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH),
                4, 2, 4, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true),
                4, 4, 4, box);

        // Наполняем сундук только в том чанке, где он находится
        BlockPos chestPos = new BlockPos(getWorldX(4, 4), getWorldY(2), getWorldZ(4, 4));
        if (box.isInside(chestPos) && level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            ShrineLoot.fill(chest, element, random);
        }
    }
}
