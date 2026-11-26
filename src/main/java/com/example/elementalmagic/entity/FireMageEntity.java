package com.example.elementalmagic.entity;

import com.example.elementalmagic.ModRegistry;
import com.example.elementalmagic.magic.Element;
import com.example.elementalmagic.magic.Spell;
import com.example.elementalmagic.magic.Spells;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FireMageEntity extends ElementalMob {

    public FireMageEntity(EntityType<? extends ElementalMob> type, Level level) {
        super(type, level, Element.FIRE, ElementalMob.Kind.MAGE);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, data);

        // Даём посох огня
        ItemStack staff = new ItemStack(ModRegistry.STAVES.get(Element.FIRE).get());
        setItemSlot(EquipmentSlot.MAINHAND, staff);
        setDropChance(EquipmentSlot.MAINHAND, 1.0f); // 100% шанс дропа

        // Вшиваем заклинания огня в посох (чтобы моб мог их использовать)
        List<Spell> spells = Spells.byElement(Element.FIRE);
        // Берём первые 10, если их больше
        List<String> ids = spells.stream().limit(10).map(Spell::id).toList();
        staff.set(ModRegistry.LEARNED_IDS.get(), ids);

        return result;
    }
}
