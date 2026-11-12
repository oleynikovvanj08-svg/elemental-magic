package com.example.elementalmagic;

import com.example.elementalmagic.entity.ElementalMob;
import com.example.elementalmagic.entity.ModEntities;
import com.example.elementalmagic.magic.Element;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Рецепты мода в коде, без JSON-файлов.
 * Добавляются при входе в мир и после /reload, до того как рецепты отправляются игроку (и JEI).
 */
@EventBusSubscriber(modid = MagicMod.MODID)
public final class ModRecipes {
    private static final ResourceLocation MARKER = id("magic_table");

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        RecipeManager manager = event.getPlayerList().getServer().getRecipeManager();
        if (manager.byKey(MARKER).isPresent()) return; // уже добавлены

        List<RecipeHolder<?>> all = new ArrayList<>(manager.getRecipes());
        all.addAll(build());
        manager.replaceRecipes(all);
    }

    private static List<RecipeHolder<?>> build() {
        List<RecipeHolder<?>> out = new ArrayList<>();
        Item mana = ModRegistry.MANA_ITEM.get();
        CraftingBookCategory equip = CraftingBookCategory.EQUIPMENT;

        for (Element e : Element.values()) {
            Item s = ModEntities.material(e, ElementalMob.Kind.GOLEM); // осколок
            Item c = ModEntities.material(e, ElementalMob.Kind.MAGE);  // кристалл
            Item k = ModEntities.material(e, ElementalMob.Kind.WISP);  // сгусток
            var armor = ModRegistry.ARMOR.get(e);

            out.add(shaped(e.id + "_helmet", equip, armor.get(ArmorItem.Type.HELMET).get(),
                    Map.of('S', s), "SSS", "S S"));
            out.add(shaped(e.id + "_chestplate", equip, armor.get(ArmorItem.Type.CHESTPLATE).get(),
                    Map.of('S', s, 'M', mana), "SMS", "SSS", "SSS"));
            out.add(shaped(e.id + "_leggings", equip, armor.get(ArmorItem.Type.LEGGINGS).get(),
                    Map.of('S', s), "SSS", "S S", "S S"));
            out.add(shaped(e.id + "_boots", equip, armor.get(ArmorItem.Type.BOOTS).get(),
                    Map.of('S', s), "S S", "S S"));

            out.add(shaped(e.id + "_staff", equip, ModRegistry.STAVES.get(e).get(),
                    Map.of('S', s, 'C', c, 'K', k, 'M', mana), " MK", " CM", "S  "));
            out.add(shaped(e.id + "_ring", equip, ModRegistry.RINGS.get(e).get(),
                    Map.of('K', k, 'G', Items.GOLD_INGOT), " K ", "G G", " G "));
            out.add(shaped(e.id + "_amulet", equip, ModRegistry.AMULETS.get(e).get(),
                    Map.of('T', Items.STRING, 'G', Items.GOLD_INGOT, 'C', c, 'K', k), "T T", " G ", "CKC"));

            // Гримуары школ с боссов падают только с боссов
            if (!e.bossOnly) {
                out.add(shaped(e.id + "_spellbook", CraftingBookCategory.MISC, ModRegistry.BOOKS.get(e).get(),
                        Map.of('M', mana, 'C', c, 'B', Items.BOOK), "MCM", "CBC", "MCM"));
            }
        }

        out.add(shaped("magic_table", CraftingBookCategory.MISC, ModRegistry.MAGIC_TABLE_ITEM.get(),
                Map.of('M', mana, 'B', Items.BOOK, 'D', Items.DIAMOND, 'O', Items.OBSIDIAN),
                "MBM", "ODO", "OOO"));

        Ingredient m = Ingredient.of(mana);
        out.add(new RecipeHolder<>(id("experience_bottle_from_mana"), new ShapelessRecipe("",
                CraftingBookCategory.MISC, new ItemStack(Items.EXPERIENCE_BOTTLE),
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.GLASS_BOTTLE), m, m, m))));
        return out;
    }

    private static RecipeHolder<?> shaped(String name, CraftingBookCategory category, ItemLike result,
                                          Map<Character, ItemLike> key, String... pattern) {
        Map<Character, Ingredient> ingredients = new HashMap<>();
        key.forEach((ch, item) -> ingredients.put(ch, Ingredient.of(item)));
        ShapedRecipe recipe = new ShapedRecipe("", category, ShapedRecipePattern.of(ingredients, pattern),
                new ItemStack(result));
        return new RecipeHolder<>(id(name), recipe);
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, "auto/" + name);
    }

    private ModRecipes() {}
}
