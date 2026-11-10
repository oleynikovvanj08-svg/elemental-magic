package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.example.elementalmagic.magic.School.MAGNETISM;
import static com.example.elementalmagic.magic.School.STORM;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class LightningSpells {
    static void init() {
        // ==================== БУРЯ ====================
        reg("static_shock", STORM, 10, 20, (level, p) -> {
            LivingEntity t = target(p, 15);
            if (t == null) return false;
            hurt(p, t, 4);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 40, 0);
            line(level, p.getEyePosition(), mid(t), ParticleTypes.ELECTRIC_SPARK);
            return true;
        });

        reg("lightning_bolt", STORM, 25, 50, (level, p) -> {
            LivingEntity t = target(p, 25);
            if (t == null) return false;
            lightning(level, p, t.position());
            return true;
        });

        reg("thunder_strike", STORM, 30, 60, (level, p) -> {
            lightning(level, p, beamEnd(p, 32));
            return true;
        });

        reg("chain_lightning", STORM, 40, 80, (level, p) -> {
            LivingEntity first = target(p, 20);
            if (first == null) return false;
            List<LivingEntity> chain = new ArrayList<>();
            chain.add(first);
            for (int i = 0; i < 4; i++) {
                LivingEntity last = chain.get(chain.size() - 1);
                LivingEntity next = enemies(level, p, last.position(), 8).stream()
                        .filter(e -> !chain.contains(e)).findFirst().orElse(null);
                if (next == null) break;
                chain.add(next);
            }
            Vec3 from = p.getEyePosition();
            for (int i = 0; i < chain.size(); i++) {
                LivingEntity e = chain.get(i);
                hurt(p, e, 7 - i);
                line(level, from, mid(e), ParticleTypes.ELECTRIC_SPARK);
                from = mid(e);
            }
            sound(level, p, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.5f);
            return true;
        });

        reg("thunderclap", STORM, 25, 60, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 5)) {
                hurt(p, e, 4);
                effect(e, MobEffects.WEAKNESS, 100, 0);
                pushFrom(e, p.position(), 1.0, 0.4);
            }
            ring(level, p.position(), 5, ParticleTypes.ELECTRIC_SPARK);
            sound(level, p, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.4f);
            return true;
        });

        reg("storm_shield", STORM, 35, 200, (level, p) -> {
            effect(p, MobEffects.DAMAGE_RESISTANCE, 200, 0);
            for (LivingEntity e : enemies(level, p, 3)) {
                hurt(p, e, 2);
                pushFrom(e, p.position(), 0.8, 0.3);
            }
            ring(level, p.position(), 2, ParticleTypes.ELECTRIC_SPARK);
            return true;
        });

        reg("overcharge", STORM, 30, 240, (level, p) -> {
            effect(p, MobEffects.MOVEMENT_SPEED, 200, 1);
            effect(p, MobEffects.DAMAGE_BOOST, 200, 0);
            burst(level, mid(p), ParticleTypes.ELECTRIC_SPARK, 80, 2);
            return true;
        });

        reg("lightning_spear", STORM, 35, 70, (level, p) -> {
            for (LivingEntity e : beam(level, p, 25, 0.5)) hurt(p, e, 8);
            line(level, p.getEyePosition(), beamEnd(p, 25), ParticleTypes.ELECTRIC_SPARK);
            sound(level, p, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.8f);
            return true;
        });

        reg("storm_call", STORM, 50, 300, (level, p) -> {
            List<LivingEntity> list = enemies(level, p, 14);
            if (list.isEmpty()) return false;
            for (int i = 0; i < Math.min(5, list.size()); i++) lightning(level, p, list.get(i).position());
            return true;
        });

        reg("tempest", STORM, 60, 400, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 12)) {
                hurt(p, e, 8);
                push(e, e.getDeltaMovement().add(0, 1.2, 0));
            }
            burst(level, mid(p), ParticleTypes.ELECTRIC_SPARK, 300, 12);
            sound(level, p, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.7f);
            return true;
        });

        // ==================== МАГНЕТИЗМ ====================
        reg("iron_attraction", MAGNETISM, 10, 20, (level, p) -> {
            List<Entity> loot = level.getEntities(p, p.getBoundingBox().inflate(16),
                    e -> e instanceof ItemEntity || e instanceof ExperienceOrb);
            if (loot.isEmpty()) return false;
            for (Entity e : loot) e.teleportTo(p.getX(), p.getY(), p.getZ());
            return true;
        });

        reg("magnetic_pull", MAGNETISM, 20, 40, (level, p) -> {
            for (LivingEntity e : around(level, p, 10)) pullTo(e, p.position(), 0.9);
            ring(level, p.position(), 3, ParticleTypes.ELECTRIC_SPARK);
            return true;
        });

        reg("magnetic_push", MAGNETISM, 20, 40, (level, p) -> {
            for (LivingEntity e : around(level, p, 8)) pushFrom(e, p.position(), 1.4, 0.4);
            ring(level, p.position(), 4, ParticleTypes.ELECTRIC_SPARK);
            return true;
        });

        reg("polarity_shift", MAGNETISM, 30, 80, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            for (LivingEntity e : around(level, p, t.position(), 7)) {
                if (e != t) pullTo(e, t.position(), 1.2);
            }
            hurt(p, t, 3);
            burst(level, mid(t), ParticleTypes.ELECTRIC_SPARK, 40, 2);
            return true;
        });

        reg("magnetic_levitation", MAGNETISM, 25, 100, (level, p) -> {
            effect(p, MobEffects.LEVITATION, 60, 1);
            effect(p, MobEffects.SLOW_FALLING, 200, 0);
            return true;
        });

        reg("magnetic_field", MAGNETISM, 40, 300, (level, p) -> {
            effect(p, MobEffects.DAMAGE_RESISTANCE, 300, 1);
            // Отражает летящие в игрока снаряды (стрелы, огненные шары)
            for (Projectile pr : level.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(8),
                    pr -> pr.getOwner() != p)) {
                push(pr, pr.getDeltaMovement().scale(-1));
            }
            ring(level, p.position(), 3, ParticleTypes.ELECTRIC_SPARK);
            return true;
        });

        reg("metal_storm", MAGNETISM, 45, 100, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 8)) hurt(p, e, 6);
            burst(level, mid(p), ParticleTypes.CRIT, 200, 8);
            return true;
        });

        reg("magnetic_crush", MAGNETISM, 50, 150, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 12);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 100, 4);
            burst(level, mid(t), ParticleTypes.CRIT, 80, 1.5);
            return true;
        });

        reg("railgun", MAGNETISM, 55, 120, (level, p) -> {
            for (LivingEntity e : beam(level, p, 40, 0.4)) hurt(p, e, 15);
            line(level, p.getEyePosition(), beamEnd(p, 40), ParticleTypes.CRIT);
            sound(level, p, SoundEvents.LIGHTNING_BOLT_IMPACT, 2f);
            return true;
        });

        reg("emp_blast", MAGNETISM, 60, 300, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 10)) {
                hurt(p, e, 8);
                effect(e, MobEffects.WEAKNESS, 200, 1);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 200, 1);
                effect(e, MobEffects.GLOWING, 200, 0);
            }
            burst(level, mid(p), ParticleTypes.ELECTRIC_SPARK, 300, 10);
            sound(level, p, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.8f);
            return true;
        });
    }

    private LightningSpells() {}
}