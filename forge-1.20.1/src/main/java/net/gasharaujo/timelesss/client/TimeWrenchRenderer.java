package net.gasharaujo.timelesss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.gasharaujo.timelesss.item.TimeWrenchItem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class TimeWrenchRenderer extends BlockEntityWithoutLevelRenderer {
    public TimeWrenchRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        float seconds = Util.getMillis() / 1000.0F;
        pose.pushPose();
        pose.translate(0.5F, 0.42F, 0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(-42.0F));

        renderCube(Blocks.NETHERITE_BLOCK, pose, buffers, light, 0.0F, -0.23F, 0.0F,
            0.16F, 0.78F, 0.16F, 0.0F);
        renderCube(Blocks.REDSTONE_BLOCK, pose, buffers, light, 0.0F, 0.25F, 0.0F,
            0.31F, 0.31F, 0.31F, seconds * 55.0F);

        Block satellite = TimeWrenchItem.hasTarget(stack) ? Blocks.EMERALD_BLOCK : Blocks.QUARTZ_BLOCK;
        float orbit = seconds * 48.0F;
        for (int index = 0; index < 4; index++) {
            double angle = Math.toRadians(orbit + index * 90.0F);
            renderCube(satellite, pose, buffers, light,
                (float) Math.cos(angle) * 0.34F, 0.25F, (float) Math.sin(angle) * 0.34F,
                0.11F, 0.11F, 0.11F, -orbit);
        }
        pose.popPose();
    }

    private static void renderCube(Block block, PoseStack pose, MultiBufferSource buffers, int light,
                                   float x, float y, float z, float sx, float sy, float sz, float rotation) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.scale(sx, sy, sz);
        pose.translate(-0.5F, -0.5F, -0.5F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
            block.defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY
        );
        pose.popPose();
    }
}
