package net.gasharaujo.timelesss.resource;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.gasharaujo.timelesss.ResourceConfig;
import net.gasharaujo.timelesss.TimelesssMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Storage is keyed by both resource kind and registry ID, so changing config never converts fuel. */
public final class ResourceStorage {
    public enum Kind { HUB, DISTRIBUTOR, BATTERY, PERSONAL }
    private final Kind kind;
    private final Supplier<CompoundTag> read;
    private final Consumer<CompoundTag> write;
    private final java.util.function.IntSupplier extras;

    public ResourceStorage(Kind kind, Supplier<CompoundTag> read, Consumer<CompoundTag> write, java.util.function.IntSupplier extras) {
        this.kind = kind; this.read = read; this.write = write; this.extras = extras;
    }
    public static ResourceStorage item(ItemStack stack) {
        return new ResourceStorage(stack.is(TimelesssMod.TIMELESSS.get()) ? Kind.PERSONAL : Kind.BATTERY,
            () -> stack.getOrCreateTag().getCompound("timelesss_resources").copy(), tag -> stack.getOrCreateTag().put("timelesss_resources", tag), () -> 0);
    }
    public ResourceType type() { return kind == Kind.PERSONAL ? ResourceConfig.personalResource() : ResourceConfig.networkResource(); }
    public String key(ResourceType type) { return type.name() + ":" + ResourceConfig.id(type); }
    public long amount(ResourceType type) { if (type == ResourceType.ITEM) { CompoundTag tag=read.get(); long total = 0; for(int i=0;i<slots();i++) total += Math.max(0,Math.min(stackLimit(),tag.getInt(key(type) + "#" + i))); return total; } return Math.min(capacity(type), Math.max(0L, read.get().getLong(key(type)))); }
    public void set(ResourceType type, long amount) {
        if (type == ResourceType.ITEM) {
            long remaining = Math.max(0, amount);
            CompoundTag tag=read.get().copy();
            for(int i=0;i<slots();i++) { int n=(int)Math.min(stackLimit(), remaining); tag.putInt(key(type)+"#"+i,n); remaining-=n; }
            write.accept(tag); return;
        }
        CompoundTag tag = read.get().copy();
        tag.putLong(key(type), Math.max(0L, Math.min(capacity(type), amount)));
        write.accept(tag);
    }
    public long insert(ResourceType type, long amount, boolean simulate) {
        if (type != type() || amount <= 0) return 0;
        long inserted = Math.min(amount, Math.max(0L, capacity(type) - amount(type)));
        if (!simulate && inserted > 0) set(type, amount(type) + inserted);
        return inserted;
    }
    public long extract(ResourceType type, long amount, boolean simulate) {
        if (type != type() || amount <= 0) return 0;
        long extracted = Math.min(amount, amount(type));
        if (!simulate && extracted > 0) set(type, amount(type) - extracted);
        return extracted;
    }
    public long capacity(ResourceType type) {
        if (type != type()) return 0;
        if (kind == Kind.PERSONAL) return switch(type) {
            case FLUID -> ResourceConfig.value(ResourceConfig.PERSONAL_TANK);
            case ENERGY -> ResourceConfig.value(ResourceConfig.PERSONAL_ENERGY);
            case DURABILITY -> ResourceConfig.value(ResourceConfig.DURABILITY_LIMIT);
            case TIME -> ResourceConfig.value(ResourceConfig.TIME_LIMIT) == 0L ? Long.MAX_VALUE : ResourceConfig.value(ResourceConfig.TIME_LIMIT);
            default -> 0L;
        };
        if (type == ResourceType.ITEM) return ResourceMath.multiply(slots(), fuelItem().getDefaultInstance().getMaxStackSize());
        if (type == ResourceType.ENERGY) return switch(kind) {
            case HUB -> ResourceMath.capacity(40000, 10000, extras.getAsInt());
            case DISTRIBUTOR -> 100000; default -> 10000;
        };
        return switch(kind) {
            case HUB -> ResourceMath.capacity(ResourceConfig.value(ResourceConfig.HUB_TANK), ResourceConfig.value(ResourceConfig.BATTERY_TANK), extras.getAsInt());
            case DISTRIBUTOR -> ResourceConfig.value(ResourceConfig.DISTRIBUTOR_TANK);
            default -> ResourceConfig.value(ResourceConfig.BATTERY_TANK);
        };
    }
    public int stackLimit() { return new ItemStack(fuelItem()).getMaxStackSize(); }
    public int slotAmount(int slot) {
        java.util.Objects.checkIndex(slot, slots());
        return Math.max(0, Math.min(stackLimit(), read.get().getInt(key(ResourceType.ITEM) + "#" + slot)));
    }
    public void setSlot(int slot, int amount) {
        java.util.Objects.checkIndex(slot, slots());
        CompoundTag tag = read.get().copy();
        tag.putInt(key(ResourceType.ITEM) + "#" + slot, Math.max(0, Math.min(stackLimit(), amount)));
        write.accept(tag);
    }
    public int slots() {
        return switch(kind) {
            case HUB -> Math.min(32768, ResourceMath.capacity(ResourceConfig.value(ResourceConfig.HUB_SLOTS), ResourceConfig.value(ResourceConfig.BATTERY_SLOTS), extras.getAsInt()));
            case DISTRIBUTOR -> ResourceConfig.value(ResourceConfig.DISTRIBUTOR_SLOTS);
            default -> ResourceConfig.value(ResourceConfig.BATTERY_SLOTS);
        };
    }
    public static Object gasChemical(Object registry) {
        ResourceLocation id = ResourceLocation.tryParse(ResourceConfig.id(ResourceType.GAS));
        if(id == null) return null;
        return ((net.minecraftforge.registries.IForgeRegistry<?>)registry).getValue(id);
    }
    public static Item fuelItem() {
        ResourceLocation id = ResourceLocation.tryParse(ResourceConfig.id(ResourceType.ITEM));
        return id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
    }
    public static Fluid fuelFluid() {
        ResourceLocation id = ResourceLocation.tryParse(ResourceConfig.id(ResourceType.FLUID));
        return id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(id);
    }
    public boolean validItem(ItemStack stack) {
        return !stack.isEmpty() && ItemStack.isSameItemSameTags(stack, new ItemStack(fuelItem()));
    }
    public CompoundTag snapshot() { return read.get().copy(); }
    public void restore(CompoundTag tag) { write.accept(tag.copy()); }
}
