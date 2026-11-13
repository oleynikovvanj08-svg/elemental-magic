package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
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
        // 1. Левитация: цель взмывает вверх
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

        // 2. Гравитационный колодец: стягивает врагов к точке взгляда
        Spells.reg("gravity_well", School.GRAVITY, 35, 300, (level, player) -> {
            Vec3 point = lookPoint(player, 8);
            List<Entity> targets = level.getEntities(player,
                    new AABB(point, point).inflate(6), e -> e instanceof
