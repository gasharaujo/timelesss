package net.gasharaujo.timelesss;

import java.util.function.Supplier;
import net.gasharaujo.timelesss.block.TimeDistributorBlock;
import net.gasharaujo.timelesss.block.BatteryHubBlock;
import net.gasharaujo.timelesss.block.entity.BatteryHubBlockEntity;
import net.gasharaujo.timelesss.block.entity.TimeDistributorBlockEntity;
import net.gasharaujo.timelesss.client.ClientEvents;
import net.gasharaujo.timelesss.item.TimeWrenchItem;
import net.gasharaujo.timelesss.item.BatteryHubItem;
import net.gasharaujo.timelesss.item.TimelesssAccelerator;
import net.gasharaujo.timelesss.item.TimelesssItem;
import net.gasharaujo.timelesss.item.TimelesssLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(TimelesssMod.MOD_ID)
public final class TimelesssMod {
    public static final String MOD_ID = "timelesss";
    private static final String NETWORK_VERSION = "4";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final RegistryObject<TimeDistributorBlock> TIME_DISTRIBUTOR = BLOCKS.register(
        "time_distributor",
        () -> new TimeDistributorBlock(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK)
            .strength(50.0F, 1200.0F).requiresCorrectToolForDrops())
    );
    public static final RegistryObject<BlockEntityType<TimeDistributorBlockEntity>> TIME_DISTRIBUTOR_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("time_distributor", () ->
            BlockEntityType.Builder.of(TimeDistributorBlockEntity::new, TIME_DISTRIBUTOR.get()).build(null));
    public static final RegistryObject<BatteryHubBlock> BATTERY_HUB = BLOCKS.register(
        "battery_hub",
        () -> new BatteryHubBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
            .strength(5.0F, 6.0F).requiresCorrectToolForDrops())
    );
    public static final RegistryObject<BlockEntityType<BatteryHubBlockEntity>> BATTERY_HUB_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("battery_hub", () ->
            BlockEntityType.Builder.of(BatteryHubBlockEntity::new, BATTERY_HUB.get()).build(null));
    public static final RegistryObject<TimelesssItem> TIMELESSS = ITEMS.register(
        "timelesss",
        () -> new TimelesssItem(new Item.Properties().stacksTo(1).fireResistant())
    );
    public static final RegistryObject<Item> TIME_DISTRIBUTOR_ITEM = ITEMS.register(
        "time_distributor", () -> new BlockItem(TIME_DISTRIBUTOR.get(), new Item.Properties().fireResistant())
    );
    public static final RegistryObject<TimeWrenchItem> TIME_WRENCH = ITEMS.register(
        "time_wrench", () -> new TimeWrenchItem(new Item.Properties().stacksTo(1).fireResistant())
    );
    public static final RegistryObject<Item> BATTERY = ITEMS.register(
        "battery", () -> new Item(new Item.Properties())
    );
    public static final RegistryObject<Item> BATTERY_HUB_ITEM = ITEMS.register(
        "battery_hub", () -> new BatteryHubItem(BATTERY_HUB.get(), new Item.Properties())
    );

    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(MOD_ID, "main"),
        () -> NETWORK_VERSION,
        NETWORK_VERSION::equals,
        NETWORK_VERSION::equals
    );

    public TimelesssMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(TimelesssMod::commonSetup);
        modBus.addListener(TimelesssMod::addCreativeTabContents);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NETWORK.messageBuilder(
                SetMultiplierMessage.class,
                0,
                NetworkDirection.PLAY_TO_SERVER
            )
            .encoder(SetMultiplierMessage::encode)
            .decoder(SetMultiplierMessage::decode)
            .consumerMainThread(SetMultiplierMessage::handle)
            .add();
            NETWORK.messageBuilder(
                    ActivateMessage.class,
                    1,
                    NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(ActivateMessage::encode)
                .decoder(ActivateMessage::decode)
                .consumerMainThread(ActivateMessage::handle)
                .add();
            NETWORK.messageBuilder(RequestTimeBindsMessage.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestTimeBindsMessage::encode)
                .decoder(RequestTimeBindsMessage::decode)
                .consumerMainThread(RequestTimeBindsMessage::handle)
                .add();
            NETWORK.messageBuilder(SetBindMultiplierMessage.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetBindMultiplierMessage::encode)
                .decoder(SetBindMultiplierMessage::decode)
                .consumerMainThread(SetBindMultiplierMessage::handle)
                .add();
            NETWORK.messageBuilder(TimeBindsSnapshotMessage.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TimeBindsSnapshotMessage::encode)
                .decoder(TimeBindsSnapshotMessage::decode)
                .consumerMainThread(TimeBindsSnapshotMessage::handle)
                .add();
            NETWORK.messageBuilder(RemoveBindMessage.class, 5, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RemoveBindMessage::encode)
                .decoder(RemoveBindMessage::decode)
                .consumerMainThread(RemoveBindMessage::handle)
                .add();
        });
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

    public record SetMultiplierMessage(int sourceType, int sourceIndex, int multiplier) {
        private static void encode(SetMultiplierMessage message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.sourceType);
            buffer.writeVarInt(message.sourceIndex);
            buffer.writeVarInt(message.multiplier);
        }

        private static SetMultiplierMessage decode(FriendlyByteBuf buffer) {
            return new SetMultiplierMessage(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        }

        private static void handle(SetMultiplierMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }

                ItemStack stack = TimelesssLocator.resolve(player, message.sourceType, message.sourceIndex);
                if (stack.is(TIMELESSS.get())) {
                    TimelesssItem.setMultiplier(stack, message.multiplier);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record ActivateMessage(int sourceType, int sourceIndex, int targetType, long targetData, int faceIndex) {
        private static void encode(ActivateMessage message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.sourceType);
            buffer.writeVarInt(message.sourceIndex);
            buffer.writeVarInt(message.targetType);
            buffer.writeVarLong(message.targetData);
            buffer.writeVarInt(message.faceIndex);
        }

        private static ActivateMessage decode(FriendlyByteBuf buffer) {
            return new ActivateMessage(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readVarInt()
            );
        }

        private static void handle(ActivateMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                ItemStack stack = TimelesssLocator.resolve(player, message.sourceType, message.sourceIndex);
                if (!stack.is(TIMELESSS.get())) {
                    return;
                }
                if (message.targetType == 0) {
                    BlockPos pos = BlockPos.of(message.targetData);
                    if (player.distanceToSqr(pos.getCenter()) <= 64.0 && player.serverLevel().isLoaded(pos)) {
                        TimelesssAccelerator.accelerateBlock(
                            player, player.serverLevel(), pos, Direction.from3DDataValue(message.faceIndex), stack
                        );
                    }
                } else if (message.targetType == 1) {
                    Entity entity = player.serverLevel().getEntity((int) message.targetData);
                    if (entity != null && entity != player && player.distanceToSqr(entity) <= 64.0) {
                        TimelesssAccelerator.accelerateEntity(player, entity, stack);
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static void sendTimeBinds(ServerPlayer player) {
        NETWORK.send(PacketDistributor.PLAYER.with(() -> player), new TimeBindsSnapshotMessage(TimeNetwork.snapshot(player)));
    }

    public record RequestTimeBindsMessage() {
        private static void encode(RequestTimeBindsMessage message, FriendlyByteBuf buffer) {
        }

        private static RequestTimeBindsMessage decode(FriendlyByteBuf buffer) {
            return new RequestTimeBindsMessage();
        }

        private static void handle(RequestTimeBindsMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (context.getSender() != null) {
                    sendTimeBinds(context.getSender());
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record SetBindMultiplierMessage(String dimension, long position, int multiplier) {
        private static void encode(SetBindMultiplierMessage message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.dimension);
            buffer.writeLong(message.position);
            buffer.writeVarInt(message.multiplier);
        }

        private static SetBindMultiplierMessage decode(FriendlyByteBuf buffer) {
            return new SetBindMultiplierMessage(buffer.readUtf(), buffer.readLong(), buffer.readVarInt());
        }

        private static void handle(SetBindMultiplierMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (context.getSender() != null) {
                    TimeNetwork.setMultiplier(context.getSender(), message.dimension, message.position, message.multiplier);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record TimeBindsSnapshotMessage(String snapshot) {
        private static void encode(TimeBindsSnapshotMessage message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.snapshot, 1_048_576);
        }

        private static TimeBindsSnapshotMessage decode(FriendlyByteBuf buffer) {
            return new TimeBindsSnapshotMessage(buffer.readUtf(1_048_576));
        }

        private static void handle(TimeBindsSnapshotMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> ClientEvents.openTimeBindsScreen(message.snapshot)
            ));
            context.setPacketHandled(true);
        }
    }

    public record RemoveBindMessage(String dimension, long position) {
        private static void encode(RemoveBindMessage message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.dimension);
            buffer.writeLong(message.position);
        }

        private static RemoveBindMessage decode(FriendlyByteBuf buffer) {
            return new RemoveBindMessage(buffer.readUtf(), buffer.readLong());
        }

        private static void handle(RemoveBindMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (context.getSender() != null) {
                    TimeNetwork.removeBind(context.getSender(), message.dimension, message.position);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
