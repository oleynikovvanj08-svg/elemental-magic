package com.example.elementalmagic.worldgen;

import com.example.elementalmagic.ModRegistry;
import com.example.elementalmagic.ScrollItem;
import com.example.elementalmagic.SpellbookItem;
import com.example.elementalmagic.magic.Element;
import com.example.elementalmagic.magic.Spell;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Содержимое сундука святилища. Все шансы собраны вверху файла, их удобно подкручивать. */
final class ShrineLoot {
    /** Сколько свитков стихии святилища: от MIN до MIN + EXTRA. */
    private static final int SCROLLS_MIN = 2;
    private static final int SCROLLS_EXTRA = 2;

    private static final float BOOK_CHANCE = 0.30f;
    private static final float SWORD_CHANCE = 0.15f;
    private static final float STAFF_CHANCE = 0.10f;
    private static final float MANA_CHANCE = 0.60f;

    static void fill(Container chest, Element element, RandomSource random) {
        List<ItemStack> items = roll(element, random);

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < chest.getContainerSize(); i++) slots.add(i);
        Collections.shuffle(slots, new java.util.Random(random.nextLong()));

        for (int i = 0; i < items.size() && i < slots.size(); i++) {
            chest.setItem(slots.get(i), items.get(i));
        }
    }

    private static List<ItemStack> roll(Element element, RandomSource random) {
        List<ItemStack> out = new ArrayList<>();

        // Свитки заклинаний этой стихии. Чем выше уровень, тем реже
        // (вес: уровень I = 25, II = 16, III = 9, IV = 4, V = 1).
        // Свитки в ModRegistry создаются в том же порядке, что и список заклинаний школы.
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
                pool.removeIf(v -> v == picked);   // без повторов
                out.add(new ItemStack(scrolls.get(picked).get()));
            }
        }

        // Пустая книга стихии (знает первое заклинание школы, остальные записываются со свитков)
        if (random.nextFloat() < BOOK_CHANCE && ModRegistry.BOOKS.containsKey(element)) {
            out.add(new ItemStack(ModRegistry.BOOKS.get(element).get()));
        }

        // Меч (только у стихий, где он есть) и посох
        if (random.nextFloat() < SWORD_CHANCE && ModRegistry.SWORDS.containsKey(element)) {
            out.add(new ItemStack(ModRegistry.SWORDS.get(element).get()));
        }
        if (random.nextFloat() < STAFF_CHANCE && ModRegistry.STAVES.containsKey(element)) {
            out.add(new ItemStack(ModRegistry.STAVES.get(element).get()));
        }

        // Мана и немного обычных припасов
        if (random.nextFloat() < MANA_CHANCE) {
            out.add(new ItemStack(ModRegistry.MANA_ITEM.get(), 1 + random.nextInt(3)));
        }
        if (random.nextFloat() < 0.7f) out.add(new ItemStack(Items.LAPIS_LAZULI, 3 + random.nextInt(6)));
        if (random.nextFloat() < 0.5f) out.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 1 + random.nextInt(3)));
        if (random.nextFloat() < 0.5f) out.add(new ItemStack(Items.GOLD_INGOT, 2 + random.nextInt(4)));

        return out;
    }

    private ShrineLoot() {}
}
