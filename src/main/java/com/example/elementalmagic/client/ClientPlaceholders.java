package com.example.elementalmagic.client;

import com.example.elementalmagic.MagicMod;
import com.example.elementalmagic.ModRegistry;
import com.example.elementalmagic.ScrollItem;
import com.example.elementalmagic.entity.ModEntities;
import com.example.elementalmagic.magic.Element;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Временные картинки: ванильные модели в цвете стихии для предметов без своей модели. */
@EventBusSubscriber(modid = MagicMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientPlaceholders {
    /** Предметы, которым подставлена заглушка. Только их перекрашиваем. */
    private static final Set<Item> TINTED = ConcurrentHashMap.newKeySet();

    /** Какой ванильный предмет показывать вместо предмета мода. */
    private static String vanillaFor(String path) {
        if (path.endsWith("_spawn_egg")) return "zombie_spawn_egg";
        if (path.endsWith("_spellbook")) return "book";
        if (path.endsWith("_staff")) return "stick";
        if (path.endsWith("_scroll")) return "paper";
        if (path.endsWith("_amulet")) return "heart_of_the_sea";
        if (path.endsWith("_ring")) return "gold_nugget";
        if (path.endsWith("_helmet")) return "iron_helmet";
        if (path.endsWith("_chestplate")) return "iron_chestplate";
        if (path.endsWith("_leggings")) return "iron_leggings";
        if (path.endsWith("_boots")) return "iron_boots";
        if (path.endsWith("_shard")) return "iron_nugget";
        if (path.endsWith("_crystal")) return "quartz";
        if (path.endsWith("_core")) return "snowball";
        if (path.equals("mana")) return "ghast_tear";
        if (path.equals("magic_table")) return "crying_obsidian";
        return null;
    }

    private static boolean tinted(String vanilla) {
        return !vanilla.equals("zombie_spawn_egg") && !vanilla.equals("crying_obsidian");
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientPlaceholders::createAccessorySlotTexture);
    }

    /** Создаёт текстуру слота для аксессуаров, если её ещё нет. */
    private static void createAccessorySlotTexture() {
        try {
            Path runDir = Path.of(System.getProperty("user.dir"));
            Path texture = runDir.resolve("src/main/resources/assets/" + MagicMod.MODID + "/textures/gui/accessory_slot.png");
            if (Files.exists(texture)) return;

            BufferedImage img = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(139, 139, 139, 255));
            g.drawRect(0, 0, 17, 17);
            g.dispose();

            Files.createDirectories(texture.getParent());
            ImageIO.write(img, "PNG", texture.toFile());
        } catch (Exception ignored) {}
    }

    @SubscribeEvent
    public static void onModelsBaked(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        TINTED.clear();

        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(MagicMod.MODID)) continue;

            // Есть своя модель: ничего не трогаем
            ResourceLocation ownModel = ResourceLocation.fromNamespaceAndPath(MagicMod.MODID,
                    "models/item/" + id.getPath() + ".json");
            if (resources.getResource(ownModel).isPresent()) continue;

            String vanilla = vanillaFor(id.getPath());
            if (vanilla == null) continue;
            BakedModel model = models.get(ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace(vanilla)));
            if (model == null) continue;

            models.put(ModelResourceLocation.inventory(id), model);
            if (tinted(vanilla)) TINTED.add(item);
        }

        // Магический стол как блок
        ResourceLocation ownState = ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, "blockstates/magic_table.json");
        if (resources.getResource(ownState).isEmpty()) {
            BakedModel table = models.get(BlockModelShaper.stateToModelLocation(Blocks.CRYING_OBSIDIAN.defaultBlockState()));
            if (table != null) {
                for (BlockState state : ModRegistry.MAGIC_TABLE.get().getStateDefinition().getPossibleStates()) {
                    models.put(BlockModelShaper.stateToModelLocation(state), table);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        Map<Item, Integer> colors = new HashMap<>();
        for (Element e : Element.values()) {
            int c = e.color;
            colors.put(ModRegistry.BOOKS.get(e).get(), c);
            colors.put(ModRegistry.STAVES.get(e).get(), c);
            colors.put(ModRegistry.AMULETS.get(e).get(), c);
            colors.put(ModRegistry.RINGS.get(e).get(), c);
            for (var armor : ModRegistry.ARMOR.get(e).values()) colors.put(armor.get(), c);
            for (DeferredItem<ScrollItem> scroll : ModRegistry.SCROLLS.get(e)) colors.put(scroll.get(), c);
        }
        for (ModEntities.MobEntry m : ModEntities.ALL) colors.put(m.material().get(), m.element().color);
        colors.put(ModRegistry.MANA_ITEM.get(), 0x3FA9F5);

        colors.forEach((item, color) -> event.register((stack, tintIndex) ->
                tintIndex == 0 && TINTED.contains(stack.getItem()) ? 0xFF000000 | color : 0xFFFFFFFF, item));
    }

    private ClientPlaceholders() {}
}
