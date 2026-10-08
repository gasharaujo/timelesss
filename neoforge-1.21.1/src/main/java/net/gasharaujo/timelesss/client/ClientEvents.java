package net.gasharaujo.timelesss.client;

import net.gasharaujo.timelesss.TimelesssMod;
import net.gasharaujo.timelesss.item.TimelesssItem;
import net.gasharaujo.timelesss.item.TimelesssLocator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = TimelesssMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    public static final KeyMapping OPEN_KEY = new KeyMapping(
        "key.timelesss.open", GLFW.GLFW_KEY_KP_5, "key.categories.timelesss"
    );
    public static final KeyMapping ACTIVATE_KEY = new KeyMapping(
        "key.timelesss.activate", GLFW.GLFW_KEY_KP_ENTER, "key.categories.timelesss"
    );
    public static final KeyMapping TIME_BINDS_KEY = new KeyMapping(
        "key.timelesss.time_binds", GLFW.GLFW_KEY_F12, "key.categories.timelesss"
    );

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private final TimelesssRenderer renderer = new TimelesssRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        }, TimelesssMod.TIMELESSS.get());
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KEY);
        event.register(ACTIVATE_KEY);
        event.register(TIME_BINDS_KEY);
    }

    public static void openMultiplierScreen(TimelesssLocator.Found source) {
        Minecraft.getInstance().setScreen(
            new MultiplierScreen(source, TimelesssItem.getMultiplier(source.stack()))
        );
    }

    public static void openTimeBindsScreen(String snapshot) {
        Minecraft.getInstance().setScreen(new TimeBindsScreen(snapshot));
    }
}
