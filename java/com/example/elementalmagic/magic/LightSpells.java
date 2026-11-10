package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import static com.example.elementalmagic.magic.School.HOLY;
import static com.example.elementalmagic.magic.School.SOLAR;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class LightSpells {
    static void init() {
        // ==================== СВЯТОСТЬ ====================
        reg("minor_heal", HOLY, 10, 20, (level, p) -> {
            p.heal(4);
            burst(level, mid(p), ParticleTypes.HEART, 5, 0.5);
            return true;
        });

        reg("holy_heal", HOLY, 25, 80, (level, p) -> {
            p.heal(8);
            effect(p, MobEffects.REGENERATION, 100, 0);
            burst(level, mid(p), ParticleTypes.HEART, 10, 1);
            return true;
        });

        reg("blessing", HOLY, 30, 300, (level, p) -> {
            effect(p, MobEffects.DAMAGE_BOOST, 600, 0);
            effect(p, MobEffects.DAMAGE_RESISTANCE, 600, 0);
            effect(p, MobEffects.LUCK, 600, 0);
            burst(level, mid(p), ParticleTypes.ENCHANT, 60, 2);
            return true;
        });

        reg("smite", HOLY, 30, 60, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, isUndead(t) ? 14 : 6);
            line(level, t.position().add(0, 6, 0), mid(t), ParticleTypes.END_ROD);
            sound(level, t, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f);
            return true;
        });

        reg("purify", HOLY, 30, 200, (level, p) -> {
            cleanse(p);
            p.setAirSupply(p.getMaxAirSupply());
            burst(level, mid(p), ParticleTypes.HAPPY_VILLAGER, 40, 1.5);
            return true;
        });

        reg("holy_ground", HOLY, 40, 300, (level, p) -> {
            for (LivingEntity e : friends(level, p, 6)) effect(e, MobEffects.REGENERATION, 200, 1);
            ring(level, p.position(), 6, ParticleTypes.END_ROD);
            return true;
        });

        reg("radiance", HOLY, 45, 150, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 10)) hurt(p, e, isUndead(e) ? 12 : 6);
            for (LivingEntity e : friends(level, p, 10)) e.heal(4);
            burst(level, mid(p), ParticleTypes.END_ROD, 200, 10);
            return true;
        });

        reg("guardian_angel", HOLY, 50, 600, (level, p) -> {
            effect(p, MobEffects.DAMAGE_RESISTANCE, 200, 2);
            effect(p, MobEffects.ABSORPTION, 600, 1);
            effect(p, MobEffects.REGENERATION, 200, 0);
            sound(level, p, SoundEvents.TOTEM_USE, 1.2f);
            return true;
        });

        reg("divine_restoration", HOLY, 70, 1200, (level, p) -> {
            p.setHealth(p.getMaxHealth());
            cleanse(p);
            p.getFoodData().setFoodLevel(20);
            burst(level, mid(p), ParticleTypes.TOTEM_OF_UNDYING, 100, 2);
            return true;
        });

        reg("divine_intervention", HOLY, 80, 800, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 15)) hurt(p, e, isUndead(e) ? 20 : 10);
            for (LivingEntity e : friends(level, p, 15)) {
                e.heal(10);
                effect(e, MobEffects.REGENERATION, 100, 1);
            }
            burst(level, mid(p), ParticleTypes.END_ROD, 400, 15);
            sound(level, p, SoundEvents.TOTEM_USE, 0.8f);
            return true;
        });

        // ==================== СОЛНЦЕ ====================
        reg("sunbeam", SOLAR, 15, 30, (level, p) -> {
            for (LivingEntity e : beam(level, p, 25, 0.4)) {
                hurt(p, e, isUndead(e) ? 10 : 5);
                e.igniteForSeconds(3);
            }
            line(level, p.getEyePosition(), beamEnd(p, 25), ParticleTypes.END_ROD);
            return true;
        });

        reg("blinding_light", SOLAR, 25, 100, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 10)) {
                effect(e, MobEffects.BLINDNESS, 100, 0);
                effect(e, MobEffects.GLOWING, 200, 0);
            }
            burst(level, mid(p), ParticleTypes.END_ROD, 150, 6);
            return true;
        });

        reg("photosynthesis", SOLAR, 20, 200, (level, p) -> {
            boolean sun = level.isDay() && level.canSeeSky(p.blockPosition());
            p.heal(sun ? 6 : 2);
            if (sun) p.getFoodData().setFoodLevel(Math.min(20, p.getFoodData().getFoodLevel() + 4));
            burst(level, mid(p), ParticleTypes.HAPPY_VILLAGER, 30, 1);
            return true;
        });

        reg("solar_shield", SOLAR, 30, 300, (level, p) -> {
            effect(p, MobEffects.FIRE_RESISTANCE, 600, 0);
            effect(p, MobEffects.DAMAGE_RESISTANCE, 200, 0);
            ring(level, p.position(), 2, ParticleTypes.FLAME);
            return true;
        });

        reg("solar_flare", SOLAR, 35, 100, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 8)) {
                hurt(p, e, isUndead(e) ? 12 : 6);
                effect(e, MobEffects.GLOWING, 200, 0);
            }
            burst(level, mid(p), ParticleTypes.END_ROD, 120, 6);
            return true;
        });

        reg("sunrise", SOLAR, 35, 150, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 12)) hurt(p, e, 5);
            for (LivingEntity e : friends(level, p, 12)) effect(e, MobEffects.REGENERATION, 160, 0);
            ring(level, p.position(), 12, ParticleTypes.END_ROD);
            return true;
        });

        reg("solar_lance", SOLAR, 40, 80, (level, p) -> {
            for (LivingEntity e : beam(level, p, 30, 0.6)) {
                hurt(p, e, isUndead(e) ? 16 : 8);
                e.igniteForSeconds(5);
            }
            line(level, p.getEyePosition(), beamEnd(p, 30), ParticleTypes.END_ROD);
            sound(level, p, SoundEvents.BLAZE_SHOOT, 1.5f);
            return true;
        });

        reg("daylight", SOLAR, 50, 1200, (level, p) -> {
            setTimeOfDay(level, 1000);
            burst(level, mid(p), ParticleTypes.END_ROD, 100, 5);
            return true;
        });

        reg("solar_eruption", SOLAR, 55, 200, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 10)) {
                hurt(p, e, 10);
                e.igniteForSeconds(8);
            }
            burst(level, mid(p), ParticleTypes.FLAME, 300, 10);
            return true;
        });

        reg("supernova", SOLAR, 90, 1200, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 15)) {
                hurt(p, e, isUndead(e) ? 24 : 14);
                e.igniteForSeconds(10);
                effect(e, MobEffects.BLINDNESS, 100, 0);
            }
            burst(level, mid(p), ParticleTypes.END_ROD, 400, 15);
            sound(level, p, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5f);
            return true;
        });
    }

    private LightSpells() {}
}