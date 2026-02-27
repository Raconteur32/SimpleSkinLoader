package fr.raconteur.simpleskinswapper.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.render.SpruceGuiGraphics;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.GlfwUtil;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class SkinCarouselScreen extends SpruceScreen {

    private final Screen parent;
    private final List<SkinCard> cards = new ArrayList<>();

    private double cardIndex = 0;
    private double lastCardIndex = 0;
    private double lastCardSwitchTime = 0;

    public SkinCarouselScreen(Screen parent) {
        super(Text.translatable("simpleskinswapper.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        cards.clear();

        List<SkinEntry> entries = SkinEntry.loadSkins();
        for (SkinEntry entry : entries) {
            SkinCard card = new SkinCard(this, entry, getCardWidth(), getCardHeight());
            cards.add(card);
            addDrawableChild(card);
        }

        // Clamp cardIndex in case we came back with fewer skins
        if (!cards.isEmpty()) {
            cardIndex = MathHelper.clamp(cardIndex, 0, getMaxCardIndex());
            lastCardIndex = cardIndex;
        }

        addDrawableChild(new SpruceButtonWidget(
                Position.of(this.width / 2 - 64, this.height - 24), 128, 20,
                ScreenTexts.CANCEL,
                button -> close()
        ));
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public void render(SpruceGuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Dark overlay behind the cards
        graphics.fill(0, textRenderer.fontHeight * 3, this.width, this.height - textRenderer.fontHeight * 3, 0x7F000000);

        // Position each card based on smooth scroll
        int cardW = getCardWidth();
        int cardH = getCardHeight();
        int gap = getCardGap();
        int cardAreaWidth = cardW + gap;
        int cardTop = this.height / 2 - cardH / 2;

        double deltaIndex = getSmoothedCardIndex();
        int startX = gap; // first card aligned to left with a small margin

        for (int i = 0; i < cards.size(); i++) {
            SkinCard card = cards.get(i);
            int cardX = (int) (startX + (i - deltaIndex) * cardAreaWidth);
            card.overridePosition(Position.of(cardX, cardTop));
        }

        renderWidgets(graphics, mouseX, mouseY, delta);

        // Scrollbar (only when there are multiple cards)
        if (cards.size() > 1) {
            renderScrollbar(graphics, deltaIndex);
        }

        // Title
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

    // --- Scrollbar geometry helpers ---

    private int sbTrackX() { return getCardGap(); }
    private int sbTrackW() { return this.width - getCardGap() * 2; }
    private int sbTrackY() { return this.height - textRenderer.fontHeight * 3 - SCROLLBAR_HEIGHT - 4; }
    private int sbThumbW() { return Math.max(20, sbTrackW() / Math.max(1, cards.size())); }
    private int sbThumbX(double smoothedIndex) {
        int thumbRange = sbTrackW() - sbThumbW();
        double maxIdx = getMaxCardIndex();
        if (thumbRange <= 0 || maxIdx <= 0) return sbTrackX();
        return sbTrackX() + (int) (smoothedIndex / maxIdx * thumbRange);
    }

    // --- Rendering ---

    private void renderScrollbar(SpruceGuiGraphics graphics, double smoothedIndex) {
        int trackY = sbTrackY();
        int trackX = sbTrackX();
        int trackW = sbTrackW();
        int thumbW = sbThumbW();
        int thumbX = sbThumbX(smoothedIndex);

        graphics.vanilla().fill(trackX, trackY, trackX + trackW, trackY + SCROLLBAR_HEIGHT, SCROLLBAR_TRACK_COLOR);
        graphics.vanilla().fill(thumbX, trackY, thumbX + thumbW, trackY + SCROLLBAR_HEIGHT, SCROLLBAR_THUMB_COLOR);
    }

    // --- Mouse interaction ---

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (cards.size() > 1 && button == 0) {
            int trackY = sbTrackY();
            int trackX = sbTrackX();
            int trackW = sbTrackW();
            int hitY1 = trackY - SCROLLBAR_HIT_PADDING;
            int hitY2 = trackY + SCROLLBAR_HEIGHT + SCROLLBAR_HIT_PADDING;
            if (mouseY >= hitY1 && mouseY <= hitY2 && mouseX >= trackX && mouseX <= trackX + trackW) {
                int thumbX = sbThumbX(getSmoothedCardIndex());
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
        if (cards.size() <= 1) return;
        double newIndex = MathHelper.clamp(cardIndex + amount, 0, getMaxCardIndex());
        setCardIndex(newIndex);
    }

    private void setCardIndex(double index) {
        lastCardIndex = getSmoothedCardIndex();
        lastCardSwitchTime = GlfwUtil.getTime();
        cardIndex = index;
    }

    private double getSmoothedCardIndex() {
        double deltaTime = (GlfwUtil.getTime() - lastCardSwitchTime) * 5;
        deltaTime = MathHelper.clamp(deltaTime, 0, 1);
        deltaTime = Math.sin(deltaTime * Math.PI / 2);
        return MathHelper.lerp(deltaTime, lastCardIndex, cardIndex);
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
}
