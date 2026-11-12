package com.example.elementalmagic.network;

import com.example.elementalmagic.MagicMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ManaSyncPayload(int mana) implements CustomPacketPayload {
    public static final Type<ManaSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, "mana_sync"));

    public static final StreamCodec<ByteBuf, ManaSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ManaSyncPayload::mana,
            ManaSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
