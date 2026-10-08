package net.gasharaujo.timelesss.client;

import net.gasharaujo.timelesss.TimelesssMod;
import net.gasharaujo.timelesss.item.TimelesssItem;
import net.gasharaujo.timelesss.item.TimelesssLocator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = TimelesssMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
        Identifier.fromNamespaceAndPath(TimelesssMod.MOD_ID, "main")
    );
    public static final KeyMapping OPEN_KEY = new KeyMapping(
        "key.timelesss.open", GLFW.GLFW_KEY_KP_5, CATEGORY
    );
    public static final KeyMapping ACTIVATE_KEY = new KeyMapping(
        "key.timelesss.activate", GLFW.GLFW_KEY_KP_ENTER, CATEGORY
    );
    public static final KeyMapping TIME_BINDS_KEY = new KeyMapping(
        "key.timelesss.time_binds", GLFW.GLFW_KEY_F12, CATEGORY
    );

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerSpecialRenderer(RegisterSpecialModelRendererEvent event) {
        event.register(
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TimelesssMod.MOD_ID, "timelesss"),
            TimelesssSpecialRenderer.Unbaked.MAP_CODEC
        );
        event.register(
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TimelesssMod.MOD_ID, "time_wrench"),
            TimeWrenchSpecialRenderer.Unbaked.MAP_CODEC
        );
    }

    @SubscribeEvent
    public static void registerItemModels(RegisterItemModelsEvent event) {
        event.register(
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TimelesssMod.MOD_ID, "animated_timelesss"),
            AnimatedTimelesssItemModel.Unbaked.MAP_CODEC
        );
        event.register(
            net.minecraft.resources.Identifier.fromNamespaceAndPath(TimelesssMod.MOD_ID, "animated_time_wrench"),
            AnimatedTimeWrenchItemModel.Unbaked.MAP_CODEC
        );
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
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
