package fr.raconteur.simpleskinswapper.gui;

import fr.raconteur.simpleskinswapper.SimpleSkinSwapper;
import fr.raconteur.simpleskinswapper.SimpleSkinSwapperClient;
import fr.raconteur.simpleskinswapper.changeskin.SkinChange;
import fr.raconteur.simpleskinswapper.changeskin.SkinSwapperState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class SkinWheelScreen extends Screen {

    private final Screen parent;
    private final List<SkinEntry> entries;
    private int selectedIndex = -1;

    private static final int MAX_ENTRIES = 10;
    private static final float OUTER_RADIUS = 90.0f;
    private static final float GAP_HALF_ANGLE = (float) Math.toRadians(2.0);

    private static final int COLOR_SECTOR       = 0xCC1A2535;
    private static final int COLOR_SECTOR_HOVER = 0xEE2B5F9E;
    private static final int COLOR_CENTER_BG    = 0xBB0D1627;
    private static final int COLOR_TEXT         = 0xFFFFFFFF;

    public SkinWheelScreen(Screen parent) {
        super(Text.empty());
        this.parent = parent;
        this.entries = loadWheelEntries();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void applyBlur(DrawContext context) {
        // No blur — wheel is a transparent overlay
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // No background — wheel is a transparent overlay
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float cx = this.width / 2.0f;
        float cy = this.height / 2.0f;

        selectedIndex = getSelectedIndex(mouseX, mouseY, cx, cy);

        int n = entries.size();
        if (n == 0) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("simpleskinswapper.screen.carousel.no_skins"),
                    (int) cx, (int) cy, COLOR_TEXT);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        // Draw pie sector backgrounds
        for (int i = 0; i < n; i++) {
            drawSector(context, cx, cy, i, n, i == selectedIndex);
        }

        // Center fill circle (on top of sectors)
        fillCircle(context, cx, cy, 28, COLOR_CENTER_BG);

        // Skin previews — painter's order: top (smallest py) first
        double sectorSize2 = 2 * Math.PI / n;
        double angleOffset2 = -Math.PI / 2 - sectorSize2 / 2.0;
        double previewDist2 = OUTER_RADIUS * 0.60;
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> {
            double pyA = cy + previewDist2 * Math.sin(angleOffset2 + sectorSize2 * a + sectorSize2 / 2.0);
            double pyB = cy + previewDist2 * Math.sin(angleOffset2 + sectorSize2 * b + sectorSize2 / 2.0);
            return Double.compare(pyA, pyB);
        });
        for (int i : order) {
            drawSectorPreview(context, cx, cy, i, n);
        }

        // Selected skin name in the center
        if (selectedIndex >= 0) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.of(entries.get(selectedIndex).displayName),
                    (int) cx, (int) cy - textRenderer.fontHeight / 2, COLOR_TEXT);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    // -------------------------------------------------------------------------
    // Sector geometry
    // -------------------------------------------------------------------------

    private int getSelectedIndex(int mouseX, int mouseY, float cx, float cy) {
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 10 || dist > OUTER_RADIUS * 1.1) return -1;

        int n = entries.size();
        double sectorSize = 2 * Math.PI / n;
        double angleOffset = -Math.PI / 2 - sectorSize / 2.0;

        double angle = Math.atan2(dy, dx);
        double adjusted = ((angle - angleOffset) % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);
        int idx = (int) (adjusted / sectorSize);
        return (idx >= 0 && idx < n) ? idx : -1;
    }

    private void drawSector(DrawContext context, float cx, float cy, int index, int n, boolean hovered) {
        double sectorSize = 2 * Math.PI / n;
        double angleOffset = -Math.PI / 2 - sectorSize / 2.0;
        double baseAngle = angleOffset + sectorSize * index;
        double startAngle = baseAngle + GAP_HALF_ANGLE;
        double endAngle   = baseAngle + sectorSize - GAP_HALF_ANGLE;
        int color = hovered ? COLOR_SECTOR_HOVER : COLOR_SECTOR;
        fillSector(context, cx, cy, OUTER_RADIUS, startAngle, endAngle, color);
    }

    private void fillCircle(DrawContext context, float cx, float cy, float radius, int color) {
        fillSector(context, cx, cy, radius, 0, 2 * Math.PI, color);
    }

    /**
     * Fills a pie sector using context.fill() — one call per pixel column.
     * Scans each column within the bounding circle and fills the y-range
     * that falls within [startAngle, endAngle].
     */
    private void fillSector(DrawContext context, float cx, float cy, float radius,
                            double startAngle, double endAngle, int color) {
        int r = (int) Math.ceil(radius);
        int icx = (int) cx;
        int icy = (int) cy;

        for (int dx = -r; dx <= r; dx++) {
            int maxAbsDy = (int) Math.sqrt(Math.max(0.0, radius * radius - (double) dx * dx));
            int segYMin = Integer.MAX_VALUE;
            int segYMax = Integer.MIN_VALUE;

            for (int dy = -maxAbsDy; dy <= maxAbsDy; dy++) {
                if (dx == 0 && dy == 0) continue;
                double angle = Math.atan2(dy, dx);
                if (angleInSector(angle, startAngle, endAngle)) {
                    if (dy < segYMin) segYMin = dy;
                    if (dy > segYMax) segYMax = dy;
                }
            }

            if (segYMin <= segYMax) {
                context.fill(icx + dx, icy + segYMin, icx + dx + 1, icy + segYMax + 1, color);
            }
        }
    }

    /** Returns true if angle (atan2 range) falls within [startAngle, endAngle]. */
    private static boolean angleInSector(double angle, double startAngle, double endAngle) {
        double norm  = ((angle - startAngle) % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);
        double range = ((endAngle - startAngle) % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);
        return norm <= range;
    }

    private void drawSectorPreview(DrawContext context, float cx, float cy, int index, int n) {
        double sectorSize = 2 * Math.PI / n;
        double angleOffset = -Math.PI / 2 - sectorSize / 2.0;
        double midAngle = angleOffset + sectorSize * index + sectorSize / 2.0;
        double previewDist = OUTER_RADIUS * 0.60;

        int px = (int) (cx + previewDist * Math.cos(midAngle));
        int py = (int) (cy + previewDist * Math.sin(midAngle));

        SkinEntry entry = entries.get(index);
        entry.ensureTextureLoaded();

        int halfW = 16;
        int halfH = 24;

        if (entry.textureId != null) {
            SkinTextures skinTextures = new SkinTextures(
                    entry.textureId, null, null, null,
                    entry.skinType == SkinType.SLIM ? SkinTextures.Model.SLIM : SkinTextures.Model.WIDE,
                    true
            );
            SkinRenderer.renderPlayer(context, px - halfW, py - halfH, px + halfW, py + halfH, halfH, skinTextures);
        }
    }

    // -------------------------------------------------------------------------
    // Input handling
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { applyAndClose(); return true; }
        if (button == 1) { close(); return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (SimpleSkinSwapperClient.openWheelKey.matchesKey(keyCode, scanCode)) {
            applyAndClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void applyAndClose() {
        if (selectedIndex >= 0 && selectedIndex < entries.size()) {
            SkinEntry entry = entries.get(selectedIndex);
            if (SkinSwapperState.beginSwap()) {
                SkinChange.changeSkin(
                        entry.file,
                        entry.skinType,
                        () -> {
                            if (client.player != null)
                                client.player.sendMessage(
                                        Text.translatable("simpleskinswapper.message.success"), true);
                        },
                        err -> {
                            if (client.player != null)
                                client.player.sendMessage(
                                        Text.translatable("simpleskinswapper.message.error", err), true);
                        }
                );
                if (client.player != null)
                    client.player.sendMessage(
                            Text.translatable("simpleskinswapper.message.applying"), true);
            }
        }
        close();
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    // -------------------------------------------------------------------------
    // Data loading — reads order.txt, no writes
    // -------------------------------------------------------------------------

    private static List<SkinEntry> loadWheelEntries() {
        List<SkinEntry> all = SkinEntry.loadSkins();
        if (all.isEmpty()) return Collections.emptyList();

        Path orderFile = FabricLoader.getInstance().getGameDir().resolve("skins").resolve("order.txt");
        if (!Files.exists(orderFile)) {
            return new ArrayList<>(all.subList(0, Math.min(MAX_ENTRIES, all.size())));
        }
        try {
            String content = Files.readString(orderFile).trim();
            if (content.isEmpty()) {
                return new ArrayList<>(all.subList(0, Math.min(MAX_ENTRIES, all.size())));
            }
            List<String> names = Arrays.stream(content.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            Map<String, SkinEntry> byName = new LinkedHashMap<>();
            all.forEach(e -> byName.put(e.file.getName(), e));
            return names.stream().map(byName::get).filter(Objects::nonNull)
                    .limit(MAX_ENTRIES).collect(Collectors.toList());
        } catch (IOException e) {
            SimpleSkinSwapper.LOGGER.warn("Could not read skin order for wheel: {}", e.getMessage());
            return new ArrayList<>(all.subList(0, Math.min(MAX_ENTRIES, all.size())));
        }
    }
}
