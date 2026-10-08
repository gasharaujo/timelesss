package net.gasharaujo.timelesss.block;

import javax.annotation.Nullable;
import net.gasharaujo.timelesss.TimeNetwork;
import net.gasharaujo.timelesss.block.entity.TimeDistributorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;

public final class TimeDistributorBlock extends BaseEntityBlock {
    public TimeDistributorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TimeDistributorBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        if (level.getBlockEntity(pos) instanceof TimeDistributorBlockEntity distributor) {
            distributor.setOwner(player.getUUID());
            TimeNetwork.addEnergyNode(player, level.dimension().location().toString(), pos.asLong());
        }
    }
}
