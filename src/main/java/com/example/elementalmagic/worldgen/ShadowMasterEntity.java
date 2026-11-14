package com.example.elementalmagic.boss;

import com.example.elementalmagic.magic.Element;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Повелитель теней: босс стихии «Тени».
 * Способности:
 *  1. Шаг сквозь тени: появляется за спиной цели и бьёт. Происходит часто.
 *  2. Покров тьмы: босс на 4 секунды становится невидимым, а игроки рядом слепнут.
 *  Дальний импульс бьёт чаще, чем у остальных боссов.
 */
public class ShadowMasterEntity extends AbstractBoss {
    private int stepCooldown = 100;
    private int veilCooldown = 200;

    public ShadowMasterEntity(EntityType<? extends ShadowMasterEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return baseAttributes(300, 15, 0.34, 7);
    }

    @Override
    protected Element bossElement() {
        return Element.SHADOW;
    }

    @Override
    protected BossEvent.BossBarColor barColor() {
        return BossEvent.BossBarColor.PURPLE;
    }

    @Override
    protected ParticleOptions boltParticle() {
        return ParticleTypes.SQUID_INK;
    }

    @Override
    protected int boltCooldownTicks(boolean enraged) {
        return enraged ? 40 : 60;
    }

    @Override
    protected void tickAbilities(ServerLevel level, LivingEntity target, boolean enraged) {
        if (--stepCooldown <= 0) {
            if (shadowStep(level, target)) stepCooldown = enraged ? 80 : 120;
            else stepCooldown = 30;
        }
        if (--veilCooldown <= 0) {
            veil(level);
            veilCooldown = enraged ? 200 : 300;
        }
    }

    /** Появляется за спиной цели и бьёт. Возвращает false, если сзади нет места. */
    private boolean shadowStep(ServerLevel level, LivingEntity target) {
        Vec3 look = target.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        Vec3 dest = target.position().subtract(flat.scale(1.8));
        if (!level.noCollision(this, getBoundingBox().move(dest.subtract(position())))) return false;

        level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.5, getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        teleportTo(dest.x, dest.y, dest.z);
        level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.5, getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.5f);
        doHurtTarget(target);
        return true;
    }

    private void veil(ServerLevel level) {
        addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false));
        for (Player p : playersNear(level, 14)) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
        }
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 80, 1.5, 1.2, 1.5, 0.02);
        ring(level, 14, ParticleTypes.SQUID_INK);
        playSound(SoundEvents.WARDEN_HEARTBEAT, 2f, 0.6f);
    }
}
