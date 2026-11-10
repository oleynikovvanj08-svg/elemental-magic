package com.example.elementalmagic.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.example.elementalmagic.magic.School.IRON;
import static com.example.elementalmagic.magic.School.STONE;
import static com.example.elementalmagic.magic.SpellUtil.*;
import static com.example.elementalmagic.magic.Spells.reg;

final class EarthSpells {
    private static final BlockParticleOption ROCK =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());

    /** Ставит стену перед игроком. Заполняет только пустые места, ничего не ломает. */
    static boolean placeWall(ServerLevel level, Player p, BlockState block, int distance, int halfWidth, int height) {
        if (!p.mayBuild()) return false;
        Direction dir = p.getDirection();
        Direction side = dir.getClockWise();
        BlockPos base = p.blockPosition().relative(dir, distance);
        boolean placed = false;
        for (int w = -halfWidth; w <= halfWidth; w++) {
            for (int h = 0; h < height; h++) {
                BlockPos pos = base.relative(side, w).above(h);
                if (level.getBlockState(pos).isAir() && level.mayInteract(p, pos)) {
                    level.setBlockAndUpdate(pos, block);
                    placed = true;
                }
            }
        }
        return placed;
    }

    static void init() {
        // ==================== ЖЕЛЕЗО ====================
        reg("iron_skin", IRON, 15, 300, (level, p) -> {
            effect(p, MobEffects.DAMAGE_RESISTANCE, 600, 0);
            effect(p, MobEffects.ABSORPTION, 600, 0);
            sound(level, p, SoundEvents.IRON_GOLEM_REPAIR, 1f);
            return true;
        });

        reg("iron_fist", IRON, 15, 30, (level, p) -> {
            LivingEntity t = target(p, 5);
            if (t == null) return false;
            hurt(p, t, 7);
            pushFrom(t, p.position(), 1.2, 0.3);
            burst(level, mid(t), ParticleTypes.CRIT, 20, 1);
            sound(level, t, SoundEvents.IRON_GOLEM_ATTACK, 1f);
            return true;
        });

        reg("steel_spikes", IRON, 25, 60, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 4)) {
                hurt(p, e, 5);
                push(e, e.getDeltaMovement().add(0, 0.7, 0));
            }
            ring(level, p.position(), 4, ParticleTypes.CRIT);
            sound(level, p, SoundEvents.ANVIL_PLACE, 1.4f);
            return true;
        });

        reg("iron_will", IRON, 25, 300, (level, p) -> {
            cleanse(p);
            effect(p, MobEffects.DAMAGE_BOOST, 400, 0);
            effect(p, MobEffects.DAMAGE_RESISTANCE, 200, 0);
            burst(level, mid(p), ParticleTypes.CRIT, 40, 1.5);
            return true;
        });

        reg("mend", IRON, 30, 200, (level, p) -> {
            List<ItemStack> stacks = new ArrayList<>(List.of(p.getMainHandItem(), p.getOffhandItem()));
            p.getArmorSlots().forEach(stacks::add);
            boolean fixed = false;
            for (ItemStack s : stacks) {
                if (s.isDamaged()) {
                    s.setDamageValue(Math.max(0, s.getDamageValue() - s.getMaxDamage() / 4));
                    fixed = true;
                }
            }
            if (!fixed) return false;
            sound(level, p, SoundEvents.ANVIL_USE, 1.2f);
            return true;
        });

        reg("blade_rain", IRON, 40, 100, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 8)) hurt(p, e, 6);
            burst(level, mid(p).add(0, 2, 0), ParticleTypes.CRIT, 250, 8);
            sound(level, p, SoundEvents.ANVIL_LAND, 1.6f);
            return true;
        });

        reg("iron_guardian", IRON, 60, 2400, (level, p) -> {
            IronGolem golem = EntityType.IRON_GOLEM.create(level);
            if (golem == null) return false;
            Vec3 look = p.getLookAngle();
            Vec3 pos = p.position().add(look.x * 2, 0, look.z * 2);
            golem.moveTo(pos.x, pos.y, pos.z, p.getYRot(), 0);
            golem.setPlayerCreated(true);
            level.addFreshEntity(golem);
            burst(level, pos.add(0, 1, 0), ParticleTypes.CRIT, 60, 2);
            sound(level, golem, SoundEvents.IRON_GOLEM_REPAIR, 0.8f);
            return true;
        });

        reg("anvil_drop", IRON, 40, 100, (level, p) -> {
            LivingEntity t = target(p, 25);
            if (t == null) return false;
            hurt(p, t, 12);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 100, 2);
            line(level, t.position().add(0, 5, 0), mid(t), ParticleTypes.CRIT);
            sound(level, t, SoundEvents.ANVIL_LAND, 0.8f);
            return true;
        });

        reg("fortress", IRON, 50, 600, (level, p) -> {
            effect(p, MobEffects.DAMAGE_RESISTANCE, 300, 2);
            effect(p, MobEffects.ABSORPTION, 300, 2);
            effect(p, MobEffects.MOVEMENT_SLOWDOWN, 300, 1);
            ring(level, p.position(), 2, ParticleTypes.CRIT);
            sound(level, p, SoundEvents.IRON_GOLEM_REPAIR, 0.6f);
            return true;
        });

        reg("iron_maiden", IRON, 70, 300, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 16);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 200, 5);
            effect(t, MobEffects.WEAKNESS, 200, 1);
            burst(level, mid(t), ParticleTypes.CRIT, 100, 1.5);
            sound(level, t, SoundEvents.ANVIL_LAND, 0.5f);
            return true;
        });

        // ==================== КАМЕНЬ ====================
        reg("rock_throw", STONE, 10, 20, (level, p) -> {
            LivingEntity t = target(p, 25);
            if (t == null) return false;
            hurt(p, t, 6);
            line(level, p.getEyePosition(), mid(t), ROCK);
            sound(level, t, SoundEvents.STONE_BREAK, 1f);
            return true;
        });

        reg("stone_wall", STONE, 25, 100, (level, p) -> {
            boolean placed = placeWall(level, p, Blocks.COBBLESTONE.defaultBlockState(), 2, 2, 3);
            if (placed) sound(level, p, SoundEvents.STONE_PLACE, 0.8f);
            return placed;
        });

        reg("earthquake", STONE, 35, 120, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 8)) {
                hurt(p, e, 5);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 100, 1);
                push(e, e.getDeltaMovement().add(0, 0.5, 0));
            }
            ring(level, p.position(), 8, ROCK);
            sound(level, p, SoundEvents.STONE_BREAK, 0.5f);
            return true;
        });

        reg("stone_skin", STONE, 25, 400, (level, p) -> {
            effect(p, MobEffects.DAMAGE_RESISTANCE, 400, 1);
            effect(p, MobEffects.MOVEMENT_SLOWDOWN, 400, 0);
            burst(level, mid(p), ROCK, 40, 1.5);
            return true;
        });

        reg("fissure", STONE, 35, 80, (level, p) -> {
            for (LivingEntity e : beam(level, p, 20, 1.0)) {
                hurt(p, e, 7);
                push(e, e.getDeltaMovement().add(0, 0.8, 0));
            }
            line(level, p.position(), beamEnd(p, 20), ROCK);
            sound(level, p, SoundEvents.STONE_BREAK, 0.6f);
            return true;
        });

        reg("petrify", STONE, 35, 150, (level, p) -> {
            LivingEntity t = target(p, 20);
            if (t == null) return false;
            hurt(p, t, 2);
            effect(t, MobEffects.MOVEMENT_SLOWDOWN, 200, 6);
            effect(t, MobEffects.WEAKNESS, 200, 2);
            effect(t, MobEffects.DIG_SLOWDOWN, 200, 2);
            burst(level, mid(t), ROCK, 50, 1.5);
            return true;
        });

        reg("seismic_sense", STONE, 20, 300, (level, p) -> {
            List<LivingEntity> list = enemies(level, p, 32);
            if (list.isEmpty()) return false;
            for (LivingEntity e : list) effect(e, MobEffects.GLOWING, 400, 0);
            ring(level, p.position(), 3, ROCK);
            return true;
        });

        reg("miners_blessing", STONE, 30, 1200, (level, p) -> {
            effect(p, MobEffects.DIG_SPEED, 2400, 1);
            effect(p, MobEffects.NIGHT_VISION, 2400, 0);
            burst(level, mid(p), ROCK, 30, 1);
            return true;
        });

        reg("boulder", STONE, 45, 120, (level, p) -> {
            LivingEntity t = target(p, 25);
            if (t == null) return false;
            for (LivingEntity e : around(level, p, t.position(), 4)) hurt(p, e, 10);
            burst(level, mid(t), ROCK, 120, 4);
            sound(level, t, SoundEvents.STONE_BREAK, 0.4f);
            return true;
        });

        reg("landslide", STONE, 70, 600, (level, p) -> {
            for (LivingEntity e : enemies(level, p, 14)) {
                hurt(p, e, 12);
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 200, 3);
                pushFrom(e, p.position(), 1.5, 0.5);
            }
            ring(level, p.position(), 14, ROCK);
            burst(level, mid(p), ROCK, 300, 14);
            sound(level, p, SoundEvents.STONE_BREAK, 0.3f);
            return true;
        });
    }

    private EarthSpells() {}
}