package net.gasharaujo.timelesss.block.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.gasharaujo.timelesss.TimelesssMod;
import net.gasharaujo.timelesss.block.BatteryHubBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;

public final class BatteryHubBlockEntity extends BlockEntity implements PlayerEnergyNode {
    public static final int INITIAL_BATTERIES = 4;
    public static final int ENERGY_PER_BATTERY = 10_000;
    public static final int MAX_BATTERIES = Integer.MAX_VALUE / ENERGY_PER_BATTERY;

    private int batteries = INITIAL_BATTERIES;
    private final HubEnergyStorage energy = new HubEnergyStorage();
    private LazyOptional<EnergyStorage> energyCapability = LazyOptional.of(() -> energy);
    private @Nullable UUID owner;

    public BatteryHubBlockEntity(BlockPos pos, BlockState state) {
        super(TimelesssMod.BATTERY_HUB_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public @Nullable UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public int getBatteryCount() {
        return batteries;
    }

    public boolean insertBattery() {
        if (batteries >= MAX_BATTERIES) {
            return false;
        }
        batteries++;
        energy.refreshCapacity();
        setChanged();
        updateChargeState();
        return true;
    }

    @Override
    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    @Override
    public int getEnergyCapacity() {
        return batteries * ENERGY_PER_BATTERY;
    }

    @Override
    public int extractEnergy(int amount) {
        return energy.extractEnergy(amount, false);
    }

    private void updateChargeState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int capacity = getEnergyCapacity();
        int stored = getEnergyStored();
        int charge = stored >= capacity ? 3 : stored * 4L >= capacity * 3L ? 2 : stored * 2L >= capacity ? 1 : 0;
        BlockState state = getBlockState();
        if (state.hasProperty(BatteryHubBlock.CHARGE) && state.getValue(BatteryHubBlock.CHARGE) != charge) {
            level.setBlock(worldPosition, state.setValue(BatteryHubBlock.CHARGE, charge), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("batteries", batteries);
        tag.put("energy", energy.serializeNBT());
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        batteries = Math.max(INITIAL_BATTERIES,
            Math.min(MAX_BATTERIES, tag.contains("batteries") ? tag.getInt("batteries") : INITIAL_BATTERIES));
        energy.refreshCapacity();
        if (tag.contains("energy")) {
            energy.deserializeNBT(tag.get("energy"));
            energy.clampToCapacity();
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

    private final class HubEnergyStorage extends EnergyStorage {
        private HubEnergyStorage() {
            super(INITIAL_BATTERIES * ENERGY_PER_BATTERY);
        }

        private void refreshCapacity() {
            capacity = BatteryHubBlockEntity.this.getEnergyCapacity();
            maxReceive = capacity;
            maxExtract = capacity;
            clampToCapacity();
        }

        private void clampToCapacity() {
            energy = Math.min(energy, capacity);
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int changed = super.receiveEnergy(amount, simulate);
            if (!simulate && changed > 0) {
                BatteryHubBlockEntity.this.setChanged();
                BatteryHubBlockEntity.this.updateChargeState();
            }
            return changed;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int changed = super.extractEnergy(amount, simulate);
            if (!simulate && changed > 0) {
                BatteryHubBlockEntity.this.setChanged();
                BatteryHubBlockEntity.this.updateChargeState();
            }
            return changed;
        }
    }
}
