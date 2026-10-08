package net.gasharaujo.timelesss.client;

import net.gasharaujo.timelesss.TimelesssMod;
import net.gasharaujo.timelesss.item.TimelesssLocator;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TimelesssMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientGameEvents {
    private ClientGameEvents() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        while (ClientEvents.OPEN_KEY.consumeClick()) {
            TimelesssLocator.findPreferred(minecraft.player).ifPresent(ClientEvents::openMultiplierScreen);
        }
        while (ClientEvents.ACTIVATE_KEY.consumeClick()) {
            TimelesssLocator.findPreferred(minecraft.player).ifPresent(source -> activate(minecraft, source));
        }
        while (ClientEvents.TIME_BINDS_KEY.consumeClick()) {
            TimelesssMod.NETWORK.sendToServer(new TimelesssMod.RequestTimeBindsMessage());
        }
    }

    /**
     * InputEvent.Key is emitted even while a screen owns keyboard input, unlike the
     * key mapping's click counter. This keeps activation available from GUIs without
     * registering a listener on the screen event lifecycle.
     */
    @SubscribeEvent
    public static void keyPressed(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen == null
            || (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_REPEAT)
            || !ClientEvents.ACTIVATE_KEY.matches(event.getKey(), event.getScanCode())) {
            return;
        }

        TimelesssLocator.findPreferred(minecraft.player).ifPresent(source -> activate(minecraft, source));
    }

    private static boolean activate(Minecraft minecraft, TimelesssLocator.Found source) {
        HitResult target = minecraft.hitResult;
        if (target instanceof BlockHitResult block) {
            TimelesssMod.NETWORK.sendToServer(new TimelesssMod.ActivateMessage(
                source.sourceType(), source.sourceIndex(), 0, block.getBlockPos().asLong(),
                block.getDirection().get3DDataValue()
            ));
            return true;
        } else if (target instanceof EntityHitResult entity) {
            TimelesssMod.NETWORK.sendToServer(new TimelesssMod.ActivateMessage(
                source.sourceType(), source.sourceIndex(), 1, entity.getEntity().getId(), 0
            ));
            return true;
        }
        return false;
    }
}
