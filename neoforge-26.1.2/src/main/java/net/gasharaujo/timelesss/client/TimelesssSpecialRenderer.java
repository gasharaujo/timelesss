package net.gasharaujo.timelesss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class TimelesssSpecialRenderer implements NoDataSpecialModelRenderer {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    @Override
    public void submit(
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int light,
        int overlay,
        boolean hasFoil,
        int outlineColor
    ) {
        float seconds = Util.getMillis() / 1000.0F;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(0.82F, 0.82F, 0.82F);

        renderCube(Blocks.REDSTONE_BLOCK, poseStack, collector, light, overlay, outlineColor,
            0.0F, 0.0F, 0.0F, 0.62F, 45.0F, -seconds * 55.0F);

        float orbitAngle = seconds * 48.0F;
        for (int index = 0; index < 4; index++) {
            double vertical = Math.toRadians(orbitAngle + index * 90.0F);
            renderCube(Blocks.QUARTZ_BLOCK, poseStack, collector, light, overlay, outlineColor,
                (float) Math.cos(vertical) * 0.62F,
                (float) Math.sin(vertical) * 0.62F,
                0.0F,
                0.19F,
                0.0F,
                -orbitAngle);

            double horizontal = Math.toRadians(orbitAngle + index * 90.0F);
            renderCube(Blocks.COAL_BLOCK, poseStack, collector, light, overlay, outlineColor,
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
        SubmitNodeCollector collector,
        int light,
        int overlay,
        int outlineColor,
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

        BlockModelRenderState renderState = new BlockModelRenderState();
        Minecraft.getInstance().getBlockModelResolver().update(renderState, block.defaultBlockState(), DISPLAY_CONTEXT);
        renderState.submitMultiLayer(poseStack, collector, light, overlay, outlineColor);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    output.accept(new Vector3f(x, y, z));
                }
            }
        }
    }

    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public TimelesssSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new TimelesssSpecialRenderer();
        }

        @Override
        public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
