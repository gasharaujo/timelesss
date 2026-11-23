package net.gasharaujo.timelesss;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.gasharaujo.timelesss.block.entity.BatteryHubBlockEntity;
import net.gasharaujo.timelesss.block.entity.PlayerEnergyNode;
import net.gasharaujo.timelesss.resource.FuelPayment;
import net.gasharaujo.timelesss.resource.ResourceMath;
import net.gasharaujo.timelesss.resource.ResourceStorage;
import net.gasharaujo.timelesss.resource.ResourceType;
import net.gasharaujo.timelesss.item.TimelesssAccelerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class TimeNetwork {
    public static int maxMultiplier() { return ResourceConfig.distributorMax(); }
    private static final String ROOT_KEY = "timelesss_time_network";
    private static final String BINDS_KEY = "binds";
    private static final String DISTRIBUTORS_KEY = "distributors";
    private static final String FULL_CAPACITY_PAYMENT_KEY = "full_capacity_payment";
    private static final int BULK_BATTERIES = 640;

    private TimeNetwork() {
    }

    public record Bind(String dimension, long position, int multiplier) {
    }

    public static List<Bind> getBinds(ServerPlayer player) {
        List<Bind> result = new ArrayList<>();
        ListTag list = data(player).getList(BINDS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            result.add(new Bind(
                entry.getString("dimension"), entry.getLong("position"),
                Math.max(0, Math.min(maxMultiplier(), entry.getInt("multiplier")))
            ));
        }
        return result;
    }

    public static boolean addBind(ServerPlayer player, String dimension, long position) {
        List<Bind> binds = getBinds(player);
        if (binds.stream().anyMatch(bind -> bind.position == position && bind.dimension.equals(dimension))) {
            return false;
        }
        binds.add(new Bind(dimension, position, 0));
        saveBinds(player, binds);
        return true;
    }

    public static void setMultiplier(ServerPlayer player, String dimension, long position, int multiplier) {
        List<Bind> binds = getBinds(player);
        for (int index = 0; index < binds.size(); index++) {
            Bind bind = binds.get(index);
            if (bind.position == position && bind.dimension.equals(dimension)) {
                binds.set(index, new Bind(dimension, position, Math.max(0, Math.min(maxMultiplier(), multiplier))));
                saveBinds(player, binds);
                return;
            }
        }
    }

    public static boolean removeBind(ServerPlayer player, String dimension, long position) {
        List<Bind> binds = getBinds(player);
        boolean removed = binds.removeIf(bind -> bind.position == position && bind.dimension.equals(dimension));
        if (removed) {
            saveBinds(player, binds);
        }
        return removed;
    }

    public static void addEnergyNode(ServerPlayer player, String dimension, long position) {
        CompoundTag network = data(player);
        ListTag list = network.getList(DISTRIBUTORS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            if (entry.getLong("position") == position && entry.getString("dimension").equals(dimension)) {
                return;
            }
        }
        CompoundTag entry = new CompoundTag();
        entry.putString("dimension", dimension);
        entry.putLong("position", position);
        list.add(entry);
        network.put(DISTRIBUTORS_KEY, list);
    }

    public static void tick(ServerLevel eventLevel) {
        if (eventLevel.dimension() != ServerLevel.OVERWORLD) {
            return;
        }
        long gameTime = eventLevel.getGameTime();
        MinecraftServer server = eventLevel.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            net.gasharaujo.timelesss.resource.PersonalResources.recharge(player);
            if (gameTime % 20L == 0L) {
                payForNextSecond(player);
            }
            acceleratePaidBinds(player, server);
        }
    }

    private static void acceleratePaidBinds(ServerPlayer player, MinecraftServer server) {
        CompoundTag network = data(player);
        int paidTicks = network.getInt("paid_ticks");
        if (paidTicks <= 0) {
            return;
        }
        network.putInt("paid_ticks", paidTicks - 1);
        for (Bind bind : getBinds(player)) {
            if (bind.multiplier <= 0) {
                continue;
            }
            ServerLevel targetLevel = resolveLevel(server, bind.dimension);
            BlockPos pos = BlockPos.of(bind.position);
            if (targetLevel != null && targetLevel.isLoaded(pos)) {
                boolean accelerated = TimelesssAccelerator.accelerateBoundBlock(targetLevel, pos, bind.multiplier);
                if (accelerated && network.getBoolean(FULL_CAPACITY_PAYMENT_KEY)) {
                    TimelesssAdvancements.award(player, "time_managing");
                    network.remove(FULL_CAPACITY_PAYMENT_KEY);
                }
            }
        }
    }

    private static void payForNextSecond(ServerPlayer player) {
        CompoundTag network = data(player);
        List<PlayerEnergyNode> nodes = validEnergyNodes(player, true);
        List<ResourceStorage> stores = nodes.stream().map(PlayerEnergyNode::resources).toList();
        network.remove(FULL_CAPACITY_PAYMENT_KEY);
        if (stores.isEmpty()) {
            network.putInt("paid_ticks", 0);
            return;
        }

        if (stores.stream().anyMatch(store -> store.amount(ResourceType.ENERGY) > 0)) {
            TimelesssAdvancements.award(player, "using_power");
        }
        checkBulkBatteries(player, nodes);

        List<Bind> binds = getBinds(player);
        var plan = FuelPayment.plan(stores, ResourceConfig.networkResources(), type -> {
            long cost = 0;
            for (Bind bind : binds) {
                cost = ResourceMath.add(cost, ResourceConfig.cost(type, bind.multiplier));
            }
            return cost;
        }, network::getString);
        boolean paid = plan != null && plan.apply(network::putString);
        network.putInt("paid_ticks", paid ? 20 : 0);
        if (paid && !plan.debits().isEmpty()) {
            ResourceType type = plan.debits().get(0).type();
            long capacity = stores.stream().mapToLong(store -> store.capacity(type)).sum();
            long charged = plan.debits().stream().mapToLong(FuelPayment.Debit::amount).sum();
            if (charged == capacity) {
                network.putBoolean(FULL_CAPACITY_PAYMENT_KEY, true);
            }
        }
    }

    public static void checkBulkBatteries(ServerPlayer player) {
        checkBulkBatteries(player, validEnergyNodes(player, false));
    }

    private static void checkBulkBatteries(ServerPlayer player, List<PlayerEnergyNode> nodes) {
        long extraBatteries = nodes.stream()
            .filter(BatteryHubBlockEntity.class::isInstance)
            .map(BatteryHubBlockEntity.class::cast)
            .mapToLong(hub -> hub.getBatteryCount() - BatteryHubBlockEntity.INITIAL_BATTERIES)
            .sum();
        if (extraBatteries >= BULK_BATTERIES) {
            TimelesssAdvancements.award(player, "bulk_accelerate");
        }
    }

    public static String snapshot(ServerPlayer player) {
        List<PlayerEnergyNode> distributors = validEnergyNodes(player, true);
        long energy = distributors.stream().mapToLong(PlayerEnergyNode::getResourceStored).sum();
        long capacity = distributors.stream().mapToLong(PlayerEnergyNode::getResourceCapacity).sum();
        StringBuilder result = new StringBuilder().append(energy).append('|')
            .append(capacity).append('|').append(ResourceConfig.unit()).append('|')
            .append(maxMultiplier()).append('|').append(ResourceConfig.value(ResourceConfig.CURVE).name()).append('|')
            .append(ResourceConfig.cost(ResourceConfig.networkResource(),1)).append('|')
            .append(net.gasharaujo.timelesss.resource.NetworkResourceView.snapshot(distributors.stream().map(PlayerEnergyNode::resources).toList())).append('\n');
        MinecraftServer server = player.getServer();
        for (Bind bind : getBinds(player)) {
            String blockId = "minecraft:air";
            ServerLevel level = resolveLevel(server, bind.dimension);
            BlockPos pos = BlockPos.of(bind.position);
            if (level != null && level.isLoaded(pos)) {
                blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getKey(level.getBlockState(pos).getBlock()).toString();
            }
            result.append(bind.dimension).append('|').append(bind.position).append('|')
                .append(bind.multiplier).append('|').append(blockId).append('\n');
        }
        return result.toString();
    }

    private static List<PlayerEnergyNode> validEnergyNodes(ServerPlayer player, boolean clean) {
        CompoundTag network = data(player);
        ListTag stored = network.getList(DISTRIBUTORS_KEY, Tag.TAG_COMPOUND);
        ListTag validTags = new ListTag();
        List<PlayerEnergyNode> result = new ArrayList<>();
        MinecraftServer server = player.getServer();
        for (int index = 0; index < stored.size(); index++) {
            CompoundTag entry = stored.getCompound(index);
            ServerLevel level = resolveLevel(server, entry.getString("dimension"));
            BlockPos pos = BlockPos.of(entry.getLong("position"));
            if (level != null && level.isLoaded(pos)
                && level.getBlockEntity(pos) instanceof PlayerEnergyNode distributor
                && Objects.equals(distributor.getOwner(), player.getUUID())) {
                result.add(distributor);
                validTags.add(entry.copy());
            } else if (level != null && !level.isLoaded(pos)) {
                validTags.add(entry.copy());
            }
        }
        if (clean) {
            network.put(DISTRIBUTORS_KEY, validTags);
        }
        return result;
    }

    private static ServerLevel resolveLevel(MinecraftServer server, String dimension) {
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    private static void saveBinds(ServerPlayer player, List<Bind> binds) {
        ListTag list = new ListTag();
        for (Bind bind : binds) {
            CompoundTag entry = new CompoundTag();
            entry.putString("dimension", bind.dimension);
            entry.putLong("position", bind.position);
            entry.putInt("multiplier", bind.multiplier);
            list.add(entry);
        }
        data(player).put(BINDS_KEY, list);
        CompoundTag network = data(player);
        network.putInt("paid_ticks", 0);
        network.remove(FULL_CAPACITY_PAYMENT_KEY);
    }

    private static CompoundTag data(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persistent.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(Player.PERSISTED_NBT_TAG, persisted);
        }
        CompoundTag network = persisted.getCompound(ROOT_KEY);
        if (!persisted.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            persisted.put(ROOT_KEY, network);
        }
        return network;
    }
}
