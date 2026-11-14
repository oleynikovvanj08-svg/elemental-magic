package com.example.elementalmagic.boss;

import com.example.elementalmagic.ModRegistry;
import com.example.elementalmagic.ScrollItem;
import com.example.elementalmagic.SpellbookItem;
import com.example.elementalmagic.magic.Element;
import com.example.elementalmagic.magic.Spell;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;

/** Добыча с боссов стихии. Подходит для любого будущего босса: достаточно передать его стихию. */
public final class BossLoot {
    private static final int SCROLLS_MIN = 3;
    private static final int SCROLLS_EXTRA = 2;     // свитков будет от 3 до 5

    private static final float STAFF_CHANCE = 0.5f;
    private static final float ACCESSORY_CHANCE = 0.6f;
    private static final float ARMOR_CHANCE = 0.6f;

    public static List<ItemStack> roll(Element element, RandomSource random) {
        List<ItemStack> out = new ArrayList<>();

        // Гарантированно: книга и меч стихии (если меч у стихии есть)
        if (ModRegistry.BOOKS.containsKey(element)) {
            out.add(new ItemStack(ModRegistry.BOOKS.get(element).get()));
        }
        if (ModRegistry.SWORDS.containsKey(element)) {
            out.add(new ItemStack(ModRegistry.SWORDS.get(element).get()));
        }

        if (random.nextFloat() < STAFF_CHANCE && ModRegistry.STAVES.containsKey(element)) {
            out.add(new ItemStack(ModRegistry.STAVES.get(element).get()));
        }

        // Свитки этой стихии. Вес по уровню: I = 25, II = 16, III = 9, IV = 4, V = 1
        List<Spell> spells = SpellbookItem.allSpells(element);
        List<DeferredItem<ScrollItem>> scrolls = ModRegistry.SCROLLS.get(element);
        int n = scrolls == null ? 0 : Math.min(spells.size(), scrolls.size());
        if (n > 0) {
            List<Integer> pool = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int weight = (int) Math.pow(6 - Math.max(1, Math.min(5, spells.get(i).tier())), 2);
                for (int k = 0; k < weight; k++) pool.add(i);
            }
            int count = SCROLLS_MIN + random.nextInt(SCROLLS_EXTRA + 1);
            for (int c = 0; c < count && !pool.isEmpty(); c++) {
                final int picked = pool.get(random.nextInt(pool.size()));
                pool.removeIf(v -> v == picked);
                out.add(new ItemStack(scrolls.get(picked).get()));
            }
        }

        // Амулет или кольцо
        if (random.nextFloat() < ACCESSORY_CHANCE) {
            boolean amulet = random.nextBoolean();
            var map = amulet ? ModRegistry.AMULETS : ModRegistry.RINGS;
            if (map.containsKey(element)) out.add(new ItemStack(map.get(element).get()));
        }

        // Случайная часть брони стихии
        if (random.nextFloat() < ARMOR_CHANCE && ModRegistry.ARMOR.containsKey(element)) {
            ArmorItem.Type type = ModRegistry.ARMOR_TYPES.get(random.nextInt(ModRegistry.ARMOR_TYPES.size()));
            out.add(new ItemStack(ModRegistry.ARMOR.get(element).get(type).get()));
        }

        // Мана
        out.add(new ItemStack(ModRegistry.MANA_ITEM.get(), 3 + random.nextInt(4)));

        return out;
    }

    private BossLoot() {}
}
