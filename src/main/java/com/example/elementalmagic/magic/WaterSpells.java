package com.example.elementalmagic.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static com.example.elementalmagic.magic.School.ICE;
import static com.example.elementalmagic.magic.School.TIDE;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class WaterSpells {

    /** Замораживает существо (синие сердца и медленный урон от холода). */
    private static void freeze(LivingEntity e, int ticks) {
        e.setTicksFrozen(Math.max(e.getTicksFrozen(), ticks));
    }

    static void init() {
        // ==================== ЛЁД ====================
        reg("frost_bolt", ICE, 10, 20, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 4);
            freeze(t, 200);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.SNOWFLAKE);
            return true;
        });

        reg("ice_shard", ICE, 20, 40, (level, p) -> {
            for (LivingEntity e : beam(level, p, 25, 0.4)) {
                hurt(p, e, 6);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 80, 1);
            }
            line(level, p.getEyePosition(), beamEnd(p, 25), ParticleTypes.SNOWFLAKE);
            sound(level, p, SoundEvents.GLASS_BREAK, 1.5f);
            return true;
        });

        reg("frost_nova", ICE, 25, 80, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 6)) {
                hurt(p, e, 4);
                freeze(e, 300);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 100, 3);
            }
            ring(level, p.position(), 6, ParticleTypes.SNOWFLAKE);
            sound(level, p, SoundEvents.PLAYER_HURT_FREEZE, 0.8f);
            return true;
        });

        reg("frozen_path", ICE, 15, 40, (level, p) -> {
            BlockPos center = p.blockPosition().below();
            int count = 0;
            for (BlockPos mpos : BlockPos.betweenClosed(center.offset(-4, 0, -4), center.offset(4, 0, 4))) {
                BlockPos pos = mpos.immutable();
                if (pos.distSqr(center) > 16) continue;
                BlockState st = level.getBlockState(pos);
                if (st.is(Blocks.WATER) && st.getFluidState().isSource()
                        && level.getBlockState(pos.above()).isAir()
                        && level.mayInteract(p, pos)) {
                    level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                    level.scheduleTick(pos, Blocks.FROSTED_ICE, 60 + level.random.nextInt(60));
                    count++;
                }
            }
            if (count == 0) return false;
            burst(level, p.position(), ParticleTypes.SNOWFLAKE, 60, 4);
            return true;
        });

        reg("ice_wall", ICE, 30, 100, (level, p) -> {
            boolean placed = EarthSpells.placeWall(level, p, Blocks.PACKED_ICE.defaultBlockState(), 3, 1, 3);
            if (placed) sound(level, p, SoundEvents.GLASS_PLACE, 0.8f);
            return placed;
        });

        reg("ice_armor", ICE, 30, 300, (level, p) -> {
            effect(p, MobEffects.DAMAGE_RESISTANCE, 400, 1);
            effect(p, MobEffects.ABSORPTION, 400, 0);
            p.clearFire();
            burst(level, mid(p), ParticleTypes.SNOWFLAKE, 50, 1.5);
            return true;
        });

        reg("glacial_spike", ICE, 35, 80, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 10);
            freeze(t, 200);
            push(t, t.getDeltaMovement().add(0, 0.9, 0));
            line(level, t.position(), t.position().add(0, 3, 0), ParticleTypes.SNOWFLAKE);
            sound(level, t, SoundEvents.GLASS_BREAK, 0.7f);
            return true;
        });

        reg("deep_freeze", ICE, 40, 150, (level, p) -> {
            LivingEntity t = target(p, 15);
            if (t == null) return false;
            hurt(p, t, 3);
            freeze(t, 600);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 200, 5);
            effect(t, MobEffects.WEAKNESS, 200, 1);
            effect(t, MobEffects.DIG_SLOWDOWN, 200, 1);
            burst(level, mid(t), ParticleTypes.SNOWFLAKE, 60, 1.5);
            return true;
        });

        reg("blizzard", ICE, 50, 300, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 12)) {
                hurt(p, e, 4);
                freeze(e, 300);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 100, 2);
            }
            burst(level, mid(p).add(0, 2, 0), ParticleTypes.SNOWFLAKE, 300, 12);
            sound(level, p, SoundEvents.PLAYER_HURT_FREEZE, 0.5f);
            return true;
        });

        reg("absolute_zero", ICE, 80, 1200, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 15)) {
                hurt(p, e, 14);
                freeze(e, 600);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 200, 3);
            }
            burst(level, mid(p), ParticleTypes.SNOWFLAKE, 500, 15);
            sound(level, p, SoundEvents.GLASS_BREAK, 0.5f);
            return true;
        });

        // ==================== ПРИЛИВ ====================
        reg("water_jet", TIDE, 10, 20, (level, p) -> {
            for (LivingEntity e : beam(level, p, 15, 0.5)) {
                hurt(p, e, 4);
                e.clearFire();
                pushFrom(e, p.position(), 1.0, 0.2);
            }
            p.clearFire();
            line(level, p.getEyePosition(), beamEnd(p, 15), ParticleTypes.SPLASH);
            sound(level, p, SoundEvents.BUCKET_EMPTY, 1.2f);
            return true;
        });

        reg("healing_rain", TIDE, 25, 200, (level, p) -> {
            for (LivingEntity e : friends(level, p, 6)) {
                e.heal(4);
                e.clearFire();
            }
            burst(level, p.position().add(0, 3, 0), ParticleTypes.FALLING_WATER, 150, 6);
            return true;
        });

        reg("tidal_push", TIDE, 20, 60, (level, p) -> {
            for (LivingEntity e : around(level, p, 8)) pushFrom(e, p.position(), 1.6, 0.3);
            ring(level, p.position(), 4, ParticleTypes.SPLASH);
            sound(level, p, SoundEvents.GENERIC_SPLASH, 1f);
            return true;
        });

        reg("aqua_breath", TIDE, 20, 600, (level, p) -> {
            effect(p, MobEffects.WATER_BREATHING, 2400, 0);
            effect(p, MobEffects.DOLPHINS_GRACE, 2400, 0);
            effect(p, MobEffects.NIGHT_VISION, 2400, 0);
            burst(level, mid(p), ParticleTypes.BUBBLE_POP, 40, 1);
            return true;
        });

        reg("whirlpool", TIDE, 35, 100, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            for (LivingEntity e : around(level, p, t.position(), 6)) {
                if (e != t) pullTo(e, t.position(), 1.0);
                hurt(p, e, 3);
            }
            ring(level, t.position(), 3, ParticleTypes.SPLASH);
            sound(level, t, SoundEvents.GENERIC_SPLASH, 0.7f);
            return true;
        });

        reg("riptide_dash", TIDE, 25, 60, (level, p) -> {
            push(p, p.getLookAngle().scale(2.0));
            p.resetFallDistance();
            effect(p, MobEffects.SLOW_FALLING, 60, 0);
            burst(level, mid(p), ParticleTypes.SPLASH, 60, 1.5);
            sound(level, p, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1f);
            return true;
        });

        reg("water_bubble", TIDE, 30, 80, (level, p) -> {
            LivingEntity t = target(p, 15);
            if (t == null) return false;
            hurt(p, t, 7);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 80, 2);
            burst(level, mid(t), ParticleTypes.BUBBLE_POP, 60, 1.5);
            return true;
        });

        reg("tidal_wave", TIDE, 40, 100, (level, p) -> {
            for (LivingEntity e : beam(level, p, 16, 1.5)) {
                hurt(p, e, 6);
                e.clearFire();
                push(e, p.getLookAngle().multiply(1.5, 0, 1.5).add(0, 0.4, 0));
            }
            line(level, p.getEyePosition(), beamEnd(p, 16), ParticleTypes.SPLASH);
            sound(level, p, SoundEvents.GENERIC_SPLASH, 0.6f);
            return true;
        });

        reg("tsunami", TIDE, 60, 400, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 12)) {
                hurt(p, e, 10);
                e.clearFire();
                pushFrom(e, p.position(), 2.0, 0.6);
            }
            ring(level, p.position(), 12, ParticleTypes.SPLASH);
            burst(level, mid(p), ParticleTypes.FALLING_WATER, 300, 12);
            sound(level, p, SoundEvents.GENERIC_SPLASH, 0.4f);
            return true;
        });

        reg("ocean_blessing", TIDE, 60, 1200, (level, p) -> {
            effect(p, MobEffects.CONDUIT_POWER, 1200, 0);
            effect(p, MobEffects.DOLPHINS_GRACE, 1200, 0);
            effect(p, MobEffects.REGENERATION, 200, 1);
            cleanse(p);
            burst(level, mid(p), ParticleTypes.BUBBLE_POP, 100, 2);
            return true;
        });
    }

    private WaterSpells() {}
}