package net.gasharaujo.timelesss.resource;

public final class ResourceMath {
    private ResourceMath() {}

    public static long multiply(long a, long b) {
        if (a <= 0 || b <= 0) return 0;
        return a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b;
    }

    public static long add(long a, long b) {
        return Long.MAX_VALUE - a < b ? Long.MAX_VALUE : a + b;
    }

    public static long cost(long base, int level, boolean linear) {
        return multiply(multiply(base, level), linear ? 1 : level);
    }

    public static int capacity(long base, long perBattery, int extraBatteries) {
        return (int) Math.min(Integer.MAX_VALUE, add(base, multiply(perBattery, extraBatteries)));
    }
}
