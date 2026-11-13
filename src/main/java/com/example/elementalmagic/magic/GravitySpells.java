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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class GravitySpells {

    static void init() {
        // 1. Левитация: цель взмывает вверх
        Spells.reg("levitate", new School("gravity", Element.GRAVITY), 20, 200, (level, player) -> {
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
        Spells.reg("gravity_well", new School("gravity", Element.GRAVITY), 35, 300, (level, player) -> {
            Vec3 point = lookPoint(player, 8);
            List<Entity> targets = level.getEntities(player,
                    new AABB(point, point).inflate(6), e -> e instanceof LivingEntity && e.isAlive());
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

        // 3. Шаг луны: мощный прыжок и мягкое падение
        Spells.reg("moon_step", new School("gravity", Element.GRAVITY), 25, 600, (level, player) -> {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 200, 8));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0));
            player.setDeltaMovement(player.getDeltaMovement().add(0, 1.5, 0));
            level.sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY(), player.getZ(), 30, 0.5, 0.2, 0.5, 0.05);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.8f);
            return true;
        });

        // 4. Сокрушение: придавливает всё вокруг к земле
        Spells.reg("crush", new School("gravity", Element.GRAVITY), 40, 300, (level, player) -> {
            List<Entity> targets = level.getEntities(player,
                    player.getBoundingBox().inflate(5), e -> e instanceof LivingEntity && e.isAlive());
            if (targets.isEmpty()) return false;
            for (Entity e : targets) {
                LivingEntity living = (LivingEntity) e;
                living.hurt(player.damageSources().indirectMagic(player, player), 6.0f);
                living.setDeltaMovement(living.getDeltaMovement().add(0, -1.5, 0));
            }
            level.sendParticles(ParticleTypes.GUST,
                    player.getX(), player.getY() + 0.5, player.getZ(), 6, 2, 0.5, 2, 0.1);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.7f);
            return true;
        });

        // 5. Чёрная дыра: мощное притяжение и урон в точке взгляда
        Spells.reg("black_hole", new School("gravity", Element.GRAVITY), 60, 800, (level, player) -> {
            Vec3 point = lookPoint(player, 12);
            List<Entity> targets = level.getEntities(player,
                    new AABB(point, point).inflate(8), e -> e instanceof LivingEntity && e.isAlive());
            for (Entity e : targets) {
                Vec3 pull = point.subtract(e.position()).normalize().scale(1.5);
                e.setDeltaMovement(e.getDeltaMovement().add(pull));
                e.hurt(player.damageSources().indirectMagic(player, player), 5.0f);
            }
            level.sendParticles(ParticleTypes.PORTAL, point.x, point.y, point.z, 150, 3, 3, 3, 0.5);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 0.5f);
            return true;
        });
    }

    // ===== Вспомогательные методы =====

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

    private GravitySpells() {}
}
