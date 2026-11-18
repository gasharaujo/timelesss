package net.gasharaujo.timelesss.block.entity;

import java.util.UUID;
import net.gasharaujo.timelesss.resource.ResourceStorage;

public interface PlayerEnergyNode {
    UUID getOwner();
    ResourceStorage resources();
    default long getResourceStored() { return resources().amount(resources().type()); }
    default long getResourceCapacity() { return resources().capacity(resources().type()); }
    default long extractResource(long amount) { return resources().extract(resources().type(), amount, false); }
    default int getEnergyStored() { return (int)getResourceStored(); }
    default int getEnergyCapacity() { return (int)getResourceCapacity(); }
    default int extractEnergy(int amount) { return (int)extractResource(amount); }
}
