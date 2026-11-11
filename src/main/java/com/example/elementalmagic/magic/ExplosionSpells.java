package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.example.elementalmagic.magic.School.DETONATION;
import static com.example.elementalmagic.magic.School.SHOCKWAVE;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class ExplosionSpells {
    static void init() {
        // ==================== ДЕТОНАЦИЯ ====================
        reg("spark_charge", DETONATION, 15, 30, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 5);
            for (LivingEntity e : around(level, p, t.position(), 2)) {
                if (e != t) hurt(p, e, 3);
            }
            burst(level, mid(t), ParticleTypes.EXPLOSION, 5, 0.5);
            sound(level, t, SoundEvents.GENERIC_EXPLODE, 1.4f);
            return true;
        });

        reg("mine_trap", DETONATION, 25, 100, (level, p) -> {
            Vec3 pos = teleportPos(p, 15);
            for (LivingEntity e : around(level, p, pos, 3)) {
                hurt(p, e, 8);
                pushFrom(e, pos, 1.0, 0.5);
            }
            burst(level, pos, ParticleTypes.EXPLOSION, 20, 3);
            sound(level, p, SoundEvents.GENERIC_EXPLODE, 0.9f);
            return true;
        });

        reg("chain_blast", DETONATION, 35, 120, (level, p) -> {
            LivingEntity t = target(p, 25);
            if (t == null) return false;
            for (int i = 0; i < 3; i++) {
                LivingEntity next = t;
                for (LivingEntity e : around(level, p, t.position(), 4)) {
                    if (e.distanceToSqr(t) < next.distanceToSqr(t)) next = e;
                }
                hurt(p, next, 7);
                burst(level, mid(next), ParticleTypes.EXPLOSION, 10, 1);
                t = next;
            }
            sound(level, p, SoundEvents.GENERIC_EXPLODE, 0.8f);
            return true;
        });

        reg("detonate", DETONATION, 40, 100, (level, p) -> {
            LivingEntity t = target(p, 30);
            if (t == null) return false;
            for (LivingEntity e : around(level, p, t.position(), 5)) {
                hurt(p, e, 12);
                pushFrom(e, t.position(), 1.5, 0.6);
            }
            burst(level, mid(t), ParticleTypes.EXPLOSION_EMITTER, 1, 0);
            burst(level, mid(t), ParticleTypes.FLAME, 100, 5);
            sound(level, t, SoundEvents.GENERIC_EXPLODE, 0.6f);
            return true;
        });

        reg("supernova", DETONATION, 70, 600, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 18)) {
                hurt(p, e, 16);
                pushFrom(e, p.position(), 2.0, 0.8);
            }
            burst(level, mid(p), ParticleTypes.EXPLOSION_EMITTER, 10, 4);
            burst(level, mid(p), ParticleTypes.FLAME, 400, 18);
            sound(level, p, SoundEvents.GENERIC_EXPLODE, 0.4f);
            return true;
        });

        // ==================== УДАРНАЯ ВОЛНА ====================
        reg("concussion", SHOCKWAVE, 20, 40, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 5)) {
                hurt(p, e, 4);
                pushFrom(e, p.position(), 1.2, 0.4);
            }
            ring(level, p.position(), 5, ParticleTypes.CLOUD);
            sound(level, p, SoundEvents.GENERIC_EXPLODE, 1.2f);
            return true;
        });

        reg("shockbolt", SHOCKWAVE, 25, 50, (level, p) -> {
            for (LivingEntity e : beam(level, p, 20, 1.0)) {
                hurt(p, e, 6);
                pushFrom(e, p.position(), 1.0, 0.3);
            }
            line(level, p.getEyePosition(), beamEnd(p, 20), ParticleTypes.CLOUD);
            sound(level, p, SoundEvents.GENERIC_EXPLODE, 1.0f);
            return true;
        });

        reg("impact_wave", SHOCKWAVE, 30, 100, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 8)) {
                hurt(p, e, 7);
                pushFrom(e, p.position(), 1.5, 0.5);
            }
            ring(level, p.position(), 8, ParticleTypes.EXPLOSION);
            burst(level, mid(p), ParticleTypes.CLOUD, 100, 4);
            sound(level, p, SoundEvents.GENERIC_EXPLODE, 0.8f);
            return true;
        });

        reg("explosion_armor", SHOCKWAVE, 35, 300, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 4)) {
                hurt(p, e, 6);
                pushFrom(e, p.position(), 1.8, 0.6);
            }
            ring(level, p.position(), 2, ParticleTypes.EXPLOSION);
            sound(level, p, SoundEvents.GENERIC_EXPLODE, 1.0f);
            return true;
        });

        reg("cataclysm", SHOCKWAVE, 80, 1200, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 20)) {
                hurt(p, e, 18);
                pushFrom(e, p.position(), 2.5, 1.0);
            }
            burst(level, mid(p), ParticleTypes.EXPLOSION_EMITTER, 15, 8);
            burst(level, mid(p), ParticleTypes.CLOUD, 500, 20);
            sound(level, p, SoundEvents.GENERIC_EXPLODE, 0.3f);
            return true;
        });
    }

    private ExplosionSpells() {}
}
