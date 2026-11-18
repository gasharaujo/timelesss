package net.gasharaujo.timelesss.resource;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import javax.annotation.Nullable;

public final class ResourceCapabilities implements ICapabilityProvider {
    private final ResourceStorage storage;
    private final LazyOptional<Object> energy, fluid, items, gas;
    public ResourceCapabilities(ItemStack stack) {
        storage=ResourceStorage.item(stack);
        energy=LazyOptional.of(() -> ResourceHandlers.energy(storage));
        fluid=LazyOptional.of(() -> ResourceHandlers.fluid(storage,stack));
        items=LazyOptional.of(() -> ResourceHandlers.items(storage));
        gas=LazyOptional.of(() -> MekanismBridge.handler(storage));
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if(storage.enabled(ResourceType.ENERGY) && cap==ForgeCapabilities.ENERGY) return energy.cast();
        if(storage.enabled(ResourceType.FLUID) && cap==ForgeCapabilities.FLUID_HANDLER_ITEM) return fluid.cast();
        if(storage.enabled(ResourceType.ITEM) && cap==ForgeCapabilities.ITEM_HANDLER) return items.cast();
        if(storage.enabled(ResourceType.GAS) && cap==MekanismBridge.itemCapability()) return gas.cast();
        return LazyOptional.empty();
    }
}
