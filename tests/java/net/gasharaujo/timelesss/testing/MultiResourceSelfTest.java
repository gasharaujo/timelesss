package net.gasharaujo.timelesss.testing;

import java.math.BigDecimal;
import java.util.*;
import net.gasharaujo.timelesss.*;
import net.gasharaujo.timelesss.block.entity.*;
import net.gasharaujo.timelesss.item.TimelesssItem;
import net.gasharaujo.timelesss.resource.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;

public final class MultiResourceSelfTest {
    private static int checks;
    private static void ok(boolean value,String message) { checks++;if(!value) throw new AssertionError(message); }
    private static void eq(long expected,long actual,String message) { ok(expected==actual,message+": expected "+expected+", got "+actual); }
    public static void run(ServerLevel level) throws Exception {
        ok(FuelRules.validTypes("ENERGY"),"legacy scalar resource");
        ok(FuelRules.validTypes(List.of("ENERGY","FLUID","ITEM")),"resource list");
        ok(!FuelRules.validTypes(List.of("XP")),"invalid network resource rejected");
        ok(!FuelRules.validTypes(List.of()),"empty resource selection rejected");
        Object[] forms={"mekanism:hydrogen = 1, mekanism:ethene = 0.25",List.of("mekanism:hydrogen",1,"mekanism:ethene",0.25),List.of(List.of("mekanism:hydrogen",1),List.of("mekanism:ethene",0.25)),new LinkedHashMap<>(Map.of("mekanism:hydrogen",1,"mekanism:ethene",0.25)),List.of(Map.of("id","mekanism:hydrogen","modifier",1),Map.of("id","mekanism:ethene","modifier",0.25))};
        for(Object form:forms) {
            var parsed=FuelRules.materials(form,"mekanism:hydrogen");
            eq(2,parsed.size(),"material format parsed");
            ok(parsed.stream().anyMatch(m -> m.id().equals("mekanism:ethene") && m.modifier().compareTo(new BigDecimal("0.25"))==0),"modifier preserved");
        }
        for(String bad:List.of("minecraft:coal = 0","minecraft:coal = -1","minecraft:coal = NaN","minecraft:coal = Infinity","invalid id","minecraft:coal,minecraft:coal")) ok(!FuelRules.validMaterials(bad),"invalid material rejected: "+bad);
        eq(1,FuelRules.materials("minecraft:coal","minecraft:coal").get(0).modifier().longValueExact(),"ID alone uses default modifier");
        configuredValues();

        ResourceConfig.NETWORK_RESOURCE.set(new ArrayList<>(List.of("ENERGY","FLUID","GAS","ITEM")));ResourceConfig.NETWORK_RESOURCE.clearCache();
        ResourceConfig.FLUID.set(List.of("minecraft:lava","minecraft:water = 0.5"));ResourceConfig.FLUID.clearCache();
        ResourceConfig.GAS.set(List.of("mekanism:hydrogen","mekanism:oxygen = 0.25"));ResourceConfig.GAS.clearCache();
        ResourceConfig.ITEM.set(List.of("minecraft:coal","minecraft:charcoal = 0.25"));ResourceConfig.ITEM.clearCache();
        ResourceConfig.normalize();
        net.minecraft.nbt.CompoundTag[] oldData={new net.minecraft.nbt.CompoundTag()};
        oldData[0].putInt("ITEM:minecraft:coal#1",8);
        var legacy=new ResourceStorage(ResourceStorage.Kind.DISTRIBUTOR,() -> oldData[0],tag -> oldData[0]=tag,() -> 0);
        eq(8,legacy.amount(ResourceType.ITEM,"minecraft:coal"),"previous 2.5 item balance retained");
        legacy.insert(ResourceType.ITEM,"minecraft:charcoal",4,false);
        eq(8,legacy.amount(ResourceType.ITEM,"minecraft:coal"),"adding another material does not overwrite old balance");
        legacy.extract(ResourceType.ITEM,"minecraft:coal",8,false);
        eq(0,legacy.slotAmount(1),"legacy slot remains empty after extraction");
        var distributor=new TimeDistributorBlockEntity(BlockPos.ZERO,TimelesssMod.TIME_DISTRIBUTOR.get().defaultBlockState());
        ResourceStorage s=distributor.resources();
        eq(100000,s.capacity(ResourceType.ENERGY),"FE capacity independent");
        eq(1000,s.capacity(ResourceType.FLUID),"fluid capacity independent");
        eq(1000,s.capacity(ResourceType.GAS),"gas capacity independent");
        s.insert(ResourceType.ENERGY,99,false);
        eq(600,s.insert(ResourceType.FLUID,"minecraft:lava",600,false),"first fluid inserted");
        eq(400,s.insert(ResourceType.FLUID,"minecraft:water",600,false),"fluids share capacity");
        eq(0,s.insert(ResourceType.FLUID,"minecraft:lava",1,false),"shared fluid tank cannot overflow");
        eq(900,s.insert(ResourceType.GAS,"mekanism:hydrogen",900,false),"gas tank does not compete with fluid");
        eq(100,s.insert(ResourceType.GAS,"mekanism:oxygen",300,false),"gases share their own capacity");
        eq(64,s.insert(ResourceType.ITEM,"minecraft:coal",64,false),"first item stack");
        eq(4,s.insert(ResourceType.ITEM,"minecraft:charcoal",4,false),"second material in another slot");
        ok(s.slotId(0).equals("minecraft:coal") && s.slotId(1).equals("minecraft:charcoal"),"item identities stay separate");
        PlatformChecks.multiHandlers(s);
        var loaded=(ResourceBlockEntity)PlatformChecks.roundTrip(distributor,level);
        eq(400,loaded.resources().amount(ResourceType.FLUID,"minecraft:water"),"second fluid survives save/load");
        eq(100,loaded.resources().amount(ResourceType.GAS,"mekanism:oxygen"),"second gas survives save/load");
        eq(4,loaded.resources().amount(ResourceType.ITEM,"minecraft:charcoal"),"second item survives save/load");
        gas(s);

        var battery=ResourceStorage.item(new ItemStack(TimelesssMod.BATTERY.get()));
        battery.insert(ResourceType.ENERGY,90,false);battery.insert(ResourceType.FLUID,"minecraft:water",25,false);battery.insert(ResourceType.ITEM,"minecraft:charcoal",3,false);
        s.set(ResourceType.ENERGY,0);s.set(ResourceType.FLUID,"minecraft:water",0);s.set(ResourceType.FLUID,"minecraft:lava",0);s.set(ResourceType.ITEM,0);
        battery.transferTo(s);
        eq(90,s.amount(ResourceType.ENERGY),"battery transfers FE");eq(25,s.amount(ResourceType.FLUID,"minecraft:water"),"battery transfers second fluid");eq(3,s.amount(ResourceType.ITEM,"minecraft:charcoal"),"battery transfers second item");
        eq(0,battery.amount(ResourceType.ENERGY)+battery.amount(ResourceType.FLUID)+battery.amount(ResourceType.ITEM),"battery debited once");
        payment();network(level);personal(level);
        ResourceConfig.NETWORK_RESOURCE.set(List.of("ENERGY"));ResourceConfig.NETWORK_RESOURCE.clearCache();
        ResourceConfig.FLUID.set("");ResourceConfig.FLUID.clearCache();ResourceConfig.GAS.set("");ResourceConfig.GAS.clearCache();ResourceConfig.ITEM.set("");ResourceConfig.ITEM.clearCache();
        ResourceConfig.PERSONAL_RESOURCE.set(ResourceType.NONE);ResourceConfig.PERSONAL_RESOURCE.clearCache();ResourceConfig.normalize();
        System.out.println("TIMELESSS_MULTI_SELFTEST_PASSED checks="+checks);
    }
    private static void configuredValues() throws Exception {
        ResourceConfig.NETWORK_RESOURCE.set("ENERGY, FLUID");ResourceConfig.NETWORK_RESOURCE.clearCache();
        ResourceConfig.GAS.set(List.of(List.of("mekanism:hydrogen",1),List.of("mekanism:oxygen",0.25)));ResourceConfig.GAS.clearCache();
        ResourceConfig.normalize();
        for(Object raw:List.of(ResourceConfig.value(ResourceConfig.NETWORK_RESOURCE),ResourceConfig.value(ResourceConfig.FLUID),ResourceConfig.value(ResourceConfig.GAS),ResourceConfig.value(ResourceConfig.ITEM))) {
            ok(raw instanceof ArrayList<?>,"Configured receives a supported mutable list");
            ok(((List<?>)raw).stream().allMatch(String.class::isInstance),"Configured rows are all text");
        }
        PlatformChecks.configured();
        var config=new com.electronwill.nightconfig.toml.TomlParser().parse("[network]\nresource = [\"ENERGY\", \"GAS\"]\ngas = [[\"mekanism:hydrogen\", 1], [\"mekanism:oxygen\", 0.25]]\n");
        ResourceConfig.SPECIFICATION.correct(config);
        eq(2,FuelRules.types(config.get("network.resource")).size(),"real TOML resource array survives loader validation");
        eq(2,FuelRules.materials(config.get("network.gas"),"mekanism:hydrogen").size(),"real TOML associated array");
        var table=new com.electronwill.nightconfig.toml.TomlParser().parse("[network.gas]\n\"mekanism:hydrogen\" = 1\n\"mekanism:oxygen\" = 0.25\n");
        ResourceConfig.SPECIFICATION.correct(table);
        eq(2,FuelRules.materials(table.get("network.gas"),"mekanism:hydrogen").size(),"real TOML associated table survives loader validation");
    }
    private static void payment() {
        ResourceStorage s=ResourceStorage.item(new ItemStack(TimelesssMod.BATTERY.get()));Map<String,String> credit=new HashMap<>();
        s.insert(ResourceType.ENERGY,99,false);s.insert(ResourceType.FLUID,"minecraft:lava",40,false);s.insert(ResourceType.FLUID,"minecraft:water",49,false);s.insert(ResourceType.GAS,"mekanism:oxygen",25,false);
        var quote=FuelPayment.plan(List.of(s),s.types(),t -> ResourceConfig.cost(t,1),credit::get);
        ok(quote!=null && quote.debits().get(0).id().equals("mekanism:oxygen"),"alternative with sufficient modified balance selected");
        ok(quote.apply(credit::put),"alternative committed");
        eq(0,s.amount(ResourceType.GAS,"mekanism:oxygen"),"100 mB times 0.25 consumes 25 mB");
        eq(99,s.amount(ResourceType.ENERGY),"other kinds not charged");eq(49,s.amount(ResourceType.FLUID,"minecraft:water"),"insufficient material untouched");
        s.insert(ResourceType.ITEM,"minecraft:charcoal",1,false);
        for(int i=0;i<4;i++) { var plan=FuelPayment.plan(List.of(s),s.types(),t -> ResourceConfig.cost(t,1),credit::get);ok(plan!=null && plan.apply(credit::put),"fractional item payment "+i); }
        eq(0,s.amount(ResourceType.ITEM,"minecraft:charcoal"),"four 0.25 payments consume exactly one item");
        var before=s.snapshot();ok(FuelPayment.plan(List.of(s),s.types(),t -> ResourceConfig.cost(t,1),credit::get)==null,"exhausted alternatives cannot pay");ok(before.equals(s.snapshot()),"failed quote does not change balances");
        ResourceStorage second=ResourceStorage.item(new ItemStack(TimelesssMod.BATTERY.get()));s.set(ResourceType.ENERGY,50);second.set(ResourceType.ENERGY,50);
        var atomic=FuelPayment.plan(List.of(s,second),List.of(ResourceType.ENERGY),t -> 100,key -> "");second.extract(ResourceType.ENERGY,1,false);
        ok(!atomic.apply(credit::put),"unexpected partial debit rolls back");eq(50,s.amount(ResourceType.ENERGY),"first debit restored");eq(49,second.amount(ResourceType.ENERGY),"second debit restored to pre-payment balance");
    }
    private static void network(ServerLevel level) throws Exception {
        ServerPlayer player=(ServerPlayer)PlatformChecks.player(level);BlockPos pos=new BlockPos(8,100,0);
        level.setBlockAndUpdate(pos,TimelesssMod.TIME_DISTRIBUTOR.get().defaultBlockState());var block=(ResourceBlockEntity)level.getBlockEntity(pos);block.setOwner(player.getUUID());
        String dimension=PlatformChecks.dimension(level);TimeNetwork.addEnergyNode(player,dimension,pos.asLong());
        // Remove earlier scenario binds and use one target at 1x.
        for(var bind:TimeNetwork.getBinds(player)) TimeNetwork.removeBind(player,bind.dimension(),bind.position());
        TimeNetwork.addBind(player,dimension,pos.above().asLong());TimeNetwork.setMultiplier(player,dimension,pos.above().asLong(),1);
        for(ResourceType type:ResourceConfig.networkResources()) PlatformChecks.capabilities(level,pos,type);
        block.resources().insert(ResourceType.GAS,"mekanism:oxygen",25,false);
        var pay=TimeNetwork.class.getDeclaredMethod("payForNextSecond",ServerPlayer.class);pay.setAccessible(true);pay.invoke(null,player);
        eq(20,PlatformChecks.paid(player),"network accepts a non-primary resource");eq(0,block.resources().amount(ResourceType.GAS,"mekanism:oxygen"),"network applies material modifier");
        block.resources().insert(ResourceType.ITEM,"minecraft:charcoal",1,false);
        for(int i=0;i<4;i++) { pay.invoke(null,player);eq(20,PlatformChecks.paid(player),"network fractional credit persists between seconds"); }
        pay.invoke(null,player);eq(0,PlatformChecks.paid(player),"network stops once credit is exhausted");
        var views=NetworkResourceView.parse(TimeNetwork.snapshot(player).split("\\n")[0].split("\\|",-1)[6]);eq(7,views.size(),"UI snapshot contains every resource/material alternative");
        ok(views.stream().anyMatch(v -> v.label().equals("mekanism:oxygen") && v.consumption(100).equals("25")),"UI displays modified cost");
    }
    private static void personal(ServerLevel level) {
        ResourceConfig.PERSONAL_RESOURCE.set(ResourceType.FLUID);ResourceConfig.PERSONAL_RESOURCE.clearCache();
        var player=PlatformChecks.player(level);var item=new ItemStack(TimelesssMod.TIMELESSS.get());TimelesssItem.setMultiplier(item,1);var s=ResourceStorage.item(item);
        s.insert(ResourceType.FLUID,"minecraft:water",50,false);ok(PersonalResources.canPay(player,item),"personal accepts second fluid with modifier");PersonalResources.pay(player,item);eq(0,s.amount(ResourceType.FLUID,"minecraft:water"),"personal charges modified cost");
    }
    private static void gas(ResourceStorage s) throws Exception {
        Object handler=MekanismBridge.handler(s);if(handler==null) return;
        MethodInfo info=new MethodInfo(handler);eq(2,((Number)info.tanks.invoke(handler)).longValue(),"Mekanism exposes two materials");
        Class<?> stackClass;try { stackClass=Class.forName("mekanism.api.chemical.gas.GasStack"); } catch(ClassNotFoundException e) { stackClass=Class.forName("mekanism.api.chemical.ChemicalStack"); }
        var make=MekanismBridge.class.getDeclaredMethod("stack",Class.class,String.class,long.class);make.setAccessible(true);Object oxygen=make.invoke(null,stackClass,"mekanism:oxygen",200L);
        Object execute=Class.forName("mekanism.api.Action").getField("EXECUTE").get(null);
        info.insert.invoke(handler,0,oxygen,execute);eq(100,s.amount(ResourceType.GAS,"mekanism:oxygen"),"wrong gas tank rejects insertion");
        s.extract(ResourceType.GAS,"mekanism:oxygen",100,false);info.insert.invoke(handler,1,oxygen,execute);eq(100,s.amount(ResourceType.GAS,"mekanism:oxygen"),"second gas tank accepts within shared capacity");
    }
    private static final class MethodInfo {
        java.lang.reflect.Method tanks,insert;
        MethodInfo(Object handler) { for(var m:handler.getClass().getInterfaces()[0].getMethods()) { if(m.getName().equals("getTanks") || m.getName().equals("getChemicalTanks")) tanks=m;if(m.getName().equals("insertChemical") && m.getParameterCount()==3) insert=m; } }
    }
}
