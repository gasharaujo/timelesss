package net.gasharaujo.timelesss.testing;

import net.gasharaujo.timelesss.TimeNetwork;
import net.gasharaujo.timelesss.TimelesssMod;
import net.gasharaujo.timelesss.block.entity.BatteryHubBlockEntity;
import net.gasharaujo.timelesss.block.entity.ResourceBlockEntity;
import net.gasharaujo.timelesss.resource.ResourceInteraction;
import net.gasharaujo.timelesss.resource.ResourceType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

final class AdvancementSelfTest {
    private AdvancementSelfTest() {
    }

    static void run(ServerLevel level) throws Exception {
        ServerPlayer player = PlatformChecks.advancementPlayer(level, "TimelesssTester");
        ServerPlayer other = PlatformChecks.advancementPlayer(level, "OtherTester");
        var pay = TimeNetwork.class.getDeclaredMethod("payForNextSecond", ServerPlayer.class);
        pay.setAccessible(true);
        var accelerate = TimeNetwork.class.getDeclaredMethod("acceleratePaidBinds", ServerPlayer.class, MinecraftServer.class);
        accelerate.setAccessible(true);

        for (String name : new String[]{"im_timelesss_now", "master_of_time", "using_power", "time_managing", "bulk_accelerate"}) {
            expect(level, player, name, false);
        }
        PlatformChecks.craft(player, new ItemStack(Items.CRAFTING_TABLE));
        expect(level, player, "im_timelesss_now", false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TimelesssMod.TIMELESSS.get()));
        expect(level, player, "im_timelesss_now", false);
        PlatformChecks.craft(player, player.getItemInHand(InteractionHand.MAIN_HAND));
        expect(level, player, "im_timelesss_now", true);
        expect(level, player, "master_of_time", false);
        PlatformChecks.craft(player, new ItemStack(TimelesssMod.TIME_DISTRIBUTOR_ITEM.get()));
        expect(level, player, "master_of_time", true);

        BlockPos origin = new BlockPos(10, 100, 0);
        level.getChunk(origin);
        level.setBlockAndUpdate(origin, TimelesssMod.TIME_DISTRIBUTOR.get().defaultBlockState());
        var distributor = (ResourceBlockEntity) level.getBlockEntity(origin);
        distributor.setOwner(player.getUUID());
        String dimension = PlatformChecks.dimension(level);
        TimeNetwork.addEnergyNode(player, dimension, origin.asLong());
        TimeNetwork.addEnergyNode(other, dimension, origin.asLong());
        pay.invoke(null, player);
        expect(level, player, "using_power", false);
        distributor.resources().insert(ResourceType.ENERGY, 100, false);
        pay.invoke(null, other);
        expect(level, other, "using_power", false);
        pay.invoke(null, player);
        expect(level, player, "using_power", true);
        expect(level, player, "time_managing", false);

        BlockPos firstTarget = origin.above();
        BlockPos secondTarget = firstTarget.above();
        level.setBlockAndUpdate(firstTarget, Blocks.FURNACE.defaultBlockState());
        level.setBlockAndUpdate(secondTarget, Blocks.FURNACE.defaultBlockState());
        bind(player, dimension, firstTarget, 30);
        bind(player, dimension, secondTarget, 9);
        distributor.resources().set(ResourceType.ENERGY, 100_000);
        pay.invoke(null, player);
        accelerate.invoke(null, player, level.getServer());
        expect(level, player, "time_managing", false);

        TimeNetwork.setMultiplier(player, dimension, secondTarget.asLong(), 10);
        distributor.resources().set(ResourceType.ENERGY, 99_999);
        pay.invoke(null, player);
        accelerate.invoke(null, player, level.getServer());
        expect(level, player, "time_managing", false);

        level.setBlockAndUpdate(firstTarget, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(secondTarget, Blocks.STONE.defaultBlockState());
        distributor.resources().set(ResourceType.ENERGY, 100_000);
        pay.invoke(null, player);
        accelerate.invoke(null, player, level.getServer());
        expect(level, player, "time_managing", false);

        level.setBlockAndUpdate(firstTarget, Blocks.FURNACE.defaultBlockState());
        level.setBlockAndUpdate(secondTarget, Blocks.FURNACE.defaultBlockState());
        distributor.resources().set(ResourceType.ENERGY, 100_000);
        pay.invoke(null, player);
        accelerate.invoke(null, player, level.getServer());
        expect(level, player, "time_managing", true);

        var firstHub = hub(level, player, dimension, origin.east());
        var secondHub = hub(level, player, dimension, origin.east(2));
        var thirdHub = hub(level, other, dimension, origin.east(3));
        for (int i = 0; i < 320; i++) {
            firstHub.insertBattery();
            if (i < 319) {
                secondHub.insertBattery();
            }
        }
        thirdHub.insertBattery();
        ItemStack batteries = new ItemStack(TimelesssMod.BATTERY.get(), 64);
        if (batteries.getMaxStackSize() != 64 || !PlatformChecks.migratedBatteryStacks(level, player)) {
            throw new AssertionError("New and migrated batteries must stack to 64");
        }
        PlatformChecks.capabilities(level, origin, ResourceType.ENERGY);
        player.setItemInHand(InteractionHand.MAIN_HAND, batteries);
        TimeNetwork.checkBulkBatteries(player);
        expect(level, player, "bulk_accelerate", false);
        ResourceInteraction.block(player, InteractionHand.MAIN_HAND, thirdHub);
        expect(level, player, "bulk_accelerate", false);
        ResourceInteraction.block(player, InteractionHand.MAIN_HAND, secondHub);
        if (batteries.getCount() != 63) {
            throw new AssertionError("Hub could not consume exactly one battery from the stack");
        }
        expect(level, player, "bulk_accelerate", true);
        expect(level, other, "bulk_accelerate", false);

        System.out.println("TIMELESSS_ADVANCEMENTS_SELFTEST_PASSED");
    }

    private static void bind(ServerPlayer player, String dimension, BlockPos pos, int multiplier) {
        TimeNetwork.addBind(player, dimension, pos.asLong());
        TimeNetwork.setMultiplier(player, dimension, pos.asLong(), multiplier);
    }

    private static BatteryHubBlockEntity hub(ServerLevel level, ServerPlayer player, String dimension, BlockPos pos) {
        level.setBlockAndUpdate(pos, TimelesssMod.BATTERY_HUB.get().defaultBlockState());
        var hub = (BatteryHubBlockEntity) level.getBlockEntity(pos);
        hub.setOwner(player.getUUID());
        TimeNetwork.addEnergyNode(player, dimension, pos.asLong());
        return hub;
    }

    private static void expect(ServerLevel level, ServerPlayer player, String advancement, boolean expected) {
        if (PlatformChecks.advancementDone(level, player, advancement) != expected) {
            throw new AssertionError(advancement + " expected done=" + expected);
        }
    }
}
