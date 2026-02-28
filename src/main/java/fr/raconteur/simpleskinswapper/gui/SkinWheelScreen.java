package fr.raconteur.simpleskinswapper.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import fr.raconteur.simpleskinswapper.SimpleSkinSwapper;
import fr.raconteur.simpleskinswapper.SimpleSkinSwapperClient;
import fr.raconteur.simpleskinswapper.changeskin.SkinChange;
import fr.raconteur.simpleskinswapper.changeskin.SkinSwapperState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import org.joml.Matrix3x2f;

import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class SkinWheelScreen extends Screen {

    private final Screen parent;
    private final List<SkinEntry> entries;
    private int selectedIndex = -1;

    private static final int MAX_ENTRIES = 5;
    /** Number of arc subdivisions for the outer curve of each sector. */
    private static final int NUM_ARC_DIVISIONS = 12;
    private static final float OUTER_RADIUS = 90.0f;
    private static final float INNER_RADIUS = 0.0f; // triangular slices from center
    /** Half-angle gap between adjacent sectors (radians). */
    private static final float GAP_HALF_ANGLE = (float) Math.toRadians(2.0);

    private static final int COLOR_SECTOR       = 0xCC1A2535;
    private static final int COLOR_SECTOR_HOVER = 0xEE2B5F9E;
    private static final int COLOR_CENTER_BG    = 0xBB0D1627;
    private static final int COLOR_TEXT         = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIM     = 0xFFAAAAAA;

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
                    (int) cx, (int) cy, COLOR_TEXT_DIM);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        // Capture current 2D pose
        Matrix3x2f pose = new Matrix3x2f(context.getMatrices());

        // Draw pie sectors
        for (int i = 0; i < n; i++) {
            drawSector(context, pose, cx, cy, i, n, i == selectedIndex);
        }

        // Center fill circle (drawn last = on top)
        drawCircle(context, pose, cx, cy, 28);

        // Skin previews at sector midpoints
        for (int i = 0; i < n; i++) {
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

    private void drawSector(DrawContext context, Matrix3x2f pose,
                            float cx, float cy, int index, int n, boolean hovered) {
        double sectorSize = 2 * Math.PI / n;
        double angleOffset = -Math.PI / 2 - sectorSize / 2.0;
        double baseAngle = angleOffset + sectorSize * index;
        float startAngle = (float) (baseAngle + GAP_HALF_ANGLE);
        float endAngle   = (float) (baseAngle + sectorSize - GAP_HALF_ANGLE);
        int color = hovered ? COLOR_SECTOR_HOVER : COLOR_SECTOR;

        context.state.addSimpleElement(new PieSliceElement(
                RenderPipelines.GUI, TextureSetup.empty(), pose,
                cx, cy, INNER_RADIUS, OUTER_RADIUS,
                startAngle, endAngle, color, null
        ));
    }

    private void drawCircle(DrawContext context, Matrix3x2f pose, float cx, float cy, float radius) {
        context.state.addSimpleElement(new PieSliceElement(
                RenderPipelines.GUI, TextureSetup.empty(), pose,
                cx, cy, 0f, radius,
                0f, (float) (2 * Math.PI), COLOR_CENTER_BG, null
        ));
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
        int x1 = px - halfW;
        int y1 = py - halfH;
        int x2 = px + halfW;
        int y2 = py + halfH;

        if (entry.textureId != null) {
            SkinTextures skinTextures = new SkinTextures(
                    entry.textureId, null, null, null,
                    entry.skinType == SkinType.SLIM ? SkinTextures.Model.SLIM : SkinTextures.Model.WIDE,
                    true
            );
            SkinRenderer.renderPlayer(context, x1, y1, x2, y2, halfH, skinTextures);
        }
    }

    // -------------------------------------------------------------------------
    // Pie slice render state — mirrors ToolBelt's BlitPieArc pattern
    // -------------------------------------------------------------------------

    record PieSliceElement(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2f pose,
            float cx, float cy,
            float radiusIn, float radiusOut,
            float startAngle, float endAngle,
            int color,
            @Nullable ScreenRect scissorArea
    ) implements SimpleGuiElementRenderState {

        @Override
        public void setupVertices(VertexConsumer consumer, float depth) {
            float span = endAngle - startAngle;
            int sections = Math.max(1, (int) Math.ceil(span / (2.5f / 360f)));
            float slice = span / sections;

            for (int i = 0; i < sections; i++) {
                float a1 = startAngle + i * slice;
                float a2 = startAngle + (i + 1) * slice;

                float ix1 = cx + radiusIn  * (float) Math.cos(a1);
                float iy1 = cy + radiusIn  * (float) Math.sin(a1);
                float ox1 = cx + radiusOut * (float) Math.cos(a1);
                float oy1 = cy + radiusOut * (float) Math.sin(a1);
                float ix2 = cx + radiusIn  * (float) Math.cos(a2);
                float iy2 = cy + radiusIn  * (float) Math.sin(a2);
                float ox2 = cx + radiusOut * (float) Math.cos(a2);
                float oy2 = cy + radiusOut * (float) Math.sin(a2);

                // QUAD: outer1, inner1, inner2, outer2
                consumer.vertex(pose, ox1, oy1, depth).color(color);
                consumer.vertex(pose, ix1, iy1, depth).color(color);
                consumer.vertex(pose, ix2, iy2, depth).color(color);
                consumer.vertex(pose, ox2, oy2, depth).color(color);
            }
        }

        @Override
        public @Nullable ScreenRect bounds() {
            return null;
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
