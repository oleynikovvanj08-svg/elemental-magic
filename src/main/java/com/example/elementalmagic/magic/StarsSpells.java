package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import static com.example.elementalmagic.magic.School.CONSTELLATION;
import static com.example.elementalmagic.magic.School.COSMOS;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class StarsSpells {
    static void init() {
        // ==================== СОЗВЕЗДИЯ ====================
        reg("star_spark", CONSTELLATION, 15, 30, (level, p) -> {
            LivingEntity t = target(p, 25);
            if (t == null) return false;
            hurt(p, t, 6);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.END_ROD);
            sound(level, t, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f);
            return true;
        });

        reg("falling_star", CONSTELLATION, 30, 80, (level, p) -> {
            LivingEntity t = target(p, 30);
            if (t == null) return false;
            hurt(p, t, 10);
            for (LivingEntity e : around(level, p, t.position(), 3)) {
                if (e != t) hurt(p, e, 6);
            }
            line(level, t.position().add(5, 20, 5), mid(t), ParticleTypes.END_ROD);
            burst(level, mid(t), ParticleTypes.FIREWORK, 40, 3);
            sound(level, t, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0f);
            return true;
        });

        reg("orion_belt", CONSTELLATION, 40, 120, (level, p) -> {
            for (LivingEntity e : beam(level, p, 25, 0.6)) {
                hurt(p, e, 9);
            }
            line(level, p.getEyePosition(), beamEnd(p, 25), ParticleTypes.END_ROD);
            burst(level, beamEnd(p, 25), ParticleTypes.FIREWORK, 30, 2);
            sound(level, p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f);
            return true;
        });

        reg("starlight_cloak", CONSTELLATION, 35, 400, (level, p) -> {
            effect(p, MobEffects.GLOWING, 600, 0);
            effect(p, MobEffects.NIGHT_VISION, 600, 0);
            effect(p, MobEffects.DAMAGE_RESISTANCE, 400, 1);
            burst(level, mid(p), ParticleTypes.END_ROD, 60, 2);
            sound(level, p, SoundEvents.BEACON_ACTIVATE, 1.2f);
            return true;
        });

        reg("stardust", CONSTELLATION, 55, 400, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 14)) {
                hurt(p, e, 11);
                effect(e, MobEffects.LEVITATION, 60, 0);
            }
            burst(level, mid(p), ParticleTypes.END_ROD, 300, 14);
            sound(level, p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f);
            return true;
        });

        // ==================== КОСМОС ====================
        reg("void_bolt", COSMOS, 25, 50, (level, p) -> {
            LivingEntity t = target(p, 25);
            if (t == null) return false;
            hurt(p, t, 8);
            effect(t, MobEffects.LEVITATION, 80, 1);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.REVERSE_PORTAL);
            sound(level, t, SoundEvents.ENDERMAN_TELEPORT, 1.0f);
            return true;
        });

        reg("gravity_well", COSMOS, 35, 100, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            for (LivingEntity e : around(level, p, t.position(), 8)) {
                pullTo(e, mid(t), 1.5);
            }
            ring(level, t.position(), 8, ParticleTypes.PORTAL);
            sound(level, t, SoundEvents.ENDER_EYE_LAUNCH, 0.8f);
            return true;
        });

        reg("cosmic_ray", COSMOS, 45, 150, (level, p) -> {
            for (LivingEntity e : beam(level, p, 30, 0.8)) {
                hurt(p, e, 12);
            }
            line(level, p.getEyePosition(), beamEnd(p, 30), ParticleTypes.END_ROD);
            line(level, p.getEyePosition(), beamEnd(p, 30), ParticleTypes.REVERSE_PORTAL);
            sound(level, p, SoundEvents.BEACON_POWER_SELECT, 0.8f);
            return true;
        });

        reg("nebula", COSMOS, 50, 300, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 10)) {
                hurt(p, e, 9);
                effect(e, MobEffects.CONFUSION, 200, 0);
                effect(e, MobEffects.LEVITATION, 100, 0);
            }
            burst(level, mid(p), ParticleTypes.PORTAL, 200, 10);
            sound(level, p, SoundEvents.PORTAL_AMBIENT, 0.7f);
            return true;
        });

        reg("supermassive", COSMOS, 80, 1200, (level, p) -> {
            LivingEntity t = target(p, 30);
            if (t == null) return false;
            for (LivingEntity e : around(level, p, t.position(), 15)) {
                hurt(p, e, 16);
                pullTo(e, mid(t), 2.5);
            }
            burst(level, mid(t), ParticleTypes.REVERSE_PORTAL, 400, 15);
            sound(level, t, SoundEvents.WITHER_SPAWN, 0.5f);
            return true;
        });
    }

    private StarsSpells() {}
}
