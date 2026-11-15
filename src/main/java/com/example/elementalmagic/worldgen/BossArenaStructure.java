package com.example.elementalmagic.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Одна «структура» на все арены боссов. Какая именно постройка, задаёт поле "arena" в JSON:
 * lava, stars, blight или shadow.
 */
public class BossArenaStructure extends Structure {
    public static final MapCodec<BossArenaStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    settingsCodec(instance),
                    Codec.STRING.fieldOf("arena").forGetter(s -> s.arena)
            ).apply(instance, BossArenaStructure::new));

    private final String arena;

    public BossArenaStructure(StructureSettings settings, String arena) {
        super(settings);
        this.arena = arena;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
            ChunkPos pos = context.chunkPos();
            RandomSource random = context.random();
            int x = pos.getMinBlockX();
            int z = pos.getMinBlockZ();
            ArenaPiece piece = switch (arena) {
                case "lava" -> new LavaCitadelPiece(random, x, z);
                case "stars" -> new StarCirclePiece(random, x, z);
                case "blight" -> new BlightTreePiece(random, x, z);
                case "shadow" -> new ShadowPyramidPiece(random, x, z);
                default -> null;
            };
            if (piece != null) builder.addPiece(piece);
        });
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.BOSS_ARENA.get();
    }
}
