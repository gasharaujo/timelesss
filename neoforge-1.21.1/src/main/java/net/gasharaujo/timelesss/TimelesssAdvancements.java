package net.gasharaujo.timelesss;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class TimelesssAdvancements {
    private TimelesssAdvancements() {
    }

    public static void award(ServerPlayer player, String name) {
        var advancement = player.getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath(TimelesssMod.MOD_ID, name));
        if (advancement != null) {
            player.getAdvancements().award(advancement, "earned");
        }
    }
}
