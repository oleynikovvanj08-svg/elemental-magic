package com.example.elementalmagic.boss.client;

import com.example.elementalmagic.MagicMod;
import com.example.elementalmagic.boss.TimeKeeperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Хранитель времени рисуется на стандартной модели гуманоида (скин 64x64, как у игрока),
 * увеличенной в 1.4 раза. Текстура: assets/elementalmagic/textures/entity/time_keeper.png
 */
public class TimeKeeperRenderer extends HumanoidMobRenderer<TimeKeeperEntity, HumanoidModel<TimeKeeperEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MagicMod.MODID, "textures/entity/time_keeper.png");

    public TimeKeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.7f);
    }

    @Override
    public ResourceLocation getTextureLocation(TimeKeeperEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(TimeKeeperEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.4f, 1.4f, 1.4f);
    }
}
