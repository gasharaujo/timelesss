package net.gasharaujo.timelesss.block;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.gasharaujo.timelesss.TimeNetwork;
import net.gasharaujo.timelesss.TimelesssMod;
import net.gasharaujo.timelesss.block.entity.BatteryHubBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jspecify.annotations.Nullable;

public final class BatteryHubBlock extends BaseEntityBlock {
    public static final MapCodec<BatteryHubBlock> CODEC = simpleCodec(BatteryHubBlock::new);
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 3);

    public BatteryHubBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHARGE, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatteryHubBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof BatteryHubBlockEntity hub) {
            addBatteryDrops(drops, hub.getBatteryCount() - BatteryHubBlockEntity.INITIAL_BATTERIES);
        }
        return drops;
    }

    private static void addBatteryDrops(List<ItemStack> drops, int count) {
        int remaining = Math.max(0, count);
        int maxStackSize = new ItemStack(TimelesssMod.BATTERY.get()).getMaxStackSize();
        while (remaining > 0) {
            int amount = Math.min(maxStackSize, remaining);
            drops.add(new ItemStack(TimelesssMod.BATTERY.get(), amount));
            remaining -= amount;
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) {
            return;
        }
        if (!(placer instanceof ServerPlayer player) || placer instanceof FakePlayer) {
            level.destroyBlock(pos, true);
            return;
        }
        if (level.getBlockEntity(pos) instanceof BatteryHubBlockEntity hub) {
            hub.setOwner(player.getUUID());
            TimeNetwork.addEnergyNode(player, level.dimension().identifier().toString(), pos.asLong());
        }
    }
}
