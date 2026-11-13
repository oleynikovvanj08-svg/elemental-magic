package com.example.elementalmagic.world;

import com.example.elementalmagic.ModRegistry;
import com.example.elementalmagic.item.ScrollItem;
import com.example.elementalmagic.magic.Element;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;

/** Заменяет предмет на случайный свиток обычной (не боссовой) школы. */
public final class RandomScrollFunction implements LootItemFunction {
    public static final MapCodec<RandomScrollFunction> CODEC = MapCodec.unit(new RandomScrollFunction());

    @Override
    public LootItemFunctionType<? extends LootItemFunction> getType() {
        return ModRegistry.RANDOM_SCROLL.get();
    }

    @Override
    public ItemStack apply(ItemStack stack, LootParams params) {
        List<DeferredItem<ScrollItem>> pool = new ArrayList<>();
        for (Element e : Element.values()) {
            if (e.bossOnly) continue;
            pool.addAll(ModRegistry.SCROLLS.get(e));
        }
        if (pool.isEmpty()) return stack;
        return new ItemStack(pool.get(params.getRandom().nextInt(pool.size())).get());
    }

    private RandomScrollFunction() {}
}
