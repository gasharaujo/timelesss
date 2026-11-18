package net.gasharaujo.timelesss;

import net.gasharaujo.timelesss.item.TimelesssAccelerator;
import java.util.Objects;
import net.gasharaujo.timelesss.block.entity.TimeDistributorBlockEntity;
import net.gasharaujo.timelesss.block.entity.BatteryHubBlockEntity;
import net.gasharaujo.timelesss.item.TimeWrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TimelesssMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonGameEvents {
    private CommonGameEvents() {
    }

    @SubscribeEvent
    public static void entityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!stack.is(TimelesssMod.TIMELESSS.get())) {
            return;
        }
        if (!player.level().isClientSide()) {
            TimelesssAccelerator.accelerateEntity(player, event.getTarget(), stack);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void blockInteract(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());
        BlockPos clickedPos = event.getPos();
        boolean distributor = player.level().getBlockState(clickedPos).is(TimelesssMod.TIME_DISTRIBUTOR.get());

        if (stack.is(TimelesssMod.TIME_WRENCH.get())) {
            if (distributor) {
                if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    if (player.level().getBlockEntity(clickedPos) instanceof TimeDistributorBlockEntity blockEntity
                        && Objects.equals(blockEntity.getOwner(), player.getUUID())) {
                        if (!TimeWrenchItem.hasTarget(stack)) {
                            player.sendSystemMessage(Component.translatable("message.timelesss.wrench_empty"));
                        } else if (TimeNetwork.addBind(serverPlayer, TimeWrenchItem.getDimension(stack),
                            TimeWrenchItem.getPosition(stack))) {
                            player.sendSystemMessage(Component.translatable("message.timelesss.bind_added"));
                        } else {
                            player.sendSystemMessage(Component.translatable("message.timelesss.bind_exists"));
                        }
                    } else {
                        player.sendSystemMessage(Component.translatable("message.timelesss.not_owner"));
                    }
                }
                succeed(event);
                return;
            }
            if (player.isShiftKeyDown()) {
                if (!player.level().isClientSide()) {
                    TimeWrenchItem.setTarget(stack, player.level().dimension().location().toString(), clickedPos.asLong());
                    player.sendSystemMessage(Component.translatable("message.timelesss.wrench_saved",
                        clickedPos.getX(), clickedPos.getY(), clickedPos.getZ()));
                }
                succeed(event);
                return;
            }
        }

        if (player.level().getBlockEntity(clickedPos) instanceof net.gasharaujo.timelesss.block.entity.ResourceBlockEntity resourceBlock) {
            if (!player.level().isClientSide()) {
                boolean fueling=stack.is(TimelesssMod.BATTERY.get()) || player.isShiftKeyDown()
                    || resourceBlock.resources().enabled(net.gasharaujo.timelesss.resource.ResourceType.FLUID)
                    || resourceBlock.resources().enabled(net.gasharaujo.timelesss.resource.ResourceType.ITEM);
                if(fueling || !distributor) net.gasharaujo.timelesss.resource.ResourceInteraction.block(player,event.getHand(),resourceBlock);
                else if(player instanceof ServerPlayer serverPlayer) TimelesssMod.sendTimeBinds(serverPlayer);
            }
            succeed(event);
        }
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof net.minecraft.server.level.ServerLevel level) {
            TimeNetwork.tick(level);
        }
    }

    private static void succeed(PlayerInteractEvent.RightClickBlock event) {
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
