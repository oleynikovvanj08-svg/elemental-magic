package com.example.elementalmagic.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public record Spell(String id, School school, int manaCost, int cooldown, CastAction action) {

    @FunctionalInterface
    public interface CastAction {
        /** Возвращает true, если заклинание сработало (тогда тратится мана). */
        boolean cast(ServerLevel level, Player player);
    }

    public MutableComponent name() {
        return Component.translatable("spell.elementalmagic." + id)
                .withStyle(s -> s.withColor(school.element.color));
    }
}