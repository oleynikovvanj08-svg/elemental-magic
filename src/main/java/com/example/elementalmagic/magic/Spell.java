package com.example.elementalmagic.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public record Spell(String id, School school, int manaCost, int cooldown, int tier, CastAction action) {

    @FunctionalInterface
    public interface CastAction {
        boolean cast(ServerLevel level, Player player);
    }

    public MutableComponent name() {
        return Component.translatable("spell.elementalmagic." + id)
                .withStyle(s -> s.withColor(school.element.color));
    }

    public MutableComponent description() {
        return Component.translatableWithFallback("spell.elementalmagic." + id + ".desc",
                "Описание пока не добавлено").withStyle(ChatFormatting.GRAY);
    }

    public Element element() {
        return school.element;
    }

    public float baseDamage() {
        return SpellInfo.damage(id);
    }

    public float damageIn(Element bookElement) {
        return baseDamage() * bookElement.bookMultiplier(school.element);
    }

    public double cooldownSeconds() {
        return cooldown / 20.0;
    }

    public boolean castFrom(ServerLevel level, Player player, Element bookElement) {
        float mult = bookElement.bookMultiplier(school.element);
        return SpellUtil.withDamageMultiplier(mult, () -> action.cast(level, player));
    }

    public boolean castWith(ServerLevel level, Player player, float damageMultiplier) {
        return SpellUtil.withDamageMultiplier(damageMultiplier, () -> action.cast(level, player));
    }
}
