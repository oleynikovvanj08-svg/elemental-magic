package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.example.elementalmagic.magic.School.FLAME;
import static com.example.elementalmagic.magic.School.LAVA;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class FireSpells {
    static void init() {
        // ==================== ЛАВА ====================
        reg("magma_shot", LAVA, 15, 30, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 5);
            t.igniteForSeconds(4);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.LAVA);
            sound(level, t, SoundEvents.LAVA_POP, 1f);
            return true;
        });

        reg("molten_ground", LAVA, 25, 100, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 6)) {
                hurt(p, e, 3);
                e.igniteForSeconds(6);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 100, 2);
            }
            ring(level, p.position(), 6, ParticleTypes.LAVA);
            sound(level, p, SoundEvents.LAVA_AMBIENT, 1f);
            return true;
        });

        reg("lava_burst", LAVA, 30, 80, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            for (LivingEntity e : around(level, p, t.position(), 3)) {
                hurt(p, e, 8);
                e.igniteForSeconds(5);
            }
            burst(level, mid(t), ParticleTypes.LAVA, 40, 3);
            sound(level, t, SoundEvents.LAVA_POP, 0.6f);
            return true;
        });

        reg("heat_wave", LAVA, 30, 80, (level, p) -> {
            for (LivingEntity e : beam(level, p, 14, 1.5)) {
                hurt(p, e, 5);
                e.igniteForSeconds(4);
                pushFrom(e, p.position(), 1.0, 0.2);
            }
            line(level, p.getEyePosition(), beamEnd(p, 14), ParticleTypes.FLAME);
            sound(level, p, SoundEvents.FIRECHARGE_USE, 0.8f);
            return true;
        });

        reg("obsidian_shield", LAVA, 35, 400, (level, p) -> {
            effect(p, MobEffects.DAMAGE_RESISTANCE, 200, 2);
            effect(p, MobEffects.FIRE_RESISTANCE, 400, 0);
            effect(p, MobEffects.MOVEMENT_SLOWDOWN, 200, 0);
            ring(level, p.position(), 2, ParticleTypes.LARGE_SMOKE);
            sound(level, p, SoundEvents.LAVA_EXTINGUISH, 0.8f);
            return true;
        });

        reg("ash_cloud", LAVA, 30, 150, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 10)) {
                effect(e, MobEffects.BLINDNESS, 160, 0);
                effect(e, MobEffects.WEAKNESS, 160, 0);
            }
            burst(level, mid(p), ParticleTypes.LARGE_SMOKE, 200, 10);
            burst(level, mid(p), ParticleTypes.ASH, 200, 10);
            return true;
        });

        reg("lava_lance", LAVA, 40, 80, (level, p) -> {
            for (LivingEntity e : beam(level, p, 30, 0.5)) {
                hurt(p, e, 11);
                e.igniteForSeconds(6);
            }
            line(level, p.getEyePosition(), beamEnd(p, 30), ParticleTypes.LAVA);
            sound(level, p, SoundEvents.BLAZE_SHOOT, 0.7f);
            return true;
        });

        reg("magma_heart", LAVA, 45, 600, (level, p) -> {
            effect(p, MobEffects.DAMAGE_BOOST, 400, 1);
            effect(p, MobEffects.FIRE_RESISTANCE, 600, 0);
            effect(p, MobEffects.REGENERATION, 200, 0);
            burst(level, mid(p), ParticleTypes.LAVA, 30, 1.5);
            sound(level, p, SoundEvents.LAVA_AMBIENT, 0.6f);
            return true;
        });

        reg("meteor", LAVA, 60, 300, (level, p) -> {
            LivingEntity t = target(p, 30);
            if (t == null) return false;
            for (LivingEntity e : around(level, p, t.position(), 5)) {
                hurt(p, e, 14);
                e.igniteForSeconds(8);
            }
            line(level, t.position().add(4, 15, 4), mid(t), ParticleTypes.FLAME);
            burst(level, mid(t), ParticleTypes.LAVA, 80, 5);
            sound(level, t, SoundEvents.BLAZE_SHOOT, 0.4f);
            return true;
        });

        reg("volcanic_eruption", LAVA, 85, 1200, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 15)) {
                hurt(p, e, 14);
                e.igniteForSeconds(10);
                push(e, e.getDeltaMovement().add(0, 1.2, 0));
            }
            burst(level, mid(p), ParticleTypes.LAVA, 300, 15);
            burst(level, mid(p).add(0, 3, 0), ParticleTypes.LARGE_SMOKE, 300, 15);
            sound(level, p, SoundEvents.LAVA_POP, 0.3f);
            return true;
        });

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
            burst(level, mid(t), ParticleTypes.LAVA, 40, 2);
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
    }

    private FireSpells() {}
}