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

    /** Уровень заклинания от 1 до 5. */
    public int tier() {
        return Spells.tierOf(id);
    }

    /** Множитель урона от уровня заклинания. */
    public float tierMultiplier() {
        return SpellInfo.tierMultiplier(tier());
    }

    /** Урон из таблицы SpellInfo без каких-либо бонусов. */
    public float rawDamage() {
        return SpellInfo.damage(id);
    }

    /** Урон с учётом уровня, но без бонуса гримуара. 0 означает, что заклинание не наносит урон. */
    public float baseDamage() {
        return rawDamage() * tierMultiplier();
    }

    /** Урон с учётом уровня и бонуса гримуара. */
    public float damageIn(Element bookElement) {
        return baseDamage() * bookElement.bookMultiplier(school.element);
    }

    /** Перезарядка в секундах. От уровня не зависит. */
    public double cooldownSeconds() {
        return cooldown / 20.0;
    }

    /** Каст из гримуара: урон умножается на бонус книги и бонус уровня. */
    public boolean castFrom(ServerLevel level, Player player, Element bookElement) {
        float mult = bookElement.bookMultiplier(school.element) * tierMultiplier();
        return SpellUtil.withDamageMultiplier(mult, () -> action.cast(level, player));
    }

    /** Каст с множителем носителя (для посохов). Бонус уровня добавляется сам. */
    public boolean castWith(ServerLevel level, Player player, float carrierMultiplier) {
        float mult = carrierMultiplier * tierMultiplier();
        return SpellUtil.withDamageMultiplier(mult, () -> action.cast(level, player));
    }
}
