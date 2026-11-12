package com.example.elementalmagic;

import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Мгновенное восстановление маны. Уровень I: 50 маны, уровень II: 100. */
public class ManaRestoreEffect extends InstantenousMobEffect {
    /** Сколько маны восстанавливает каждый уровень эффекта. */
    public static final int PER_LEVEL = 50;

    public ManaRestoreEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x3FA9F5);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        restore(entity, amplifier, 1.0);
        return true;
    }

    @Override
    public void applyInstantenousEffect(Entity source, Entity indirectSource, LivingEntity target,
                                        int amplifier, double strength) {
        // strength меньше 1, если взрывное зелье упало не рядом с игроком
        restore(target, amplifier, strength);
    }

    private static void restore(LivingEntity target, int amplifier, double strength) {
        if (!(target instanceof Player player) || player.level().isClientSide) return;
        int amount = (int) Math.round(PER_LEVEL * (amplifier + 1) * strength);
        int current = Mana.get(player);
        int cap = Math.max(current, Mana.BASE_MAX);
        Mana.set(player, Math.min(cap, current + amount));
    }
}
