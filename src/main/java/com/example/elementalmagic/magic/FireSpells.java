package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.example.elementalmagic.magic.School.FLAME;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class FireSpells {
    static void init() {
        // ==================== ПЛАМЯ ====================
        reg("flame_spark", FLAME, 10, 20, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 3);
            t.igniteForSeconds(5);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.FLAME);
            sound(level, t, SoundEvents.FIRECHARGE_USE, 1.6f);
            return true;
        });

        reg("fire_bolt", FLAME, 20, 40, (level, p) -> {
            LivingEntity t = target(p, 25);
            if (t == null) return false;
            hurt(p, t, 7);
            t.igniteForSeconds(4);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.FLAME);
            sound(level, p, SoundEvents.BLAZE_SHOOT, 1.2f);
            return true;
        });

        reg("fire_ring", FLAME, 25, 80, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 5)) {
                hurt(p, e, 4);
                e.igniteForSeconds(6);
                pushFrom(e, p.position(), 0.8, 0.3);
            }
            ring(level, p.position(), 5, ParticleTypes.FLAME);
            sound(level, p, SoundEvents.FIRECHARGE_USE, 1f);
            return true;
        });

        reg("flamethrower", FLAME, 25, 40, (level, p) -> {
            for (LivingEntity e : beam(level, p, 10, 1.0)) {
                hurt(p, e, 5);
                e.igniteForSeconds(6);
            }
            line(level, p.getEyePosition(), beamEnd(p, 10), ParticleTypes.FLAME);
            burst(level, beamEnd(p, 10), ParticleTypes.FLAME, 30, 1.5);
            sound(level, p, SoundEvents.FIRE_AMBIENT, 1.5f);
            return true;
        });

        reg("inferno_cloak", FLAME, 35, 300, (level, p) -> {
            effect(p, MobEffects.FIRE_RESISTANCE, 600, 0);
            for (LivingEntity e : enemies(level, p, 4)) {
                hurt(p, e, 4);
                e.igniteForSeconds(8);
            }
            ring(level, p.position(), 2, ParticleTypes.FLAME);
            burst(level, mid(p), ParticleTypes.FLAME, 60, 1.5);
            return true;
        });

        reg("combustion", FLAME, 35, 80, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, t.isOnFire() ? 14 : 7);
            t.igniteForSeconds(6);
            burst(level, mid(t), ParticleTypes.FLAME, 60, 1.5);
            sound(level, t, SoundEvents.FIRECHARGE_USE, 0.6f);
            return true;
        });

        reg("phoenix_feather", FLAME, 40, 600, (level, p) -> {
            p.heal(8);
            p.clearFire();
            effect(p, MobEffects.FIRE_RESISTANCE, 1200, 0);
            effect(p, MobEffects.ABSORPTION, 600, 1);
            burst(level, mid(p), ParticleTypes.FLAME, 80, 2);
            sound(level, p, SoundEvents.BLAZE_AMBIENT, 1.4f);
            return true;
        });

        reg("fire_dash", FLAME, 25, 60, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 3)) e.igniteForSeconds(5);
            Vec3 look = p.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            if (flat.lengthSqr() < 1.0E-4) return false;
            push(p, flat.normalize().scale(2.0).add(0, 0.2, 0));
            p.resetFallDistance();
            effect(p, MobEffects.FIRE_RESISTANCE, 100, 0);
            burst(level, mid(p), ParticleTypes.FLAME, 50, 1.5);
            sound(level, p, SoundEvents.FIRECHARGE_USE, 1.4f);
            return true;
        });

        reg("pyroblast", FLAME, 55, 200, (level, p) -> {
            LivingEntity t = target(p, 30);
            if (t == null) return false;
            hurt(p, t, 16);
            t.igniteForSeconds(8);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.FLAME);
            burst(level, mid(t), ParticleTypes.FLAME, 40, 2);
            sound(level, p, SoundEvents.BLAZE_SHOOT, 0.5f);
            return true;
        });

        reg("firestorm", FLAME, 75, 900, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 14)) {
                hurt(p, e, 11);
                e.igniteForSeconds(10);
            }
            ring(level, p.position(), 14, ParticleTypes.FLAME);
            burst(level, mid(p).add(0, 2, 0), ParticleTypes.FLAME, 400, 14);
            sound(level, p, SoundEvents.BLAZE_SHOOT, 0.3f);
            return true;
        });

        // ===== ОСОБОЕ ЗАКЛИНАНИЕ ОГНЕННОГО БОССА (падает с MagmaLordEntity) =====
        reg("fire_lord_inferno", FLAME, 60, 600, 5, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 8)) {
                hurt(p, e, 15);
                e.igniteForSeconds(10);
                burst(level, mid(e), ParticleTypes.FLAME, 30, 1);
            }
            ring(level, p.position(), 8, ParticleTypes.FLAME);
            burst(level, mid(p), ParticleTypes.FLAME, 200, 8);
            sound(level, p, SoundEvents.GENERIC_EXPLODE.value(), 2.0f, 0.5f);
            return true;
        });
    }

    private FireSpells() {}
}
