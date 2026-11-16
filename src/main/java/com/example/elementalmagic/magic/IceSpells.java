package com.example.elementalmagic.magic;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

import java.util.List;

public final class IceSpells {

    static void init() {
        // 1. Ледяная стрела
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

        // 2. Ледяная поступь
        Spells.reg("ice_walk", School.GLACIER, 30, 300, (level, player) -> {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
            for (int i = 0; i < 40; i++) {
                int delay = i * 5;
                level.getServer().tell(new TickTask(
                        level.getServer().getTickCount() + delay, () -> {
                    if (!player.isAlive()) return;
                    BlockPos below = player.blockPosition().below();
                    var state = level.getBlockState(below);
                    if (state.isAir() || state.liquid()) {
                        level.setBlock(below, Blocks.FROSTED_ICE.defaultBlockState(), 3);
                    }
                }));
            }
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    player.getX(), player.getY() + 0.5, player.getZ(), 60, 1, 0.5, 1, 0.05);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.0f, 1.5f);
            return true;
        });

        // 3. Ледяная тропа
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

        // 4. Метель
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

        // 5. Ледяная тюрьма
        Spells.reg("glacier_prison", School.GLACIER, 40, 400, (level, player) -> {
            LivingEntity target = nearest(player, 10);
            if (target == null) return false;
            BlockPos center = target.blockPosition();
            int placed = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) continue;
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

        // 6. Ледяные шипы (новое)
        Spells.reg("ice_spikes", School.GLACIER, 30, 200, (level, player) -> {
            List<Entity> targets = level.getEntities(player,
                    player.getBoundingBox().inflate(4), e -> e instanceof LivingEntity && e.isAlive());
            if (targets.isEmpty()) return false;
            for (Entity e : targets) {
                LivingEntity living = (LivingEntity) e;
                living.hurt(player.damageSources().indirectMagic(player, player), 6.0f);
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        living.getX(), living.getY() + 0.5, living.getZ(), 15, 0.5, 0.5, 0.5, 0.05);
            }
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    player.getX(), player.getY(), player.getZ(), 80, 4, 1, 4, 0.1);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 1.0f);
            return true;
        });

        // 7. Морозный взрыв (новое)
        Spells.reg("frost_burst", School.GLACIER, 35, 300, (level, player) -> {
            LivingEntity target = nearest(player, 8);
            if (target == null) return false;
            target.hurt(player.damageSources().freeze(), 8.0f);
            MagicEvents.freezeAi(target, 40);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getEyeY(), target.getZ(), 40, 1, 1, 1, 0.1);
            level.sendParticles(ParticleTypes.FLASH,
                    target.getX(), target.getEyeY(), target.getZ(), 3, 0, 0, 0, 0);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 0.7f);
            return true;
        });

        // 8. Ледяной панцирь (новое)
        Spells.reg("ice_armor", School.GLACIER, 25, 400, (level, player) -> {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0));
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    player.getX(), player.getY() + 1, player.getZ(), 60, 1.5, 1.5, 1.5, 0.05);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.0f, 1.2f);
            return true;
        });

        // 9. Снежная буря (новое)
        Spells.reg("snowstorm", School.GLACIER, 45, 500, (level, player) -> {
            Vec3 point = lookPoint(player, 12);
            int pulses = 10; // 10 импульсов по 10 тиков = 100 тиков (5 секунд)
            for (int i = 0; i < pulses; i++) {
                int delay = i * 10;
                level.getServer().tell(new TickTask(level.getServer().getTickCount() + delay, () -> {
                    List<Entity> targets = level.getEntities(player,
                            new AABB(point, point).inflate(5), e -> e instanceof LivingEntity && e.isAlive());
                    for (Entity e : targets) {
                        LivingEntity living = (LivingEntity) e;
                        living.hurt(player.damageSources().freeze(), 2.0f);
                        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                    }
                    level.sendParticles(ParticleTypes.SNOWFLAKE, point.x, point.y, point.z, 30, 4, 2, 4, 0.1);
                }));
            }
            level.playSound(null, player.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.6f);
            return true;
        });

        // 10. Айсберг (новое)
        Spells.reg("iceberg", School.GLACIER, 55, 600, (level, player) -> {
            Vec3 point = lookPoint(player, 10);
            List<Entity> targets = level.getEntities(player,
                    new AABB(point, point).inflate(4), e -> e instanceof LivingEntity && e.isAlive());
            if (targets.isEmpty()) return false;
            for (Entity e : targets) {
                LivingEntity living = (LivingEntity) e;
                living.hurt(player.damageSources().freeze(), 15.0f);
                MagicEvents.freezeAi(living, 40);
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SNOWFLAKE, point.x, point.y, point.z, 150, 4, 2, 4, 0.2);
            level.playSound(null, BlockPos.containing(point),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0f, 0.5f);
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
