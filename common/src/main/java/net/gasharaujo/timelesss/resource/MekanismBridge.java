package net.gasharaujo.timelesss.resource;

import java.lang.reflect.*;
import net.gasharaujo.timelesss.ResourceConfig;

/** No Mekanism types are linked when the optional mod is absent. */
public final class MekanismBridge {
    private static final System.Logger LOG = System.getLogger("timelesss");
    private static boolean warned;
    private MekanismBridge() {}

    public static Object blockCapability() { return capability(false); }
    public static Object itemCapability() { return capability(true); }
    private static Object capability(boolean item) {
        try {
            Class<?> caps = Class.forName("mekanism.common.capabilities.Capabilities");
            try { return caps.getField("GAS_HANDLER").get(null); }
            catch (NoSuchFieldException ignored) {
                Object multi = caps.getField("CHEMICAL").get(null);
                return multi.getClass().getMethod(item ? "item" : "block").invoke(multi);
            }
        } catch (ReflectiveOperationException | LinkageError e) { return null; }
    }

    public static Object handler(ResourceStorage storage) {
        if (storage.type() != ResourceType.GAS) return null;
        try {
            Class<?> handler;
            Class<?> stack;
            try {
                handler = Class.forName("mekanism.api.chemical.gas.IGasHandler");
                stack = Class.forName("mekanism.api.chemical.gas.GasStack");
            } catch (ClassNotFoundException e) {
                handler = Class.forName("mekanism.api.chemical.IChemicalHandler");
                stack = Class.forName("mekanism.api.chemical.ChemicalStack");
            }
            Class<?> stackClass = stack;
            return Proxy.newProxyInstance(handler.getClassLoader(), new Class<?>[]{handler}, (proxy, method, args) -> {
                String name = method.getName();
                if (method.getDeclaringClass() == Object.class) return switch (name) {
                    case "toString" -> "Timelesss chemical storage";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
                if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                if (name.equals("getTanks") || name.equals("getChemicalTanks")) return 1;
                if (name.equals("getEmptyStack")) return stackClass.getField("EMPTY").get(null);
                if (args != null && args.length > 0 && args[0] instanceof Integer index) java.util.Objects.checkIndex(index, 1);
                if (name.equals("getTankCapacity") || name.equals("getChemicalTankCapacity")) return storage.capacity(ResourceType.GAS);
                if (name.equals("getChemicalInTank")) return stack(stackClass, storage.amount(ResourceType.GAS));
                if (name.equals("isValid")) return valid(args[1]);
                if (name.equals("setChemicalInTank")) {
                    if (valid(args[1])) storage.set(ResourceType.GAS, amount(args[1]));
                    else if ((boolean)args[1].getClass().getMethod("isEmpty").invoke(args[1])) storage.set(ResourceType.GAS, 0);
                    return null;
                }
                boolean simulate = args[args.length - 1].toString().equals("SIMULATE");
                if (name.equals("insertChemical")) {
                    Object offered = args[1];
                    if (!valid(offered)) return offered;
                    long moved = storage.insert(ResourceType.GAS, amount(offered), simulate);
                    Object remainder = offered.getClass().getMethod("copy").invoke(offered);
                    remainder.getClass().getMethod("setAmount", long.class).invoke(remainder, amount(offered) - moved);
                    return remainder;
                }
                if (name.equals("extractChemical")) return stack(stackClass, storage.extract(ResourceType.GAS, ((Number)args[1]).longValue(), simulate));
                throw new UnsupportedOperationException(method.toString());
            });
        } catch (ReflectiveOperationException | LinkageError e) {
            if (!warned) { warned = true; LOG.log(System.Logger.Level.WARNING, "GAS selected, but no compatible optional Mekanism chemical API is installed."); }
            return null;
        }
    }
    private static long amount(Object stack) throws ReflectiveOperationException {
        return ((Number)stack.getClass().getMethod("getAmount").invoke(stack)).longValue();
    }
    private static boolean valid(Object stack) throws ReflectiveOperationException {
        return !(boolean)stack.getClass().getMethod("isEmpty").invoke(stack)
            && ResourceConfig.id(ResourceType.GAS).equals(stack.getClass().getMethod("getTypeRegistryName").invoke(stack).toString());
    }
    private static Object stack(Class<?> stackClass, long amount) throws ReflectiveOperationException {
        if (amount <= 0) return stackClass.getField("EMPTY").get(null);
        Class<?> api = Class.forName("mekanism.api.MekanismAPI");
        Object registry;
        boolean legacy;
        try { registry = api.getMethod("gasRegistry").invoke(null); legacy = true; }
        catch (NoSuchMethodException e) { registry = api.getField("CHEMICAL_REGISTRY").get(null); legacy = false; }
        Object chemical = ResourceStorage.gasChemical(registry);
        if (chemical == null) return stackClass.getField("EMPTY").get(null);
        for (Constructor<?> constructor : stackClass.getConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length == 2 && types[0].isInstance(chemical) && types[1] == long.class) return constructor.newInstance(chemical, amount);
        }
        throw new NoSuchMethodException("Chemical stack constructor");
    }
}
