package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

import java.util.List;

public final class GravitySpells {

    static void init() {
        // 1. Левитация: цель взмывает вверх (автоуровень 1)
        Spells.reg("levitate", School.GRAVITY, 20, 200, (level, player) -> {
            LivingEntity target = nearest(player, 10);
            if (target == null) return false;
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 0));
            level.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getEyeY(), target.getZ(), 25, 0.4, 0.8, 0.4, 0.02);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.5f);
            return true;
        });

        // 2. Гравитационный колодец: стягивает врагов к точке взгляда (автоуровень 2)
        Spells.reg("gravity_well", School.GRAVITY, 35, 300, (level, player) -> {
            Vec3 point = lookPoint(player, 8);
            List<Entity> targets = level.getEntities(player,
                    new AABB(point, point).inflate(6), e -> e instanceof LivingEntity && e.isAlive());
            if (targets.isEmpty()) return false;
            for (Entity e : targets) {
                Vec3 pull = point.subtract(e.position()).normalize().scale(0.8);
                e.setDeltaMovement(e.getDeltaMovement().add(pull));
                e.hurt(player.damageSources().indirectMagic(player, player), 2.0f);
            }
            level.sendParticles(ParticleTypes.ENCHANT, point.x, point.y, point.z, 80, 2, 2, 2, 0.3);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.6f);
            return true;
        });

        // 3. Шаг луны: мощный прыжок и мягкое падение (автоуровень 3)
        Spells.reg("moon_step", School.GRAVITY, 25, 600, (level, player) -> {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 8));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0));
            player.setDeltaMovement(player.getDeltaMovement().add(0, 1.5, 0));
            level.sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY(), player.getZ(), 30, 0.5, 0.2, 0.5, 0.05);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.8f);
            return
