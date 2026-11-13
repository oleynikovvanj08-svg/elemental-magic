package com.example.elementalmagic.world;

import com.example.elementalmagic.ModRegistry;
import com.example.elementalmagic.ScrollItem;
import com.example.elementalmagic.magic.Element;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;

/** Заменяет предмет на случайный свиток обычной (не боссовой) школы. */
public final class RandomScrollFunction implements LootItemFunction {
    public static final MapCodec<RandomScrollFunction> CODEC = MapCodec.unit(RandomScrollFunction::new);

    @Override
    public LootItemFunctionType<? extends LootItemFunction> getType() {
        return ModRegistry.RANDOM_SCROLL.get();
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext ctx) {
        List<DeferredItem<ScrollItem>> pool = new ArrayList<>();
        for (Element e : Element.values()) {
            if (e.bossOnly) continue;
            pool.addAll(ModRegistry.SCROLLS.get(e));
        }
        if (pool.isEmpty()) return stack;
        return new ItemStack(pool.get(ctx.getRandom().nextInt(pool.size())).get());
    }

    private RandomScrollFunction() {}
}
