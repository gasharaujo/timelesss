package net.gasharaujo.timelesss;

import java.util.Objects;
import net.gasharaujo.timelesss.block.entity.TimeDistributorBlockEntity;
import net.gasharaujo.timelesss.block.entity.BatteryHubBlockEntity;
import net.gasharaujo.timelesss.item.TimeWrenchItem;
import net.gasharaujo.timelesss.item.TimelesssAccelerator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = TimelesssMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
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
                boolean fueling=player.isShiftKeyDown()
                    || resourceBlock.resources().enabled(net.gasharaujo.timelesss.resource.ResourceType.FLUID)
                    || resourceBlock.resources().enabled(net.gasharaujo.timelesss.resource.ResourceType.ITEM);
                if(fueling || !distributor) net.gasharaujo.timelesss.resource.ResourceInteraction.block(player,event.getHand(),resourceBlock);
                else if(player instanceof ServerPlayer serverPlayer) TimelesssMod.sendTimeBinds(serverPlayer);
            }
            succeed(event);
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) TimeNetwork.tick(level);
    }

    @SubscribeEvent
    public static void itemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack crafted = event.getCrafting();
            if (crafted.is(TimelesssMod.TIMELESSS.get())) {
                TimelesssAdvancements.award(player, "im_timelesss_now");
            } else if (crafted.is(TimelesssMod.TIME_DISTRIBUTOR_ITEM.get())) {
                TimelesssAdvancements.award(player, "master_of_time");
            }
        }
    }

    private static void succeed(PlayerInteractEvent.RightClickBlock event) {
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
