package net.gasharaujo.timelesss.block.entity;

import java.util.UUID;
import net.gasharaujo.timelesss.resource.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;

public abstract class ResourceBlockEntity extends BlockEntity implements PlayerEnergyNode {
    protected UUID owner;
    protected CompoundTag resourceData = new CompoundTag();
    private final ResourceStorage resources;


    protected ResourceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, ResourceStorage.Kind kind) {
        super(type, pos, state);
        resources = new ResourceStorage(kind, () -> resourceData, tag -> { resourceData = tag; resourceChanged(); }, this::extraBatteries);

    }
    protected int extraBatteries() { return 0; }
    protected void resourceChanged() { setChanged(); }
    public ResourceStorage resources() { return resources; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner=owner; setChanged(); }
    public net.neoforged.neoforge.energy.IEnergyStorage getEnergyHandler() { return resources.enabled(ResourceType.ENERGY) ? ResourceHandlers.energy(resources) : null; }
    public net.neoforged.neoforge.fluids.capability.IFluidHandler getFluidHandler() { return resources.enabled(ResourceType.FLUID) ? ResourceHandlers.fluid(resources, ItemStack.EMPTY) : null; }
    public net.neoforged.neoforge.items.IItemHandler getItemHandler() { return resources.enabled(ResourceType.ITEM) ? ResourceHandlers.items(resources) : null; }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("resources25",resourceData.copy());
        if(owner!=null) tag.putUUID("owner",owner);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        resourceData=tag.getCompound("resources25").copy();
        if(!tag.contains("resources25")) resourceData.putLong(resources.key(ResourceType.ENERGY), tag.getInt("energy"));
        owner=tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }

}
