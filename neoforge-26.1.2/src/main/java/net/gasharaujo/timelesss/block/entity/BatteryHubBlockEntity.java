package net.gasharaujo.timelesss.block.entity;

import net.gasharaujo.timelesss.TimelesssMod;
import net.gasharaujo.timelesss.block.BatteryHubBlock;
import net.gasharaujo.timelesss.resource.ResourceStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class BatteryHubBlockEntity extends ResourceBlockEntity {
    public static final int INITIAL_BATTERIES=4;
    public static final int ENERGY_PER_BATTERY=10000;
    public static final int MAX_BATTERIES=Integer.MAX_VALUE / ENERGY_PER_BATTERY;
    private int batteries=INITIAL_BATTERIES;
    public BatteryHubBlockEntity(BlockPos pos, BlockState state) {
        super(TimelesssMod.BATTERY_HUB_BLOCK_ENTITY.get(),pos,state,ResourceStorage.Kind.HUB);
    }
    public int getBatteryCount() { return batteries; }
    @Override protected int extraBatteries() { return Math.max(0,batteries-INITIAL_BATTERIES); }
    public boolean insertBattery() {
        if(batteries>=MAX_BATTERIES) return false;
        batteries++; resourceChanged(); return true;
    }
    @Override protected void resourceChanged() {
        super.resourceChanged();
        if(level==null || level.isClientSide()) return;
        int charge=0;
        for(var type:resources().types()) {
            long capacity=resources().capacity(type),stored=resources().amount(type);
            charge=Math.max(charge,stored>=capacity && capacity>0 ? 3 : stored*4L>=capacity*3L && capacity>0 ? 2 : stored*2L>=capacity && capacity>0 ? 1 : 0);
        }
        BlockState state=getBlockState();
        if(state.hasProperty(BatteryHubBlock.CHARGE) && state.getValue(BatteryHubBlock.CHARGE)!=charge)
            level.setBlock(worldPosition,state.setValue(BatteryHubBlock.CHARGE,charge),Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(ValueOutput output) { super.saveAdditional(output);output.putInt("batteries",batteries); }
    @Override protected void loadAdditional(ValueInput input) { super.loadAdditional(input);batteries=Math.max(INITIAL_BATTERIES,Math.min(MAX_BATTERIES,input.getIntOr("batteries",INITIAL_BATTERIES))); }
}
