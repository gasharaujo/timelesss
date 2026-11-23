package net.gasharaujo.timelesss.resource;

import net.gasharaujo.timelesss.TimelesssMod;
import net.gasharaujo.timelesss.block.entity.ResourceBlockEntity;
import net.neoforged.neoforge.capabilities.*;

public final class ResourceCapabilities {
    private ResourceCapabilities() {}
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, TimelesssMod.TIME_DISTRIBUTOR_BLOCK_ENTITY.get(), (be,side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, TimelesssMod.BATTERY_HUB_BLOCK_ENTITY.get(), (be,side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, TimelesssMod.TIME_DISTRIBUTOR_BLOCK_ENTITY.get(), (be,side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, TimelesssMod.BATTERY_HUB_BLOCK_ENTITY.get(), (be,side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, TimelesssMod.TIME_DISTRIBUTOR_BLOCK_ENTITY.get(), (be,side) -> be.getItemHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, TimelesssMod.BATTERY_HUB_BLOCK_ENTITY.get(), (be,side) -> be.getItemHandler());
        event.registerItem(Capabilities.Energy.ITEM, (stack,access) -> ResourceStorage.item(stack).enabled(ResourceType.ENERGY) ? new ResourceHandlers(access).energy() : null, TimelesssMod.TIMELESSS.get());
        event.registerItem(Capabilities.Fluid.ITEM, (stack,access) -> ResourceStorage.item(stack).enabled(ResourceType.FLUID) ? new ResourceHandlers(access).fluid() : null, TimelesssMod.TIMELESSS.get());
        gas(event);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void gas(RegisterCapabilitiesEvent event) {
        Object block=MekanismBridge.blockCapability();
        if(block instanceof BlockCapability cap) {
            ICapabilityProvider provider=(be,side) -> { ResourceStorage s=((ResourceBlockEntity)be).resources(); return s.enabled(ResourceType.GAS) ? MekanismBridge.handler(s) : null; };
            event.registerBlockEntity(cap, TimelesssMod.TIME_DISTRIBUTOR_BLOCK_ENTITY.get(), provider);
            event.registerBlockEntity(cap, TimelesssMod.BATTERY_HUB_BLOCK_ENTITY.get(), provider);
        }
    }
}
