package net.gasharaujo.timelesss.block.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.gasharaujo.timelesss.TimelesssMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;

public final class TimeDistributorBlockEntity extends BlockEntity implements PlayerEnergyNode {
    public static final int CAPACITY = 100_000;
    private final EnergyStorage energy = new EnergyStorage(CAPACITY, CAPACITY, CAPACITY) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) {
                TimeDistributorBlockEntity.this.setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (!simulate && extracted > 0) {
                TimeDistributorBlockEntity.this.setChanged();
            }
            return extracted;
        }
    };
    private LazyOptional<EnergyStorage> energyCapability = LazyOptional.of(() -> energy);
    private @Nullable UUID owner;

    public TimeDistributorBlockEntity(BlockPos pos, BlockState state) {
        super(TimelesssMod.TIME_DISTRIBUTOR_BLOCK_ENTITY.get(), pos, state);
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
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("energy", energy.getEnergyStored());
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("energy")) {
            energy.deserializeNBT(tag.get("energy"));
        }
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY) {
            return energyCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyCapability = LazyOptional.of(() -> energy);
    }
}
