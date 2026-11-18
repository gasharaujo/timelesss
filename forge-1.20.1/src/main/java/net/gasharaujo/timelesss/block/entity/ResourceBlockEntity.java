package net.gasharaujo.timelesss.block.entity;

import java.util.UUID;
import net.gasharaujo.timelesss.resource.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

public abstract class ResourceBlockEntity extends BlockEntity implements PlayerEnergyNode {
    protected UUID owner;
    protected CompoundTag resourceData = new CompoundTag();
    private final ResourceStorage resources;
    private LazyOptional<Object> energyCap=LazyOptional.of(() -> ResourceHandlers.energy(resources()));
    private LazyOptional<Object> fluidCap=LazyOptional.of(() -> ResourceHandlers.fluid(resources(),ItemStack.EMPTY));
    private LazyOptional<Object> itemCap=LazyOptional.of(() -> ResourceHandlers.items(resources()));
    private LazyOptional<Object> gasCap=LazyOptional.of(() -> MekanismBridge.handler(resources()));

    protected ResourceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, ResourceStorage.Kind kind) {
        super(type, pos, state);
        resources = new ResourceStorage(kind, () -> resourceData, tag -> { resourceData = tag; resourceChanged(); }, this::extraBatteries);

    }
    protected int extraBatteries() { return 0; }
    protected void resourceChanged() { setChanged(); }
    public ResourceStorage resources() { return resources; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner=owner; setChanged(); }
    public net.minecraftforge.energy.IEnergyStorage getEnergyHandler() { return resources.type()==ResourceType.ENERGY ? ResourceHandlers.energy(resources) : null; }
    public net.minecraftforge.fluids.capability.IFluidHandler getFluidHandler() { return resources.type()==ResourceType.FLUID ? ResourceHandlers.fluid(resources, ItemStack.EMPTY) : null; }
    public net.minecraftforge.items.IItemHandler getItemHandler() { return resources.type()==ResourceType.ITEM ? ResourceHandlers.items(resources) : null; }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("resources25",resourceData.copy());
        if(owner!=null) tag.putUUID("owner",owner);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        resourceData=tag.getCompound("resources25").copy();
        if(!tag.contains("resources25")) resourceData.putLong(resources.key(ResourceType.ENERGY), tag.getInt("energy"));
        owner=tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if(resources.type()==ResourceType.ENERGY && cap==ForgeCapabilities.ENERGY) return energyCap.cast();
        if(resources.type()==ResourceType.FLUID && cap==ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        if(resources.type()==ResourceType.ITEM && cap==ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        if(resources.type()==ResourceType.GAS && cap==MekanismBridge.blockCapability()) return gasCap.cast();
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); energyCap.invalidate();fluidCap.invalidate();itemCap.invalidate();gasCap.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps();
        energyCap=LazyOptional.of(() -> ResourceHandlers.energy(resources));
        fluidCap=LazyOptional.of(() -> ResourceHandlers.fluid(resources,ItemStack.EMPTY));
        itemCap=LazyOptional.of(() -> ResourceHandlers.items(resources));
        gasCap=LazyOptional.of(() -> MekanismBridge.handler(resources));
    }
}
