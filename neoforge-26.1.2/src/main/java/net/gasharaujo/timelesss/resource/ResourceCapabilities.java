package net.gasharaujo.timelesss.resource;

import net.gasharaujo.timelesss.TimelesssMod;
import net.gasharaujo.timelesss.block.entity.ResourceBlockEntity;
import net.minecraft.world.item.ItemStack;
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
        event.registerItem(Capabilities.Energy.ITEM, (stack,access) -> ResourceStorage.item(stack).type()==ResourceType.ENERGY ? new ResourceHandlers(access).energy() : null, TimelesssMod.TIMELESSS.get(), TimelesssMod.BATTERY.get());
        event.registerItem(Capabilities.Fluid.ITEM, (stack,access) -> ResourceStorage.item(stack).type()==ResourceType.FLUID ? new ResourceHandlers(access).fluid() : null, TimelesssMod.TIMELESSS.get(), TimelesssMod.BATTERY.get());
        event.registerItem(Capabilities.Item.ITEM, (stack,access) -> ResourceStorage.item(stack).type()==ResourceType.ITEM ? new ResourceHandlers(access).items() : null, TimelesssMod.BATTERY.get());
        gas(event);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void gas(RegisterCapabilitiesEvent event) {
        Object block=MekanismBridge.blockCapability(), item=MekanismBridge.itemCapability();
        if(block instanceof BlockCapability cap) {
            ICapabilityProvider provider=(be,side) -> { ResourceStorage s=((ResourceBlockEntity)be).resources(); return s.type()==ResourceType.GAS ? MekanismBridge.handler(s) : null; };
            event.registerBlockEntity(cap, TimelesssMod.TIME_DISTRIBUTOR_BLOCK_ENTITY.get(), provider);
            event.registerBlockEntity(cap, TimelesssMod.BATTERY_HUB_BLOCK_ENTITY.get(), provider);
        }
        if(item instanceof ItemCapability cap) event.registerItem(cap, (stack,context) -> MekanismBridge.handler(ResourceStorage.item((ItemStack)stack)), TimelesssMod.BATTERY.get());
    }
}
