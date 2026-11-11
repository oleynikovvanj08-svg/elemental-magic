package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import static com.example.elementalmagic.magic.School.ROT;
import static com.example.elementalmagic.magic.School.THORN;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class DarkNatureSpells {
    static void init() {
        // ==================== ГНИЛЬ ====================
        reg("rot_touch", ROT, 20, 40, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 6);
            effect(t, MobEffects.WITHER, 140, 1);
            effect(t, MobEffects.POISON, 140, 1);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.SCULK_SOUL);
            sound(level, t, SoundEvents.WITHER_HURT, 1.3f);
            return true;
        });

        reg("plague_spores", ROT, 30, 100, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 8)) {
                hurt(p, e, 5);
                effect(e, MobEffects.WITHER, 180, 1);
                effect(e, MobEffects.POISON, 180, 1);
                effect(e, MobEffects.WEAKNESS, 180, 1);
            }
            burst(level, mid(p), ParticleTypes.SCULK_SOUL, 120, 8);
            sound(level, p, SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.9f);
            return true;
        });

        reg("life_drain", ROT, 35, 80, (level, p) -> {
            LivingEntity t = target(p, 22);
            if (t == null) return false;
            hurt(p, t, 8);
            p.heal(4);
            line(level, mid(t), mid(p), ParticleTypes.SCULK_SOUL);
            sound(level, p, SoundEvents.WITHER_AMBIENT, 1.1f);
            return true;
        });

        reg("decay_aura", ROT, 40, 300, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 6)) {
                hurt(p, e, 6);
                effect(e, MobEffects.WITHER, 240, 2);
            }
            ring(level, p.position(), 6, ParticleTypes.SCULK_SOUL);
            sound(level, p, SoundEvents.WITHER_HURT, 0.7f);
            return true;
        });

        reg("world_blight", ROT, 75, 900, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 16)) {
                hurt(p, e, 14);
                effect(e, MobEffects.WITHER, 400, 3);
                effect(e, MobEffects.POISON, 400, 2);
            }
            burst(level, mid(p), ParticleTypes.SCULK_SOUL, 400, 16);
            sound(level, p, SoundEvents.WITHER_SPAWN, 0.6f);
            return true;
        });

        // ==================== ШИПЫ ====================
        reg("thorn_strike", THORN, 20, 40, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 7);
            effect(t, MobEffects.POISON, 100, 0);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.SCULK_CHARGE);
            sound(level, t, SoundEvents.SWEET_BERRY_BUSH_HURT_PLAYER, 1.0f);
            return true;
        });

        reg("thorn_armor", THORN, 30, 300, (level, p) -> {
            effect(p, MobEffects.DAMAGE_RESISTANCE, 400, 1);
            for (LivingEntity e : enemies(level, p, 3)) {
                hurt(p, e, 5);
            }
            ring(level, p.position(), 2, ParticleTypes.SCULK_CHARGE);
            sound(level, p, SoundEvents.SWEET_BERRY_BUSH_PLACE, 0.8f);
            return true;
        });

        reg("vampiric_vine", THORN, 35, 100, (level, p) -> {
            LivingEntity t = target(p, 25);
            if (t == null) return false;
            hurt(p, t, 10);
            p.heal(6);
            effect(t, MobEffects.POISON, 120, 1);
            line(level, mid(t), mid(p), ParticleTypes.SCULK_CHARGE);
            sound(level, p, SoundEvents.WEEPING_VINES_BREAK, 0.9f);
            return true;
        });

        reg("thorn_wall", THORN, 40, 150, (level, p) -> {
            for (LivingEntity e : beam(level, p, 15, 1.5)) {
                hurt(p, e, 9);
                effect(e, MobEffects.POISON, 140, 1);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 140, 1);
            }
            line(level, p.getEyePosition(), beamEnd(p, 15), ParticleTypes.SCULK_CHARGE);
            sound(level, p, SoundEvents.SWEET_BERRY_BUSH_HURT_PLAYER, 0.7f);
            return true;
        });

        reg("nightmare_garden", THORN, 70, 800, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 14)) {
                hurt(p, e, 13);
                effect(e, MobEffects.WITHER, 300, 2);
                effect(e, MobEffects.POISON, 300, 2);
                effect(e, MobEffects.WEAKNESS, 300, 2);
            }
            burst(level, mid(p), ParticleTypes.SCULK_CHARGE, 400, 14);
            burst(level, mid(p), ParticleTypes.SCULK_SOUL, 400, 14);
            sound(level, p, SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.5f);
            return true;
        });
    }

    private DarkNatureSpells() {}
}
