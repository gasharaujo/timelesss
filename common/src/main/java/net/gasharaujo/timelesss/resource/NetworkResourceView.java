package net.gasharaujo.timelesss.resource;

import com.google.gson.*;
import java.math.BigDecimal;
import java.util.*;
import net.gasharaujo.timelesss.ResourceConfig;

public record NetworkResourceView(String label,String unit,long stored,long capacity,long base,BigDecimal modifier) {
    public static String snapshot(List<ResourceStorage> stores) {
        JsonArray entries=new JsonArray();
        for(ResourceType type:ResourceConfig.networkResources()) for(var material:ResourceConfig.materials(type)) {
            JsonObject entry=new JsonObject();
            entry.addProperty("label",material.id().isEmpty() ? type.name() : material.id());
            entry.addProperty("unit",ResourceConfig.unit(type));
            entry.addProperty("stored",stores.stream().mapToLong(s -> s.amount(type,material.id())).sum());
            entry.addProperty("capacity",stores.stream().mapToLong(s -> s.capacity(type)).sum());
            entry.addProperty("base",ResourceConfig.cost(type,1));
            entry.addProperty("modifier",material.modifier());entries.add(entry);
        }
        return entries.toString();
    }
    public static List<NetworkResourceView> parse(String json) {
        List<NetworkResourceView> result=new ArrayList<>();
        for(JsonElement value:JsonParser.parseString(json).getAsJsonArray()) {
            JsonObject e=value.getAsJsonObject();
            result.add(new NetworkResourceView(e.get("label").getAsString(),e.get("unit").getAsString(),e.get("stored").getAsLong(),e.get("capacity").getAsLong(),e.get("base").getAsLong(),e.get("modifier").getAsBigDecimal()));
        }
        return List.copyOf(result);
    }
    public String consumption(long unmodified) { return modifier.multiply(BigDecimal.valueOf(unmodified)).stripTrailingZeros().toPlainString(); }
}
