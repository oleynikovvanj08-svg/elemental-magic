package com.example.elementalmagic.worldgen;

import com.example.elementalmagic.MagicMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Регистрация построек. Вызови ModStructures.register(modBus) в конструкторе MagicMod. */
public final class ModStructures {
    private static final DeferredRegister<StructureType<?>> TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MagicMod.MODID);
    private static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MagicMod.MODID);

    // ---- Стихийное святилище ----
    public static final DeferredHolder<StructureType<?>, StructureType<ShrineStructure>> SHRINE =
            TYPES.register("elemental_shrine", ModStructures::shrineType);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SHRINE_PIECE =
            PIECES.register("elemental_shrine", () -> (StructurePieceType) ShrinePiece::new);

    // ---- Замок Хранителя времени ----
    public static final DeferredHolder<StructureType<?>, StructureType<TimeCastleStructure>> TIME_CASTLE =
            TYPES.register("time_castle", ModStructures::timeCastleType);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> TIME_CASTLE_PIECE =
            PIECES.register("time_castle", () -> (StructurePieceType) TimeCastlePiece::new);

    // ---- Арены остальных боссов (один тип структуры, поле "arena" в JSON выбирает постройку) ----
    public static final DeferredHolder<StructureType<?>, StructureType<BossArenaStructure>> BOSS_ARENA =
            TYPES.register("boss_arena", ModStructures::bossArenaType);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> LAVA_CITADEL_PIECE =
            PIECES.register("lava_citadel", () -> (StructurePieceType) LavaCitadelPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> STAR_CIRCLE_PIECE =
            PIECES.register("star_circle", () -> (StructurePieceType) StarCirclePiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> BLIGHT_TREE_PIECE =
            PIECES.register("blight_tree", () -> (StructurePieceType) BlightTreePiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SHADOW_PYRAMID_PIECE =
            PIECES.register("shadow_pyramid", () -> (StructurePieceType) ShadowPyramidPiece::new);

    private static StructureType<ShrineStructure> shrineType() {
        return () -> ShrineStructure.CODEC;
    }

    private static StructureType<TimeCastleStructure> timeCastleType() {
        return () -> TimeCastleStructure.CODEC;
    }

    private static StructureType<BossArenaStructure> bossArenaType() {
        return () -> BossArenaStructure.CODEC;
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        PIECES.register(bus);
    }

    private ModStructures() {}
}
