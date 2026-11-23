package net.gasharaujo.timelesss.testing;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    public static ServerPlayer advancementPlayer(ServerLevel level, String name) {
        var profile = new GameProfile(java.util.UUID.randomUUID(), name);
        return new ServerPlayer(level.getServer(), level, profile, net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override
            public void sendSystemMessage(net.minecraft.network.chat.Component message) {
            }
        };
    }

    public static boolean advancementDone(ServerLevel level, ServerPlayer player, String path) {
        var advancement = level.getServer().getAdvancements().get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("timelesss", path));
        if (advancement == null) {
            throw new AssertionError("Advancement did not load: " + path);
        }
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    public static void craft(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        net.neoforged.neoforge.event.EventHooks.firePlayerCraftingEvent(player, stack, new net.minecraft.world.SimpleContainer(9));
    }

    public static boolean migratedBatteryStacks(ServerLevel level, ServerPlayer player) {
        var stack = new net.minecraft.world.item.ItemStack(TimelesssMod.BATTERY.get());
        var legacy = new net.minecraft.nbt.CompoundTag();
        legacy.putLong("ENERGY:", 100);
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            stack, tag -> tag.put("timelesss_resources", legacy));
        stack.getItem().verifyComponentsAfterLoad(stack);
        return net.minecraft.world.item.ItemStack.isSameItemSameComponents(stack, new net.minecraft.world.item.ItemStack(TimelesssMod.BATTERY.get()));
    }

    public static BlockEntity roundTrip(BlockEntity entity, ServerLevel level) { return BlockEntity.loadStatic(entity.getBlockPos(),entity.getBlockState(),entity.saveWithFullMetadata(level.registryAccess()),level.registryAccess()); }
    public static int paid(Player player) { return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound("timelesss_time_network").getInt("paid_ticks"); }
    public static void handlers(net.gasharaujo.timelesss.resource.ResourceStorage s) {
        var type=s.type();
        if(type==net.gasharaujo.timelesss.resource.ResourceType.ENERGY) {
            var energy=net.gasharaujo.timelesss.resource.ResourceHandlers.energy(s);
            eq(99,energy.receiveEnergy(99,true),"FE simulated insert");eq(0,s.amount(type),"FE simulation leaves amount unchanged");
            energy.receiveEnergy(99,false);eq(99,s.amount(type),"real FE insertion");
        } else if(type==net.gasharaujo.timelesss.resource.ResourceType.FLUID) {
            var fluid=net.gasharaujo.timelesss.resource.ResourceHandlers.fluid(s,net.minecraft.world.item.ItemStack.EMPTY);
            var fuel=new net.neoforged.neoforge.fluids.FluidStack(net.gasharaujo.timelesss.resource.ResourceStorage.fuelFluid(),100);
            var wrong=new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,100);
            eq(0,fluid.fill(wrong,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE),"wrong fluid rejected");
            eq(100,fluid.fill(fuel,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE),"fluid simulated insert");eq(0,s.amount(type),"fluid simulation leaves amount unchanged");
            fluid.fill(fuel,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);eq(100,s.amount(type),"real fluid insertion");
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
    public static void capabilities(ServerLevel level, net.minecraft.core.BlockPos pos) { capabilities(level,pos,ResourceConfig.networkResource()); }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static void capabilities(ServerLevel level, net.minecraft.core.BlockPos pos,net.gasharaujo.timelesss.resource.ResourceType type) {
        net.neoforged.neoforge.capabilities.BlockCapability blockCap=switch(type) {
            case ENERGY -> net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK;
            case FLUID -> net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK;
            case ITEM -> net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK;
            default -> (net.neoforged.neoforge.capabilities.BlockCapability)net.gasharaujo.timelesss.resource.MekanismBridge.blockCapability();
        };
        if(blockCap==null) return;
        if(level.getCapability(blockCap,pos,net.minecraft.core.Direction.UP)==null) throw new AssertionError(type+" block capability not exposed");
        var stack=new net.minecraft.world.item.ItemStack(TimelesssMod.BATTERY.get());
        net.neoforged.neoforge.capabilities.ItemCapability itemCap=switch(type) {
            case ENERGY -> net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM;
            case FLUID -> net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM;
            case ITEM -> net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ITEM;
            default -> (net.neoforged.neoforge.capabilities.ItemCapability)net.gasharaujo.timelesss.resource.MekanismBridge.itemCapability();
        };
        if(stack.getCapability(itemCap)!=null) throw new AssertionError(type+" battery capability still exposed");
    }

    public static void configured() throws Exception {
        Class<?> wrapper;
        try { wrapper=Class.forName("com.mrcrayfish.configured.impl.neoforge.NeoForgeListValue"); }
        catch(ClassNotFoundException e) { System.out.println("CONFIGURED_MODEL_TEST_SKIPPED (optional mod absent)");return; }
        for(var option:java.util.List.of(ResourceConfig.NETWORK_RESOURCE,ResourceConfig.FLUID,ResourceConfig.GAS,ResourceConfig.ITEM)) {
            Object spec=ResourceConfig.SPECIFICATION.getSpec().get(option.getPath());
            Object model=wrapper.getConstructors()[0].newInstance(option,spec);
            if(!(wrapper.getMethod("getConverted").invoke(model) instanceof java.util.List<?>)) throw new AssertionError("Configured cannot convert list");
            Object rows=wrapper.getMethod("get").invoke(model);
            if(!(boolean)wrapper.getMethod("isValid",Object.class).invoke(model,rows)) throw new AssertionError("Configured rejects normalized rows");
            if(!wrapper.getMethod("getTranslationKey").invoke(model).toString().startsWith("config.timelesss.")) throw new AssertionError("Configured field has no friendly name");
            var updated=new java.util.ArrayList<>((java.util.List<?>)rows);
            wrapper.getMethod("set",java.util.List.class).invoke(model,updated);
            if(!(wrapper.getMethod("getConverted").invoke(model) instanceof java.util.List<?>)) throw new AssertionError("Configured cannot edit rows");
        }
        System.out.println("CONFIGURED_MODEL_TEST_PASSED");
    }

    public static void multiHandlers(net.gasharaujo.timelesss.resource.ResourceStorage s) {
        var fluid=net.gasharaujo.timelesss.resource.ResourceHandlers.fluid(s,net.minecraft.world.item.ItemStack.EMPTY);
        eq(2,fluid.getTanks(),"multiple fluid tanks");
        if(fluid.getFluidInTank(1).getFluid()!=net.minecraft.world.level.material.Fluids.WATER) throw new AssertionError("wrong second fluid identity");
        var water=new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,10);
        if(fluid.isFluidValid(0,water) || !fluid.isFluidValid(1,water)) throw new AssertionError("fluid tank filter mismatch");
        eq(10,fluid.drain(water,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE).getAmount(),"second fluid simulated extraction");
        eq(400,s.amount(net.gasharaujo.timelesss.resource.ResourceType.FLUID,"minecraft:water"),"fluid simulation preserves second material");
        var items=net.gasharaujo.timelesss.resource.ResourceHandlers.items(s);
        if(!items.getStackInSlot(1).is(net.minecraft.world.item.Items.CHARCOAL)) throw new AssertionError("wrong second item identity");
        eq(2,items.extractItem(1,2,true).getCount(),"second item simulated extraction");
        eq(4,s.amount(net.gasharaujo.timelesss.resource.ResourceType.ITEM,"minecraft:charcoal"),"item simulation preserves second material");
    }
}
