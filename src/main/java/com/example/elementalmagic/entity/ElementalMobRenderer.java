package com.example.elementalmagic.client;

import com.example.elementalmagic.MagicMod;
import com.example.elementalmagic.entity.ElementalMob;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ElementalMobRenderer extends HumanoidMobRenderer<ElementalMob, HumanoidModel<ElementalMob>> {
    private final ResourceLocation texture;
    private final float scale;

    public ElementalMobRenderer(EntityRendererProvider.Context ctx, String id, float scale) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f * scale);
        this.texture = ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, "textures/entity/" + id + ".png");
        this.scale = scale;
    }

    @Override
    public ResourceLocation getTextureLocation(ElementalMob entity) {
        return texture;
    }

    @Override
    protected void scale(ElementalMob entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(scale, scale, scale);
    }
}
