package com.example.elementalmagic.boss;

import com.example.elementalmagic.magic.Element;
import com.example.elementalmagic.magic.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Страж звёзд: босс стихии «Звёзды».
 * Способности:
 *  1. Звездопад: с неба падают звёзды (за секунду до удара видна линия падения).
 *  2. Колодец гравитации: затягивает игроков к точке, затем взрывается.
 */
public class StarSentinelEntity extends AbstractBoss {
    private int starfallCooldown = 100;
    private int wellCooldown = 240;

    public StarSentinelEntity(EntityType<? extends StarSentinelEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return baseAttributes(320, 12, 0.30, 8);
    }

    @Override
    protected Element bossElement() {
        return Element.STARS;
    }

    @Override
    protected BossEvent.BossBarColor barColor() {
        return BossEvent.BossBarColor.BLUE;
    }

    @Override
    protected void tickAbilities(ServerLevel level, LivingEntity target, boolean enraged) {
        if (--starfallCooldown <= 0) {
            starfall(level, enraged ? 7 : 4);
            starfallCooldown = enraged ? 130 : 190;
        }
        if (--wellCooldown <= 0) {
            gravityWell(level, target.position());
            wellCooldown = enraged ? 200 : 300;
        }
    }

    private void starfall(ServerLevel level, int count) {
        List<Player> players = playersNear(level, 24);
        if (players.isEmpty()) return;
        for (int i = 0; i < count; i++) {
            Player p = players.get(random.nextInt(players.size()));
            double ox = i == 0 ? 0 : (random.nextDouble() - 0.5) * 10;
            double oz = i == 0 ? 0 : (random.nextDouble() - 0.5) * 10;
            Vec3 at = new Vec3(p.getX() + ox, p.getY(), p.getZ() + oz);
            particleLine(level, at.add(0, 14, 0), at, ParticleTypes.END_ROD, 0.7);
            ringAt(level, at, 2.2, ParticleTypes.END_ROD);
            Scheduler.later(24, () -> {
                if (!isAlive()) return;
                for (Player victim : level.getEntitiesOfClass(Player.class, new AABB(at, at).inflate(2.5),
                        AbstractBoss::valid)) {
                    victim.hurt(damageSources().indirectMagic(this, this), 9f);
                    victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.5, 0));
                    victim.hurtMarked = true;
                }
                level.sendParticles(ParticleTypes.FIREWORK, at.x, at.y + 0.5, at.z, 50, 1.0, 0.5, 1.0, 0.1);
                level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1.0, at.z, 40, 0.8, 0.8, 0.8, 0.1);
                playSound(SoundEvents.FIREWORK_ROCKET_BLAST, 2f, 0.8f);
            });
        }
    }

    /** Две секунды притягивает игроков к точке, затем взрыв с отбрасыванием. */
    private void gravityWell(ServerLevel level, Vec3 center) {
        for (int i = 0; i < 8; i++) {
            double r = 7 - i * 0.7;
            Scheduler.later(i * 5, () -> {
                if (!isAlive()) return;
                ringAt(level, center, r, ParticleTypes.END_ROD);
                for (Player p : level.getEntitiesOfClass(Player.class, new AABB(center, center).inflate(9),
                        AbstractBoss::valid)) {
                    Vec3 dir = center.subtract(p.position());
                    if (dir.lengthSqr() > 1.0E-4) {
                        p.setDeltaMovement(dir.normalize().scale(0.6).add(0, 0.1, 0));
                        p.hurtMarked = true;
                    }
                }
            });
        }
        Scheduler.later(45, () -> {
            if (!isAlive()) return;
            for (Player p : level.getEntitiesOfClass(Player.class, new AABB(center, center).inflate(4.5),
                    AbstractBoss::valid)) {
                p.hurt(damageSources().indirectMagic(this, this), 12f);
                Vec3 away = p.position().subtract(center);
                away = new Vec3(away.x, 0, away.z);
                if (away.lengthSqr() > 1.0E-4) {
                    p.setDeltaMovement(away.normalize().scale(1.5).add(0, 0.6, 0));
                    p.hurtMarked = true;
                }
            }
            level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 1, center.z, 3, 0.3, 0.3, 0.3, 0);
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1, center.z, 100, 2.0, 1.0, 2.0, 0.2);
            playSound(SoundEvents.WITHER_BREAK_BLOCK, 2f, 0.8f);
        });
        playSound(SoundEvents.BEACON_ACTIVATE, 2f, 0.6f);
    }
}
