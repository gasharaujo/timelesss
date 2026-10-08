package net.gasharaujo.timelesss.block.entity;

import java.util.UUID;
import net.gasharaujo.timelesss.TimelesssMod;
import net.gasharaujo.timelesss.block.BatteryHubBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public final class BatteryHubBlockEntity extends BlockEntity implements PlayerEnergyNode {
    public static final int INITIAL_BATTERIES = 4;
    public static final int ENERGY_PER_BATTERY = 10_000;
    public static final int MAX_BATTERIES = Integer.MAX_VALUE / ENERGY_PER_BATTERY;

    private int batteries = INITIAL_BATTERIES;
    private final HubEnergyStorage energy = new HubEnergyStorage();
    private @Nullable UUID owner;

    public BatteryHubBlockEntity(BlockPos pos, BlockState state) {
        super(TimelesssMod.BATTERY_HUB_BLOCK_ENTITY.get(), pos, state);
    }

    public SimpleEnergyHandler getEnergyHandler() {
        return energy;
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
        return energy.getAmountAsInt();
    }

    @Override
    public int getEnergyCapacity() {
        return batteries * ENERGY_PER_BATTERY;
    }

    @Override
    public int extractEnergy(int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = energy.extract(amount, transaction);
            transaction.commit();
            return extracted;
        }
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("batteries", batteries);
        energy.serialize(output.child("energy"));
        if (owner != null) {
            output.putString("owner", owner.toString());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        batteries = Math.max(INITIAL_BATTERIES,
            Math.min(MAX_BATTERIES, input.getIntOr("batteries", INITIAL_BATTERIES)));
        energy.refreshCapacity();
        energy.deserialize(input.childOrEmpty("energy"));
        energy.clampToCapacity();
        String storedOwner = input.getStringOr("owner", "");
        try {
            owner = storedOwner.isEmpty() ? null : UUID.fromString(storedOwner);
        } catch (IllegalArgumentException ignored) {
            owner = null;
        }
    }

    private final class HubEnergyStorage extends SimpleEnergyHandler {
        private HubEnergyStorage() {
            super(INITIAL_BATTERIES * ENERGY_PER_BATTERY);
        }

        private void refreshCapacity() {
            capacity = BatteryHubBlockEntity.this.getEnergyCapacity();
            maxInsert = capacity;
            maxExtract = capacity;
            clampToCapacity();
        }

        private void clampToCapacity() {
            energy = Math.min(energy, capacity);
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            BatteryHubBlockEntity.this.setChanged();
            BatteryHubBlockEntity.this.updateChargeState();
        }
    }
}
