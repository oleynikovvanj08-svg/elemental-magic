package com.example.elementalmagic.boss;

import com.example.elementalmagic.magic.Element;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Хранитель времени: босс стихии «Время».
 * Способности:
 *  1. Замедление времени: игроки рядом получают замедление и усталость.
 *  2. Временной скачок: появляется за спиной цели и бьёт.
 *  3. Разлом (когда здоровья меньше половины): волна урона вокруг босса с отбрасыванием.
 */
public class TimeKeeperEntity extends Monster {
    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);

    private int slowCooldown = 100;
    private int blinkCooldown = 160;
    private int riftCooldown = 200;

    public TimeKeeperEntity(EntityType<? extends TimeKeeperEntity> type, Level level) {
        super(type, level);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300)
                .add(Attributes.ATTACK_DAMAGE, 12)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ===== Босс-бар =====

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    // ===== Поведение =====

    /** Босс не исчезает ни вдали от игроков, ни на мирной сложности. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || !(level() instanceof ServerLevel serverLevel)) return;
        boolean enraged = getHealth() < getMaxHealth() * 0.5f;

        if (--slowCooldown <= 0) {
            slowField(serverLevel);
            slowCooldown = enraged ? 160 : 240;
        }
        if (--blinkCooldown <= 0 && distanceToSqr(target) > 36) {
            blink(serverLevel, target);
            blinkCooldown = enraged ? 140 : 220;
        }
        if (enraged && --riftCooldown <= 0) {
            rift(serverLevel);
            riftCooldown = 200;
        }
    }

    /** Замедление времени: игроки в радиусе 10 блоков замедлены на 4 секунды. */
    private void slowField(ServerLevel level) {
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(10),
                pl -> pl.isAlive() && !pl.isSpectator() && !pl.isCreative())) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 1));
        }
        ring(level, 10, ParticleTypes.ENCHANT);
        playSound(SoundEvents.BEACON_DEACTIVATE, 2f, 0.7f);
    }

    /** Временной скачок: появляется за спиной цели и бьёт. */
    private void blink(ServerLevel level, LivingEntity target) {
        Vec3 look = target.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        Vec3 dest = target.position().subtract(flat.scale(1.8));
        if (!level.noCollision(this, getBoundingBox().move(dest.subtract(position())))) return;

        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        teleportTo(dest.x, dest.y, dest.z);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.6f);
        doHurtTarget(target);
    }

    /** Разлом времени: урон и отбрасывание игроков в радиусе 7 блоков. */
    private void rift(ServerLevel level) {
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(7),
                pl -> pl.isAlive() && !pl.isSpectator())) {
            p.hurt(damageSources().indirectMagic(this, this), 8f);
            Vec3 away = p.position().subtract(position());
            away = new Vec3(away.x, 0, away.z);
            if (away.lengthSqr() > 1.0E-4) {
                p.setDeltaMovement(away.normalize().scale(1.2).add(0, 0.4, 0));
                p.hurtMarked = true;
            }
        }
        ring(level, 7, ParticleTypes.REVERSE_PORTAL);
        ring(level, 4, ParticleTypes.REVERSE_PORTAL);
        playSound(SoundEvents.WITHER_BREAK_BLOCK, 1.5f, 0.6f);
    }

    private void ring(ServerLevel level, double radius, net.minecraft.core.particles.ParticleOptions type) {
        int n = (int) (radius * 10);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            level.sendParticles(type, getX() + Math.cos(a) * radius, getY() + 0.3,
                    getZ() + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    // ===== Добыча =====

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (ItemStack stack : BossLoot.roll(Element.TIME, random)) {
            spawnAtLocation(stack);
        }
    }
}
