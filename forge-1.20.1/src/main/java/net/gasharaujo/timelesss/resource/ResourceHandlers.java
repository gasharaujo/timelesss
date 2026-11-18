package net.gasharaujo.timelesss.resource;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;

public final class ResourceHandlers {
    private ResourceHandlers() {}
    public static IEnergyStorage energy(ResourceStorage s) {
        return new IEnergyStorage() {
            public int getEnergyStored() { return (int)s.amount(ResourceType.ENERGY); }
            public int getMaxEnergyStored() { return (int)s.capacity(ResourceType.ENERGY); }
            public boolean canExtract() { return s.type()==ResourceType.ENERGY; }
            public boolean canReceive() { return canExtract(); }
            public int receiveEnergy(int amount, boolean simulate) { return (int)s.insert(ResourceType.ENERGY, amount, simulate); }
            public int extractEnergy(int amount, boolean simulate) { return (int)s.extract(ResourceType.ENERGY, amount, simulate); }
        };
    }
    public static IFluidHandlerItem fluid(ResourceStorage s, ItemStack container) {
        return new IFluidHandlerItem() {
            public ItemStack getContainer() { return container; }
            public int getTanks() { return 1; }
            public FluidStack getFluidInTank(int tank) { java.util.Objects.checkIndex(tank,1); return new FluidStack(ResourceStorage.fuelFluid(),(int)s.amount(ResourceType.FLUID)); }
            public int getTankCapacity(int tank) { java.util.Objects.checkIndex(tank,1); return (int)s.capacity(ResourceType.FLUID); }
            public boolean isFluidValid(int tank, FluidStack stack) { java.util.Objects.checkIndex(tank,1); return s.type()==ResourceType.FLUID && !stack.isEmpty() && stack.getFluid()==ResourceStorage.fuelFluid() && !stack.hasTag(); }
            public int fill(FluidStack stack, FluidAction action) { return isFluidValid(0,stack) ? (int)s.insert(ResourceType.FLUID,stack.getAmount(),action.simulate()) : 0; }
            public FluidStack drain(FluidStack stack, FluidAction action) { return isFluidValid(0,stack) ? drain(stack.getAmount(), action) : FluidStack.EMPTY; }
            public FluidStack drain(int amount, FluidAction action) { return new FluidStack(ResourceStorage.fuelFluid(),(int)s.extract(ResourceType.FLUID,amount,action.simulate())); }
        };
    }
    public static IItemHandler items(ResourceStorage s) {
        return new IItemHandler() {
            public int getSlots() { return s.slots(); }
            public ItemStack getStackInSlot(int slot) { return new ItemStack(ResourceStorage.fuelItem(),s.slotAmount(slot)); }
            public int getSlotLimit(int slot) { java.util.Objects.checkIndex(slot,getSlots()); return s.stackLimit(); }
            public boolean isItemValid(int slot, ItemStack stack) { java.util.Objects.checkIndex(slot,getSlots()); return s.type()==ResourceType.ITEM && s.validItem(stack); }
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!isItemValid(slot,stack)) return stack;
                int moved=Math.min(stack.getCount(),getSlotLimit(slot)-s.slotAmount(slot));
                if (!simulate && moved>0) s.setSlot(slot,s.slotAmount(slot)+moved);
                ItemStack rest=stack.copy(); rest.shrink(moved); return rest;
            }
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                int moved=s.type()==ResourceType.ITEM ? Math.min(Math.max(0,amount),s.slotAmount(slot)) : 0;
                if(!simulate && moved>0) s.setSlot(slot,s.slotAmount(slot)-moved);
                return new ItemStack(ResourceStorage.fuelItem(),moved);
            }
        };
    }
}
