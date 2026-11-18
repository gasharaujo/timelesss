package net.gasharaujo.timelesss.resource;

import net.gasharaujo.timelesss.ResourceConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;

public final class ResourceHandlers {
    private ResourceHandlers() {}
    public static IEnergyStorage energy(ResourceStorage s) {
        return new IEnergyStorage() {
            public int getEnergyStored() { return (int)s.amount(ResourceType.ENERGY); }
            public int getMaxEnergyStored() { return (int)s.capacity(ResourceType.ENERGY); }
            public boolean canExtract() { return s.enabled(ResourceType.ENERGY); }
            public boolean canReceive() { return canExtract(); }
            public int receiveEnergy(int amount,boolean simulate) { return (int)s.insert(ResourceType.ENERGY,amount,simulate); }
            public int extractEnergy(int amount,boolean simulate) { return (int)s.extract(ResourceType.ENERGY,amount,simulate); }
        };
    }
    public static IFluidHandlerItem fluid(ResourceStorage s,ItemStack container) {
        return new IFluidHandlerItem() {
            public ItemStack getContainer() { return container; }
            public int getTanks() { return s.tanks(ResourceType.FLUID); }
            public FluidStack getFluidInTank(int tank) { String id=s.tankId(ResourceType.FLUID,tank);return new FluidStack(ResourceStorage.fuelFluid(id),(int)s.amount(ResourceType.FLUID,id)); }
            public int getTankCapacity(int tank) { return (int)s.tankCapacity(ResourceType.FLUID,tank); }
            public boolean isFluidValid(int tank,FluidStack stack) { return s.enabled(ResourceType.FLUID) && valid(stack) && s.tankId(ResourceType.FLUID,tank).equals(ResourceStorage.fluidId(stack.getFluid())); }
            private boolean valid(FluidStack stack) { return !stack.isEmpty() && ResourceConfig.accepts(ResourceType.FLUID,ResourceStorage.fluidId(stack.getFluid())) && !stack.hasTag(); }
            public int fill(FluidStack stack,FluidAction action) { return valid(stack) ? (int)s.insert(ResourceType.FLUID,ResourceStorage.fluidId(stack.getFluid()),stack.getAmount(),action.simulate()) : 0; }
            public FluidStack drain(FluidStack stack,FluidAction action) {
                if(!valid(stack)) return FluidStack.EMPTY;
                String id=ResourceStorage.fluidId(stack.getFluid());return new FluidStack(stack.getFluid(),(int)s.extract(ResourceType.FLUID,id,stack.getAmount(),action.simulate()));
            }
            public FluidStack drain(int amount,FluidAction action) {
                for(int tank=0;tank<getTanks();tank++) {
                    String id=s.tankId(ResourceType.FLUID,tank);int moved=(int)s.extract(ResourceType.FLUID,id,amount,action.simulate());
                    if(moved>0) return new FluidStack(ResourceStorage.fuelFluid(id),moved);
                }
                return FluidStack.EMPTY;
            }
        };
    }
    public static IItemHandler items(ResourceStorage s) {
        return new IItemHandler() {
            public int getSlots() { return s.slots(); }
            public ItemStack getStackInSlot(int slot) { return new ItemStack(ResourceStorage.fuelItem(s.slotId(slot)),s.slotAmount(slot)); }
            public int getSlotLimit(int slot) { return s.slotLimit(slot); }
            public boolean isItemValid(int slot,ItemStack stack) { java.util.Objects.checkIndex(slot,getSlots());return s.validItem(stack); }
            public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
                if(!isItemValid(slot,stack)) return stack;
                String id=ResourceStorage.itemId(stack);int count=s.slotAmount(slot);
                if(count>0 && !s.slotId(slot).equals(id)) return stack;
                int moved=Math.min(stack.getCount(),s.stackLimit(id)-count);
                if(!simulate && moved>0) s.setSlot(slot,id,count+moved);
                ItemStack rest=stack.copy();rest.shrink(moved);return rest;
            }
            public ItemStack extractItem(int slot,int amount,boolean simulate) {
                String id=s.slotId(slot);int count=s.slotAmount(slot),moved=s.enabled(ResourceType.ITEM) ? Math.min(Math.max(0,amount),count) : 0;
                if(!simulate && moved>0) s.setSlot(slot,id,count-moved);
                return new ItemStack(ResourceStorage.fuelItem(id),moved);
            }
        };
    }
}
