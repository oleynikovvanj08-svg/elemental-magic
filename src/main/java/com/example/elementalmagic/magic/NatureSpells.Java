package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import static com.example.elementalmagic.magic.School.BEAST;
import static com.example.elementalmagic.magic.School.FLORA;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class NatureSpells {
    static void init() {
        // ==================== ФЛОРА ====================
        reg("vine_grasp", FLORA, 15, 40, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 3);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 60, 2);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.SPORE_BLOSSOM_AIR);
            sound(level, t, SoundEvents.WEEPING_VINES_PLACE, 1.2f);
            return true;
        });

        reg("thorn_lash", FLORA, 20, 50, (level, p) -> {
            for (LivingEntity e : beam(level, p, 12, 1.0)) {
                hurt(p, e, 5);
                effect(e, MobEffects.POISON, 80, 0);
            }
            line(level, p.getEyePosition(), beamEnd(p, 12), ParticleTypes.SPORE_BLOSSOM_AIR);
            sound(level, p, SoundEvents.SWEET_BERRY_BUSH_HURT_PLAYER, 0.9f);
            return true;
        });

        reg("healing_bloom", FLORA, 25, 200, (level, p) -> {
            for (LivingEntity e : friends(level, p, 8)) {
                e.heal(6);
                cleanse(e);
            }
            burst(level, mid(p), ParticleTypes.SPORE_BLOSSOM_AIR, 100, 8);
            sound(level, p, SoundEvents.BONEMEAL_USE, 1.0f);
            return true;
        });

        reg("root_prison", FLORA, 30, 100, (level, p) -> {
            LivingEntity t = target(p, 25);
            if (t == null) return false;
            hurt(p, t, 4);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 140, 4);
            effect(t, MobEffects.WEAKNESS, 140, 1);
            ring(level, t.position(), 2, ParticleTypes.SPORE_BLOSSOM_AIR);
            sound(level, t, SoundEvents.WEEPING_VINES_BREAK, 0.8f);
            return true;
        });

        reg("natures_wrath", FLORA, 50, 300, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 12)) {
                hurt(p, e, 10);
                effect(e, MobEffects.POISON, 200, 1);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 100, 1);
            }
            burst(level, mid(p), ParticleTypes.SPORE_BLOSSOM_AIR, 200, 12);
            sound(level, p, SoundEvents.WEEPING_VINES_PLACE, 0.6f);
            return true;
        });

        // ==================== ЗВЕРИ ====================
        reg("wolfs_howl", BEAST, 20, 100, (level, p) -> {
            effect(p, MobEffects.DAMAGE_BOOST, 200, 1);
            effect(p, MobEffects.MOVEMENT_SPEED, 200, 1);
            for (LivingEntity e : enemies(level, p, 10)) {
                effect(e, MobEffects.WEAKNESS, 100, 0);
            }
            burst(level, mid(p), ParticleTypes.NOTE, 30, 3);
            sound(level, p, SoundEvents.WOLF_HOWL, 1.0f);
            return true;
        });

        reg("bears_fury", BEAST, 25, 80, (level, p) -> {
            LivingEntity t = target(p, 15);
            if (t == null) return false;
            hurt(p, t, 12);
            pushFrom(t, p.position(), 1.2, 0.4);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.CRIT);
            sound(level, t, SoundEvents.POLAR_BEAR_WARNING, 0.8f);
            return true;
        });

        reg("eagle_dive", BEAST, 30, 120, (level, p) -> {
            push(p, p.getLookAngle().scale(2.5).add(0, 0.5, 0));
            p.resetFallDistance();
            effect(p, MobEffects.SLOW_FALLING, 80, 0);
            for (LivingEntity e : around(level, p, 3)) {
                hurt(p, e, 6);
            }
            burst(level, mid(p), ParticleTypes.CLOUD, 40, 2);
            sound(level, p, SoundEvents.PHANTOM_FLAP, 1.2f);
            return true;
        });

        reg("pack_instinct", BEAST, 35, 400, (level, p) -> {
            for (LivingEntity e : friends(level, p, 12)) {
                effect(e, MobEffects.DAMAGE_BOOST, 400, 1);
                effect(e, MobEffects.DAMAGE_RESISTANCE, 400, 0);
                effect(e, MobEffects.MOVEMENT_SPEED, 400, 0);
            }
            burst(level, mid(p), ParticleTypes.HAPPY_VILLAGER, 60, 4);
            sound(level, p, SoundEvents.WOLF_HOWL, 0.9f);
            return true;
        });

        reg("primal_rage", BEAST, 60, 600, (level, p) -> {
            effect(p, MobEffects.DAMAGE_BOOST, 400, 2);
            effect(p, MobEffects.MOVEMENT_SPEED, 400, 2);
            effect(p, MobEffects.DAMAGE_RESISTANCE, 400, 1);
            effect(p, MobEffects.REGENERATION, 200, 1);
            burst(level, mid(p), ParticleTypes.ANGRY_VILLAGER, 80, 3);
            sound(level, p, SoundEvents.RAVAGER_ROAR, 1.0f);
            return true;
        });
    }

    private NatureSpells() {}
}
