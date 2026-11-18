package net.gasharaujo.timelesss.testing;

import net.minecraft.server.level.ServerLevel;
import net.gasharaujo.timelesss.ResourceConfig;
import net.gasharaujo.timelesss.TimelesssMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="timelesss")
public final class PlatformChecks {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void started(net.minecraftforge.event.server.ServerStartedEvent event) {
        if(!Boolean.getBoolean("timelesss.selftest")) return;
        try {
            // Avoid racing Forge autosave/file-watcher while exercising every config mode.
            ResourceConfig.SPECIFICATION.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
            RuntimeSelfTest.run(event.getServer().overworld());
        }
        catch(Throwable error) { error.printStackTrace();System.out.println("TIMELESSS_SELFTEST_FAILED"); }
        finally { event.getServer().halt(false); }
    }
    public static Player player(ServerLevel level) { return net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level); }
    public static BlockEntity roundTrip(BlockEntity entity, ServerLevel level) { return BlockEntity.loadStatic(entity.getBlockPos(),entity.getBlockState(),entity.saveWithFullMetadata()); }
    public static int paid(Player player) { return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound("timelesss_time_network").getInt("paid_ticks"); }
    public static void handlers(net.gasharaujo.timelesss.resource.ResourceStorage s) {
        var type=s.type();
        if(type==net.gasharaujo.timelesss.resource.ResourceType.ENERGY) {
            var energy=net.gasharaujo.timelesss.resource.ResourceHandlers.energy(s);
            eq(99,energy.receiveEnergy(99,true),"FE simulated insert");eq(0,s.amount(type),"FE simulation leaves amount unchanged");
            energy.receiveEnergy(99,false);eq(99,s.amount(type),"real FE insertion");
        } else if(type==net.gasharaujo.timelesss.resource.ResourceType.FLUID) {
            var fluid=net.gasharaujo.timelesss.resource.ResourceHandlers.fluid(s,net.minecraft.world.item.ItemStack.EMPTY);
            var fuel=new net.minecraftforge.fluids.FluidStack(net.gasharaujo.timelesss.resource.ResourceStorage.fuelFluid(),100);
            var wrong=new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,100);
            eq(0,fluid.fill(wrong,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE),"wrong fluid rejected");
            eq(100,fluid.fill(fuel,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE),"fluid simulated insert");eq(0,s.amount(type),"fluid simulation leaves amount unchanged");
            fluid.fill(fuel,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);eq(100,s.amount(type),"real fluid insertion");
        } else if(type==net.gasharaujo.timelesss.resource.ResourceType.ITEM) {
            var items=net.gasharaujo.timelesss.resource.ResourceHandlers.items(s);
            var coal=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL,8);
            var wrong=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,8);
            eq(8,items.insertItem(1,wrong,false).getCount(),"wrong item rejected");
            eq(0,items.insertItem(1,coal,true).getCount(),"simulated insertion into specified slot");eq(0,s.amount(type),"item simulation leaves amount unchanged");
            items.insertItem(1,coal,false);eq(0,s.slotAmount(0),"other slot unchanged");eq(8,s.slotAmount(1),"correct item slot persisted");
            items.extractItem(1,3,false);eq(5,s.slotAmount(1),"item extraction");
        }
        s.set(type,0);
    }
    private static void eq(long expected,long actual,String message) {
        if(expected!=actual) throw new AssertionError(message+": expected "+expected+", got "+actual);
    }
    public static String dimension(ServerLevel level) { return level.dimension().location().toString(); }

    @SuppressWarnings({"rawtypes","unchecked"})
    public static void capabilities(ServerLevel level, net.minecraft.core.BlockPos pos) {
        var type=ResourceConfig.networkResource();
        net.minecraftforge.common.capabilities.Capability cap=switch(type) {
            case ENERGY -> net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY;
            case FLUID -> net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER;
            case ITEM -> net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER;
            default -> (net.minecraftforge.common.capabilities.Capability)net.gasharaujo.timelesss.resource.MekanismBridge.blockCapability();
        };
        if(cap==null) return;
        if(!level.getBlockEntity(pos).getCapability(cap,net.minecraft.core.Direction.UP).isPresent()) throw new AssertionError(type+" block capability not exposed");
        var stack=new net.minecraft.world.item.ItemStack(TimelesssMod.BATTERY.get());
        if(type==net.gasharaujo.timelesss.resource.ResourceType.FLUID) cap=net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM;
        if(!stack.getCapability(cap).isPresent()) throw new AssertionError(type+" battery capability not exposed");
    }
}
