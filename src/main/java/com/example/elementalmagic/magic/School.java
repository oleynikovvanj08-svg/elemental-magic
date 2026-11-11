package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import static com.example.elementalmagic.magic.School.BEAST;
import static com.example.elementalmagic.magic.School.FLORA;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class NatureSpells {
    static void init() {
        // ==================== ФЛОРА ====================
        reg("vine_grasp", FLORA, 15, 40, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 4);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 80, 2);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.HAPPY_VILLAGER);
            sound(level, t, SoundEvents.GRASS_BREAK, 0.8f);
            return true;
        });

        reg("thorn_barrage", FLORA, 25, 60, (level, p) -> {
            for (LivingEntity e : beam(level, p, 15, 1.0)) {
                hurt(p, e, 6);
                effect(e, MobEffects.POISON, 100, 0);
            }
            line(level, p.getEyePosition(), beamEnd(p, 15), ParticleTypes.COMPOSTER);
            sound(level, p, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 0.6f);
            return true;
        });

        reg("healing_spores", FLORA, 30, 200, (level, p) -> {
            for (LivingEntity e : friends(level, p, 8)) {
                e.heal(6);
                effect(e, MobEffects.REGENERATION, 100, 1);
            }
            burst(level, mid(p), ParticleTypes.HAPPY_VILLAGER, 60, 8);
            sound(level, p, SoundEvents.CHORUS_FLOWER_GROW, 1.2f);
            return true;
        });

        reg("root_prison", FLORA, 35, 120, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 200, 10);
            effect(t, MobEffects.WEAKNESS, 200, 1);
            ring(level, t.position(), 2, ParticleTypes.HAPPY_VILLAGER);
            sound(level, t, SoundEvents.GRASS_PLACE, 0.5f);
            return true;
        });

        reg("bloom", FLORA, 45, 400, (level, p) -> {
            for (LivingEntity e : friends(level, p, 10)) {
                e.heal(10);
                cleanse(e);
                effect(e, MobEffects.REGENERATION, 200, 2);
            }
            burst(level, mid(p), ParticleTypes.CHERRY_LEAVES, 150, 10);
            sound(level, p, SoundEvents.FLOWERING_AZALEA_BREAK, 0.8f);
            return true;
        });

        // ==================== ЗВЕРИ ====================
        reg("wolf_spirit", BEAST, 20, 80, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 7);
            effect(p, MobEffects.MOVEMENT_SPEED, 100, 1);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.CLOUD);
            sound(level, t, SoundEvents.WOLF_GROWL, 1.0f);
            return true;
        });

        reg("bear_rage", BEAST, 30, 200, (level, p) -> {
            effect(p, MobEffects.DAMAGE_BOOST, 200, 2);
            effect(p, MobEffects.DAMAGE_RESISTANCE, 200, 1);
            effect(p, MobEffects.MOVEMENT_SLOWDOWN, 200, 0);
            burst(level, mid(p), ParticleTypes.ANGRY_VILLAGER, 30, 2);
            sound(level, p, SoundEvents.POLAR_BEAR_WARNING, 0.8f);
            return true;
        });

        reg("eagles_eye", BEAST, 25, 300, (level, p) -> {
            effect(p, MobEffects.NIGHT_VISION, 600, 0);
            effect(p, MobEffects.JUMP, 600, 2);
            effect(p, MobEffects.SLOW_FALLING, 600, 0);
            burst(level, mid(p), ParticleTypes.CLOUD, 40, 2);
            sound(level, p, SoundEvents.PARROT_FLY, 0.6f);
            return true;
        });

        reg("pack_howl", BEAST, 40, 400, (level, p) -> {
            for (LivingEntity e : friends(level, p, 12)) {
                effect(e, MobEffects.DAMAGE_BOOST, 300, 1);
                effect(e, MobEffects.MOVEMENT_SPEED, 300, 1);
            }
            ring(level, p.position(), 12, ParticleTypes.NOTE);
            sound(level, p, SoundEvents.WOLF_HOWL, 0.5f);
            return true;
        });

        reg("primal_fury", BEAST, 50, 600, (level, p) -> {
            effect(p, MobEffects.DAMAGE_BOOST, 400, 3);
            effect(p, MobEffects.DAMAGE_RESISTANCE, 400, 2);
            effect(p, MobEffects.MOVEMENT_SPEED, 400, 2);
            for (LivingEntity e : enemies(level, p, 5)) {
                hurt(p, e, 8);
                pushFrom(e, p.position(), 1.5, 0.5);
            }
            burst(level, mid(p), ParticleTypes.SWEEP_ATTACK, 50, 3);
            sound(level, p, SoundEvents.RAVAGER_ROAR, 0.6f);
            return true;
        });
    }

    private NatureSpells() {}
}
