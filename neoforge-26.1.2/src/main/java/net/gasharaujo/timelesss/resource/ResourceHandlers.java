package net.gasharaujo.timelesss.resource;

import java.util.function.ToIntFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Blocks journal their data; item capabilities exchange the item through ItemAccess. */
public final class ResourceHandlers {
    private final ResourceStorage storage;
    private final ItemAccess access;
    private final SnapshotJournal<CompoundTag> journal = new SnapshotJournal<>() {
        protected CompoundTag createSnapshot() { return storage.snapshot(); }
        protected void revertToSnapshot(CompoundTag tag) { storage.restore(tag); }
    };

    public ResourceHandlers(ResourceStorage storage) { this.storage = storage; this.access = null; }
    public ResourceHandlers(ItemAccess access) { this.storage = null; this.access = access; }
    private ResourceStorage current() { return access == null ? storage : ResourceStorage.item(access.getResource().toStack()); }
    private int change(TransactionContext transaction, ToIntFunction<ResourceStorage> operation) {
        if (access == null) {
            journal.updateSnapshots(transaction);
            return operation.applyAsInt(storage);
        }
        if (access.getAmount() != 1) return 0;
        ItemStack stack = access.getResource().toStack();
        int moved = operation.applyAsInt(ResourceStorage.item(stack));
        return moved > 0 && access.exchange(ItemResource.of(stack), 1, transaction) == 1 ? moved : 0;
    }
    public EnergyHandler energy() {
        return new EnergyHandler() {
            public long getAmountAsLong() { return current().amount(ResourceType.ENERGY); }
            public long getCapacityAsLong() { return current().capacity(ResourceType.ENERGY); }
            public int insert(int amount, TransactionContext transaction) { return change(transaction, s -> (int)s.insert(ResourceType.ENERGY, amount, false)); }
            public int extract(int amount, TransactionContext transaction) { return change(transaction, s -> (int)s.extract(ResourceType.ENERGY, amount, false)); }
        };
    }
    public ResourceHandler<FluidResource> fluid() {
        return new FuelHandler<FluidResource>(ResourceType.FLUID) {
            protected FluidResource resource(String id) { return FluidResource.of(ResourceStorage.fuelFluid(id)); }
            protected String id(FluidResource resource) { return ResourceStorage.fluidId(resource.getFluid()); }
            protected FluidResource empty() { return FluidResource.EMPTY; }
        };
    }
    public ResourceHandler<ItemResource> items() {
        return new FuelHandler<ItemResource>(ResourceType.ITEM) {
            protected ItemResource resource(String id) { return ItemResource.of(ResourceStorage.fuelItem(id)); }
            protected String id(ItemResource resource) { return ResourceStorage.itemId(resource.toStack()); }
            protected ItemResource empty() { return ItemResource.EMPTY; }
        };
    }
    private abstract class FuelHandler<T extends Resource> implements ResourceHandler<T> {
        private final ResourceType type;
        FuelHandler(ResourceType type) { this.type=type; }
        protected abstract T resource(String id);
        protected abstract String id(T resource);
        protected abstract T empty();
        private void check(int index) { java.util.Objects.checkIndex(index,size()); }
        private String storedId(int index) { check(index);return type==ResourceType.ITEM ? current().slotId(index) : current().tankId(type,index); }
        public int size() { return type==ResourceType.ITEM ? current().slots() : current().tanks(type); }
        public T getResource(int index) { return getAmountAsLong(index)>0 ? resource(storedId(index)) : empty(); }
        public long getAmountAsLong(int index) { String id=storedId(index);return type==ResourceType.ITEM ? current().slotAmount(index) : current().amount(type,id); }
        public long getCapacityAsLong(int index,T offered) {
            check(index);
            return type==ResourceType.ITEM ? (offered.isEmpty() ? current().slotLimit(index) : offered instanceof ItemResource item ? item.getMaxStackSize() : 0) : current().tankCapacity(type,index);
        }
        public boolean isValid(int index,T offered) {
            check(index);
            if(!current().enabled(type) || offered.isEmpty()) return false;
            String id=id(offered);
            return net.gasharaujo.timelesss.ResourceConfig.accepts(type,id) && offered.equals(resource(id))
                && (type==ResourceType.ITEM || current().tankId(type,index).equals(id));
        }
        public int insert(int index,T offered,int amount,TransactionContext transaction) {
            if(amount<=0 || !isValid(index,offered)) return 0;
            String id=id(offered);
            return change(transaction,s -> {
                if(type!=ResourceType.ITEM) return (int)s.insert(type,id,amount,false);
                int count=s.slotAmount(index);
                if(count>0 && !s.slotId(index).equals(id)) return 0;
                int n=Math.min(amount,s.stackLimit(id)-count);
                if(n>0) s.setSlot(index,id,count+n);
                return n;
            });
        }
        public int extract(int index,T offered,int amount,TransactionContext transaction) {
            check(index);
            if(amount<=0 || !current().enabled(type) || offered.isEmpty() || !offered.equals(getResource(index))) return 0;
            String id=id(offered);
            return change(transaction,s -> {
                if(type!=ResourceType.ITEM) return (int)s.extract(type,id,amount,false);
                int count=s.slotAmount(index),n=Math.min(amount,count);
                if(n>0) s.setSlot(index,id,count-n);
                return n;
            });
        }
    }
}
