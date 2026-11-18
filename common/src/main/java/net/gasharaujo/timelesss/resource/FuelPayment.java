package net.gasharaujo.timelesss.resource;

import java.math.*;
import java.util.*;
import java.util.function.*;
import net.gasharaujo.timelesss.ResourceConfig;

/** Plans one complete alternative before mutating storage. Fractions become prepaid fuel credit. */
public final class FuelPayment {
    private FuelPayment() {}
    public record Debit(ResourceStorage store,ResourceType type,String id,long amount) {}
    public record Plan(List<Debit> debits,String creditKey,String credit) {
        public boolean apply(BiConsumer<String,String> saveCredit) {
            var snapshots=debits.stream().map(d -> d.store.snapshot()).toList();
            for(int i=0;i<debits.size();i++) {
                Debit d=debits.get(i);
                if(d.store.extract(d.type,d.id,d.amount,false)!=d.amount) {
                    for(int j=0;j<=i;j++) debits.get(j).store.restore(snapshots.get(j));
                    return false;
                }
            }
            saveCredit.accept(creditKey,credit);
            return true;
        }
    }
    public static Plan plan(List<ResourceStorage> stores,List<ResourceType> types,ToLongFunction<ResourceType> baseCost,Function<String,String> readCredit) {
        for(ResourceType type:types) for(FuelRules.Material material:ResourceConfig.materials(type)) {
            String creditKey="fuel_credit:"+type+":"+material.id();
            BigDecimal credit;
            try { credit=new BigDecimal(readCredit.apply(creditKey)); }
            catch(RuntimeException ignored) { credit=BigDecimal.ZERO; }
            if(credit.signum()<0 || credit.compareTo(BigDecimal.ONE)>=0) credit=BigDecimal.ZERO;
            BigDecimal exact=BigDecimal.valueOf(baseCost.applyAsLong(type)).multiply(material.modifier()).subtract(credit);
            BigDecimal charge=exact.max(BigDecimal.ZERO).setScale(0,RoundingMode.CEILING);
            if(charge.compareTo(BigDecimal.valueOf(Long.MAX_VALUE))>0) continue;
            long remaining=charge.longValueExact();
            List<Debit> debits=new ArrayList<>();
            for(ResourceStorage store:stores) {
                long amount=Math.min(remaining,store.amount(type,material.id()));
                if(amount>0) { debits.add(new Debit(store,type,material.id(),amount));remaining-=amount; }
                if(remaining==0) break;
            }
            if(remaining==0) return new Plan(List.copyOf(debits),creditKey,charge.subtract(exact).stripTrailingZeros().toPlainString());
        }
        return null;
    }
}
