package fr.raconteur.simpleskinswapper.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.render.SpruceGuiGraphics;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import fr.raconteur.simpleskinswapper.SimpleSkinSwapper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SkinCarouselScreen extends SpruceScreen {

    private final Screen parent;
    private final List<SkinCard> cards = new ArrayList<>();

    private double cardIndex = 0;
    private WatchService watchService;

    public SkinCarouselScreen(Screen parent) {
        super(Text.translatable("simpleskinswapper.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        cards.clear();

        List<SkinEntry> entries = loadOrderedEntries();
        for (SkinEntry entry : entries) {
            SkinCard card = new SkinCard(this, entry, getCardWidth(), getCardHeight());
            cards.add(card);
            addDrawableChild(card);
        }

        if (!cards.isEmpty()) {
            cardIndex = MathHelper.clamp(cardIndex, 0, getMaxCardIndex());
        }

        updateAllArrowStates();

        addDrawableChild(new SpruceButtonWidget(
                Position.of(this.width / 2 - 122, this.height - 24), 120, 20,
                ScreenTexts.CANCEL,
                button -> close()
        ));
        addDrawableChild(new SpruceButtonWidget(
                Position.of(this.width / 2 + 2, this.height - 24), 120, 20,
                Text.translatable("simpleskinswapper.screen.carousel.open_folder"),
                button -> Util.getOperatingSystem().open(
                        FabricLoader.getInstance().getGameDir().resolve("skins").toFile())
        ));

        stopWatching();
        startWatching();
    }

    @Override
    public void close() {
        stopWatching();
        this.client.setScreen(parent);
    }

    @Override
    public void tick() {
        super.tick();
        if (watchService == null) return;
        WatchKey key = watchService.poll();
        if (key == null) return;
        boolean changed = key.pollEvents().stream()
                .anyMatch(e -> e.context() instanceof Path p
                        && p.toString().toLowerCase().endsWith(".png"));
        key.reset();
        if (changed) {
            this.init(client, width, height);
        }
    }

    private void startWatching() {
        Path skinsDir = FabricLoader.getInstance().getGameDir().resolve("skins");
        try {
            watchService = FileSystems.getDefault().newWatchService();
            skinsDir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            SimpleSkinSwapper.LOGGER.warn("Could not watch skins folder: {}", e.getMessage());
            watchService = null;
        }
    }

    private void stopWatching() {
        if (watchService != null) {
            try { watchService.close(); } catch (IOException ignored) {}
            watchService = null;
        }
    }

    @Override
    public void render(SpruceGuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, textRenderer.fontHeight * 3, this.width, this.height - textRenderer.fontHeight * 3, 0x7F000000);

        int cardW = getCardWidth();
        int cardH = getCardHeight();
        int gap = getCardGap();
        int cardAreaWidth = cardW + gap;
        int cardTop = this.height / 2 - cardH / 2;

        int startX = gap;

        for (int i = 0; i < cards.size(); i++) {
            SkinCard card = cards.get(i);
            int cardX = (int) (startX + (i - cardIndex) * cardAreaWidth);
            card.overridePosition(cardX, cardTop);
        }

        renderWidgets(graphics, mouseX, mouseY, delta);

        if (getMaxCardIndex() > 0) {
            renderScrollbar(graphics, cardIndex);
        }

        graphics.vanilla().drawCenteredTextWithShadow(
                textRenderer,
                getTitle().asOrderedText(),
                this.width / 2, textRenderer.fontHeight, 0xFFFFFFFF
        );

        // "No skins found" hint when directory is empty
        if (cards.isEmpty()) {
            graphics.vanilla().drawCenteredTextWithShadow(
                    textRenderer,
                    Text.translatable("simpleskinswapper.screen.carousel.no_skins"),
                    this.width / 2, this.height / 2 - textRenderer.fontHeight / 2, 0xFFAAAAAA
            );
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hozAmount, double vertAmount) {
        scroll((-vertAmount - hozAmount) / 4.0);
        return true;
    }

    private static final int SCROLLBAR_HEIGHT = 4;
    private static final int SCROLLBAR_HIT_PADDING = 6;
    private static final int SCROLLBAR_TRACK_COLOR = 0x4FFFFFFF;
    private static final int SCROLLBAR_THUMB_COLOR = 0xCCFFFFFF;

    private boolean isDraggingScrollbar = false;
    private int scrollbarDragOffsetX = 0;

    private int sbTrackX() { return getCardGap(); }
    private int sbTrackW() { return this.width - getCardGap() * 2; }
    private int sbTrackY() { return this.height - textRenderer.fontHeight * 3 - SCROLLBAR_HEIGHT - 4; }
    private int sbThumbW() { return Math.max(20, sbTrackW() / Math.max(1, cards.size())); }
    private int sbThumbX(double index) {
        int thumbRange = sbTrackW() - sbThumbW();
        double maxIdx = getMaxCardIndex();
        if (thumbRange <= 0 || maxIdx <= 0) return sbTrackX();
        return sbTrackX() + (int) (index / maxIdx * thumbRange);
    }

    private void renderScrollbar(SpruceGuiGraphics graphics, double index) {
        int trackY = sbTrackY();
        int trackX = sbTrackX();
        int trackW = sbTrackW();
        int thumbW = sbThumbW();
        int thumbX = sbThumbX(index);

        graphics.vanilla().fill(trackX, trackY, trackX + trackW, trackY + SCROLLBAR_HEIGHT, SCROLLBAR_TRACK_COLOR);
        graphics.vanilla().fill(thumbX, trackY, thumbX + thumbW, trackY + SCROLLBAR_HEIGHT, SCROLLBAR_THUMB_COLOR);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (getMaxCardIndex() > 0 && button == 0) {
            int trackY = sbTrackY();
            int trackX = sbTrackX();
            int trackW = sbTrackW();
            int hitY1 = trackY - SCROLLBAR_HIT_PADDING;
            int hitY2 = trackY + SCROLLBAR_HEIGHT + SCROLLBAR_HIT_PADDING;
            if (mouseY >= hitY1 && mouseY <= hitY2 && mouseX >= trackX && mouseX <= trackX + trackW) {
                int thumbX = sbThumbX(cardIndex);
                int thumbW = sbThumbW();
                if (mouseX >= thumbX && mouseX <= thumbX + thumbW) {
                    scrollbarDragOffsetX = (int) mouseX - thumbX;
                } else {
                    scrollbarDragOffsetX = thumbW / 2;
                    updateScrollFromMouseX((int) mouseX);
                }
                isDraggingScrollbar = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingScrollbar && button == 0) {
            updateScrollFromMouseX((int) mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingScrollbar && button == 0) {
            isDraggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateScrollFromMouseX(int mouseX) {
        int trackX = sbTrackX();
        int thumbW = sbThumbW();
        int thumbRange = sbTrackW() - thumbW;
        if (thumbRange <= 0) return;
        int newThumbX = mouseX - scrollbarDragOffsetX - trackX;
        double fraction = MathHelper.clamp((double) newThumbX / thumbRange, 0.0, 1.0);
        setCardIndex(fraction * getMaxCardIndex());
    }

    private void scroll(double amount) {
        if (getMaxCardIndex() <= 0) return;
        double newIndex = MathHelper.clamp(cardIndex + amount, 0, getMaxCardIndex());
        setCardIndex(newIndex);
    }

    private void setCardIndex(double index) {
        cardIndex = index;
    }

    private int getCardWidth() {
        return this.width / 5;
    }

    private int getCardHeight() {
        return (int) (this.height / 1.5);
    }

    private int getCardGap() {
        return 10;
    }

    /**
     * Maximum card index we can scroll to, so that the last card
     * ends up in the last visible slot on the right (not further left).
     */
    private double getMaxCardIndex() {
        int cardW = getCardWidth();
        int gap = getCardGap();
        int cardAreaWidth = cardW + gap;
        // Exact number of card-widths that fit between the left margin and the right margin
        // so that the last card's right edge lands exactly at (screenWidth - gap)
        double slotsFromLeft = (double)(this.width - 2 * gap - cardW) / cardAreaWidth;
        return Math.max(0.0, cards.size() - 1 - slotsFromLeft);
    }

    public void moveCard(SkinCard card, int direction) {
        int idx = cards.indexOf(card);
        int newIdx = idx + direction;
        if (newIdx < 0 || newIdx >= cards.size()) return;
        Collections.swap(cards, idx, newIdx);
        saveCurrentOrder();
        updateAllArrowStates();
    }

    private void updateAllArrowStates() {
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).updateArrowStates(i, cards.size());
        }
    }

    private List<SkinEntry> loadOrderedEntries() {
        List<SkinEntry> allEntries = SkinEntry.loadSkins();
        Path orderFile = FabricLoader.getInstance().getGameDir().resolve("skins").resolve("order.txt");

        if (!Files.exists(orderFile)) {
            saveOrder(allEntries, orderFile);
            return allEntries;
        }

        try {
            String content = Files.readString(orderFile).trim();
            if (content.isEmpty()) {
                saveOrder(allEntries, orderFile);
                return allEntries;
            }

            List<String> orderedNames = Arrays.stream(content.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            Map<String, SkinEntry> byName = new LinkedHashMap<>();
            for (SkinEntry e : allEntries) byName.put(e.file.getName(), e);

            List<SkinEntry> result = new ArrayList<>();
            for (String name : orderedNames) {
                SkinEntry e = byName.remove(name);
                if (e != null) result.add(e);
            }
            result.addAll(byName.values());

            saveOrder(result, orderFile);
            return result;
        } catch (IOException e) {
            SimpleSkinSwapper.LOGGER.warn("Could not read skin order: {}", e.getMessage());
            return allEntries;
        }
    }

    private void saveCurrentOrder() {
        Path orderFile = FabricLoader.getInstance().getGameDir().resolve("skins").resolve("order.txt");
        String content = cards.stream()
                .map(c -> c.getEntry().file.getName())
                .collect(Collectors.joining(","));
        try {
            Files.writeString(orderFile, content);
        } catch (IOException e) {
            SimpleSkinSwapper.LOGGER.warn("Could not save skin order: {}", e.getMessage());
        }
    }

    private void saveOrder(List<SkinEntry> entries, Path orderFile) {
        String content = entries.stream()
                .map(e -> e.file.getName())
                .collect(Collectors.joining(","));
        try {
            Files.writeString(orderFile, content);
        } catch (IOException e) {
            SimpleSkinSwapper.LOGGER.warn("Could not save skin order: {}", e.getMessage());
        }
    }
}
