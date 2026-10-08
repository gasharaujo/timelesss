package net.gasharaujo.timelesss.client;

import net.gasharaujo.timelesss.TimelesssMod;
import net.gasharaujo.timelesss.item.TimelesssItem;
import net.gasharaujo.timelesss.item.TimelesssLocator;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TimelesssMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
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
