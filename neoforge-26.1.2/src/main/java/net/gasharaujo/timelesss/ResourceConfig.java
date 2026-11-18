package net.gasharaujo.timelesss;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.gasharaujo.timelesss.resource.ResourceMath;
import net.gasharaujo.timelesss.resource.ResourceType;

/** Server-authoritative, automatically synchronized by the loader. Capacities require a world restart. */
public final class ResourceConfig {
    public enum Curve { DEFAULT, LINEAR }
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue PERSONAL_MAX = B.comment("Maximum personal acceleration multiplier.").defineInRange("limits.personalMax", 25, 1, 10000);
    public static final ModConfigSpec.IntValue DISTRIBUTOR_MAX = B.comment("Maximum extra ticks per server tick for each bind.").defineInRange("limits.distributorMax", 50, 1, 10000);
    public static final ModConfigSpec.EnumValue<ResourceType> NETWORK_RESOURCE = B.comment("Resource used by the distributor and hub. GAS needs optional Mekanism.").worldRestart().defineEnum("network.resource", ResourceType.ENERGY, ResourceType.ENERGY, ResourceType.FLUID, ResourceType.GAS, ResourceType.ITEM);
    public static final ModConfigSpec.ConfigValue<String> FLUID = B.comment("Registry ID; empty means minecraft:lava.").worldRestart().define("network.fluid", "");
    public static final ModConfigSpec.ConfigValue<String> GAS = B.comment("Mekanism gas/chemical registry ID; empty means mekanism:hydrogen.").worldRestart().define("network.gas", "");
    public static final ModConfigSpec.ConfigValue<String> ITEM = B.comment("Registry ID; empty means minecraft:coal.").worldRestart().define("network.item", "");
    public static final ModConfigSpec.EnumValue<Curve> CURVE = B.comment("DEFAULT: base * multiplier squared. LINEAR: base * multiplier. XP is per level; TIME is per accelerated tick.").defineEnum("consumption.curve", Curve.DEFAULT);
    public static final ModConfigSpec.IntValue ENERGY_COST = integer("consumption.energyBase", 100, 0, "FE per second at 1x; also FE per personal activation at 1x.");
    public static final ModConfigSpec.IntValue FLUID_COST = integer("consumption.fluidBase", 100, 0, "mB per second at 1x, or per personal activation at 1x.");
    public static final ModConfigSpec.IntValue GAS_COST = integer("consumption.gasBase", 100, 0, "mB per second at 1x.");
    public static final ModConfigSpec.IntValue ITEM_COST = integer("consumption.itemBase", 1, 0, "Items per second at 1x.");
    public static final ModConfigSpec.IntValue HUB_TANK = integer("storage.hubTank", 16000, 1, "Initial hub fluid/gas capacity in mB (1000 mB = 1 B).");
    public static final ModConfigSpec.IntValue BATTERY_TANK = integer("storage.batteryTank", 1000, 1, "Battery fluid/gas capacity; added to hub for every EXTRA battery.");
    public static final ModConfigSpec.IntValue DISTRIBUTOR_TANK = integer("storage.distributorTank", 1000, 1, "Distributor fluid/gas capacity in mB.");
    public static final ModConfigSpec.IntValue HUB_SLOTS = integer("storage.hubSlots", 16, 1, "Initial hub item slots.");
    public static final ModConfigSpec.IntValue BATTERY_SLOTS = integer("storage.batterySlots", 2, 1, "Battery item slots; added to hub for every EXTRA battery.");
    public static final ModConfigSpec.IntValue DISTRIBUTOR_SLOTS = integer("storage.distributorSlots", 2, 1, "Distributor item slots. Each uses the configured item's actual stack limit.");
    public static final ModConfigSpec.EnumValue<ResourceType> PERSONAL_RESOURCE = B.comment("Personal item consumption. NONE preserves free use.").worldRestart().defineEnum("personal.resource", ResourceType.NONE, ResourceType.NONE, ResourceType.FLUID, ResourceType.DURABILITY, ResourceType.XP, ResourceType.ENERGY, ResourceType.TIME);
    public static final ModConfigSpec.IntValue DURABILITY_LIMIT = integer("personal.durabilityLimit", 2048, 1, "Total uses of durability; depleted item breaks.");
    public static final ModConfigSpec.IntValue DURABILITY_COST = integer("personal.durabilityBase", 1, 0, "Durability per activation at 1x, using the consumption curve.");
    public static final ModConfigSpec.IntValue PERSONAL_TANK = integer("personal.fluidLimit", 16000, 1, "Personal fluid capacity in mB; uses network.fluid.");
    public static final ModConfigSpec.IntValue PERSONAL_ENERGY = integer("personal.energyLimit", 100000, 1, "Personal energy capacity in FE.");
    public static final ModConfigSpec.IntValue XP_COST = integer("personal.xpPerLevel", 2, 0, "Raw XP points per multiplier level per activation; default 2.");
    public static final ModConfigSpec.IntValue TIME_COST = integer("personal.timePerTick", 1, 0, "Stored ticks consumed per accelerated tick; default 1.");
    public static final ModConfigSpec.IntValue TIME_CHARGE = integer("personal.timeChargePerTick", 1, 0, "Stored ticks gained each server tick in inventory or Curios. No offline charging.");
    public static final ModConfigSpec.LongValue TIME_LIMIT = B.comment("Stored tick limit, 0 = unlimited.").worldRestart().defineInRange("personal.timeLimit", 0L, 0L, Long.MAX_VALUE);
    public static final ModConfigSpec SPECIFICATION = B.build();

    private ResourceConfig() {}
    private static ModConfigSpec.IntValue integer(String key, int value, int min, String comment) {
        return B.comment(comment).worldRestart().defineInRange(key, value, min, key.endsWith("Slots") ? 32768 : Integer.MAX_VALUE);
    }
    public static <T> T value(ModConfigSpec.ConfigValue<T> v) { return SPECIFICATION.isLoaded() ? v.get() : v.getDefault(); }
    public static int personalMax() { return value(PERSONAL_MAX); }
    public static int distributorMax() { return value(DISTRIBUTOR_MAX); }
    public static ResourceType networkResource() { return value(NETWORK_RESOURCE); }
    public static ResourceType personalResource() { return value(PERSONAL_RESOURCE); }
    public static String id(ResourceType type) {
        String id = switch (type) {
            case FLUID -> value(FLUID);
            case GAS -> value(GAS);
            case ITEM -> value(ITEM);
            default -> "";
        };
        if (!id.isBlank()) return id.trim();
        return switch (type) { case FLUID -> "minecraft:lava"; case GAS -> "mekanism:hydrogen"; case ITEM -> "minecraft:coal"; default -> ""; };
    }
    public static long cost(ResourceType type, int multiplier) {
        int base = switch(type) {
            case ENERGY -> value(ENERGY_COST); case FLUID -> value(FLUID_COST);
            case GAS -> value(GAS_COST); case ITEM -> value(ITEM_COST);
            case DURABILITY -> value(DURABILITY_COST); case XP -> value(XP_COST);
            case TIME -> value(TIME_COST); default -> 0;
        };
        if (type == ResourceType.TIME) return ResourceMath.multiply(ResourceMath.multiply(base, multiplier), 20);
        return ResourceMath.cost(base, multiplier, type == ResourceType.XP || value(CURVE) == Curve.LINEAR);
    }
    public static String unit() {
        return switch (networkResource()) { case ENERGY -> "FE"; case FLUID, GAS -> "mB"; case ITEM -> "items"; default -> ""; };
    }
}
