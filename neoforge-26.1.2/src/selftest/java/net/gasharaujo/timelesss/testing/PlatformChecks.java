package net.gasharaujo.timelesss.testing;

import net.minecraft.server.level.ServerLevel;
import net.gasharaujo.timelesss.ResourceConfig;
import net.gasharaujo.timelesss.TimelesssMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

@net.neoforged.fml.common.EventBusSubscriber(modid="timelesss")
public final class PlatformChecks {
    @net.neoforged.bus.api.SubscribeEvent
    public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if(!Boolean.getBoolean("timelesss.selftest")) return;
        try { RuntimeSelfTest.run(event.getServer().overworld()); }
        catch(Throwable error) { error.printStackTrace();System.out.println("TIMELESSS_SELFTEST_FAILED"); }
        finally { event.getServer().halt(false); }
    }
    public static Player player(ServerLevel level) { return net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level); }
    public static BlockEntity roundTrip(BlockEntity entity, ServerLevel level) { return BlockEntity.loadStatic(entity.getBlockPos(),entity.getBlockState(),entity.saveWithFullMetadata(level.registryAccess()),level.registryAccess()); }
    public static int paid(Player player) { return player.getPersistentData().getCompoundOrEmpty(Player.PERSISTED_NBT_TAG).getCompoundOrEmpty("timelesss_time_network").getIntOr("paid_ticks",0); }
    public static void handlers(net.gasharaujo.timelesss.resource.ResourceStorage s) {
        var handlers=new net.gasharaujo.timelesss.resource.ResourceHandlers(s);
        var type=s.type();
        if(type==net.gasharaujo.timelesss.resource.ResourceType.ENERGY) {
            var energy=handlers.energy();
            try(var tx=net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) { eq(99,energy.insert(99,tx),"FE insert in aborted transaction"); }
            eq(0,s.amount(type),"FE transaction rollback");
            try(var tx=net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) { energy.insert(99,tx);tx.commit(); }
            eq(99,s.amount(type),"FE transaction commit");
        } else if(type==net.gasharaujo.timelesss.resource.ResourceType.FLUID) {
            var fluid=handlers.fluid();
            var fuel=net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.gasharaujo.timelesss.resource.ResourceStorage.fuelFluid());
            var wrong=net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);
            try(var tx=net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) { eq(0,fluid.insert(0,wrong,100,tx),"wrong fluid rejected");eq(100,fluid.insert(0,fuel,100,tx),"fluid insertion"); }
            eq(0,s.amount(type),"fluid transaction rollback");
            try(var tx=net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) { fluid.insert(0,fuel,100,tx);tx.commit(); }
            eq(100,s.amount(type),"fluid transaction commit");
            ResourceConfig.PERSONAL_RESOURCE.set(type);ResourceConfig.PERSONAL_RESOURCE.clearCache();
            var stack=new net.minecraft.world.item.ItemStack(TimelesssMod.TIMELESSS.get());
            var access=net.neoforged.neoforge.transfer.access.ItemAccess.forStack(stack);
            var itemTank=new net.gasharaujo.timelesss.resource.ResourceHandlers(access).fluid();
            try(var tx=net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) { itemTank.insert(0,fuel,123,tx); }
            eq(0,net.gasharaujo.timelesss.resource.ResourceStorage.item(stack).amount(type),"item fluid transaction rollback");
            try(var tx=net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) { itemTank.insert(0,fuel,123,tx);tx.commit(); }
            eq(123,net.gasharaujo.timelesss.resource.ResourceStorage.item(stack).amount(type),"item fluid transaction commit");
            ResourceConfig.PERSONAL_RESOURCE.set(net.gasharaujo.timelesss.resource.ResourceType.NONE);ResourceConfig.PERSONAL_RESOURCE.clearCache();
        } else if(type==net.gasharaujo.timelesss.resource.ResourceType.ITEM) {
            var items=handlers.items();
            var coal=net.neoforged.neoforge.transfer.item.ItemResource.of(net.minecraft.world.item.Items.COAL);
            var wrong=net.neoforged.neoforge.transfer.item.ItemResource.of(net.minecraft.world.item.Items.DIAMOND);
            try(var tx=net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) { eq(0,items.insert(1,wrong,8,tx),"wrong item rejected");eq(8,items.insert(1,coal,8,tx),"insert specified slot"); }
            eq(0,s.amount(type),"item transaction rollback");
            try(var tx=net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) { items.insert(1,coal,8,tx);tx.commit(); }
            eq(0,s.slotAmount(0),"other slot unchanged");eq(8,s.slotAmount(1),"correct item slot persisted");
        }
        s.set(type,0);
    }
    private static void eq(long expected,long actual,String message) {
        if(expected!=actual) throw new AssertionError(message+": expected "+expected+", got "+actual);
    }
    public static String dimension(ServerLevel level) { return level.dimension().identifier().toString(); }

    @SuppressWarnings({"rawtypes","unchecked"})
    public static void capabilities(ServerLevel level, net.minecraft.core.BlockPos pos) {
        var type=ResourceConfig.networkResource();
        net.neoforged.neoforge.capabilities.BlockCapability blockCap=switch(type) {
            case ENERGY -> net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK;
            case FLUID -> net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK;
            case ITEM -> net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK;
            default -> (net.neoforged.neoforge.capabilities.BlockCapability)net.gasharaujo.timelesss.resource.MekanismBridge.blockCapability();
        };
        if(blockCap==null) return;
        if(level.getCapability(blockCap,pos,net.minecraft.core.Direction.UP)==null) throw new AssertionError(type+" block capability not exposed");
        var stack=new net.minecraft.world.item.ItemStack(TimelesssMod.BATTERY.get());
        net.neoforged.neoforge.capabilities.ItemCapability itemCap=switch(type) {
            case ENERGY -> net.neoforged.neoforge.capabilities.Capabilities.Energy.ITEM;
            case FLUID -> net.neoforged.neoforge.capabilities.Capabilities.Fluid.ITEM;
            case ITEM -> net.neoforged.neoforge.capabilities.Capabilities.Item.ITEM;
            default -> (net.neoforged.neoforge.capabilities.ItemCapability)net.gasharaujo.timelesss.resource.MekanismBridge.itemCapability();
        };
        if(net.neoforged.neoforge.transfer.access.ItemAccess.forStack(stack).getCapability(itemCap)==null) throw new AssertionError(type+" battery capability not exposed");
    }
}
