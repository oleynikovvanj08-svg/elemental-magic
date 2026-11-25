package com.example.elementalmagic.magic;

import com.example.elementalmagic.MagicMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/**
 * Попадание метеора из заклинания «Метеоритный дождь». Ванильное попадание отменяется
 * (иначе шар поджёг бы блоки и взорвался), вместо него: урон по площади, поджог и частицы.
 */
@EventBusSubscriber(modid = MagicMod.MODID)
public final class MeteorEvents {

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Fireball fireball)) return;
        CompoundTag data = fireball.getPersistentData();
        if (!data.getBoolean(FireExtraSpells.METEOR_TAG)) return;
        if (!(fireball.level() instanceof ServerLevel level)) return;   // на клиенте ничего не делаем

        event.setCanceled(true);
        Vec3 pos = event.getRayTraceResult().getLocation();
        float damage = data.getFloat(FireExtraSpells.METEOR_DAMAGE);
        float radius = data.getFloat(FireExtraSpells.METEOR_RADIUS);

        if (fireball.getOwner() instanceof Player owner) {
            for (LivingEntity e : SpellUtil.around(level, owner, pos, radius)) {
                e.invulnerableTime = 0;          // иначе кадры неуязвимости «съедят» удары следующих шаров
                e.hurt(owner.damageSources().indirectMagic(owner, owner), damage);
                e.igniteForSeconds(4);
            }
        }

        SpellUtil.burst(level, pos, ParticleTypes.EXPLOSION, 3, radius);
        SpellUtil.burst(level, pos.add(0, 0.5, 0), ParticleTypes.FLAME, 30, radius);
        SpellUtil.burst(level, pos, ParticleTypes.LAVA, 8, radius * 0.6);
        SpellUtil.sound(level, fireball, SoundEvents.GENERIC_EXPLODE, 1.1f);
        fireball.discard();
    }

    private MeteorEvents() {}
}
