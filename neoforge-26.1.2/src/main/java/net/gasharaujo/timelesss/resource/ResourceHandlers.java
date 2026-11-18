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
            protected FluidResource fuel() { return FluidResource.of(ResourceStorage.fuelFluid()); }
            protected FluidResource empty() { return FluidResource.EMPTY; }
        };
    }
    public ResourceHandler<ItemResource> items() {
        return new FuelHandler<ItemResource>(ResourceType.ITEM) {
            protected ItemResource fuel() { return ItemResource.of(ResourceStorage.fuelItem()); }
            protected ItemResource empty() { return ItemResource.EMPTY; }
        };
    }
    private abstract class FuelHandler<T extends Resource> implements ResourceHandler<T> {
        private final ResourceType type;
        FuelHandler(ResourceType type) { this.type = type; }
        protected abstract T fuel();
        protected abstract T empty();
        private void check(int index) { java.util.Objects.checkIndex(index, size()); }
        public int size() { return type == ResourceType.ITEM ? current().slots() : 1; }
        public T getResource(int index) { return getAmountAsLong(index) > 0 ? fuel() : empty(); }
        public long getAmountAsLong(int index) { check(index); return type == ResourceType.ITEM ? current().slotAmount(index) : current().amount(type); }
        public long getCapacityAsLong(int index, T resource) { check(index); return type == ResourceType.ITEM ? current().stackLimit() : current().capacity(type); }
        public boolean isValid(int index, T resource) { check(index); return current().type() == type && !resource.isEmpty() && resource.equals(fuel()); }
        public int insert(int index, T resource, int amount, TransactionContext transaction) {
            if (amount <= 0 || !isValid(index, resource)) return 0;
            return change(transaction, s -> {
                if (type != ResourceType.ITEM) return (int)s.insert(type, amount, false);
                int n = Math.min(amount, s.stackLimit() - s.slotAmount(index));
                if (n > 0) s.setSlot(index, s.slotAmount(index) + n);
                return n;
            });
        }
        public int extract(int index, T resource, int amount, TransactionContext transaction) {
            if (amount <= 0 || !isValid(index, resource)) return 0;
            return change(transaction, s -> {
                if (type != ResourceType.ITEM) return (int)s.extract(type, amount, false);
                int n = Math.min(amount, s.slotAmount(index));
                if (n > 0) s.setSlot(index, s.slotAmount(index) - n);
                return n;
            });
        }
    }
}
