package com.example.elementalmagic.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SummoningSpells {

    static void init() {
        // 1. Призыв волков: два верных волка
        Spells.reg("summon_wolves", new School("summoning", Element.SUMMONING), 40, 1200, (level, player) -> {
            boolean ok = true;
            ok &= spawn(player, EntityType.WOLF, "Призванный волк") != null;
            ok &= spawn(player, EntityType.WOLF, "Призванный волк") != null;
            return ok;
        });

        // 2. Призыв лисы-компаньона
        Spells.reg("summon_fox", new School("summoning", Element.SUMMONING), 30, 1200, (level, player) -> {
            return spawn(player, EntityType.FOX, "Призванная лиса") != null;
        });

        // 3. Призыв попугая-скаута
        Spells.reg("summon_parrot", new School("summoning", Element.SUMMONING), 20, 1200, (level, player) -> {
            return spawn(player, EntityType.PARROT, "Призванный попугай") != null;
        });

        // 4. Стая духов: рой летучих мышей
        Spells.reg("summon_swarm", new School("summoning", Element.SUMMONING), 30, 1200, (level, player) -> {
            boolean ok = true;
            for (int i = 0; i < 4; i++) {
                ok &= spawn(player, EntityType.BAT, "Малый дух") != null;
            }
            return ok;
        });

        // 5. Призыв каменного стража
        Spells.reg("summon_stone_guard", new School("summoning", Element.SUMMONING), 70, 2400, (level, player) -> {
            return spawn(player, EntityType.IRON_GOLEM, "Каменный страж") != null;
        });
    }

    private static Mob spawn(ServerLevel level, EntityType<? extends Mob> type, String name) {
        // ВАЖНО: этот метод нужен в сигнатуре (level, type, name), поэтому
        // позицию берём от игрока внутри через коннект к уровню нельзя —
        // см. перегрузку ниже; вызовы выше передают level, а игрок нужен для позиции.
        throw new UnsupportedOperationException();
    }

    private SummoningSpells() {}
}
