package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.example.elementalmagic.magic.School.BLADE;
import static com.example.elementalmagic.magic.School.STEALTH;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class ShadowSpells {
    static void init() {
        // ==================== СКРЫТНОСТЬ ====================
        reg("shadow_step", STEALTH, 25, 60, (level, p) -> {
            LivingEntity t = target(p, 15);
            Vec3 behind = t != null ? mid(t).add(t.getLookAngle().scale(-2)) : teleportPos(p, 15);
            teleport(p, behind);
            effect(p, MobEffects.INVISIBILITY, 60, 0);
            burst(level, mid(p), ParticleTypes.SMOKE, 40, 2);
            sound(level, p, SoundEvents.ENDERMAN_TELEPORT, 1.0f);
            return true;
        });

        reg("shadow_cloak", STEALTH, 30, 300, (level, p) -> {
            effect(p, MobEffects.INVISIBILITY, 400, 0);
            effect(p, MobEffects.NIGHT_VISION, 400, 0);
            effect(p, MobEffects.MOVEMENT_SPEED, 400, 1);
            burst(level, mid(p), ParticleTypes.SMOKE, 60, 2);
            sound(level, p, SoundEvents.ENDERMAN_TELEPORT, 0.9f);
            return true;
        });

        reg("shadow_walk", STEALTH, 20, 40, (level, p) -> {
            teleport(p, teleportPos(p, 18));
            effect(p, MobEffects.INVISIBILITY, 40, 0);
            burst(level, mid(p), ParticleTypes.SMOKE, 30, 1.5);
            sound(level, p, SoundEvents.ENDERMAN_TELEPORT, 1.2f);
            return true;
        });

        reg("misdirection", STEALTH, 35, 200, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 12)) {
                effect(e, MobEffects.BLINDNESS, 160, 0);
                effect(e, MobEffects.CONFUSION, 160, 0);
            }
            effect(p, MobEffects.INVISIBILITY, 100, 0);
            burst(level, mid(p), ParticleTypes.LARGE_SMOKE, 120, 6);
            sound(level, p, SoundEvents.ENDERMAN_SCREAM, 0.8f);
            return true;
        });

        reg("eclipse", STEALTH, 65, 800, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 20)) {
                effect(e, MobEffects.BLINDNESS, 300, 0);
                effect(e, MobEffects.WEAKNESS, 300, 2);
            }
            effect(p, MobEffects.INVISIBILITY, 300, 0);
            burst(level, mid(p), ParticleTypes.LARGE_SMOKE, 400, 20);
            sound(level, p, SoundEvents.WITHER_AMBIENT, 0.5f);
            return true;
        });

        // ==================== ТЕНЕВОЙ КЛИНОК ====================
        reg("dark_strike", BLADE, 20, 30, (level, p) -> {
            LivingEntity t = target(p, 18);
            if (t == null) return false;
            hurt(p, t, 8);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.SMOKE);
            sound(level, t, SoundEvents.PLAYER_ATTACK_CRIT, 1.3f);
            return true;
        });

        reg("shadow_slash", BLADE, 30, 60, (level, p) -> {
            for (LivingEntity e : beam(level, p, 12, 1.2)) {
                hurt(p, e, 9);
            }
            line(level, p.getEyePosition(), beamEnd(p, 12), ParticleTypes.SMOKE);
            sound(level, p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f);
            return true;
        });

        reg("backstab", BLADE, 35, 80, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            double dot = t.getLookAngle().dot(p.position().subtract(t.position()).normalize());
            hurt(p, t, dot > 0.5 ? 18 : 10);
            burst(level, mid(t), ParticleTypes.SMOKE, 30, 1.5);
            sound(level, t, SoundEvents.PLAYER_ATTACK_CRIT, 0.8f);
            return true;
        });

        reg("shadow_clone", BLADE, 40, 300, (level, p) -> {
            effect(p, MobEffects.DAMAGE_BOOST, 300, 2);
            effect(p, MobEffects.MOVEMENT_SPEED, 300, 2);
            effect(p, MobEffects.INVISIBILITY, 100, 0);
            burst(level, mid(p), ParticleTypes.SMOKE, 80, 3);
            sound(level, p, SoundEvents.ENDERMAN_TELEPORT, 0.9f);
            return true;
        });

        reg("void_blade", BLADE, 70, 600, (level, p) -> {
            for (LivingEntity e : beam(level, p, 25, 1.5)) {
                hurt(p, e, 16);
            }
            line(level, p.getEyePosition(), beamEnd(p, 25), ParticleTypes.REVERSE_PORTAL);
            burst(level, beamEnd(p, 25), ParticleTypes.SMOKE, 100, 4);
            sound(level, p, SoundEvents.ENDERMAN_SCREAM, 0.6f);
            return true;
        });
    }

    private ShadowSpells() {}
}
