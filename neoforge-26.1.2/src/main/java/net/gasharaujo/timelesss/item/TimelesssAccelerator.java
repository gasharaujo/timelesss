package net.gasharaujo.timelesss.item;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

public final class TimelesssAccelerator {
    private TimelesssAccelerator() {
    }

    public static boolean accelerateEntity(Player player, Entity entity, ItemStack stack) {
        if (entity instanceof AbstractVillager villager) {
            villager.getOffers().forEach(MerchantOffer::resetUses);
            return true;
        }

        int ticks = TimelesssItem.getMultiplier(stack) * 20;
        for (int tick = 0; tick < ticks && !entity.isRemoved(); tick++) {
            entity.tick();
        }
        return true;
    }

    public static boolean accelerateBlock(Player player, Level level, BlockPos pos, Direction face, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        int ticks = TimelesssItem.getMultiplier(stack) * 20;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null && tryRestockEasyTrader(blockEntity, level, pos)) {
            return true;
        }
        if (tryAccelerateAe(level, pos, face, blockEntity, ticks)) {
            return true;
        }

        boolean accelerated = false;
        BlockState initialState = level.getBlockState(pos);
        if (blockEntity != null && initialState.getBlock() instanceof EntityBlock entityBlock) {
            @SuppressWarnings("unchecked")
            BlockEntityTicker<BlockEntity> ticker = (BlockEntityTicker<BlockEntity>) entityBlock.getTicker(
                level, initialState, blockEntity.getType()
            );
            if (ticker != null) {
                accelerated = true;
                for (int tick = 0; tick < ticks && level.getBlockEntity(pos) == blockEntity; tick++) {
                    ticker.tick(level, pos, level.getBlockState(pos), blockEntity);
                }
            }
        }

        for (int tick = 0; tick < ticks; tick++) {
            BlockState state = level.getBlockState(pos);
            if (!state.isRandomlyTicking()) {
                break;
            }
            accelerated = true;
            state.randomTick(serverLevel, pos, serverLevel.getRandom());
        }

        if (blockEntity != null && !accelerated) {
            player.sendSystemMessage(Component.literal("Deu ruim"));
        }
        return accelerated;
    }

    public static boolean accelerateBoundBlock(ServerLevel level, BlockPos pos, int ticks) {
        if (ticks <= 0 || !level.isLoaded(pos)) return false;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (tryAccelerateAeAllFaces(level, pos, blockEntity, ticks)) return true;
        boolean accelerated = false;
        BlockState initialState = level.getBlockState(pos);
        if (blockEntity != null && initialState.getBlock() instanceof EntityBlock entityBlock) {
            @SuppressWarnings("unchecked")
            BlockEntityTicker<BlockEntity> ticker = (BlockEntityTicker<BlockEntity>) entityBlock.getTicker(
                level, initialState, blockEntity.getType());
            if (ticker != null) {
                accelerated = true;
                for (int tick = 0; tick < ticks && level.getBlockEntity(pos) == blockEntity; tick++)
                    ticker.tick(level, pos, level.getBlockState(pos), blockEntity);
            }
        }
        for (int tick = 0; tick < ticks; tick++) {
            BlockState state = level.getBlockState(pos);
            if (!state.isRandomlyTicking()) break;
            accelerated = true;
            state.randomTick(level, pos, level.getRandom());
        }
        return accelerated;
    }

    private static boolean tryAccelerateAeAllFaces(Level level, BlockPos pos, BlockEntity blockEntity, int ticks) {
        try {
            boolean accelerated = false;
            Class<?> partHelperClass = Class.forName("appeng.api.parts.PartHelper");
            Method getPart = null;
            for (Method method : partHelperClass.getMethods()) {
                if (method.getName().equals("getPart") && Modifier.isStatic(method.getModifiers())
                    && method.getParameterCount() == 3) { getPart = method; break; }
            }
            if (getPart != null) {
                for (Direction direction : Direction.values()) {
                    Object part = getPart.invoke(null, level, pos, direction);
                    if (part != null) accelerated |= tickAeNodeOwner(part, ticks);
                }
            }
            if (blockEntity != null) accelerated |= tickAeNodeOwner(blockEntity, ticks);
            return accelerated;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean tryRestockEasyTrader(BlockEntity blockEntity, Level level, BlockPos pos) {
        String className = blockEntity.getClass().getName().toLowerCase();
        if (!className.startsWith("de.maxhenkel.easyvillagers.") || !className.contains("tradertileentity")) {
            return false;
        }

        try {
            Object storedVillager = blockEntity.getClass().getMethod("getVillagerEntity").invoke(blockEntity);
            if (storedVillager instanceof AbstractVillager villager) {
                villager.getOffers().forEach(MerchantOffer::resetUses);
                invokeIfPresent(blockEntity, "saveVillagerEntity");
                blockEntity.setChanged();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return true;
    }

    private static boolean tryAccelerateAe(
        Level level,
        BlockPos pos,
        Direction face,
        BlockEntity blockEntity,
        int ticks
    ) {
        try {
            Class<?> partHelperClass = Class.forName("appeng.api.parts.PartHelper");
            for (Method method : partHelperClass.getMethods()) {
                if (method.getName().equals("getPart") && Modifier.isStatic(method.getModifiers())
                    && method.getParameterCount() == 3) {
                    Object part = method.invoke(null, level, pos, face);
                    if (part != null && tickAeNodeOwner(part, ticks)) {
                        return true;
                    }
                    break;
                }
            }
            return blockEntity != null && tickAeNodeOwner(blockEntity, ticks);
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean tickAeNodeOwner(Object owner, int ticks) throws ReflectiveOperationException {
        Class<?> nodeClass = Class.forName("appeng.api.networking.IGridNode");
        Class<?> tickableClass = Class.forName("appeng.api.networking.ticking.IGridTickable");
        Object node = findNode(owner, nodeClass);
        if (node == null) {
            return false;
        }

        Object tickable = nodeClass.getMethod("getService", Class.class).invoke(node, tickableClass);
        if (tickable == null && tickableClass.isInstance(owner)) {
            tickable = owner;
        }
        if (tickable == null) {
            return false;
        }

        Class<?> gridClass = Class.forName("appeng.api.networking.IGrid");
        Object grid = nodeClass.getMethod("getGrid").invoke(node);
        if (grid != null) {
            Class<?> tickManagerClass = Class.forName("appeng.api.networking.ticking.ITickManager");
            Object tickManager = gridClass.getMethod("getTickManager").invoke(grid);
            if (tickManager != null) {
                tickManagerClass.getMethod("wakeDevice", nodeClass).invoke(tickManager, node);
            }
        }

        Method tickingRequest = tickableClass.getMethod("tickingRequest", nodeClass, int.class);
        for (int tick = 0; tick < ticks; tick++) {
            Object modulation = tickingRequest.invoke(tickable, node, 1);
            if (modulation instanceof Enum<?> value && "SLEEP".equals(value.name())) {
                break;
            }
        }
        return true;
    }

    private static Object findNode(Object owner, Class<?> nodeClass) {
        if (nodeClass.isInstance(owner)) {
            return owner;
        }
        for (String accessor : new String[]{"getGridNode", "getMainNode", "getNode"}) {
            try {
                Object candidate = owner.getClass().getMethod(accessor).invoke(owner);
                if (nodeClass.isInstance(candidate)) {
                    return candidate;
                }
                if (candidate != null) {
                    Class<?> managedNodeClass = Class.forName("appeng.api.networking.IManagedGridNode");
                    if (managedNodeClass.isInstance(candidate)) {
                        Object node = managedNodeClass.getMethod("getNode").invoke(candidate);
                        if (nodeClass.isInstance(node)) {
                            return node;
                        }
                    }
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static void invokeIfPresent(Object target, String methodName) {
        try {
            target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
