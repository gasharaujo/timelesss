package net.gasharaujo.timelesss;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.gasharaujo.timelesss.resource.ResourceMath;
import net.gasharaujo.timelesss.resource.FuelRules;
import java.util.List;
import java.math.BigDecimal;
import net.gasharaujo.timelesss.resource.ResourceType;

/** Server-authoritative, automatically synchronized by the loader. Capacities require a world restart. */
public final class ResourceConfig {
    public enum Curve { DEFAULT, LINEAR }
    private static final ModConfigSpec.Builder B = groups(new ModConfigSpec.Builder());
    public static final ModConfigSpec.IntValue PERSONAL_MAX = B.comment("Maximum personal acceleration multiplier.").translation("config.timelesss.limits.personalMax").defineInRange("limits.personalMax", 25, 1, 10000);
    public static final ModConfigSpec.IntValue DISTRIBUTOR_MAX = B.comment("Maximum extra ticks per server tick for each bind.").translation("config.timelesss.limits.distributorMax").defineInRange("limits.distributorMax", 50, 1, 10000);
    public static final ModConfigSpec.ConfigValue<Object> NETWORK_RESOURCE = B.comment("Accepted alternatives in priority order: ENERGY, FLUID, GAS, ITEM. One alternative pays the whole second. Legacy single strings remain valid.").translation("config.timelesss.network.resource").worldRestart().define("network.resource", (Object)new java.util.ArrayList<>(List.of("ENERGY")), FuelRules::validTypes);
    public static final ModConfigSpec.ConfigValue<Object> FLUID = B.comment("One material per row: namespace:id or namespace:id = consumptionModifier. Example: mekanism:hydrogen = 1, mekanism:ethene = 0.25. Also accepts an ID string, pairs or an associated table in TOML. Empty = minecraft:lava.").translation("config.timelesss.network.fluid").worldRestart().define("network.fluid", (Object)new java.util.ArrayList<>(List.of("minecraft:lava")), FuelRules::validMaterials);
    public static final ModConfigSpec.ConfigValue<Object> GAS = B.comment("One material per row: namespace:id or namespace:id = consumptionModifier. Example: mekanism:hydrogen = 1, mekanism:ethene = 0.25. Also accepts an ID string, pairs or an associated table in TOML. Empty = mekanism:hydrogen.").translation("config.timelesss.network.gas").worldRestart().define("network.gas", (Object)new java.util.ArrayList<>(List.of("mekanism:hydrogen")), FuelRules::validMaterials);
    public static final ModConfigSpec.ConfigValue<Object> ITEM = B.comment("One material per row: namespace:id or namespace:id = consumptionModifier. Example: mekanism:hydrogen = 1, mekanism:ethene = 0.25. Also accepts an ID string, pairs or an associated table in TOML. Empty = minecraft:coal.").translation("config.timelesss.network.item").worldRestart().define("network.item", (Object)new java.util.ArrayList<>(List.of("minecraft:coal")), FuelRules::validMaterials);
    public static final ModConfigSpec.EnumValue<Curve> CURVE = B.comment("DEFAULT: base * multiplier squared. LINEAR: base * multiplier. XP is per level; TIME is per accelerated tick.").translation("config.timelesss.consumption.curve").defineEnum("consumption.curve", Curve.DEFAULT);
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
    public static final ModConfigSpec.EnumValue<ResourceType> PERSONAL_RESOURCE = B.comment("Personal item consumption. NONE preserves free use.").worldRestart().translation("config.timelesss.personal.resource").defineEnum("personal.resource", ResourceType.NONE, ResourceType.NONE, ResourceType.FLUID, ResourceType.DURABILITY, ResourceType.XP, ResourceType.ENERGY, ResourceType.TIME);
    public static final ModConfigSpec.IntValue DURABILITY_LIMIT = integer("personal.durabilityLimit", 2048, 1, "Total uses of durability; depleted item breaks.");
    public static final ModConfigSpec.IntValue DURABILITY_COST = integer("personal.durabilityBase", 1, 0, "Durability per activation at 1x, using the consumption curve.");
    public static final ModConfigSpec.IntValue PERSONAL_TANK = integer("personal.fluidLimit", 16000, 1, "Personal fluid capacity in mB; uses network.fluid.");
    public static final ModConfigSpec.IntValue PERSONAL_ENERGY = integer("personal.energyLimit", 100000, 1, "Personal energy capacity in FE.");
    public static final ModConfigSpec.IntValue XP_COST = integer("personal.xpPerLevel", 2, 0, "Raw XP points per multiplier level per activation; default 2.");
    public static final ModConfigSpec.IntValue TIME_COST = integer("personal.timePerTick", 1, 0, "Stored ticks consumed per accelerated tick; default 1.");
    public static final ModConfigSpec.IntValue TIME_CHARGE = integer("personal.timeChargePerTick", 1, 0, "Stored ticks gained each server tick in inventory or Curios. No offline charging.");
    public static final ModConfigSpec.LongValue TIME_LIMIT = B.comment("Stored tick limit, 0 = unlimited.").worldRestart().translation("config.timelesss.personal.timeLimit").defineInRange("personal.timeLimit", 0L, 0L, Long.MAX_VALUE);
    public static final ModConfigSpec SPECIFICATION = B.build();

    private ResourceConfig() {}
    private static ModConfigSpec.Builder groups(ModConfigSpec.Builder builder) {
        for(String group:List.of("limits","network","consumption","storage","personal")) builder.comment("Timelesss: "+group).translation("config.timelesss."+group).push(group).pop();
        return builder;
    }
    private static ModConfigSpec.IntValue integer(String key, int value, int min, String comment) {
        return B.comment(comment).translation("config.timelesss."+key).worldRestart().defineInRange(key, value, min, key.endsWith("Slots") ? 32768 : Integer.MAX_VALUE);
    }
    public static <T> T value(ModConfigSpec.ConfigValue<T> v) { return SPECIFICATION.isLoaded() ? v.get() : v.getDefault(); }
    public static int personalMax() { return value(PERSONAL_MAX); }
    public static int distributorMax() { return value(DISTRIBUTOR_MAX); }
    public static List<ResourceType> networkResources() { return FuelRules.types(value(NETWORK_RESOURCE)); }
    public static ResourceType networkResource() { return networkResources().get(0); }
    public static boolean accepts(ResourceType type) { return networkResources().contains(type); }
    public static ResourceType personalResource() { return value(PERSONAL_RESOURCE); }
    private record MaterialCache(Object raw,List<FuelRules.Material> entries) {}
    private static final java.util.Map<ResourceType,MaterialCache> MATERIAL_CACHE=new java.util.EnumMap<>(ResourceType.class);
    public static synchronized List<FuelRules.Material> materials(ResourceType type) {
        Object raw=switch(type) { case FLUID -> value(FLUID);case GAS -> value(GAS);case ITEM -> value(ITEM);default -> null; };
        if(raw==null) return List.of(new FuelRules.Material("",BigDecimal.ONE));
        MaterialCache cached=MATERIAL_CACHE.get(type);
        if(cached!=null && java.util.Objects.equals(raw,cached.raw())) return cached.entries();
        var parsed=FuelRules.materials(raw,switch(type) { case FLUID -> "minecraft:lava";case GAS -> "mekanism:hydrogen";default -> "minecraft:coal"; });
        MATERIAL_CACHE.put(type,new MaterialCache(raw,parsed));return parsed;
    }
    public static String id(ResourceType type) { return materials(type).get(0).id(); }
    public static boolean accepts(ResourceType type,String id) { return materials(type).stream().anyMatch(m -> m.id().equals(id)); }
    public static synchronized void normalize() {
        MATERIAL_CACHE.clear();
        if(!SPECIFICATION.isLoaded()) return;
        normalize(NETWORK_RESOURCE,networkResources().stream().map(Enum::name).toList());
        normalize(FLUID,materials(ResourceType.FLUID).stream().map(FuelRules.Material::editable).toList());
        normalize(GAS,materials(ResourceType.GAS).stream().map(FuelRules.Material::editable).toList());
        normalize(ITEM,materials(ResourceType.ITEM).stream().map(FuelRules.Material::editable).toList());
    }
    private static void normalize(ModConfigSpec.ConfigValue<Object> option,List<String> values) {
        if(!(value(option) instanceof java.util.ArrayList<?>) || !values.equals(value(option))) { option.set(new java.util.ArrayList<>(values));option.clearCache(); }
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
    public static String unit() { return unit(networkResource()); }
    public static String unit(ResourceType type) {
        return switch (type) { case ENERGY -> "FE"; case FLUID, GAS -> "mB"; case ITEM -> "items"; default -> ""; };
    }
}
