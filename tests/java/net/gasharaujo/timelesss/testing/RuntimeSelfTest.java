package net.gasharaujo.timelesss.testing;

import java.lang.reflect.Method;
import net.gasharaujo.timelesss.*;
import net.gasharaujo.timelesss.block.entity.*;
import net.gasharaujo.timelesss.item.*;
import net.gasharaujo.timelesss.resource.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Runs in an isolated server world, never in a user's existing save. */
public final class RuntimeSelfTest {
    private static int checks;
    private static void eq(long expected, long actual, String message) {
        checks++;
        if(expected!=actual) throw new AssertionError(message+": expected "+expected+", got "+actual);
    }
    private static void ok(boolean actual, String message) { eq(1,actual?1:0,message); }
    public static void run(ServerLevel level) throws Exception {
        eq(25,ResourceConfig.personalMax(),"default personal limit");
        eq(50,ResourceConfig.distributorMax(),"default network limit");
        ok(ResourceConfig.networkResource()==ResourceType.ENERGY,"default FE");
        ok(ResourceConfig.personalResource()==ResourceType.NONE,"default free personal item");
        ResourceConfig.PERSONAL_MAX.set(73);ResourceConfig.PERSONAL_MAX.clearCache();
        ItemStack personal=new ItemStack(TimelesssMod.TIMELESSS.get());
        TimelesssItem.setMultiplier(personal,999);
        eq(73,TimelesssItem.getMultiplier(personal),"server clamps to custom personal limit");
        ResourceConfig.PERSONAL_MAX.set(1);ResourceConfig.PERSONAL_MAX.clearCache();
        eq(1,TimelesssItem.getMultiplier(personal),"lower limit clamps an existing item");
        ResourceConfig.PERSONAL_MAX.set(25);ResourceConfig.PERSONAL_MAX.clearCache();
        ResourceConfig.DISTRIBUTOR_MAX.set(81);ResourceConfig.DISTRIBUTOR_MAX.clearCache();
        eq(81,TimeNetwork.maxMultiplier(),"custom distributor limit");
        ResourceConfig.DISTRIBUTOR_MAX.set(50);ResourceConfig.DISTRIBUTOR_MAX.clearCache();

        eq(900,ResourceConfig.cost(ResourceType.ENERGY,3),"default quadratic FE");
        eq(900,ResourceConfig.cost(ResourceType.FLUID,3),"default quadratic fluid");
        eq(900,ResourceConfig.cost(ResourceType.GAS,3),"default quadratic gas");
        eq(9,ResourceConfig.cost(ResourceType.ITEM,3),"default quadratic items");
        ResourceConfig.CURVE.set(ResourceConfig.Curve.LINEAR);ResourceConfig.CURVE.clearCache();
        eq(300,ResourceConfig.cost(ResourceType.ENERGY,3),"linear FE");
        eq(300,ResourceConfig.cost(ResourceType.FLUID,3),"linear fluid");
        eq(3,ResourceConfig.cost(ResourceType.ITEM,3),"linear items");
        eq(6,ResourceConfig.cost(ResourceType.XP,3),"two XP per level");
        eq(60,ResourceConfig.cost(ResourceType.TIME,3),"one stored tick per accelerated tick");
        ResourceConfig.CURVE.set(ResourceConfig.Curve.DEFAULT);ResourceConfig.CURVE.clearCache();
        eq(Long.MAX_VALUE,ResourceMath.multiply(Long.MAX_VALUE,2),"overflow saturates");
        eq(Integer.MAX_VALUE,ResourceMath.capacity(Integer.MAX_VALUE,1000,5),"capacity overflow saturates");

        TimeDistributorBlockEntity distributor=new TimeDistributorBlockEntity(BlockPos.ZERO,TimelesssMod.TIME_DISTRIBUTOR.get().defaultBlockState());
        BatteryHubBlockEntity hub=new BatteryHubBlockEntity(BlockPos.ZERO,TimelesssMod.BATTERY_HUB.get().defaultBlockState());
        for(ResourceType type:new ResourceType[]{ResourceType.ENERGY,ResourceType.FLUID,ResourceType.GAS,ResourceType.ITEM}) {
            ResourceConfig.NETWORK_RESOURCE.set(type);ResourceConfig.NETWORK_RESOURCE.clearCache();
            long hubCap=type==ResourceType.ENERGY?40000:type==ResourceType.ITEM?16*64:16000;
            long batteryCap=type==ResourceType.ENERGY?10000:type==ResourceType.ITEM?2*64:1000;
            long distributorCap=type==ResourceType.ENERGY?100000:type==ResourceType.ITEM?2*64:1000;
            // A fresh hub is used for every mode.
            hub=new BatteryHubBlockEntity(BlockPos.ZERO,TimelesssMod.BATTERY_HUB.get().defaultBlockState());
            eq(hubCap,hub.getResourceCapacity(),type+" hub capacity");
            eq(distributorCap,distributor.getResourceCapacity(),type+" distributor capacity");
            ok(hub.insertBattery(),"extra battery accepted");
            eq(hubCap+batteryCap,hub.getResourceCapacity(),type+" hub expansion");
            ResourceStorage store=distributor.resources();
            eq(11,store.insert(type,11,true),type+" simulated insertion");
            eq(0,store.amount(type),type+" simulation leaves storage unchanged");
            eq(distributorCap,store.insert(type,Long.MAX_VALUE,false),type+" capacity enforced");
            eq(7,store.extract(type,7,true),type+" simulated extraction");
            eq(distributorCap,store.amount(type),type+" simulation leaves fuel unchanged");
            eq(7,store.extract(type,7,false),type+" real extraction");
            ResourceBlockEntity loaded=(ResourceBlockEntity)PlatformChecks.roundTrip(distributor,level);
            eq(distributorCap-7,loaded.getResourceStored(),type+" survives Minecraft block serialization");
            store.set(type,0);
            PlatformChecks.handlers(store);
            if(type==ResourceType.GAS) gas(store);
        }
        ResourceConfig.NETWORK_RESOURCE.set(ResourceType.FLUID);ResourceConfig.NETWORK_RESOURCE.clearCache();
        distributor.resources().insert(ResourceType.FLUID,700,false);
        ResourceConfig.FLUID.set("minecraft:water");ResourceConfig.FLUID.clearCache();
        eq(0,distributor.resources().amount(ResourceType.FLUID),"changing fluid ID does not convert lava to water");
        ResourceConfig.FLUID.set("");ResourceConfig.FLUID.clearCache();
        eq(700,distributor.resources().amount(ResourceType.FLUID),"old fluid preserved under original ID");
        ResourceConfig.NETWORK_RESOURCE.set(ResourceType.ITEM);ResourceConfig.NETWORK_RESOURCE.clearCache();
        ok(distributor.resources().validItem(new ItemStack(Items.COAL)),"empty item ID defaults to coal");
        ok(!distributor.resources().validItem(new ItemStack(Items.DIAMOND)),"wrong fuel item rejected");
        ResourceConfig.ITEM.set("minecraft:ender_pearl");ResourceConfig.ITEM.clearCache();
        eq(32,distributor.resources().capacity(ResourceType.ITEM),"non-64 item stack size respected");
        ResourceConfig.ITEM.set("");ResourceConfig.ITEM.clearCache();

        Player player=PlatformChecks.player(level);
        ResourceConfig.PERSONAL_RESOURCE.set(ResourceType.NONE);ResourceConfig.PERSONAL_RESOURCE.clearCache();
        ok(PersonalResources.canPay(player,personal),"NONE needs no fuel");
        TimelesssItem.setMultiplier(personal,3);
        ResourceConfig.PERSONAL_RESOURCE.set(ResourceType.XP);ResourceConfig.PERSONAL_RESOURCE.clearCache();
        player.experienceLevel=10;player.experienceProgress=0;player.totalExperience=160;
        eq(160,PersonalResources.xp(player),"raw XP balance");
        PersonalResources.pay(player,personal);
        eq(154,PersonalResources.xp(player),"XP deducted as points rather than levels");
        player.experienceLevel=0;player.experienceProgress=0;
        ok(!PersonalResources.canPay(player,personal),"insufficient XP blocks use");
        ResourceConfig.PERSONAL_RESOURCE.set(ResourceType.TIME);ResourceConfig.PERSONAL_RESOURCE.clearCache();
        player.getInventory().setItem(0,personal);
        ResourceStorage personalStore=ResourceStorage.item(personal);
        eq(0,personalStore.amount(ResourceType.TIME),"new item has no accumulated time");
        for(int i=0;i<60;i++) PersonalResources.recharge(player);
        eq(60,personalStore.amount(ResourceType.TIME),"inventory time charging");
        ok(PersonalResources.canPay(player,personal),"stored time pays activation");
        PersonalResources.pay(player,personal);
        eq(0,personalStore.amount(ResourceType.TIME),"exact accelerated tick cost");
        ResourceConfig.TIME_LIMIT.set(5L);ResourceConfig.TIME_LIMIT.clearCache();
        for(int i=0;i<9;i++) PersonalResources.recharge(player);
        eq(5,personalStore.amount(ResourceType.TIME),"configured time cap");
        player.getInventory().setItem(0,ItemStack.EMPTY);
        PersonalResources.recharge(player);
        eq(5,personalStore.amount(ResourceType.TIME),"item outside inventory does not recharge");
        ResourceConfig.TIME_LIMIT.set(0L);ResourceConfig.TIME_LIMIT.clearCache();
        ResourceConfig.PERSONAL_RESOURCE.set(ResourceType.FLUID);ResourceConfig.PERSONAL_RESOURCE.clearCache();
        eq(16000,personalStore.capacity(ResourceType.FLUID),"personal default tank");
        ok(!PersonalResources.canPay(player,personal),"empty personal tank blocks activation");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,personal);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.LAVA_BUCKET));
        ResourceInteraction.fillPersonal(player,net.minecraft.world.InteractionHand.MAIN_HAND,personal);
        eq(1000,personalStore.amount(ResourceType.FLUID),"refill personal tank from offhand bucket");
        ok(!TimelesssAccelerator.accelerateBlock(player,level,new BlockPos(2,100,0),net.minecraft.core.Direction.UP,personal),"unsupported target returns false");
        eq(1000,personalStore.amount(ResourceType.FLUID),"unsupported target costs nothing");
        var entity=new net.minecraft.world.entity.decoration.ArmorStand(level,0,100,0);
        entity.setNoGravity(true);
        ok(TimelesssAccelerator.accelerateEntity(player,entity,personal),"successful personal activation");
        eq(100,personalStore.amount(ResourceType.FLUID),"successful activation charges personal fluid");
        ok(!TimelesssAccelerator.accelerateEntity(player,entity,personal),"insufficient fuel blocks actual acceleration");
        eq(100,personalStore.amount(ResourceType.FLUID),"failed activation preserves remaining fluid");
        ResourceConfig.PERSONAL_RESOURCE.set(ResourceType.ENERGY);ResourceConfig.PERSONAL_RESOURCE.clearCache();
        personalStore.insert(ResourceType.ENERGY,1000,false);
        PersonalResources.pay(player,personal);
        eq(100,personalStore.amount(ResourceType.ENERGY),"personal matches distributor FE rule");
        ResourceConfig.PERSONAL_RESOURCE.set(ResourceType.DURABILITY);ResourceConfig.PERSONAL_RESOURCE.clearCache();
        ResourceConfig.DURABILITY_LIMIT.set(9);ResourceConfig.DURABILITY_LIMIT.clearCache();
        PersonalResources.pay(player,personal);
        ok(personal.isEmpty(),"depleted durability breaks item");
        ResourceConfig.DURABILITY_LIMIT.set(2048);ResourceConfig.DURABILITY_LIMIT.clearCache();
        ResourceConfig.PERSONAL_RESOURCE.set(ResourceType.NONE);ResourceConfig.PERSONAL_RESOURCE.clearCache();

        network(level,(ServerPlayer)player);
        ResourceConfig.NETWORK_RESOURCE.set(ResourceType.ENERGY);ResourceConfig.NETWORK_RESOURCE.clearCache();
        ResourceConfig.CURVE.set(ResourceConfig.Curve.DEFAULT);ResourceConfig.CURVE.clearCache();
        MultiResourceSelfTest.run(level);
        AdvancementSelfTest.run(level);
        System.out.println("TIMELESSS_SELFTEST_PASSED checks="+checks);
    }

    private static void network(ServerLevel level,ServerPlayer player) throws Exception {
        BlockPos hubPos=new BlockPos(0,100,0), distributorPos=new BlockPos(1,100,0);
        level.getChunk(hubPos);
        level.setBlockAndUpdate(hubPos,TimelesssMod.BATTERY_HUB.get().defaultBlockState());
        level.setBlockAndUpdate(distributorPos,TimelesssMod.TIME_DISTRIBUTOR.get().defaultBlockState());
        BatteryHubBlockEntity hub=(BatteryHubBlockEntity)level.getBlockEntity(hubPos);
        TimeDistributorBlockEntity distributor=(TimeDistributorBlockEntity)level.getBlockEntity(distributorPos);
        hub.setOwner(player.getUUID());distributor.setOwner(player.getUUID());
        String dimension=PlatformChecks.dimension(level);
        TimeNetwork.addEnergyNode(player,dimension,hubPos.asLong());
        TimeNetwork.addEnergyNode(player,dimension,distributorPos.asLong());
        TimeNetwork.addBind(player,dimension,new BlockPos(4,100,0).asLong());
        TimeNetwork.setMultiplier(player,dimension,new BlockPos(4,100,0).asLong(),3);
        Method pay=TimeNetwork.class.getDeclaredMethod("payForNextSecond",ServerPlayer.class);pay.setAccessible(true);
        for(ResourceType type:new ResourceType[]{ResourceType.ENERGY,ResourceType.FLUID,ResourceType.GAS,ResourceType.ITEM}) {
            ResourceConfig.NETWORK_RESOURCE.set(type);ResourceConfig.NETWORK_RESOURCE.clearCache();
            hub.resources().set(type,0);distributor.resources().set(type,0);
            PlatformChecks.capabilities(level,distributorPos);
            long cost=ResourceConfig.cost(type,3);
            hub.resources().insert(type,cost-1,false);
            pay.invoke(null,player);
            eq(0,PlatformChecks.paid(player),type+" insufficient network fuel blocks acceleration");
            eq(cost-1,hub.getResourceStored(),type+" failed payment consumes nothing");
            distributor.resources().insert(type,1,false);
            pay.invoke(null,player);
            eq(20,PlatformChecks.paid(player),type+" combined nodes pay full second");
            eq(0,hub.getResourceStored()+distributor.getResourceStored(),type+" exact network charge");
            TimeNetwork.setMultiplier(player,dimension,new BlockPos(4,100,0).asLong(),4);
            eq(0,PlatformChecks.paid(player),"editing binds invalidates previous payment");
            TimeNetwork.setMultiplier(player,dimension,new BlockPos(4,100,0).asLong(),3);
            ok(TimeNetwork.snapshot(player).startsWith("0|"),"snapshot uses active resource amount");
        }
        hub.setOwner(java.util.UUID.randomUUID());
        hub.resources().insert(ResourceType.ITEM,99,false);
        pay.invoke(null,player);
        eq(0,PlatformChecks.paid(player),"another owner's fuel is not consumed");
    }

    private static void gas(ResourceStorage store) throws Exception {
        if(MekanismBridge.blockCapability()==null) { ok(MekanismBridge.handler(store)==null,"Mekanism is optional");return; }
        Object handler=MekanismBridge.handler(store);
        ok(handler!=null,"Mekanism gas/chemical handler registered");
        Class<?> stackClass;
        try { stackClass=Class.forName("mekanism.api.chemical.gas.GasStack"); }
        catch(ClassNotFoundException e) { stackClass=Class.forName("mekanism.api.chemical.ChemicalStack"); }
        Method make=MekanismBridge.class.getDeclaredMethod("stack",Class.class,long.class);make.setAccessible(true);
        Object offered=make.invoke(null,stackClass,300L);
        Class<?> action=Class.forName("mekanism.api.Action");
        Object simulate=action.getField("SIMULATE").get(null),execute=action.getField("EXECUTE").get(null);
        Method insert=null,extract=null;
        for(Method m:handler.getClass().getInterfaces()[0].getMethods()) {
            if(m.getName().equals("insertChemical") && m.getParameterCount()==3) insert=m;
            if(m.getName().equals("extractChemical") && m.getParameterCount()==3) extract=m;
        }
        insert.invoke(handler,0,offered,simulate);
        eq(0,store.amount(ResourceType.GAS),"gas simulation leaves storage unchanged");
        insert.invoke(handler,0,offered,execute);
        eq(300,store.amount(ResourceType.GAS),"gas insertion through real Mekanism API");
        extract.invoke(handler,0,100L,execute);
        eq(200,store.amount(ResourceType.GAS),"gas extraction through real Mekanism API");
        store.set(ResourceType.GAS,0);
    }
}
