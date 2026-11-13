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

public final class PoisonSpells {

    static void init() {
        // 1. Ядовитый выстрел
        Spells.reg("poison_bolt", School.POISON, 10, 40, (level, player) -> {
            LivingEntity target = rayTarget(player, 15);
            if (target == null) return false;
            target.hurt(player.damageSources().indirectMagic(player, player), 3.0f);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
            level.sendParticles(ParticleTypes.SMOKE,
                    target.getX(), target.getEyeY(), target.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.WITCH_CELEBRATE, SoundSource.PLAYERS, 1.0f, 0.8f);
            return true;
        });

        // 2. Токсичное облако в точке взгляда
        Spells.reg("toxic_cloud", School.POISON, 30, 300, (level, player) -> {
            Vec3 point = lookPoint(player, 10);
            List<Entity> targets = level.getEntities(player,
                    new AABB(point, point).inflate(4), e -> e instanceof LivingEntity && e.isAlive());
            if (targets.isEmpty()) return false;
            for (Entity e : targets) {
                LivingEntity living = (LivingEntity) e;
                living.hurt(player.damageSources().indirectMagic(player, player), 2.0f);
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
            }
            level.sendParticles(ParticleTypes.POOF, point.x, point.y, point.z, 150, 3, 2, 3, 0.1);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.WITCH_CELEBRATE, SoundSource.PLAYERS, 1.2f, 0.6f);
            return true;
        });

        // 3. Ядовитая аура вокруг игрока
        Spells.reg("venom_aura", School.POISON, 25, 400, (level, player) -> {
            List<Entity> targets = level.getEntities(player,
                    player.getBoundingBox().inflate(3.5), e -> e instanceof LivingEntity && e.isAlive());
            if (targets.isEmpty()) return false;
            for (Entity e : targets) {
                LivingEntity living = (LivingEntity) e;
                living.hurt(player.damageSources().indirectMagic(player, player), 2.0f);
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            }
            level.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 1, player.getZ(), 80, 1.5, 1, 1.5, 0.1);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.WITCH_CELEBRATE, SoundSource.PLAYERS, 1.0f, 1.0f);
            return true;
        });

        // 4. Разложение: сильный длительный яд по цели
        Spells.reg("corruption", School.POISON, 35, 300, (level, player) -> {
            LivingEntity target = nearest(player, 12);
            if (target == null) return false;
            target.hurt(player.damageSources().indirectMagic(player, player), 4.0f);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 600, 2));
            level.sendParticles(ParticleTypes.SCULK_SOUL,
                    target.getX(), target.getEyeY(), target.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.WITCH_CELEBRATE, SoundSource.PLAYERS, 1.2f, 0.5f);
            return true;
        });

        // 5. Очищение: снимает яд и иссушение с себя
        Spells.reg("cleansing", School.POISON, 20, 400, (level, player) -> {
            boolean hadPoison = player.removeEffect(MobEffects.POISON);
            boolean hadWither = player.removeEffect(MobEffects.WITHER);
            if (!hadPoison && !hadWither) return false;
            player.heal(4.0f);
            level.sendParticles(ParticleTypes.HEART,
                    player.getX(), player.getY() + 1.5, player.getZ(), 10, 0.5, 0.5, 0.5, 0.02);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            return true;
        });
    }

    // ===== Вспомогательные методы =====

    private static LivingEntity rayTarget(Player player, double maxDist) {
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getViewVector(1.0f);
        Vec3 end = start.add(dir.scale(maxDist));
        BlockHitResult block = player.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double blockDist = block.getType() == HitResult.Type.MISS
                ? maxDist : block.getLocation().distanceTo(start);
        EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player.level(), player, start, end,
                player.getBoundingBox().expandTowards(dir.scale(maxDist)).inflate(1.0),
                e -> e instanceof LivingEntity && e != player && e.isAlive());
        if (hit != null && hit.getEntity().position().distanceTo(start) <= blockDist) {
            return (LivingEntity) hit.getEntity();
        }
        return null;
    }

    private static Vec3 lookPoint(Player player, double maxDist) {
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getViewVector(1.0f);
        BlockHitResult block = player.level().clip(new ClipContext(
                start, start.add(dir.scale(maxDist)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double dist = block.getType() == HitResult.Type.MISS
                ? maxDist : block.getLocation().distanceTo(start);
        return start.add(dir.scale(dist));
    }

    private static LivingEntity nearest(Player player, double range) {
        List<Entity> list = player.level().getEntities(player,
                player.getBoundingBox().inflate(range),
                e -> e instanceof LivingEntity && e.isAlive());
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : list) {
            double d = e.distanceToSqr(player);
            if (d < bestD) { bestD = d; best = (LivingEntity) e; }
        }
        return best;
    }

    private PoisonSpells() {}
}
