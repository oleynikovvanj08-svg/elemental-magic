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

    public static final DeferredHolder<StructureType<?>, StructureType<ShrineStructure>> SHRINE =
            TYPES.register("elemental_shrine", ModStructures::shrineType);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SHRINE_PIECE =
            PIECES.register("elemental_shrine", () -> (StructurePieceType) ShrinePiece::new);

    private static StructureType<ShrineStructure> shrineType() {
        return () -> ShrineStructure.CODEC;
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        PIECES.register(bus);
    }

    private ModStructures() {}
}
