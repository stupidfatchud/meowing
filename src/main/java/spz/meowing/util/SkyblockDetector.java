package spz.meowing.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;

/**
 * Detects the player's current location context on Hypixel.
 * Tracks: Hypixel connection, Skyblock, Dungeons, floor, boss room, F7 phase.
 * Updated every tick from ModuleManager.
 */
public final class SkyblockDetector {

    private static boolean onHypixel = false;
    private static boolean inSkyblock = false;
    private static boolean inDungeon = false;
    private static boolean inBoss = false;
    private static String dungeonFloor = "";
    private static int dungeonFloorNumber = 0;
    private static int f7Phase = 0;
    private static String worldArea = "";

    private SkyblockDetector() {}

    /** Call once per tick from ModuleManager */
    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            reset();
            return;
        }

        // Detect Hypixel via scoreboard title or tab header
        onHypixel = false;
        try {
            Scoreboard sb = mc.world.getScoreboard();
            if (sb != null) {
                for (var obj : sb.getObjectives()) {
                    String name = obj.getDisplayName().getString().replaceAll("§.", "").toLowerCase();
                    if (name.contains("hypixel") || name.contains("skyblock")) {
                        onHypixel = true;
                        break;
                    }
                }
            }
            // Also check server address
            if (!onHypixel && mc.getCurrentServerEntry() != null) {
                String addr = mc.getCurrentServerEntry().address.toLowerCase();
                if (addr.contains("hypixel")) onHypixel = true;
            }
        } catch (Exception ignored) {}
        if (!onHypixel) {
            reset();
            return;
        }

        // Scan scoreboard for Skyblock/Dungeon info
        Scoreboard scoreboard = mc.world.getScoreboard();
        if (scoreboard == null) return;

        boolean foundSkyblock = false;
        boolean foundDungeon = false;

        for (Team team : scoreboard.getTeams()) {
            try {
                String prefix = team.getPrefix().getString().replaceAll("§.", "");
                String suffix = team.getSuffix().getString().replaceAll("§.", "");
                String line = (prefix + suffix).trim();

                // Skyblock detection
                if (line.contains("SKYBLOCK") || line.contains("skyblock")) {
                    foundSkyblock = true;
                }

                // Dungeon detection
                if (line.contains("The Catacombs (") && !line.contains("Queue")) {
                    foundDungeon = true;
                    // Extract floor: "The Catacombs (F7)" -> "F7"
                    int start = line.indexOf("(");
                    int end = line.indexOf(")");
                    if (start >= 0 && end > start) {
                        dungeonFloor = line.substring(start + 1, end).trim();
                        char lastChar = dungeonFloor.charAt(dungeonFloor.length() - 1);
                        dungeonFloorNumber = Character.isDigit(lastChar) ? Character.getNumericValue(lastChar) : 0;
                    }
                }

                // Area detection from tab
                if (line.startsWith("Area: ") || line.startsWith("Dungeon: ")) {
                    worldArea = line.replace("Area: ", "").replace("Dungeon: ", "").trim();
                }
            } catch (Exception ignored) {}
        }

        // Also check tab list for skyblock
        if (!foundSkyblock && mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                if (entry.getDisplayName() != null) {
                    String text = entry.getDisplayName().getString().replaceAll("§.", "");
                    if (text.contains("SKYBLOCK") || text.contains("Skyblock")) {
                        foundSkyblock = true;
                        break;
                    }
                }
            }
        }

        inSkyblock = foundSkyblock;
        inDungeon = foundDungeon;

        // Boss room detection
        if (inDungeon && mc.player != null) {
            inBoss = checkBossRoom(mc);
            f7Phase = inBoss && dungeonFloorNumber == 7 ? getF7Phase(mc) : 0;
        }
    }

    /** Reset on world change */
    public static void onWorldChange() {
        reset();
    }

    private static void reset() {
        inSkyblock = false;
        inDungeon = false;
        inBoss = false;
        dungeonFloor = "";
        dungeonFloorNumber = 0;
        f7Phase = 0;
        worldArea = "";
    }

    // Boss room bounds per floor (approximate)
    private static boolean checkBossRoom(MinecraftClient mc) {
        if (mc.player == null || dungeonFloorNumber < 1 || dungeonFloorNumber > 7) return false;
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();

        return switch (dungeonFloorNumber) {
            case 1 -> x > -72 && x < -14 && y > 55 && y < 146 && z > -40 && z < 49;
            case 2 -> x > -40 && x < 24 && y > 54 && y < 99 && z > -40 && z < 59;
            case 3 -> x > -40 && x < 42 && y > 64 && y < 118 && z > -40 && z < 37;
            case 4 -> x > -40 && x < 50 && y > 53 && y < 112 && z > -40 && z < 47;
            case 5 -> x > -40 && x < 50 && y > 53 && y < 112 && z > -8 && z < 118;
            case 6 -> x > -40 && x < 22 && y > 51 && y < 110 && z > -8 && z < 134;
            case 7 -> x > -8 && x < 134 && y > 0 && y < 254 && z > -8 && z < 147;
            default -> false;
        };
    }

    private static int getF7Phase(MinecraftClient mc) {
        if (mc.player == null) return 0;
        double y = mc.player.getY();
        if (y > 210) return 1;
        if (y > 155) return 2;
        if (y > 100) return 3;
        if (y > 45) return 4;
        return 5;
    }

    // Public getters
    public static boolean isOnHypixel() { return onHypixel; }
    public static boolean isInSkyblock() { return inSkyblock; }
    public static boolean isInDungeon() { return inDungeon; }
    public static boolean isInBoss() { return inBoss; }
    public static boolean isMasterMode() { return dungeonFloor.startsWith("M"); }
    public static String getDungeonFloor() { return dungeonFloor; }
    public static int getDungeonFloorNumber() { return dungeonFloorNumber; }
    public static int getF7Phase() { return f7Phase; }
    public static String getWorldArea() { return worldArea; }
}
