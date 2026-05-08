package spz.meowing.gui.tooltip;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import spz.meowing.module.ModuleManager;
import spz.meowing.module.impl.CustomTooltip;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkyblockTooltipRenderer {

    private static final int PAD = 3;
    private static final int BORDER = 2;

    private static final String[][] STAT_ICONS = {
        {"Gear Score", "⚙"}, {"Damage", "❁"}, {"Strength", "❁"},
        {"Crit Damage", "☠"}, {"Crit Chance", "☣"}, {"Attack Speed", "⚔"},
        {"Bonus Attack Speed", "⚔"}, {"Health", "❤"}, {"Health Regen", "❣"},
        {"Defense", "❈"}, {"Speed", "✦"}, {"Walk Speed", "✦"},
        {"Intelligence", "✎"}, {"Magic Find", "✯"}, {"Pet Luck", "♣"},
        {"True Defense", "❂"}, {"Ferocity", "⫽"}, {"Ability Damage", "☄"},
        {"Mining Speed", "⸕"}, {"Mining Fortune", "☘"}, {"Farming Fortune", "☘"},
        {"Foraging Fortune", "☘"}, {"Sea Creature Chance", "α"}, {"Fishing Speed", "☂"},
        {"Swing Range", "Ⓢ"}, {"Vitality", "♨"}, {"Mending", "☄"},
        {"Health Cost", "❤"}, {"Mana Cost", "✎"}, {"Soulflow Cost", "⸎"}, {"Cooldown", "⌚"},
    };

    // Matches stat lines like "Damage: +262" or "Gear Score  664"
    // The key fix: [: ] instead of just \s — handles both colon-separated and space-separated
    private static final Pattern STAT_PATTERN = Pattern.compile(
        "^(Gear Score|Damage|Strength|Crit Damage|Crit Chance|Health Regen|Health|Defense|" +
        "Walk Speed|Speed|Intelligence|True Defense|Magic Find|Pet Luck|Ferocity|" +
        "Attack Speed|Bonus Attack Speed|Ability Damage|Mining Speed|Mining Fortune|" +
        "Farming Fortune|Foraging Fortune|Sea Creature Chance|Fishing Speed|Swing Range|" +
        "Health Cost|Mana Cost|Soulflow Cost|Cooldown|Vitality|Mending)[: ]"
    );

    private static List<Text> rawText = new ArrayList<>();

    public static void setRawText(List<Text> text) {
        rawText = text;
    }

    public static void render(DrawContext context, TextRenderer tr,
                              List<TooltipComponent> components, int x, int y,
                              int maxWidth, int totalHeight) {
        var mod = ModuleManager.getInstance().getModule("CustomTooltip");
        if (!(mod instanceof CustomTooltip ct)) return;

        // Detect rarity scanning bottom-up
        int rarityColor = -1;
        int rarityLineIdx = -1;
        for (int i = rawText.size() - 1; i >= 0; i--) {
            int c = CustomTooltip.getRarityColor(rawText.get(i).getString());
            if (c != -1) { rarityColor = c; rarityLineIdx = i; break; }
        }

        int borderCol = rarityColor != -1 ? rarityColor : 0xFF5555FF;
        float bA = ct.borderOpacity();
        int border = alpha(borderCol, bA);
        int bg = alpha(0xFF0C0C18, ct.bgOpacity());

        int bx = x - PAD - BORDER, by = y - PAD - BORDER;
        int bw = maxWidth + PAD * 2 + BORDER * 2;
        int bh = totalHeight + PAD * 2 + BORDER * 2;
        int ix = bx + BORDER, iy = by + BORDER;
        int iw = bw - BORDER * 2, ih = bh - BORDER * 2;

        context.getMatrices().pushMatrix();

        // Border + background
        context.fill(RenderPipelines.GUI, bx, by, bx + bw, by + bh, border);
        context.fill(RenderPipelines.GUI, ix, iy, ix + iw, iy + ih, bg);
        context.fill(RenderPipelines.GUI, ix, iy, ix + iw, iy + Math.min(14, ih), alpha(border, bA * 0.08f));

        // Separator under title
        if (components.size() > 1) {
            int sy = y + components.get(0).getHeight(tr) + 1;
            context.fill(RenderPipelines.GUI, ix + 2, sy, ix + iw - 2, sy + 1, alpha(border, bA * 0.3f));
        }

        // Separator above rarity
        if (rarityLineIdx > 0 && rarityLineIdx < components.size()) {
            int sy = y;
            for (int i = 0; i < rarityLineIdx; i++)
                sy += components.get(i).getHeight(tr) + (i == 0 ? 2 : 0);
            context.fill(RenderPipelines.GUI, ix + 2, sy - 2, ix + iw - 2, sy - 1, alpha(border, bA * 0.3f));
        }

        // Track stat section: stats are only at the top (lines 1+), before any empty line
        Map<String, StatData> panelStats = new LinkedHashMap<>();
        boolean inStatSection = true;
        boolean seenAnyStat = false;

        // Render components
        int py = y;
        for (int i = 0; i < components.size(); i++) {
            TooltipComponent comp = components.get(i);
            String plain = i < rawText.size() ? rawText.get(i).getString() : "";

            // Detect end of stat section (empty line or first non-stat after seeing stats)
            if (i > 0 && inStatSection) {
                if (plain.trim().isEmpty()) {
                    inStatSection = false;
                } else if (seenAnyStat && !STAT_PATTERN.matcher(plain).find()) {
                    // Hit a non-stat line like gem slots [⛏] — end stat section
                    inStatSection = false;
                }
            }

            boolean isStat = i > 0 && ct.dottedStats() && !plain.isEmpty() && STAT_PATTERN.matcher(plain).find();

            if (isStat) {
                seenAnyStat = true;
                renderStatLine(context, tr, rawText.get(i), plain, py, x, maxWidth,
                        inStatSection ? panelStats : null);
            } else {
                comp.drawText(context, tr, x, py);
            }

            comp.drawItems(tr, x, py, maxWidth, totalHeight, context);
            py += comp.getHeight(tr) + (i == 0 ? 2 : 0);
        }

        context.getMatrices().popMatrix();

        // Stat panel (only top-section stats, not ability costs)
        if (ct.statPanel() && !panelStats.isEmpty()) {
            renderStatPanel(context, tr, bx + bw + 4, by, panelStats, border, bg);
        }
    }

    // ==================== STAT LINE ====================

    private static void renderStatLine(DrawContext context, TextRenderer tr,
                                       Text origText, String plain, int y, int lx, int maxW,
                                       Map<String, StatData> panelStats) {
        Matcher m = STAT_PATTERN.matcher(plain);
        if (!m.find()) {
            context.drawText(tr, origText.asOrderedText(), lx, y, -1, true);
            return;
        }

        String statName = m.group(1);

        // Find where the value starts (after "StatName: " or "StatName ")
        int valueStart = m.end();
        // If there's a colon, skip past it and any trailing space
        int colonIdx = plain.indexOf(':', statName.length());
        if (colonIdx >= 0 && colonIdx <= valueStart) {
            valueStart = colonIdx + 1;
            while (valueStart < plain.length() && plain.charAt(valueStart) == ' ') valueStart++;
        }

        // Look up icon
        String icon = "";
        int iconColor = 0xFFAAAAAA;
        for (String[] e : STAT_ICONS) {
            if (statName.equals(e[0])) { icon = e[1]; iconColor = iconColor(statName); break; }
        }

        // Collect styled value characters from the original formatted text
        List<StyledChar> valueChars = collectCharsAfter(origText, valueStart);
        int valueW = measureChars(tr, valueChars);
        String valuePlain = plain.substring(valueStart).trim();

        // Save to panel (only if panelStats is provided, i.e., we're in the stat section)
        if (panelStats != null) {
            panelStats.put(statName, new StatData(icon, iconColor, statName, valuePlain, valueChars));
        }

        int cx = lx;
        int iconW = 0;

        // Icon
        if (!icon.isEmpty()) {
            context.drawText(tr, icon, cx, y, iconColor, true);
            iconW = tr.getWidth(icon) + 2;
            cx += iconW;
        }

        // Stat name (gray)
        context.drawText(tr, statName, cx, y, 0xFF999999, true);
        int nameEnd = cx + tr.getWidth(statName);

        // Right-aligned value
        int valueX = lx + maxW - valueW;

        // If value overlaps name, don't use dots — just render with spacing
        if (valueX < nameEnd + 10) {
            // Render icon + original text (shifted for icon)
            if (!icon.isEmpty()) {
                context.drawText(tr, icon, lx, y, iconColor, true);
            }
            context.drawText(tr, origText.asOrderedText(), lx + iconW, y, -1, true);
            return;
        }

        // Dots between name and value
        int dotStart = nameEnd + 5;
        int dotEnd = valueX - 5;
        if (dotEnd > dotStart + 8) {
            for (int dx = dotStart; dx < dotEnd; dx += 5) {
                context.fill(RenderPipelines.GUI, dx, y + 4, dx + 2, y + 5, 0xFF252538);
            }
        }

        // Value with original colors
        renderStyledChars(context, tr, valueChars, valueX, y);
    }

    // ==================== STAT PANEL ====================

    private static void renderStatPanel(DrawContext context, TextRenderer tr,
                                        int px, int py,
                                        Map<String, StatData> stats,
                                        int borderCol, int bgCol) {
        int lineH = 11;
        int panelW = 0;
        for (var e : stats.values()) {
            int lineW = (e.icon.isEmpty() ? 0 : tr.getWidth(e.icon) + 2)
                    + tr.getWidth(e.name + ": " + e.valuePlain);
            panelW = Math.max(panelW, lineW);
        }
        panelW += 10;
        int panelH = stats.size() * lineH + 6;

        context.fill(RenderPipelines.GUI, px, py, px + panelW + 4, py + panelH + 4, borderCol);
        context.fill(RenderPipelines.GUI, px + BORDER, py + BORDER, px + panelW + 4 - BORDER, py + panelH + 4 - BORDER, bgCol);

        int ty = py + 4;
        int tx = px + 5;

        for (var e : stats.values()) {
            int cx = tx;
            if (!e.icon.isEmpty()) {
                context.drawText(tr, e.icon, cx, ty, e.iconColor, true);
                cx += tr.getWidth(e.icon) + 2;
            }
            context.drawText(tr, e.name, cx, ty, 0xFF888888, true);
            cx += tr.getWidth(e.name);
            context.drawText(tr, ": ", cx, ty, 0xFF555555, true);
            cx += tr.getWidth(": ");
            renderStyledChars(context, tr, e.valueChars, cx, ty);
            ty += lineH;
        }
    }

    // ==================== HELPERS ====================

    private static List<StyledChar> collectCharsAfter(Text text, int afterIdx) {
        List<StyledChar> chars = new ArrayList<>();
        final int[] idx = {0};

        text.asOrderedText().accept((index, style, codePoint) -> {
            if (idx[0] >= afterIdx) {
                int color = 0xFFFFFFFF;
                TextColor tc = style.getColor();
                if (tc != null) color = tc.getRgb() | 0xFF000000;
                chars.add(new StyledChar((char) codePoint, color));
            }
            idx[0]++;
            return true;
        });

        // Trim leading spaces
        while (!chars.isEmpty() && chars.get(0).ch == ' ') chars.remove(0);
        return chars;
    }

    private static int measureChars(TextRenderer tr, List<StyledChar> chars) {
        int w = 0;
        for (StyledChar sc : chars) w += tr.getWidth(String.valueOf(sc.ch));
        return w;
    }

    private static void renderStyledChars(DrawContext ctx, TextRenderer tr,
                                          List<StyledChar> chars, int x, int y) {
        int cx = x;
        for (StyledChar sc : chars) {
            String ch = String.valueOf(sc.ch);
            ctx.drawText(tr, ch, cx, y, sc.color, true);
            cx += tr.getWidth(ch);
        }
    }

    private record StyledChar(char ch, int color) {}
    private record StatData(String icon, int iconColor, String name, String valuePlain, List<StyledChar> valueChars) {}

    private static int iconColor(String stat) {
        return switch (stat) {
            case "Damage", "Strength", "Health", "Ferocity", "Health Cost", "Health Regen" -> 0xFFFF5555;
            case "Crit Damage", "Crit Chance" -> 0xFF5555FF;
            case "Defense" -> 0xFF55FF55;
            case "Speed", "Walk Speed" -> 0xFFFFFFFF;
            case "Intelligence", "Magic Find", "Mana Cost" -> 0xFF55FFFF;
            case "Mining Speed", "Mining Fortune" -> 0xFFFFAA00;
            case "Farming Fortune", "Foraging Fortune" -> 0xFF55FF55;
            case "Swing Range" -> 0xFFFFFF55;
            case "Gear Score" -> 0xFFAAAAAA;
            case "Cooldown" -> 0xFF55FF55;
            case "Soulflow Cost" -> 0xFFAA00AA;
            default -> 0xFFAAAAAA;
        };
    }

    private static int alpha(int color, float a) {
        int av = Math.max(0, Math.min(255, (int) (((color >> 24) & 0xFF) * a)));
        return (av << 24) | (color & 0x00FFFFFF);
    }
}
