package com.example.elementalmagic.client;

import com.example.elementalmagic.MagicMod;
import com.example.elementalmagic.entity.ElementalMob;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

public class ElementalMobRenderer extends HumanoidMobRenderer<ElementalMob, HumanoidModel<ElementalMob>> {
    /** Светлая ванильная текстура, которая перекрашивается в цвет стихии. */
    private static final ResourceLocation PLACEHOLDER =
            ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

    private final ResourceLocation texture;
    private final float scale;
    private final boolean placeholder;

    public ElementalMobRenderer(EntityRendererProvider.Context ctx, String id, float scale) {
        this(ctx, ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, "textures/entity/" + id + ".png"), scale);
    }

    private ElementalMobRenderer(EntityRendererProvider.Context ctx, ResourceLocation own, float scale) {
        this(ctx, own, scale, ctx.getResourceManager().getResource(own).isPresent());
    }

    private ElementalMobRenderer(EntityRendererProvider.Context ctx, ResourceLocation own, float scale, boolean hasOwn) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(hasOwn ? ModelLayers.ZOMBIE : ModelLayers.SKELETON)), 0.5f * scale);
        this.texture = hasOwn ? own : PLACEHOLDER;
        this.scale = scale;
        this.placeholder = !hasOwn;
        if (placeholder) addLayer(new TintLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(ElementalMob entity) {
        return texture;
    }

    @Override
    protected void scale(ElementalMob entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(scale, scale, scale);
    }

    /** Для заглушки обычная модель не рисуется: её рисует TintLayer в цвете стихии. */
    @Override
    protected RenderType getRenderType(ElementalMob mob, boolean bodyVisible, boolean translucent, boolean glowing) {
        return placeholder ? null : super.getRenderType(mob, bodyVisible, translucent, glowing);
    }

    private static class TintLayer extends RenderLayer<ElementalMob, HumanoidModel<ElementalMob>> {
        TintLayer(RenderLayerParent<ElementalMob, HumanoidModel<ElementalMob>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, ElementalMob mob,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            if (mob.isInvisible()) return;
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(PLACEHOLDER));
            getParentModel().renderToBuffer(poseStack, vc, light,
                    LivingEntityRenderer.getOverlayCoords(mob, 0f), 0xFF000000 | mob.element.color);
        }
    }
}
