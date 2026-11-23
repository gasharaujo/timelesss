package net.gasharaujo.timelesss.resource;

import java.util.*;
import java.util.function.*;
import net.gasharaujo.timelesss.ResourceConfig;
import net.gasharaujo.timelesss.TimelesssMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.*;

/** Independent resource kinds; materials share their kind's tank or item slots. */
public final class ResourceStorage {
    public enum Kind { HUB, DISTRIBUTOR, BATTERY, PERSONAL }
    private final Kind kind;
    private final Supplier<CompoundTag> read;
    private final Consumer<CompoundTag> write;
    private final IntSupplier extras;
    public ResourceStorage(Kind kind,Supplier<CompoundTag> read,Consumer<CompoundTag> write,IntSupplier extras) {
        this.kind=kind;this.read=read;this.write=write;this.extras=extras;
    }
    public static ResourceStorage item(ItemStack stack) {
        return new ResourceStorage(stack.is(TimelesssMod.TIMELESSS.get()) ? Kind.PERSONAL : Kind.BATTERY,
            () -> stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getCompoundOrEmpty("timelesss_resources"), tag -> net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,stack,data -> data.put("timelesss_resources",tag)), () -> 0);
    }
    public List<ResourceType> types() { return kind==Kind.PERSONAL ? List.of(ResourceConfig.personalResource()) : ResourceConfig.networkResources(); }
    public ResourceType type() { return types().get(0); }
    public boolean enabled(ResourceType type) { return types().contains(type); }
    public String key(ResourceType type) { return key(type,ResourceConfig.id(type)); }
    public String key(ResourceType type,String id) { return type.name()+":"+id; }
    private long number(CompoundTag tag,String key) { return tag.getLongOr(key,0L); }
    private String text(CompoundTag tag,String key) { return tag.getStringOr(key,""); }
    public String credit(String key) { return text(read.get(),key); }
    public void credit(String key,String credit) { CompoundTag tag=read.get().copy();tag.putString(key,credit);write.accept(tag); }
    public long amount(ResourceType type) {
        if(!enabled(type)) return 0;
        if(type==ResourceType.ITEM) { long total=0;for(int i=0;i<slots();i++) total+=slotAmount(i);return total; }
        long total=0;
        for(var material:ResourceConfig.materials(type)) total=ResourceMath.add(total,amount(type,material.id()));
        return Math.min(capacity(type),total);
    }
    public long amount(ResourceType type,String id) {
        if(!enabled(type)) return 0;
        if(type==ResourceType.ITEM) { long total=0;for(int i=0;i<slots();i++) if(slotId(i).equals(id)) total+=slotAmount(i);return total; }
        return Math.min(capacity(type),Math.max(0,number(read.get(),key(type,id))));
    }
    public void set(ResourceType type,long amount) {
        if(type==ResourceType.ITEM) {
            CompoundTag tag=read.get().copy();
            long remaining=Math.max(0,amount);String id=ResourceConfig.id(type);
            for(int i=0;i<slots();i++) { int n=(int)Math.min(stackLimit(id),remaining);putSlot(tag,i,id,n);remaining-=n; }
            write.accept(tag);
        } else set(type,ResourceConfig.id(type),amount);
    }
    public void set(ResourceType type,String id,long amount) {
        if(type==ResourceType.ITEM) {
            for(int i=0;i<slots();i++) if(slotId(i).equals(id)) setSlot(i,id,0);
            insert(type,id,amount,false);return;
        }
        CompoundTag tag=read.get().copy();
        long own=amount(type,id), free=Math.max(0,capacity(type)-amount(type));
        tag.putLong(key(type,id),Math.max(0,Math.min(own+free,amount)));
        write.accept(tag);
    }
    public long insert(ResourceType type,long amount,boolean simulate) { return insert(type,ResourceConfig.id(type),amount,simulate); }
    public long insert(ResourceType type,String id,long amount,boolean simulate) {
        if(!enabled(type) || !ResourceConfig.accepts(type,id) || amount<=0) return 0;
        if(type==ResourceType.ITEM) {
            long moved=0;
            for(int i=0;i<slots() && moved<amount;i++) {
                int present=slotAmount(i);
                if(present>0 && !slotId(i).equals(id)) continue;
                int n=(int)Math.min(amount-moved,Math.max(0,stackLimit(id)-present));
                if(n>0 && !simulate) setSlot(i,id,present+n);
                moved+=n;
            }
            return moved;
        }
        long inserted=Math.min(amount,Math.max(0,capacity(type)-amount(type)));
        if(!simulate && inserted>0) set(type,id,amount(type,id)+inserted);
        return inserted;
    }
    public long extract(ResourceType type,long amount,boolean simulate) { return extract(type,ResourceConfig.id(type),amount,simulate); }
    public long extract(ResourceType type,String id,long amount,boolean simulate) {
        if(!enabled(type) || amount<=0) return 0;
        long extracted=Math.min(amount,amount(type,id));
        if(!simulate && extracted>0) {
            if(type==ResourceType.ITEM) {
                long remaining=extracted;
                for(int i=0;i<slots() && remaining>0;i++) if(slotId(i).equals(id)) {
                    int count=slotAmount(i), n=(int)Math.min(count,remaining);setSlot(i,id,count-n);remaining-=n;
                }
            } else set(type,id,amount(type,id)-extracted);
        }
        return extracted;
    }
    public long capacity(ResourceType type) {
        if(!enabled(type)) return 0;
        if(kind==Kind.PERSONAL) return switch(type) {
            case FLUID -> ResourceConfig.value(ResourceConfig.PERSONAL_TANK);
            case ENERGY -> ResourceConfig.value(ResourceConfig.PERSONAL_ENERGY);
            case DURABILITY -> ResourceConfig.value(ResourceConfig.DURABILITY_LIMIT);
            case TIME -> ResourceConfig.value(ResourceConfig.TIME_LIMIT)==0 ? Long.MAX_VALUE : ResourceConfig.value(ResourceConfig.TIME_LIMIT);
            default -> 0;
        };
        if(type==ResourceType.ITEM) { long capacity=0;for(int i=0;i<slots();i++) capacity+=slotLimit(i);return capacity; }
        if(type==ResourceType.ENERGY) return switch(kind) { case HUB -> ResourceMath.capacity(40000,10000,extras.getAsInt());case DISTRIBUTOR -> 100000;default -> 10000; };
        return switch(kind) {
            case HUB -> ResourceMath.capacity(ResourceConfig.value(ResourceConfig.HUB_TANK),ResourceConfig.value(ResourceConfig.BATTERY_TANK),extras.getAsInt());
            case DISTRIBUTOR -> ResourceConfig.value(ResourceConfig.DISTRIBUTOR_TANK);default -> ResourceConfig.value(ResourceConfig.BATTERY_TANK);
        };
    }
    public int tanks(ResourceType type) { return ResourceConfig.materials(type).size(); }
    public String tankId(ResourceType type,int index) { return ResourceConfig.materials(type).get(index).id(); }
    public long tankCapacity(ResourceType type,int index) { return Math.max(0,capacity(type)-amount(type))+amount(type,tankId(type,index)); }
    public int slots() { return switch(kind) {
        case HUB -> Math.min(32768,ResourceMath.capacity(ResourceConfig.value(ResourceConfig.HUB_SLOTS),ResourceConfig.value(ResourceConfig.BATTERY_SLOTS),extras.getAsInt()));
        case DISTRIBUTOR -> ResourceConfig.value(ResourceConfig.DISTRIBUTOR_SLOTS);default -> ResourceConfig.value(ResourceConfig.BATTERY_SLOTS);
    }; }
    public int slotLimit(int slot) { String id=slotId(slot);return id.isEmpty() ? ResourceConfig.materials(ResourceType.ITEM).stream().mapToInt(m -> stackLimit(m.id())).max().orElse(64) : stackLimit(id); }
    public int stackLimit() { return stackLimit(ResourceConfig.id(ResourceType.ITEM)); }
    public int stackLimit(String id) { return new ItemStack(fuelItem(id)).getMaxStackSize(); }
    public String slotId(int slot) {
        Objects.checkIndex(slot,slots());CompoundTag tag=read.get();
        String marker="slot_id#"+slot;
        if(tag.contains(marker)) return text(tag,marker);
        for(var material:ResourceConfig.materials(ResourceType.ITEM)) if(number(tag,key(ResourceType.ITEM,material.id())+"#"+slot)>0) return material.id();
        return "";
    }
    public int slotAmount(int slot) {
        String id=slotId(slot);if(id.isEmpty()) return 0;
        return (int)Math.max(0,Math.min(stackLimit(id),number(read.get(),key(ResourceType.ITEM,id)+"#"+slot)));
    }
    private void putSlot(CompoundTag tag,int slot,String id,int amount) {
        String previous=slotId(slot);
        if(!previous.isEmpty()) tag.putLong(key(ResourceType.ITEM,previous)+"#"+slot,0);
        int count=Math.max(0,Math.min(stackLimit(id),amount));
        tag.putString("slot_id#"+slot,count==0 ? "" : id);
        tag.putLong(key(ResourceType.ITEM,id)+"#"+slot,count);
    }
    public void setSlot(int slot,int amount) { String id=slotId(slot);setSlot(slot,id.isEmpty() ? ResourceConfig.id(ResourceType.ITEM) : id,amount); }
    public void setSlot(int slot,String id,int amount) {
        Objects.checkIndex(slot,slots());CompoundTag tag=read.get().copy();putSlot(tag,slot,id,amount);write.accept(tag);
    }
    public static String itemId(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    public static Item fuelItem() { return fuelItem(ResourceConfig.id(ResourceType.ITEM)); }
    public static Item fuelItem(String value) { Identifier id=Identifier.tryParse(value);return id==null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id); }
    public static Fluid fuelFluid() { return fuelFluid(ResourceConfig.id(ResourceType.FLUID)); }
    public static Fluid fuelFluid(String value) { Identifier id=Identifier.tryParse(value);return id==null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getValue(id); }
    public static String fluidId(Fluid fluid) { return BuiltInRegistries.FLUID.getKey(fluid).toString(); }
    public static Object gasChemical(Object registry) { return gasChemical(registry,ResourceConfig.id(ResourceType.GAS)); }
    public static Object gasChemical(Object registry,String value) {
        Identifier id=Identifier.tryParse(value);if(id==null) return null;return ((net.minecraft.core.Registry<?>)registry).getValue(id);
    }
    public boolean validItem(ItemStack stack) { return enabled(ResourceType.ITEM) && !stack.isEmpty() && ResourceConfig.accepts(ResourceType.ITEM,itemId(stack)) && ItemStack.isSameItemSameComponents(stack,new ItemStack(stack.getItem())); }
    public void transferTo(ResourceStorage target) {
        for(ResourceType type:types()) if(target.enabled(type)) for(var material:ResourceConfig.materials(type)) {
            long moved=target.insert(type,material.id(),amount(type,material.id()),false);extract(type,material.id(),moved,false);
        }
    }
    public CompoundTag snapshot() { return read.get().copy(); }
    public void restore(CompoundTag tag) { write.accept(tag.copy()); }
}
