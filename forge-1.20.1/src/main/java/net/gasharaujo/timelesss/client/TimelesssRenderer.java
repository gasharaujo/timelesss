package net.gasharaujo.timelesss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class TimelesssRenderer extends BlockEntityWithoutLevelRenderer {
    public TimelesssRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(
        ItemStack stack,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int light,
        int overlay
    ) {
        float seconds = Util.getMillis() / 1000.0F;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(0.82F, 0.82F, 0.82F);

        renderCube(Blocks.REDSTONE_BLOCK, poseStack, buffers, light,
            0.0F, 0.0F, 0.0F, 0.62F, 45.0F, -seconds * 55.0F);

        float orbitAngle = seconds * 48.0F;
        for (int index = 0; index < 4; index++) {
            double vertical = Math.toRadians(orbitAngle + index * 90.0F);
            renderCube(Blocks.QUARTZ_BLOCK, poseStack, buffers, light,
                (float) Math.cos(vertical) * 0.62F,
                (float) Math.sin(vertical) * 0.62F,
                0.0F,
                0.19F,
                0.0F,
                -orbitAngle);

            double horizontal = Math.toRadians(orbitAngle + index * 90.0F);
            renderCube(Blocks.COAL_BLOCK, poseStack, buffers, light,
                (float) Math.cos(horizontal) * 0.62F,
                0.0F,
                (float) Math.sin(horizontal) * 0.62F,
                0.19F,
                0.0F,
                -orbitAngle);
        }

        poseStack.popPose();
    }

    private static void renderCube(
        Block block,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int light,
        float x,
        float y,
        float z,
        float size,
        float xRotation,
        float yRotation
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(xRotation));
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotation));
        poseStack.scale(size, size, size);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
            block.defaultBlockState(),
            poseStack,
            buffers,
            light,
            OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
    }
}
