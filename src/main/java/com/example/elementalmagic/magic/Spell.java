package com.example.elementalmagic.magic;

import net.minecraft.ChatFormatting;
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

    /** Описание из ru_ru.json по ключу spell.elementalmagic.<id>.desc */
    public MutableComponent description() {
        return Component.translatableWithFallback("spell.elementalmagic." + id + ".desc",
                "Описание пока не добавлено").withStyle(ChatFormatting.GRAY);
    }

    /** Стихия заклинания. */
    public Element element() {
        return school.element;
    }

    /** Базовый урон без бонусов книги. 0 означает, что заклинание не наносит урон. */
    public float baseDamage() {
        return SpellInfo.damage(id);
    }

    /** Урон с учётом бонуса гримуара. */
    public float damageIn(Element bookElement) {
        return baseDamage() * bookElement.bookMultiplier(school.element);
    }

    /** Перезарядка в секундах. */
    public double cooldownSeconds() {
        return cooldown / 20.0;
    }

    /** Каст из гримуара: весь урон через hurt() умножается на бонус книги. */
    public boolean castFrom(ServerLevel level, Player player, Element bookElement) {
        float mult = bookElement.bookMultiplier(school.element);
        return SpellUtil.withDamageMultiplier(mult, () -> action.cast(level, player));
    }
}
