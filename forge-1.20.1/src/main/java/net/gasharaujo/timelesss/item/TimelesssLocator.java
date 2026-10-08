package net.gasharaujo.timelesss.item;

import java.lang.reflect.Method;
import java.util.Optional;
import net.gasharaujo.timelesss.TimelesssMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class TimelesssLocator {
    public static final int HAND = 0;
    public static final int INVENTORY = 1;
    public static final int CURIO = 2;

    private TimelesssLocator() {
    }

    public static Found inHand(InteractionHand hand, ItemStack stack) {
        return new Found(stack, HAND, hand == InteractionHand.MAIN_HAND ? 0 : 1);
    }

    public static Optional<Found> findPreferred(Player player) {
        ItemStack curio = findCurio(player);
        if (curio.is(TimelesssMod.TIMELESSS.get())) {
            return Optional.of(new Found(curio, CURIO, 0));
        }

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(TimelesssMod.TIMELESSS.get())) {
            return Optional.of(new Found(mainHand, HAND, 0));
        }

        ItemStack offHand = player.getOffhandItem();
        if (offHand.is(TimelesssMod.TIMELESSS.get())) {
            return Optional.of(new Found(offHand, HAND, 1));
        }

        int hotbarEnd = Math.min(9, player.getInventory().getContainerSize());
        for (int slot = 0; slot < hotbarEnd; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(TimelesssMod.TIMELESSS.get())) {
                return Optional.of(new Found(stack, INVENTORY, slot));
            }
        }

        for (int slot = hotbarEnd; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(TimelesssMod.TIMELESSS.get())) {
                return Optional.of(new Found(stack, INVENTORY, slot));
            }
        }
        return Optional.empty();
    }

    public static ItemStack resolve(Player player, int sourceType, int sourceIndex) {
        ItemStack stack = switch (sourceType) {
            case HAND -> player.getItemInHand(sourceIndex == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
            case INVENTORY -> sourceIndex >= 0 && sourceIndex < player.getInventory().getContainerSize()
                ? player.getInventory().getItem(sourceIndex)
                : ItemStack.EMPTY;
            case CURIO -> findCurio(player);
            default -> ItemStack.EMPTY;
        };
        return stack.is(TimelesssMod.TIMELESSS.get()) ? stack : ItemStack.EMPTY;
    }

    private static ItemStack findCurio(Player player) {
        try {
            Class<?> apiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getInventory = apiClass.getMethod("getCuriosInventory", LivingEntity.class);
            Object handler = unwrap(getInventory.invoke(null, player));
            if (handler == null) {
                return ItemStack.EMPTY;
            }

            Class<?> handlerClass = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
            Method findFirst = handlerClass.getMethod("findFirstCurio", Item.class);
            Object slotResult = unwrap(findFirst.invoke(handler, TimelesssMod.TIMELESSS.get()));
            if (slotResult == null) {
                return ItemStack.EMPTY;
            }

            for (String accessor : new String[]{"stack", "getStack"}) {
                try {
                    Object result = slotResult.getClass().getMethod(accessor).invoke(slotResult);
                    if (result instanceof ItemStack stack) {
                        return stack;
                    }
                } catch (NoSuchMethodException ignored) {
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return ItemStack.EMPTY;
    }

    private static Object unwrap(Object value) throws ReflectiveOperationException {
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        if (value == null) {
            return null;
        }
        try {
            Object resolved = value.getClass().getMethod("resolve").invoke(value);
            return resolved instanceof Optional<?> optional ? optional.orElse(null) : resolved;
        } catch (NoSuchMethodException ignored) {
            return value;
        }
    }

    public record Found(ItemStack stack, int sourceType, int sourceIndex) {
    }
}
