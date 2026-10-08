package net.gasharaujo.timelesss;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.gasharaujo.timelesss.block.entity.PlayerEnergyNode;
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
    public static final int MAX_MULTIPLIER = 50;
    private static final String ROOT_KEY = "timelesss_time_network";
    private static final String BINDS_KEY = "binds";
    private static final String DISTRIBUTORS_KEY = "distributors";

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
                Math.max(0, Math.min(MAX_MULTIPLIER, entry.getInt("multiplier")))
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
                binds.set(index, new Bind(dimension, position, Math.max(0, Math.min(MAX_MULTIPLIER, multiplier))));
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
            if (gameTime % 20L == 0L) {
                payForNextSecond(player);
            }
            CompoundTag network = data(player);
            int paidTicks = network.getInt("paid_ticks");
            if (paidTicks <= 0) {
                continue;
            }
            network.putInt("paid_ticks", paidTicks - 1);
            for (Bind bind : getBinds(player)) {
                if (bind.multiplier <= 0) {
                    continue;
                }
                ServerLevel targetLevel = resolveLevel(server, bind.dimension);
                BlockPos pos = BlockPos.of(bind.position);
                if (targetLevel != null && targetLevel.isLoaded(pos)) {
                    TimelesssAccelerator.accelerateBoundBlock(targetLevel, pos, bind.multiplier);
                }
            }
        }
    }

    private static void payForNextSecond(ServerPlayer player) {
        long cost = 0L;
        for (Bind bind : getBinds(player)) {
            cost += 100L * bind.multiplier * bind.multiplier;
        }
        CompoundTag network = data(player);
        if (cost == 0L) {
            network.putInt("paid_ticks", 0);
            return;
        }

        List<PlayerEnergyNode> distributors = validEnergyNodes(player, true);
        long available = distributors.stream().mapToLong(PlayerEnergyNode::getEnergyStored).sum();
        if (available < cost) {
            network.putInt("paid_ticks", 0);
            return;
        }

        long remaining = cost;
        for (PlayerEnergyNode distributor : distributors) {
            int extracted = distributor.extractEnergy((int) Math.min(Integer.MAX_VALUE, remaining));
            remaining -= extracted;
            if (remaining == 0L) {
                break;
            }
        }
        network.putInt("paid_ticks", 20);
    }

    public static String snapshot(ServerPlayer player) {
        List<PlayerEnergyNode> distributors = validEnergyNodes(player, true);
        long energy = distributors.stream().mapToLong(PlayerEnergyNode::getEnergyStored).sum();
        long capacity = distributors.stream().mapToLong(PlayerEnergyNode::getEnergyCapacity).sum();
        StringBuilder result = new StringBuilder().append(energy).append('|')
            .append(capacity).append('\n');
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
