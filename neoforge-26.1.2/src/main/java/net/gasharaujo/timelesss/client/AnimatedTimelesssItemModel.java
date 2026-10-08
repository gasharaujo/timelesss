package net.gasharaujo.timelesss.client;

import com.mojang.serialization.MapCodec;
import net.gasharaujo.timelesss.TimelesssMod;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/** Keeps the GUI atlas from caching the first frame of the animated special renderer. */
public final class AnimatedTimelesssItemModel extends SpecialModelWrapper<Void> {
    private AnimatedTimelesssItemModel(ModelRenderProperties properties, Matrix4fc transformation) {
        super(new TimelesssSpecialRenderer(), properties, transformation);
    }

    @Override
    public void update(
        ItemStackRenderState output,
        ItemStack item,
        ItemModelResolver resolver,
        ItemDisplayContext displayContext,
        @Nullable ClientLevel level,
        @Nullable ItemOwner owner,
        int seed
    ) {
        output.setAnimated();
        super.update(output, item, resolver, displayContext, level, owner, seed);
    }

    public record Unbaked() implements ItemModel.Unbaked {
        private static final Identifier BASE_MODEL =
            Identifier.fromNamespaceAndPath(TimelesssMod.MOD_ID, "item/timelesss");
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(BASE_MODEL);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            ResolvedModel base = baker.getModel(BASE_MODEL);
            TextureSlots textureSlots = base.getTopTextureSlots();
            ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, base, textureSlots);
            return new AnimatedTimelesssItemModel(properties, transformation);
        }

        @Override
        public MapCodec<? extends ItemModel.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
