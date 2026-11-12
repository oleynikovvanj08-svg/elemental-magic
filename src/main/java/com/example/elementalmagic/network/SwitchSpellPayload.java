package com.example.elementalmagic.network;

import com.example.elementalmagic.MagicMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Пакет от клиента: direction = 1 следующее заклинание, -1 предыдущее. */
public record SwitchSpellPayload(int direction) implements CustomPacketPayload {
    public static final Type<SwitchSpellPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, "switch_spell"));

    public static final StreamCodec<ByteBuf, SwitchSpellPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(SwitchSpellPayload::new, SwitchSpellPayload::direction);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}