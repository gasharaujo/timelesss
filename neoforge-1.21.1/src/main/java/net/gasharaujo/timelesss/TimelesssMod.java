package net.gasharaujo.timelesss;

import java.util.Set;
import net.gasharaujo.timelesss.block.TimeDistributorBlock;
import net.gasharaujo.timelesss.block.BatteryHubBlock;
import net.gasharaujo.timelesss.block.entity.BatteryHubBlockEntity;
import net.gasharaujo.timelesss.block.entity.TimeDistributorBlockEntity;
import net.gasharaujo.timelesss.item.TimeWrenchItem;
import net.gasharaujo.timelesss.item.BatteryHubItem;
import net.gasharaujo.timelesss.item.TimelesssItem;
import net.gasharaujo.timelesss.item.TimelesssAccelerator;
import net.gasharaujo.timelesss.item.TimelesssLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(TimelesssMod.MOD_ID)
public final class TimelesssMod {
    public static final String MOD_ID = "timelesss";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredHolder<Block, TimeDistributorBlock> TIME_DISTRIBUTOR = BLOCKS.register(
        "time_distributor", () -> new TimeDistributorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
            .strength(50.0F, 1200.0F).requiresCorrectToolForDrops())
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TimeDistributorBlockEntity>>
        TIME_DISTRIBUTOR_BLOCK_ENTITY = BLOCK_ENTITIES.register("time_distributor", () ->
            BlockEntityType.Builder.of(TimeDistributorBlockEntity::new, TIME_DISTRIBUTOR.get()).build(null));
    public static final DeferredHolder<Block, BatteryHubBlock> BATTERY_HUB = BLOCKS.register(
        "battery_hub", () -> new BatteryHubBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
            .strength(5.0F, 6.0F).requiresCorrectToolForDrops())
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryHubBlockEntity>>
        BATTERY_HUB_BLOCK_ENTITY = BLOCK_ENTITIES.register("battery_hub", () ->
            BlockEntityType.Builder.of(BatteryHubBlockEntity::new, BATTERY_HUB.get()).build(null));
    public static final DeferredItem<TimelesssItem> TIMELESSS = ITEMS.registerItem(
        "timelesss",
        properties -> new TimelesssItem(withDefaultMultiplier(properties))
    );
    public static final DeferredItem<Item> TIME_DISTRIBUTOR_ITEM = ITEMS.register(
        "time_distributor", () -> new BlockItem(TIME_DISTRIBUTOR.get(), new Item.Properties().fireResistant())
    );
    public static final DeferredItem<TimeWrenchItem> TIME_WRENCH = ITEMS.registerItem(
        "time_wrench", properties -> new TimeWrenchItem(properties.stacksTo(1).fireResistant())
    );
    public static final DeferredItem<Item> BATTERY = ITEMS.registerSimpleItem("battery");
    public static final DeferredItem<Item> BATTERY_HUB_ITEM = ITEMS.register(
        "battery_hub", () -> new BatteryHubItem(BATTERY_HUB.get(), new Item.Properties())
    );

    public TimelesssMod(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(TimelesssMod::addCreativeTabContents);
        modBus.addListener(TimelesssMod::registerPayloads);
        modBus.addListener(TimelesssMod::registerCapabilities);
    }

    private static Item.Properties withDefaultMultiplier(Item.Properties properties) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TimelesssItem.MULTIPLIER_KEY, TimelesssItem.MIN_MULTIPLIER);
        return properties
            .stacksTo(1)
            .fireResistant()
            .component(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(TIMELESSS.get());
            event.accept(TIME_WRENCH.get());
            event.accept(TIME_DISTRIBUTOR_ITEM.get());
            event.accept(BATTERY.get());
            event.accept(BATTERY_HUB_ITEM.get());
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("4");
        registrar.playToServer(
            SetMultiplierPayload.TYPE,
            SetMultiplierPayload.STREAM_CODEC,
            SetMultiplierPayload::handle
        );
        registrar.playToServer(
            ActivatePayload.TYPE,
            ActivatePayload.STREAM_CODEC,
            ActivatePayload::handle
        );
        registrar.playToServer(RequestTimeBindsPayload.TYPE, RequestTimeBindsPayload.STREAM_CODEC,
            RequestTimeBindsPayload::handle);
        registrar.playToServer(SetBindMultiplierPayload.TYPE, SetBindMultiplierPayload.STREAM_CODEC,
            SetBindMultiplierPayload::handle);
        registrar.playToClient(TimeBindsSnapshotPayload.TYPE, TimeBindsSnapshotPayload.STREAM_CODEC,
            TimeBindsSnapshotPayload::handle);
        registrar.playToServer(RemoveBindPayload.TYPE, RemoveBindPayload.STREAM_CODEC, RemoveBindPayload::handle);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TIME_DISTRIBUTOR_BLOCK_ENTITY.get(),
            (blockEntity, side) -> blockEntity.getEnergyHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BATTERY_HUB_BLOCK_ENTITY.get(),
            (blockEntity, side) -> blockEntity.getEnergyHandler());
    }

    public record SetMultiplierPayload(int sourceType, int sourceIndex, int multiplier) implements CustomPacketPayload {
        public static final Type<SetMultiplierPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "set_multiplier")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, SetMultiplierPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SetMultiplierPayload::sourceType,
            ByteBufCodecs.VAR_INT,
            SetMultiplierPayload::sourceIndex,
            ByteBufCodecs.VAR_INT,
            SetMultiplierPayload::multiplier,
            SetMultiplierPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static void handle(SetMultiplierPayload payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ItemStack stack = TimelesssLocator.resolve(player, payload.sourceType, payload.sourceIndex);
            if (stack.is(TIMELESSS.get())) {
                TimelesssItem.setMultiplier(stack, payload.multiplier);
            }
        }
    }

    public record ActivatePayload(int sourceType, int sourceIndex, int targetType, long targetData, int faceIndex)
        implements CustomPacketPayload {
        public static final Type<ActivatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "activate")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, ActivatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ActivatePayload::sourceType,
            ByteBufCodecs.VAR_INT,
            ActivatePayload::sourceIndex,
            ByteBufCodecs.VAR_INT,
            ActivatePayload::targetType,
            ByteBufCodecs.VAR_LONG,
            ActivatePayload::targetData,
            ByteBufCodecs.VAR_INT,
            ActivatePayload::faceIndex,
            ActivatePayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static void handle(ActivatePayload payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = TimelesssLocator.resolve(player, payload.sourceType, payload.sourceIndex);
            if (!stack.is(TIMELESSS.get())) {
                return;
            }
            if (payload.targetType == 0) {
                BlockPos pos = BlockPos.of(payload.targetData);
                if (player.distanceToSqr(pos.getCenter()) <= 64.0 && player.serverLevel().isLoaded(pos)) {
                    TimelesssAccelerator.accelerateBlock(
                        player, player.serverLevel(), pos, Direction.from3DDataValue(payload.faceIndex), stack
                    );
                }
            } else if (payload.targetType == 1) {
                Entity entity = player.serverLevel().getEntity((int) payload.targetData);
                if (entity != null && entity != player && player.distanceToSqr(entity) <= 64.0) {
                    TimelesssAccelerator.accelerateEntity(player, entity, stack);
                }
            }
        }
    }

    public static void sendTimeBinds(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new TimeBindsSnapshotPayload(TimeNetwork.snapshot(player)));
    }

    public record RequestTimeBindsPayload(int ignored) implements CustomPacketPayload {
        public static final Type<RequestTimeBindsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "request_time_binds"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestTimeBindsPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, RequestTimeBindsPayload::ignored, RequestTimeBindsPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
        private static void handle(RequestTimeBindsPayload payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) sendTimeBinds(player);
        }
    }

    public record SetBindMultiplierPayload(String dimension, long position, int multiplier)
        implements CustomPacketPayload {
        public static final Type<SetBindMultiplierPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "set_bind_multiplier"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetBindMultiplierPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetBindMultiplierPayload::dimension,
                ByteBufCodecs.VAR_LONG, SetBindMultiplierPayload::position,
                ByteBufCodecs.VAR_INT, SetBindMultiplierPayload::multiplier, SetBindMultiplierPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
        private static void handle(SetBindMultiplierPayload payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player)
                TimeNetwork.setMultiplier(player, payload.dimension, payload.position, payload.multiplier);
        }
    }

    public record TimeBindsSnapshotPayload(String snapshot) implements CustomPacketPayload {
        public static final Type<TimeBindsSnapshotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "time_binds_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TimeBindsSnapshotPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, TimeBindsSnapshotPayload::snapshot,
                TimeBindsSnapshotPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
        private static void handle(TimeBindsSnapshotPayload payload, IPayloadContext context) {
            net.gasharaujo.timelesss.client.ClientEvents.openTimeBindsScreen(payload.snapshot);
        }
    }

    public record RemoveBindPayload(String dimension, long position) implements CustomPacketPayload {
        public static final Type<RemoveBindPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "remove_bind"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RemoveBindPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RemoveBindPayload::dimension,
                ByteBufCodecs.VAR_LONG, RemoveBindPayload::position, RemoveBindPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
        private static void handle(RemoveBindPayload payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player)
                TimeNetwork.removeBind(player, payload.dimension, payload.position);
        }
    }
}
