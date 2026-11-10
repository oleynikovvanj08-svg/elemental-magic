package com.example.elementalmagic;

import com.example.elementalmagic.magic.Element;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ModRegistry {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MagicMod.MODID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MagicMod.MODID);
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MagicMod.MODID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MagicMod.MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MagicMod.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MagicMod.MODID);

    public static final List<ArmorItem.Type> ARMOR_TYPES = List.of(
            ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS);

    // ===== Мана игрока (сохраняется, не теряется после смерти) =====
    public static final Supplier<AttachmentType<Integer>> MANA = ATTACHMENTS.register("mana",
            () -> AttachmentType.builder(() -> Mana.BASE_MAX).serialize(Codec.INT).copyOnDeath().build());

    // ===== Номер выбранного заклинания в гримуаре =====
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SPELL_INDEX =
            COMPONENTS.register("spell_index", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    // ===== Номера заклинаний, записанных в гримуар =====
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Integer>>> LEARNED_SPELLS =
            COMPONENTS.register("learned_spells", () -> DataComponentType.<List<Integer>>builder()
                    .persistent(Codec.INT.listOf())
                    .networkSynchronized(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()))
                    .build());

    // ===== Магический стол =====
    public static final DeferredBlock<MagicTableBlock> MAGIC_TABLE = BLOCKS.register("magic_table",
            () -> new MagicTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0f, 1200f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 7)));
    public static final DeferredItem<BlockItem> MAGIC_TABLE_ITEM =
            ITEMS.registerSimpleBlockItem("magic_table", MAGIC_TABLE);

    // ===== Предметы =====
    public static final Map<Element, DeferredItem<SpellbookItem>> BOOKS = new EnumMap<>(Element.class);
    public static final Map<Element, DeferredHolder<ArmorMaterial, ArmorMaterial>> ARMOR_MATS = new EnumMap<>(Element.class);
    public static final Map<Element, Map<ArmorItem.Type, DeferredItem<ElementalArmorItem>>> ARMOR = new EnumMap<>(Element.class);
    public static final Map<Element, DeferredItem<StaffItem>> STAVES = new EnumMap<>(Element.class);
    public static final Map<Element, DeferredItem<AccessoryItem>> AMULETS = new EnumMap<>(Element.class);
    public static final Map<Element, DeferredItem<AccessoryItem>> RINGS = new EnumMap<>(Element.class);
    public static final Map<Element, List<DeferredItem<ScrollItem>>> SCROLLS = new EnumMap<>(Element.class);

    static {
        for (Element e : Element.values()) {
            // Гримуар
            BOOKS.put(e, ITEMS.register(e.id + "_spellbook",
                    () -> new SpellbookItem(e, new Item.Properties().stacksTo(1)
                            .rarity(e.bossOnly ? Rarity.EPIC : Rarity.RARE))));

            // Материал брони
            ARMOR_MATS.put(e, ARMOR_MATERIALS.register(e.id, () -> new ArmorMaterial(
                    defense(e), 20, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.EMPTY,
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, e.id))),
                    e == Element.EARTH ? 3f : 2f,
                    e == Element.EARTH ? 0.1f : 0f)));

            // Броня
            Map<ArmorItem.Type, DeferredItem<ElementalArmorItem>> set = new EnumMap<>(ArmorItem.Type.class);
            for (ArmorItem.Type type : ARMOR_TYPES) {
                set.put(type, ITEMS.register(e.id + "_" + type.getName(),
                        () -> new ElementalArmorItem(e, ARMOR_MATS.get(e), type, new Item.Properties()
                                .durability(type.getDurability(33)).rarity(Rarity.RARE))));
            }
            ARMOR.put(e, set);

            // Посох
            STAVES.put(e, ITEMS.register(e.id + "_staff",
                    () -> new StaffItem(e, new Item.Properties()
                            .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.4f))
                            .rarity(Rarity.RARE))));

            // Аксессуары
            AMULETS.put(e, ITEMS.register(e.id + "_amulet",
                    () -> new AccessoryItem(e, AccessoryItem.Kind.AMULET,
                            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))));
            RINGS.put(e, ITEMS.register(e.id + "_ring",
                    () -> new AccessoryItem(e, AccessoryItem.Kind.RING,
                            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))));

            // Свитки 5 уровней
            List<DeferredItem<ScrollItem>> scrolls = new ArrayList<>();
            for (int t = 1; t <= ScrollItem.TIERS; t++) {
                final int tier = t;
                scrolls.add(ITEMS.register(e.id + "_scroll_" + tier,
                        () -> new ScrollItem(e, tier, new Item.Properties()
                                .stacksTo(16).rarity(ScrollItem.rarity(e, tier)))));
            }
            SCROLLS.put(e, scrolls);
        }
    }

    /** Защита брони: у Земли самая крепкая, у Тьмы и Воздуха самая лёгкая. */
    private static Map<ArmorItem.Type, Integer> defense(Element e) {
        int helmet, chest, legs, boots;
        switch (e) {
            case EARTH -> { helmet = 3; chest = 8; legs = 6; boots = 3; }
            case DARKNESS, AIR -> { helmet = 2; chest = 6; legs = 5; boots = 2; }
            default -> { helmet = 3; chest = 7; legs = 5; boots = 2; }
        }
        Map<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.HELMET, helmet);
        map.put(ArmorItem.Type.CHESTPLATE, chest);
        map.put(ArmorItem.Type.LEGGINGS, legs);
        map.put(ArmorItem.Type.BOOTS, boots);
        map.put(ArmorItem.Type.BODY, chest + 3);
        return map;
    }

    /** Гримуар, в который записаны все заклинания школы (для творческой вкладки). */
    public static ItemStack fullBook(Element e) {
        ItemStack stack = new ItemStack(BOOKS.get(e).get());
        int size = SpellbookItem.allSpells(e).size();
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < size; i++) all.add(i);
        stack.set(LEARNED_SPELLS.get(), List.copyOf(all));
        return stack;
    }

    // ===== Вкладка в творческом режиме =====
    public static final Supplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatableWithFallback("itemGroup.elementalmagic", "Стихийная магия"))
            .icon(() -> new ItemStack(BOOKS.get(Element.FIRE).get()))
            .displayItems((params, out) -> {
                out.accept(MAGIC_TABLE_ITEM.get());
                for (Element e : Element.values()) {
                    out.accept(fullBook(e));
                    out.accept(STAVES.get(e).get());
                    for (ArmorItem.Type type : ARMOR_TYPES) out.accept(ARMOR.get(e).get(type).get());
                    out.accept(AMULETS.get(e).get());
                    out.accept(RINGS.get(e).get());
                    for (DeferredItem<ScrollItem> scroll : SCROLLS.get(e)) out.accept(scroll.get());
                }
            })
            .build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ARMOR_MATERIALS.register(bus);
        COMPONENTS.register(bus);
        ATTACHMENTS.register(bus);
        TABS.register(bus);
    }

    private ModRegistry() {}
}
