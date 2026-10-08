package net.gasharaujo.timelesss.block.entity;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public interface PlayerEnergyNode {
    @Nullable UUID getOwner();

    int getEnergyStored();

    int getEnergyCapacity();

    int extractEnergy(int amount);
}
