package net.gasharaujo.timelesss.resource;

import java.math.BigDecimal;
import java.util.*;
import com.electronwill.nightconfig.core.UnmodifiableConfig;

/** Accepts legacy scalar values and normalizes them to Configured-friendly string lists. */
public final class FuelRules {
    public record Material(String id, BigDecimal modifier) {
        public String editable() { return modifier.compareTo(BigDecimal.ONE)==0 ? id : id + " = " + modifier.stripTrailingZeros().toPlainString(); }
    }
    private FuelRules() {}
    public static List<ResourceType> types(Object raw) {
        List<?> values=raw instanceof List<?> list ? list : Arrays.asList(raw.toString().split(","));
        LinkedHashSet<ResourceType> result=new LinkedHashSet<>();
        for(Object value:values) {
            ResourceType type=value instanceof ResourceType t ? t : ResourceType.valueOf(value.toString().trim().toUpperCase(Locale.ROOT));
            if(!EnumSet.of(ResourceType.ENERGY,ResourceType.FLUID,ResourceType.GAS,ResourceType.ITEM).contains(type)) throw new IllegalArgumentException("Invalid network resource");
            result.add(type);
        }
        if(result.isEmpty()) throw new IllegalArgumentException("Select at least one network resource");
        return List.copyOf(result);
    }
    public static boolean validTypes(Object raw) { try { types(raw);return true; } catch(RuntimeException e) { return false; } }
    public static boolean validMaterials(Object raw) { try { materials(raw,"minecraft:lava");return true; } catch(RuntimeException e) { return false; } }
    public static List<Material> materials(Object raw,String fallback) {
        LinkedHashMap<String,Material> result=new LinkedHashMap<>();
        parse(raw,result);
        if(result.isEmpty()) add(result,fallback,1);
        return List.copyOf(result.values());
    }
    private static void parse(Object raw,LinkedHashMap<String,Material> out) {
        if(raw instanceof String text) {
            if(text.isBlank()) return;
            for(String token:text.split(",")) {
                String[] pair=token.trim().split("\\s*[=@|]\\s*",-1);
                if(pair.length>2) throw new IllegalArgumentException("Use id = modifier");
                add(out,pair[0],pair.length==1 ? 1 : pair[1]);
            }
        } else if(raw instanceof List<?> list) {
            for(int i=0;i<list.size();i++) {
                Object value=list.get(i);
                if(value instanceof String id && i+1<list.size() && list.get(i+1) instanceof Number modifier) { add(out,id,modifier);i++; }
                else parse(value,out);
            }
        } else if(raw instanceof UnmodifiableConfig config) parse(config.valueMap(),out);
        else if(raw instanceof Map<?,?> map) {
            if(map.containsKey("id")) add(out,Objects.toString(map.get("id")),map.containsKey("modifier") ? map.get("modifier") : 1);
            else for(var entry:map.entrySet()) add(out,Objects.toString(entry.getKey()),entry.getValue());
        } else throw new IllegalArgumentException("Use an ID or a list of ID/modifier entries");
    }
    private static void add(LinkedHashMap<String,Material> out,String id,Object modifier) {
        id=id.trim();
        if(!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Use namespace:material");
        BigDecimal factor=new BigDecimal(modifier.toString()).stripTrailingZeros();
        if(factor.signum()<=0 || factor.scale()>12 || factor.precision()>24) throw new IllegalArgumentException("Use a positive finite modifier with at most 12 decimal places");
        if(out.putIfAbsent(id,new Material(id,factor))!=null) throw new IllegalArgumentException("Duplicate material: "+id);
    }
}
