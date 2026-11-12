package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Призванные существа: помечаются хозяином, живут ограниченное время и сражаются за игрока. */
final class Minions {
    private static final Map<String, List<Mob>> ACTIVE = new HashMap<>();

    /**
     * Берёт существо под учёт: помечает хозяина, ограничивает количество в группе
     * (лишние старые исчезают) и убирает его через lifeTicks.
     */
    static void track(ServerLevel level, Player owner, String group, Mob mob, int lifeTicks, int cap) {
        mob.getPersistentData().putUUID(SpellUtil.OWNER_TAG, owner.getUUID());
        List<Mob> list = ACTIVE.computeIfAbsent(owner.getUUID() + ":" + group, k -> new ArrayList<>());
        list.removeIf(m -> m.isRemoved() || !m.isAlive());
        while (list.size() >= cap && !list.isEmpty()) dismiss(level, list.remove(0));
        list.add(mob);
        Scheduler.later(lifeTicks, () -> {
            list.remove(mob);
            dismiss(level, mob);
        });
    }

    private static void dismiss(ServerLevel level, Mob mob) {
        if (mob.isRemoved()) return;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + 0.5, mob.getZ(),
                15, 0.3, 0.5, 0.3, 0.02);
        mob.discard();
    }

    /**
     * Призывает боевого моба (зомби, скелета и т.д.), который защищает хозяина,
     * атакует его цели и ближайших враждебных мобов и не трогает самого игрока.
     */
    static Mob combat(ServerLevel level, Player owner, EntityType<? extends Mob> type,
                      int lifeTicks, String group, int cap, boolean invisible) {
        Mob mob = type.create(level);
        if (mob == null) return null;

        RandomSource r = level.random;
        mob.moveTo(owner.getX() + (r.nextDouble() - 0.5) * 4, owner.getY(),
                owner.getZ() + (r.nextDouble() - 0.5) * 4, owner.getYRot(), 0f);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);

        if (mob instanceof Zombie zombie) {
            zombie.setBaby(false);
            var reinforcements = zombie.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
            if (reinforcements != null) reinforcements.setBaseValue(0);
        }

        // Свой ИИ целей: никогда не нападает на хозяина
        mob.targetSelector.removeAllGoals(g -> true);
        mob.targetSelector.addGoal(1, new OwnerTargetGoal(mob, owner));
        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, LivingEntity.class, 10, true, false,
                e -> e instanceof Enemy && !e.getPersistentData().hasUUID(SpellUtil.OWNER_TAG)));
        mob.goalSelector.addGoal(6, new FollowSummonerGoal(mob, owner));

        mob.setPersistenceRequired();
        mob.setCanPickUpLoot(false);
        // Не горят на солнце
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, lifeTicks + 40, 0, false, false));

        if (invisible) {
            for (EquipmentSlot slot : EquipmentSlot.values()) mob.setItemSlot(slot, ItemStack.EMPTY);
            mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, lifeTicks + 40, 0, false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, lifeTicks + 40, 1, false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, lifeTicks + 40, 1, false, false));
        }

        level.addFreshEntity(mob);
        track(level, owner, group, mob, lifeTicks, cap);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + 0.5, mob.getZ(),
                20, 0.3, 0.6, 0.3, 0.02);
        return mob;
    }

    // ---------- Цели: то, что бьёт хозяин или что бьёт хозяина ----------

    private static final class OwnerTargetGoal extends TargetGoal {
        private final Player owner;
        private LivingEntity wanted;

        OwnerTargetGoal(Mob mob, Player owner) {
            super(mob, false);
            this.owner = owner;
            setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (owner.isRemoved() || !owner.isAlive()) return false;
            LivingEntity attacker = owner.getLastHurtByMob();
            if (valid(attacker) && owner.tickCount - owner.getLastHurtByMobTimestamp() < 200) {
                wanted = attacker;
                return true;
            }
            LivingEntity victim = owner.getLastHurtMob();
            if (valid(victim) && owner.tickCount - owner.getLastHurtMobTimestamp() < 200) {
                wanted = victim;
                return true;
            }
            return false;
        }

        private boolean valid(LivingEntity e) {
            return e != null && e.isAlive() && e != owner && e != mob
                    && !e.getPersistentData().hasUUID(SpellUtil.OWNER_TAG)
                    && !(e instanceof TamableAnimal t && t.isOwnedBy(owner));
        }

        @Override
        public void start() {
            mob.setTarget(wanted);
            super.start();
        }
    }

    // ---------- Следование за хозяином ----------

    private static final class FollowSummonerGoal extends Goal {
        private final Mob mob;
        private final Player owner;
        private int recalc;

        FollowSummonerGoal(Mob mob, Player owner) {
            this.mob = mob;
            this.owner = owner;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !owner.isRemoved() && owner.isAlive() && mob.getTarget() == null
                    && mob.distanceToSqr(owner) > 64;
        }

        @Override
        public boolean canContinueToUse() {
            return !owner.isRemoved() && owner.isAlive() && mob.getTarget() == null
                    && mob.distanceToSqr(owner) > 16;
        }

        @Override
        public void tick() {
            if (--recalc > 0) return;
            recalc = 10;
            if (mob.distanceToSqr(owner) > 30 * 30 && owner.level() == mob.level()) {
                mob.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            } else {
                mob.getNavigation().moveTo(owner, 1.2);
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }
    }

    private Minions() {}
}
