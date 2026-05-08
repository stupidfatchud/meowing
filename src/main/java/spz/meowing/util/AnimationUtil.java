package spz.meowing.util;

public final class AnimationUtil {

    private AnimationUtil() {}

    public static float lerp(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001f) return target;
        return current + diff * speed;
    }

    public static double lerp(double current, double target, double speed) {
        double diff = target - current;
        if (Math.abs(diff) < 0.001) return target;
        return current + diff * speed;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float easeOutQuad(float t) {
        return 1 - (1 - t) * (1 - t);
    }

    public static float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    public static float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }
}
