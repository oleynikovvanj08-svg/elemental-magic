package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.example.elementalmagic.magic.School.SOUND;
import static com.example.elementalmagic.magic.School.WIND;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class AirSpells {
    static void init() {
        // ==================== ЗВУК ====================
        reg("echo_pulse", SOUND, 10, 100, (level, p) -> {
            List<LivingEntity> list = enemies(level, p, 24);
            if (list.isEmpty()) return false;
            for (LivingEntity e : list) {
                effect(e, MobEffects.GLOWING, 300, 0);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 0);
            }
            ring(level, p.position(), 4, ParticleTypes.NOTE);
            sound(level, p, SoundEvents.BELL_BLOCK, 1.5f);
            return true;
        });

        reg("shout", SOUND, 15, 40, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 5)) {
                hurt(p, e, 3);
                pushFrom(e, p.position(), 1.2, 0.3);
            }
            ring(level, p.position(), 5, ParticleTypes.NOTE);
            sound(level, p, SoundEvents.RAVAGER_ROAR, 1.6f);
            return true;
        });

        reg("sonic_wave", SOUND, 20, 40, (level, p) -> {
            for (LivingEntity e : beam(level, p, 20, 0.8)) {
                hurt(p, e, 6);
                pushFrom(e, p.position(), 0.8, 0.2);
            }
            line(level, p.getEyePosition(), beamEnd(p, 20), ParticleTypes.NOTE);
            sound(level, p, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f);
            return true;
        });

        reg("deafening_roar", SOUND, 30, 120, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 8)) {
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 120, 3);
                effect(e, MobEffects.WEAKNESS, 120, 1);
            }
            ring(level, p.position(), 8, ParticleTypes.NOTE);
            sound(level, p, SoundEvents.RAVAGER_ROAR, 0.8f);
            return true;
        });

        reg("resonance", SOUND, 30, 60, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 9);
            effect(t, MobEffects.WEAKNESS, 100, 0);
            burst(level, mid(t), ParticleTypes.NOTE, 30, 1.5);
            sound(level, t, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6f);
            return true;
        });

        reg("sound_barrier", SOUND, 35, 300, (level, p) -> {
            effect(p, MobEffects.DAMAGE_RESISTANCE, 300, 1);
            for (LivingEntity e : enemies(level, p, 4)) pushFrom(e, p.position(), 1.4, 0.4);
            ring(level, p.position(), 2, ParticleTypes.NOTE);
            sound(level, p, SoundEvents.BELL_BLOCK, 0.8f);
            return true;
        });

        reg("harmony", SOUND, 35, 300, (level, p) -> {
            for (LivingEntity e : friends(level, p, 8)) {
                e.heal(6);
                effect(e, MobEffects.REGENERATION, 200, 0);
            }
            burst(level, mid(p), ParticleTypes.NOTE, 40, 4);
            sound(level, p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f);
            return true;
        });

        reg("silence", SOUND, 40, 400, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 12)) {
                if (e instanceof Mob m) m.setTarget(null);
                effect(e, MobEffects.WEAKNESS, 200, 1);
            }
            effect(p, MobEffects.INVISIBILITY, 200, 0);
            burst(level, mid(p), ParticleTypes.SMOKE, 40, 2);
            return true;
        });

        reg("sonic_boom", SOUND, 50, 150, (level, p) -> {
            for (LivingEntity e : beam(level, p, 20, 0.6)) hurt(p, e, 14);
            Vec3 start = p.getEyePosition();
            Vec3 end = beamEnd(p, 20);
            Vec3 step = end.subtract(start);
            int n = Math.max(1, (int) step.length());
            for (int i = 1; i <= n; i++) {
                Vec3 pt = start.add(step.scale(i / (double) n));
                level.sendParticles(ParticleTypes.SONIC_BOOM, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
            }
            sound(level, p, SoundEvents.WARDEN_SONIC_BOOM, 1f);
            return true;
        });

        reg("grand_symphony", SOUND, 80, 1200, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 14)) {
                hurt(p, e, 12);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 200, 2);
            }
            for (LivingEntity e : friends(level, p, 14)) effect(e, MobEffects.REGENERATION, 200, 1);
            burst(level, mid(p), ParticleTypes.NOTE, 300, 14);
            sound(level, p, SoundEvents.WARDEN_SONIC_BOOM, 0.6f);
            return true;
        });

        // ==================== ВЕТЕР ====================
        reg("gust", WIND, 10, 20, (level, p) -> {
            for (LivingEntity e : beam(level, p, 15, 1.0)) {
                hurt(p, e, 2);
                pushFrom(e, p.position(), 1.8, 0.3);
            }
            line(level, p.getEyePosition(), beamEnd(p, 15), ParticleTypes.CLOUD);
            sound(level, p, SoundEvents.PHANTOM_FLAP, 1.2f);
            return true;
        });

        reg("updraft", WIND, 20, 80, (level, p) -> {
            Vec3 m = p.getDeltaMovement();
            push(p, new Vec3(m.x, 1.4, m.z));
            p.resetFallDistance();
            effect(p, MobEffects.SLOW_FALLING, 200, 0);
            burst(level, p.position(), ParticleTypes.CLOUD, 40, 1.5);
            sound(level, p, SoundEvents.ENDER_DRAGON_FLAP, 1.4f);
            return true;
        });

        reg("feather_fall", WIND, 15, 300, (level, p) -> {
            p.resetFallDistance();
            effect(p, MobEffects.SLOW_FALLING, 1200, 0);
            burst(level, mid(p), ParticleTypes.CLOUD, 20, 1);
            return true;
        });

        reg("wind_dash", WIND, 20, 50, (level, p) -> {
            Vec3 look = p.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            if (flat.lengthSqr() < 1.0E-4) return false;
            push(p, flat.normalize().scale(2.2).add(0, 0.2, 0));
            p.resetFallDistance();
            burst(level, mid(p), ParticleTypes.CLOUD, 40, 1.5);
            sound(level, p, SoundEvents.PHANTOM_FLAP, 1.6f);
            return true;
        });

        reg("tailwind", WIND, 25, 400, (level, p) -> {
            for (LivingEntity e : friends(level, p, 6)) effect(e, MobEffects.MOVEMENT_SPEED, 600, 1);
            ring(level, p.position(), 3, ParticleTypes.CLOUD);
            sound(level, p, SoundEvents.ELYTRA_FLYING, 1.5f);
            return true;
        });

        reg("air_blade", WIND, 30, 50, (level, p) -> {
            for (LivingEntity e : beam(level, p, 20, 0.4)) hurt(p, e, 8);
            line(level, p.getEyePosition(), beamEnd(p, 20), ParticleTypes.SWEEP_ATTACK);
            sound(level, p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f);
            return true;
        });

        reg("cyclone", WIND, 35, 120, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 7)) {
                hurt(p, e, 4);
                push(e, e.getDeltaMovement().add(0, 1.0, 0));
            }
            ring(level, p.position(), 3, ParticleTypes.CLOUD);
            ring(level, p.position().add(0, 1.5, 0), 5, ParticleTypes.CLOUD);
            sound(level, p, SoundEvents.ENDER_DRAGON_FLAP, 0.8f);
            return true;
        });

        reg("vacuum", WIND, 40, 100, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            for (LivingEntity e : around(level, p, t.position(), 7)) {
                if (e != t) pullTo(e, t.position(), 1.2);
                hurt(p, e, 4);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 80, 1);
            }
            ring(level, t.position(), 4, ParticleTypes.CLOUD);
            return true;
        });

        reg("sky_leap", WIND, 30, 600, (level, p) -> {
            effect(p, MobEffects.JUMP, 600, 2);
            effect(p, MobEffects.SLOW_FALLING, 600, 0);
            burst(level, p.position(), ParticleTypes.CLOUD, 30, 1);
            return true;
        });

        reg("wind_fury", WIND, 70, 600, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 12)) {
                hurt(p, e, 9);
                pushFrom(e, p.position(), 2.2, 0.8);
            }
            ring(level, p.position(), 12, ParticleTypes.CLOUD);
            burst(level, mid(p), ParticleTypes.CLOUD, 300, 12);
            sound(level, p, SoundEvents.ENDER_DRAGON_FLAP, 0.5f);
            return true;
        });
    }

    private AirSpells() {}
}