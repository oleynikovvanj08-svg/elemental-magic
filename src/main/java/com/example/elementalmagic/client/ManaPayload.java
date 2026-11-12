package com.example.elementalmagic;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ManaPayload(int mana) implements CustomPacketPayload {
    public static final Type<ManaPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, "mana_sync"));
    public static final StreamCodec<ByteBuf, ManaPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(ManaPayload::new, ManaPayload::mana);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
