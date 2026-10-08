package net.gasharaujo.timelesss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.gasharaujo.timelesss.item.TimeWrenchItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class TimeWrenchSpecialRenderer implements SpecialModelRenderer<Boolean> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    @Override
    public void submit(@Nullable Boolean linked, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
                       boolean foil, int outlineColor) {
        float seconds = Util.getMillis() / 1000.0F;
        pose.pushPose();
        pose.translate(0.5F, 0.42F, 0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(-42.0F));
        renderCube(Blocks.NETHERITE_BLOCK, pose, collector, light, overlay, outlineColor,
            0.0F, -0.23F, 0.0F, 0.16F, 0.78F, 0.16F, 0.0F);
        renderCube(Blocks.REDSTONE_BLOCK, pose, collector, light, overlay, outlineColor,
            0.0F, 0.25F, 0.0F, 0.31F, 0.31F, 0.31F, seconds * 55.0F);
        Block satellite = Boolean.TRUE.equals(linked) ? Blocks.EMERALD_BLOCK : Blocks.QUARTZ_BLOCK;
        float orbit = seconds * 48.0F;
        for (int index = 0; index < 4; index++) {
            double angle = Math.toRadians(orbit + index * 90.0F);
            renderCube(satellite, pose, collector, light, overlay, outlineColor,
                (float) Math.cos(angle) * 0.34F, 0.25F, (float) Math.sin(angle) * 0.34F,
                0.11F, 0.11F, 0.11F, -orbit);
        }
        pose.popPose();
    }

    private static void renderCube(Block block, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
                                   int outline, float x, float y, float z, float sx, float sy, float sz, float rotation) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.scale(sx, sy, sz);
        pose.translate(-0.5F, -0.5F, -0.5F);
        BlockModelRenderState state = new BlockModelRenderState();
        Minecraft.getInstance().getBlockModelResolver().update(state, block.defaultBlockState(), DISPLAY_CONTEXT);
        state.submitMultiLayer(pose, collector, light, overlay, outline);
        pose.popPose();
    }

    @Override public Boolean extractArgument(ItemStack stack) { return TimeWrenchItem.hasTarget(stack); }
    @Override public void getExtents(Consumer<Vector3fc> output) {
        for (int x = -1; x <= 1; x += 2) for (int y = -1; y <= 1; y += 2)
            for (int z = -1; z <= 1; z += 2) output.accept(new Vector3f(x, y, z));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Boolean> {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);
        @Override public TimeWrenchSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new TimeWrenchSpecialRenderer();
        }
        @Override public MapCodec<? extends SpecialModelRenderer.Unbaked<Boolean>> type() { return MAP_CODEC; }
    }
}
