package spz.meowing.gui;

import java.util.HashMap;
import java.util.Map;

public final class HudPositions {

    private static final Map<String, int[]> positions = new HashMap<>();
    private static final Map<String, Float> scales = new HashMap<>();

    private HudPositions() {}

    public static int[] get(String name) {
        return positions.get(name);
    }

    public static int getX(String name, int defaultX) {
        int[] pos = positions.get(name);
        return pos != null ? pos[0] : defaultX;
    }

    public static int getY(String name, int defaultY) {
        int[] pos = positions.get(name);
        return pos != null ? pos[1] : defaultY;
    }

    public static float getScale(String name) {
        return scales.getOrDefault(name, 1.0f);
    }

    public static void set(String name, int x, int y) {
        positions.put(name, new int[]{x, y});
    }

    public static void setScale(String name, float scale) {
        scales.put(name, Math.max(0.5f, Math.min(3.0f, scale)));
    }

    public static Map<String, int[]> getAll() {
        return positions;
    }

    public static Map<String, Float> getAllScales() {
        return scales;
    }
}
