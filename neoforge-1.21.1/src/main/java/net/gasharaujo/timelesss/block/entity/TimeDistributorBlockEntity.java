package net.gasharaujo.timelesss.block.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.gasharaujo.timelesss.TimelesssMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

public final class TimeDistributorBlockEntity extends BlockEntity implements PlayerEnergyNode {
    public static final int CAPACITY = 100_000;
    private final EnergyStorage energy = new EnergyStorage(CAPACITY, CAPACITY, CAPACITY) {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int changed = super.receiveEnergy(amount, simulate);
            if (!simulate && changed > 0) TimeDistributorBlockEntity.this.setChanged();
            return changed;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int changed = super.extractEnergy(amount, simulate);
            if (!simulate && changed > 0) TimeDistributorBlockEntity.this.setChanged();
            return changed;
        }
    };
    private @Nullable UUID owner;

    public TimeDistributorBlockEntity(BlockPos pos, BlockState state) {
        super(TimelesssMod.TIME_DISTRIBUTOR_BLOCK_ENTITY.get(), pos, state);
    }

    public EnergyStorage getEnergyHandler() {
        return energy;
    }

    public @Nullable UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    @Override
    public int getEnergyCapacity() {
        return CAPACITY;
    }

    public int extractEnergy(int amount) {
        return energy.extractEnergy(amount, false);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy", energy.serializeNBT(registries));
        if (owner != null) tag.putUUID("owner", owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("energy")) energy.deserializeNBT(registries, tag.get("energy"));
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }
}
