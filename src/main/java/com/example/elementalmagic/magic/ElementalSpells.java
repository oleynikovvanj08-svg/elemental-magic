package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import static com.example.elementalmagic.magic.School.FROST;
import static com.example.elementalmagic.magic.School.FUSION;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class ElementalSpells {
    static void init() {
        // ==================== ЛЁД ====================
        reg("ice_shard", FROST, 15, 30, (level, p) -> {
            LivingEntity t = target(p, 22);
            if (t == null) return false;
            hurt(p, t, 5);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.SNOWFLAKE);
            sound(level, t, SoundEvents.GLASS_BREAK, 1.4f);
            return true;
        });

        reg("frost_lance", FROST, 25, 60, (level, p) -> {
            for (LivingEntity e : beam(level, p, 18, 0.8)) {
                hurt(p, e, 7);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 100, 2);
            }
            line(level, p.getEyePosition(), beamEnd(p, 18), ParticleTypes.SNOWFLAKE);
            sound(level, p, SoundEvents.GLASS_BREAK, 1.0f);
            return true;
        });

        reg("ice_prison", FROST, 30, 120, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 4);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 200, 4);
            effect(t, MobEffects.WEAKNESS, 200, 1);
            ring(level, t.position(), 2, ParticleTypes.SNOWFLAKE);
            sound(level, t, SoundEvents.GLASS_PLACE, 0.9f);
            return true;
        });

        reg("blizzard", FROST, 45, 200, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 12)) {
                hurt(p, e, 8);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 160, 2);
            }
            burst(level, mid(p), ParticleTypes.SNOWFLAKE, 300, 12);
            sound(level, p, SoundEvents.POWDER_SNOW_BREAK, 0.7f);
            return true;
        });

        reg("absolute_zero", FROST, 65, 600, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 15)) {
                hurt(p, e, 12);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 300, 9);
                effect(e, MobEffects.WEAKNESS, 300, 2);
            }
            ring(level, p.position(), 15, ParticleTypes.SNOWFLAKE);
            burst(level, mid(p), ParticleTypes.SNOWFLAKE, 400, 15);
            sound(level, p, SoundEvents.GLASS_BREAK, 0.5f);
            return true;
        });

        // ==================== СЛИЯНИЕ ====================
        reg("steam_burst", FUSION, 20, 50, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 6)) {
                hurt(p, e, 5);
                pushFrom(e, p.position(), 1.0, 0.4);
            }
            burst(level, mid(p), ParticleTypes.CLOUD, 80, 6);
            sound(level, p, SoundEvents.LAVA_EXTINGUISH, 1.0f);
            return true;
        });

        reg("thunderstone", FUSION, 30, 80, (level, p) -> {
            LivingEntity t = target(p, 25);
            if (t == null) return false;
            for (LivingEntity e : around(level, p, t.position(), 4)) {
                hurt(p, e, 9);
            }
            lightning(level, p, mid(t));
            burst(level, mid(t), ParticleTypes.ELECTRIC_SPARK, 60, 4);
            sound(level, t, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f);
            return true;
        });

        reg("inferno_tide", FUSION, 35, 100, (level, p) -> {
            for (LivingEntity e : beam(level, p, 15, 1.5)) {
                hurt(p, e, 8);
                e.igniteForSeconds(6);
            }
            line(level, p.getEyePosition(), beamEnd(p, 15), ParticleTypes.FLAME);
            line(level, p.getEyePosition(), beamEnd(p, 15), ParticleTypes.DRIPPING_WATER);
            sound(level, p, SoundEvents.FIRE_AMBIENT, 0.9f);
            return true;
        });

        reg("prismatic_shield", FUSION, 40, 400, (level, p) -> {
            effect(p, MobEffects.DAMAGE_RESISTANCE, 400, 2);
            effect(p, MobEffects.FIRE_RESISTANCE, 400, 0);
            effect(p, MobEffects.REGENERATION, 200, 1);
            burst(level, mid(p), ParticleTypes.END_ROD, 60, 2);
            sound(level, p, SoundEvents.BEACON_POWER_SELECT, 1.2f);
            return true;
        });

        reg("elemental_storm", FUSION, 75, 900, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 16)) {
                hurt(p, e, 14);
                e.igniteForSeconds(8);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 140, 1);
                if (level.canSeeSky(e.blockPosition())) lightning(level, p, mid(e));
            }
            burst(level, mid(p), ParticleTypes.FLAME, 200, 16);
            burst(level, mid(p), ParticleTypes.SNOWFLAKE, 200, 16);
            burst(level, mid(p), ParticleTypes.ELECTRIC_SPARK, 200, 16);
            sound(level, p, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5f);
            return true;
        });
    }

    private ElementalSpells() {}
}
