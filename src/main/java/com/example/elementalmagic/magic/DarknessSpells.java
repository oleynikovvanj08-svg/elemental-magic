package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.example.elementalmagic.magic.School.NECROMANCY;
import static com.example.elementalmagic.magic.School.SHADOW;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class DarknessSpells {
    static void init() {
        // ==================== ТЕНИ ====================
        reg("shadow_bolt", SHADOW, 15, 30, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 5);
            effect(t, MobEffects.WEAKNESS, 60, 0);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.SMOKE);
            return true;
        });

        reg("shadow_step", SHADOW, 25, 60, (level, p) -> {
            Vec3 pos = teleportPos(p, 16);
            burst(level, mid(p), ParticleTypes.LARGE_SMOKE, 30, 1);
            teleport(p, pos);
            burst(level, mid(p), ParticleTypes.LARGE_SMOKE, 30, 1);
            sound(level, p, SoundEvents.ENDERMAN_TELEPORT, 0.8f);
            return true;
        });

        reg("shadow_binding", SHADOW, 30, 80, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 200, 4);
            effect(t, MobEffects.WEAKNESS, 200, 1);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.SQUID_INK);
            return true;
        });

        reg("darkness", SHADOW, 30, 120, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 10)) {
                effect(e, MobEffects.BLINDNESS, 200, 0);
                effect(e, MobEffects.DARKNESS, 200, 0);
            }
            burst(level, mid(p), ParticleTypes.SQUID_INK, 150, 8);
            return true;
        });

        reg("shadow_cloak", SHADOW, 35, 300, (level, p) -> {
            effect(p, MobEffects.INVISIBILITY, 600, 0);
            effect(p, MobEffects.NIGHT_VISION, 600, 0);
            burst(level, mid(p), ParticleTypes.SMOKE, 40, 1);
            return true;
        });

        reg("shadow_strike", SHADOW, 40, 80, (level, p) -> {
            LivingEntity t = target(p, 16);
            if (t == null) return false;
            Vec3 look = t.getLookAngle();
            teleport(p, t.position().subtract(look.x * 1.5, 0, look.z * 1.5));
            hurt(p, t, 10);
            burst(level, mid(t), ParticleTypes.LARGE_SMOKE, 30, 1);
            sound(level, p, SoundEvents.ENDERMAN_TELEPORT, 1.2f);
            return true;
        });

        reg("umbral_form", SHADOW, 45, 400, (level, p) -> {
            effect(p, MobEffects.INVISIBILITY, 400, 0);
            effect(p, MobEffects.DAMAGE_RESISTANCE, 400, 1);
            effect(p, MobEffects.MOVEMENT_SPEED, 400, 1);
            burst(level, mid(p), ParticleTypes.LARGE_SMOKE, 60, 1.5);
            return true;
        });

        reg("void_walk", SHADOW, 50, 300, (level, p) -> {
            effect(p, MobEffects.INVISIBILITY, 600, 0);
            effect(p, MobEffects.MOVEMENT_SPEED, 600, 2);
            effect(p, MobEffects.JUMP, 600, 1);
            return true;
        });

        reg("nightfall", SHADOW, 40, 1200, (level, p) -> {
            setTimeOfDay(level, 13000);
            burst(level, mid(p), ParticleTypes.LARGE_SMOKE, 100, 5);
            return true;
        });

        reg("eclipse", SHADOW, 70, 600, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 20)) {
                hurt(p, e, 10);
                effect(e, MobEffects.DARKNESS, 400, 0);
                effect(e, MobEffects.WEAKNESS, 400, 1);
            }
            burst(level, mid(p), ParticleTypes.LARGE_SMOKE, 400, 20);
            return true;
        });

        // ==================== НЕКРОМАНТИЯ ====================
        reg("wither_touch", NECROMANCY, 15, 40, (level, p) -> {
            LivingEntity t = target(p, 10);
            if (t == null) return false;
            effect(t, MobEffects.WITHER, 100, 1);
            burst(level, mid(t), ParticleTypes.SOUL, 20, 1);
            return true;
        });

        reg("life_drain", NECROMANCY, 30, 60, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 6);
            p.heal(4);
            line(level, mid(t), p.getEyePosition(), ParticleTypes.SOUL);
            return true;
        });

        reg("bone_spear", NECROMANCY, 25, 50, (level, p) -> {
            for (LivingEntity e : beam(level, p, 20, 0.5)) hurt(p, e, 7);
            line(level, p.getEyePosition(), beamEnd(p, 20), ParticleTypes.SOUL);
            sound(level, p, SoundEvents.SKELETON_SHOOT, 0.7f);
            return true;
        });

        reg("curse", NECROMANCY, 30, 100, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            effect(t, MobEffects.WEAKNESS, 300, 1);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 300, 1);
            effect(t, MobEffects.UNLUCK, 300, 2);
            effect(t, MobEffects.HUNGER, 300, 1);
            burst(level, mid(t), ParticleTypes.WITCH, 30, 1);
            return true;
        });

        reg("soul_harvest", NECROMANCY, 35, 100, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 8)) {
                hurt(p, e, 4);
                p.heal(1);
                line(level, mid(e), p.getEyePosition(), ParticleTypes.SOUL);
            }
            return true;
        });

        reg("death_coil", NECROMANCY, 40, 80, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 8);
            if (t.isDeadOrDying()) p.heal(6); // убил цель: восстанавливает здоровье
            line(level, p.getEyePosition(), mid(t), ParticleTypes.SOUL_FIRE_FLAME);
            return true;
        });

        reg("plague", NECROMANCY, 45, 150, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 10)) {
                effect(e, MobEffects.WITHER, 200, 1);
                effect(e, MobEffects.POISON, 200, 0);
                effect(e, MobEffects.HUNGER, 200, 2);
            }
            burst(level, mid(p), ParticleTypes.WITCH, 150, 10);
            return true;
        });

        reg("corpse_explosion", NECROMANCY, 50, 100, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            Vec3 center = t.position();
            for (LivingEntity e : around(level, p, center, 5)) hurt(p, e, 8);
            burst(level, center.add(0, 1, 0), ParticleTypes.SOUL, 100, 5);
            sound(level, t, SoundEvents.WITHER_SHOOT, 0.6f);
            return true;
        });

        reg("undeath", NECROMANCY, 60, 600, (level, p) -> {
            effect(p, MobEffects.DAMAGE_RESISTANCE, 400, 1);
            effect(p, MobEffects.REGENERATION, 400, 1);
            effect(p, MobEffects.FIRE_RESISTANCE, 400, 0);
            burst(level, mid(p), ParticleTypes.SOUL, 60, 1.5);
            return true;
        });

        reg("apocalypse", NECROMANCY, 80, 1200, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 15)) {
                hurt(p, e, 12);
                effect(e, MobEffects.WITHER, 200, 2);
            }
            burst(level, mid(p), ParticleTypes.SOUL, 500, 15);
            sound(level, p, SoundEvents.WITHER_SHOOT, 0.4f);
            return true;
        });
    }

    private DarknessSpells() {}
}