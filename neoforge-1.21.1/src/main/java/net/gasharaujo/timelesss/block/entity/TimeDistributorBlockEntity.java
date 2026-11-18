package net.gasharaujo.timelesss.block.entity;

import net.gasharaujo.timelesss.TimelesssMod;
import net.gasharaujo.timelesss.resource.ResourceStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class TimeDistributorBlockEntity extends ResourceBlockEntity {
    public static final int CAPACITY = 100000;
    public TimeDistributorBlockEntity(BlockPos pos, BlockState state) {
        super(TimelesssMod.TIME_DISTRIBUTOR_BLOCK_ENTITY.get(),pos,state,ResourceStorage.Kind.DISTRIBUTOR);
    }
}
