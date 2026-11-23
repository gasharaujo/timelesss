package net.gasharaujo.timelesss;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class TimelesssAdvancements {
    private TimelesssAdvancements() {
    }

    public static void award(ServerPlayer player, String name) {
        var advancement = player.level().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(TimelesssMod.MOD_ID, name));
        if (advancement != null) {
            player.getAdvancements().award(advancement, "earned");
        }
    }
}
