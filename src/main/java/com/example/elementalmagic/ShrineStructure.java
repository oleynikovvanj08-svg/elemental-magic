package com.example.elementalmagic.worldgen;

import com.example.elementalmagic.magic.Element;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Arrays;
import java.util.Optional;

/** Стихийное святилище: небольшой павильон с сундуком. Стихия выбирается случайно для каждого. */
public class ShrineStructure extends Structure {
    public static final MapCodec<ShrineStructure> CODEC = simpleCodec(ShrineStructure::new);

    /** Святилища только у обычных стихий: боссовые школы падают только с боссов. */
    private static final Element[] ELEMENTS = Arrays.stream(Element.values())
            .filter(e -> !e.bossOnly)
            .toArray(Element[]::new);

    public ShrineStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG,
                builder -> addPieces(builder, context));
    }

    private void addPieces(StructurePiecesBuilder builder, GenerationContext context) {
        ChunkPos pos = context.chunkPos();
        Element element = ELEMENTS[context.random().nextInt(ELEMENTS.length)];
        builder.addPiece(new ShrinePiece(context.random(), pos.getMinBlockX(), pos.getMinBlockZ(), element));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SHRINE.get();
    }
}
