package com.example.elementalmagic.magic;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

import java.util.List;

public final class IceSpells {

    static void init() {
        // 1. Ледяная стрела: попадание по взгляду, замедление
        Spells.reg("glacier_bolt", School.GLACIER, 15, 60, (level, player) -> {
            LivingEntity target = rayTarget(player, 20);
            if (target == null) return false;
            target.hurt(player.damageSources().indirectMagic(player, player), 6.0f);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getEyeY(), target.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.2f);
            return true;
        });

        // 2. Ледяная нова: волна холода вокруг игрока
        Spells.reg("glacier_nova", School.GLACIER, 30, 200, (level, player) -> {
            List<Entity> targets = level.getEntities(player,
                    player.getBoundingBox().inflate(5), e -> e instanceof LivingEntity && e.isAlive());
            if (targets.isEmpty()) return false;
            for (Entity e : targets) {
                LivingEntity living = (LivingEntity) e;
                living.hurt(player.damageSources().indirectMagic(player, player), 4.0f);
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 3));
            }
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    player.getX(), player.getY() + 1, player.getZ(), 150, 5, 1, 5, 0.1);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 0.8f);
            return true;
        });

        // 3. Ледяная тропа: площадка из льда под ногами
        Spells.reg("glacier_path", School.GLACIER, 20, 300, (level, player) -> {
            BlockPos base = player.blockPosition();
            int placed = 0;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos p = base.offset(dx, -1, dz);
                    var state = level.getBlockState(p);
                    if (state.isAir() || state.liquid()) {
                        level.setBlock(p, Blocks.ICE.defaultBlockState(), 3);
                        placed++;
                    }
                }
            }
            if (placed == 0) return false;
            level.playSound(null, base, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.0f, 1.5f);
            return true;
        });

        // 4. Метель: область заморозки в точке взгляда
        Spells.reg("glacier_blizzard", School.GLACIER, 50, 600, (level, player) -> {
            Vec3 point = lookPoint(player, 20);
            List<Entity> targets = level.getEntities(player,
                    new AABB(point, point).inflate(6, 3, 6), e -> e instanceof LivingEntity && e.isAlive());
            for (Entity e : targets) {
                LivingEntity living = (LivingEntity) e;
                living.hurt(player.damageSources().freeze(), 3.0f);
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
            }
            level.sendParticles(ParticleTypes.SNOWFLAKE, point.x, point.y, point.z, 300, 6, 3, 6, 0.2);
            level.playSound(null, BlockPos.containing(point),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 0.6f);
            return true;
        });

        // 5. Ледяная тюрьма: клетка из уплотнённого льда вокруг цели
        Spells.reg("glacier_prison", School.GLACIER, 40, 400, (level, player) -> {
            LivingEntity target = nearest(player, 10);
            if (target == null) return false;
            BlockPos center = target.blockPosition();
            int placed = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) continue; // колонну под целью не трогаем
                        BlockPos p = center.offset(dx, dy, dz);
                        if (level.getBlockState(p).isAir()) {
                            level.setBlock(p, Blocks.PACKED_ICE.defaultBlockState(), 3);
                            placed++;
                        }
                    }
                }
            }
            if (placed == 0) return false;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5, 40, 1, 1, 1, 0.05);
            level.playSound(null, center, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.2f, 0.7f);
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

    private IceSpells() {}
}
