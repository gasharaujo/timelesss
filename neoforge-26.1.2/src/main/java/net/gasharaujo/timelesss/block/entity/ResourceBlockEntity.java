package net.gasharaujo.timelesss.block.entity;

import java.util.UUID;
import net.gasharaujo.timelesss.resource.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class ResourceBlockEntity extends BlockEntity implements PlayerEnergyNode {
    protected UUID owner;
    protected CompoundTag resourceData = new CompoundTag();
    private final ResourceStorage resources;
    private final ResourceHandlers handlers;

    protected ResourceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, ResourceStorage.Kind kind) {
        super(type, pos, state);
        resources = new ResourceStorage(kind, () -> resourceData, tag -> { resourceData = tag; resourceChanged(); }, this::extraBatteries);
        handlers = new ResourceHandlers(resources);
    }
    protected int extraBatteries() { return 0; }
    protected void resourceChanged() { setChanged(); }
    public ResourceStorage resources() { return resources; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner=owner; setChanged(); }
    public net.neoforged.neoforge.transfer.energy.EnergyHandler getEnergyHandler() { return resources.type()==ResourceType.ENERGY ? handlers.energy() : null; }
    public net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource> getFluidHandler() { return resources.type()==ResourceType.FLUID ? handlers.fluid() : null; }
    public net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> getItemHandler() { return resources.type()==ResourceType.ITEM ? handlers.items() : null; }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("resources25", CompoundTag.CODEC, resourceData);
        if(owner!=null) output.putString("owner", owner.toString());
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        resourceData=input.read("resources25",CompoundTag.CODEC).orElseGet(CompoundTag::new);
        if(!input.read("resources25",CompoundTag.CODEC).isPresent()) resourceData.putLong(resources.key(ResourceType.ENERGY),input.childOrEmpty("energy").getIntOr("energy",0));
        try { owner=UUID.fromString(input.getStringOr("owner","")); } catch(IllegalArgumentException ex) { owner=null; }
    }

}
