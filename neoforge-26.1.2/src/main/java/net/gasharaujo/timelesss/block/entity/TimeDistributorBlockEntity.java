package net.gasharaujo.timelesss.block.entity;

import java.util.UUID;
import net.gasharaujo.timelesss.TimelesssMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public final class TimeDistributorBlockEntity extends BlockEntity implements PlayerEnergyNode {
    public static final int CAPACITY = 100_000;
    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(CAPACITY, CAPACITY, CAPACITY) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            TimeDistributorBlockEntity.this.setChanged();
        }
    };
    private @Nullable UUID owner;

    public TimeDistributorBlockEntity(BlockPos pos, BlockState state) {
        super(TimelesssMod.TIME_DISTRIBUTOR_BLOCK_ENTITY.get(), pos, state);
    }

    public SimpleEnergyHandler getEnergyHandler() { return energy; }
    public @Nullable UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; setChanged(); }
    public int getEnergyStored() { return energy.getAmountAsInt(); }
    @Override public int getEnergyCapacity() { return CAPACITY; }

    public int extractEnergy(int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = energy.extract(amount, transaction);
            transaction.commit();
            return extracted;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output.child("energy"));
        if (owner != null) output.putString("owner", owner.toString());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input.childOrEmpty("energy"));
        String storedOwner = input.getStringOr("owner", "");
        try {
            owner = storedOwner.isEmpty() ? null : UUID.fromString(storedOwner);
        } catch (IllegalArgumentException ignored) {
            owner = null;
        }
    }
}
