package com.example.elementalmagic.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Замок Хранителя времени: крепость 25x25 на поверхности. */
public class TimeCastleStructure extends Structure {
    public static final MapCodec<TimeCastleStructure> CODEC = simpleCodec(TimeCastleStructure::new);

    public TimeCastleStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
            ChunkPos pos = context.chunkPos();
            builder.addPiece(new TimeCastlePiece(context.random(), pos.getMinBlockX(), pos.getMinBlockZ()));
        });
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.TIME_CASTLE.get();
    }
}
