package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import static com.example.elementalmagic.magic.School.DECAY;
import static com.example.elementalmagic.magic.School.SMOKE;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class AshSpells {
    static void init() {
        // ==================== ТЛЕН ====================
        reg("ash_touch", DECAY, 15, 30, (level, p) -> {
            LivingEntity t = target(p, 18);
            if (t == null) return false;
            hurt(p, t, 4);
            effect(t, MobEffects.WEAKNESS, 100, 1);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.ASH);
            sound(level, t, SoundEvents.FIRE_EXTINGUISH, 1.4f);
            return true;
        });

        reg("smoldering_embers", DECAY, 20, 60, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 3);
            effect(t, MobEffects.WITHER, 120, 0);
            t.igniteForSeconds(6);
            burst(level, mid(t), ParticleTypes.ASH, 30, 1.5);
            sound(level, t, SoundEvents.LAVA_EXTINGUISH, 1.0f);
            return true;
        });

        reg("erosion", DECAY, 25, 80, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 6)) {
                hurt(p, e, 5);
                effect(e, MobEffects.WEAKNESS, 140, 1);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 140, 0);
            }
            ring(level, p.position(), 6, ParticleTypes.ASH);
            sound(level, p, SoundEvents.FIRE_EXTINGUISH, 0.8f);
            return true;
        });

        reg("withering_curse", DECAY, 35, 120, (level, p) -> {
            LivingEntity t = target(p, 25);
            if (t == null) return false;
            hurt(p, t, 6);
            effect(t, MobEffects.WITHER, 200, 1);
            effect(t, MobEffects.WEAKNESS, 200, 2);
            burst(level, mid(t), ParticleTypes.ASH, 60, 2);
            sound(level, t, SoundEvents.WITHER_HURT, 1.2f);
            return true;
        });

        reg("cinder_storm", DECAY, 55, 400, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 14)) {
                hurt(p, e, 9);
                effect(e, MobEffects.WITHER, 240, 1);
                e.igniteForSeconds(8);
            }
            burst(level, mid(p), ParticleTypes.ASH, 300, 14);
            burst(level, mid(p), ParticleTypes.LARGE_SMOKE, 200, 14);
            sound(level, p, SoundEvents.FIRE_EXTINGUISH, 0.5f);
            return true;
        });

        // ==================== ДЫМ ====================
        reg("smoke_veil", SMOKE, 20, 100, (level, p) -> {
            effect(p, MobEffects.INVISIBILITY, 200, 0);
            for (LivingEntity e : enemies(level, p, 8)) {
                effect(e, MobEffects.BLINDNESS, 100, 0);
            }
            burst(level, mid(p), ParticleTypes.LARGE_SMOKE, 80, 4);
            sound(level, p, SoundEvents.FIRE_EXTINGUISH, 1.0f);
            return true;
        });

        reg("suffocate", SMOKE, 25, 80, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 6);
            effect(t, MobEffects.BLINDNESS, 120, 0);
            effect(t, MobEffects.CONFUSION, 120, 0);
            burst(level, mid(t), ParticleTypes.LARGE_SMOKE, 50, 2);
            sound(level, t, SoundEvents.FIRE_EXTINGUISH, 0.9f);
            return true;
        });

        reg("choking_cloud", SMOKE, 30, 150, (level, p) -> {
            for (LivingEntity e : around(level, p, 8)) {
                hurt(p, e, 4);
                effect(e, MobEffects.BLINDNESS, 140, 0);
                effect(e, MobEffects.WEAKNESS, 140, 1);
            }
            burst(level, mid(p), ParticleTypes.LARGE_SMOKE, 200, 8);
            sound(level, p, SoundEvents.FIRE_EXTINGUISH, 0.7f);
            return true;
        });

        reg("phantom_step", SMOKE, 25, 80, (level, p) -> {
            teleport(p, teleportPos(p, 15));
            effect(p, MobEffects.INVISIBILITY, 80, 0);
            burst(level, mid(p), ParticleTypes.LARGE_SMOKE, 40, 2);
            sound(level, p, SoundEvents.ENDERMAN_TELEPORT, 1.0f);
            return true;
        });

        reg("ash_tempest", SMOKE, 65, 800, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 16)) {
                hurt(p, e, 8);
                effect(e, MobEffects.BLINDNESS, 240, 0);
                effect(e, MobEffects.WITHER, 200, 1);
                effect(e, MobEffects.WEAKNESS, 200, 1);
            }
            burst(level, mid(p), ParticleTypes.ASH, 400, 16);
            burst(level, mid(p), ParticleTypes.LARGE_SMOKE, 400, 16);
            sound(level, p, SoundEvents.WITHER_AMBIENT, 0.6f);
            return true;
        });
    }

    private AshSpells() {}
}
