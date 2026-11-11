package com.example.elementalmagic.magic;

import com.example.elementalmagic.PositionHistory;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static com.example.elementalmagic.magic.School.ACCELERATION;
import static com.example.elementalmagic.magic.School.STASIS;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class TimeSpells {
    static void init() {
        // ==================== УСКОРЕНИЕ ====================
        reg("haste", ACCELERATION, 20, 200, (level, p) -> {
            effect(p, MobEffects.DIG_SPEED, 400, 2);
            effect(p, MobEffects.MOVEMENT_SPEED, 400, 1);
            burst(level, mid(p), ParticleTypes.END_ROD, 40, 2);
            sound(level, p, SoundEvents.BEACON_POWER_SELECT, 1.3f);
            return true;
        });

        reg("rapid_strikes", ACCELERATION, 30, 100, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            for (int i = 0; i < 5; i++) hurt(p, t, 3);
            burst(level, mid(t), ParticleTypes.CRIT, 30, 1.5);
            sound(level, t, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f);
            return true;
        });

        reg("time_warp", ACCELERATION, 25, 80, (level, p) -> {
            teleport(p, teleportPos(p, 20));
            effect(p, MobEffects.MOVEMENT_SPEED, 100, 2);
            burst(level, mid(p), ParticleTypes.REVERSE_PORTAL, 60, 2);
            sound(level, p, SoundEvents.ENDERMAN_TELEPORT, 0.9f);
            return true;
        });

        reg("sunrise", ACCELERATION, 10, 1200, (level, p) -> {
            setTimeOfDay(level, 1000);
            burst(level, mid(p), ParticleTypes.END_ROD, 80, 4);
            sound(level, p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f);
            return true;
        });

        reg("temporal_surge", ACCELERATION, 60, 600, (level, p) -> {
            effect(p, MobEffects.DIG_SPEED, 600, 4);
            effect(p, MobEffects.MOVEMENT_SPEED, 600, 3);
            effect(p, MobEffects.DAMAGE_BOOST, 600, 2);
            burst(level, mid(p), ParticleTypes.END_ROD, 100, 3);
            sound(level, p, SoundEvents.BEACON_POWER_SELECT, 0.8f);
            return true;
        });

        // ==================== ЗАСТЫВАНИЕ ====================
        reg("slow", STASIS, 20, 60, (level, p) -> {
            LivingEntity t = target(p, 22);
            if (t == null) return false;
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 120, 2);
            effect(t, MobEffects.DIG_SLOWDOWN, 120, 2);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.REVERSE_PORTAL);
            sound(level, t, SoundEvents.ELDER_GUARDIAN_CURSE, 1.4f);
            return true;
        });

        reg("stasis_field", STASIS, 35, 150, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 8)) {
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 200, 4);
                effect(e, MobEffects.WEAKNESS, 200, 1);
            }
            ring(level, p.position(), 8, ParticleTypes.REVERSE_PORTAL);
            sound(level, p, SoundEvents.ELDER_GUARDIAN_CURSE, 1.0f);
            return true;
        });

        reg("rewind", STASIS, 30, 200, (level, p) -> {
            Vec3 past = PositionHistory.get(p, 100);
            if (past == null) return false;
            teleport(p, past);
            p.heal(6);
            burst(level, mid(p), ParticleTypes.REVERSE_PORTAL, 80, 2);
            sound(level, p, SoundEvents.ENDERMAN_TELEPORT, 0.7f);
            return true;
        });

        reg("nightfall", STASIS, 10, 1200, (level, p) -> {
            setTimeOfDay(level, 13000);
            burst(level, mid(p), ParticleTypes.REVERSE_PORTAL, 80, 4);
            sound(level, p, SoundEvents.WITHER_SPAWN, 1.2f);
            return true;
        });

        reg("time_stop", STASIS, 70, 1200, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 18)) {
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 300, 9);
                effect(e, MobEffects.DIG_SLOWDOWN, 300, 9);
                effect(e, MobEffects.WEAKNESS, 300, 4);
            }
            burst(level, mid(p), ParticleTypes.REVERSE_PORTAL, 400, 18);
            sound(level, p, SoundEvents.ELDER_GUARDIAN_CURSE, 0.5f);
            return true;
        });
    }

    private TimeSpells() {}
}
