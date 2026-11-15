package com.example.elementalmagic.boss.client;

import com.example.elementalmagic.MagicMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * Общий отрисовщик боссов на модели гуманоида (скин 64x64, как у игрока).
 * Текстура: assets/elementalmagic/textures/entity/<имя>.png
 */
public class HumanoidBossRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation texture;
    private final float scale;

    public HumanoidBossRenderer(EntityRendererProvider.Context context, String textureName, float scale) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.7f * scale);
        this.texture = ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, "textures/entity/" + textureName + ".png");
        this.scale = scale;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(scale, scale, scale);
    }
}
