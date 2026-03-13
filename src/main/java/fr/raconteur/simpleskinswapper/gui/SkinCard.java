package fr.raconteur.simpleskinswapper.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.render.SpruceGuiGraphics;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import fr.raconteur.simpleskinswapper.changeskin.SkinChange;
import fr.raconteur.simpleskinswapper.changeskin.SkinSwapperState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;

public class SkinCard extends SpruceContainerWidget {

    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_MARGIN = 4;

    private final SkinEntry entry;
    private final SkinCarouselScreen parent;
    private final SpruceButtonWidget leftArrow;
    private final SpruceButtonWidget rightArrow;
    private final SpruceButtonWidget typeButton;

    public SkinCard(SkinCarouselScreen parent, SkinEntry entry, int width, int height) {
        super(Position.of(0, 0), width, height);
        this.parent = parent;
        this.entry = entry;

        int arrowW = (width - BUTTON_MARGIN * 3) / 2;

        SpruceButtonWidget applyButton = new SpruceButtonWidget(
                Position.of(BUTTON_MARGIN, height - BUTTON_HEIGHT * 3 - BUTTON_MARGIN * 3),
                width - BUTTON_MARGIN * 2, BUTTON_HEIGHT,
                Text.translatable("simpleskinswapper.screen.carousel.apply"),
                button -> applySkin()
        );
        addChild(applyButton);

        typeButton = new SpruceButtonWidget(
                Position.of(BUTTON_MARGIN, height - BUTTON_HEIGHT * 2 - BUTTON_MARGIN * 2),
                width - BUTTON_MARGIN * 2, BUTTON_HEIGHT,
                typeLabel(),
                button -> toggleType()
        );
        addChild(typeButton);

        leftArrow = new SpruceButtonWidget(
                Position.of(BUTTON_MARGIN, height - BUTTON_HEIGHT - BUTTON_MARGIN),
                arrowW, BUTTON_HEIGHT,
                Text.literal("←"),
                button -> parent.moveCard(this, -1)
        );
        addChild(leftArrow);

        rightArrow = new SpruceButtonWidget(
                Position.of(BUTTON_MARGIN * 2 + arrowW, height - BUTTON_HEIGHT - BUTTON_MARGIN),
                arrowW, BUTTON_HEIGHT,
                Text.literal("→"),
                button -> parent.moveCard(this, +1)
        );
        addChild(rightArrow);
    }

    public void updateArrowStates(int index, int total) {
        leftArrow.setActive(index > 0);
        rightArrow.setActive(index < total - 1);
    }

    SkinEntry getEntry() {
        return entry;
    }

    private Text typeLabel() {
        return Text.translatable("simpleskinswapper.screen.carousel.type",
                Text.translatable("simpleskinswapper.screen.carousel.skin_type." + entry.skinType.getMojangVariant()));
    }

    private void toggleType() {
        entry.skinType = (entry.skinType == SkinType.CLASSIC) ? SkinType.SLIM : SkinType.CLASSIC;
        SkinTypeStore.setType(entry.file.getName(), entry.skinType);
        typeButton.setMessage(typeLabel());
    }

    private void applySkin() {
        if (!SkinSwapperState.beginSwap()) return;
        SkinChange.changeSkin(entry.file, entry.skinType,
                () -> showOverlay(Text.translatable("simpleskinswapper.message.success")),
                err -> showOverlay(Text.translatable("simpleskinswapper.message.error", err))
        );
        parent.close();
        showOverlay(Text.translatable("simpleskinswapper.message.applying"));
    }

    private void showOverlay(Text text) {
        if (client.player != null) {
            client.player.sendMessage(text, true);
        }
    }

    public void overridePosition(int x, int y) {
        this.getPosition().move(x, y);
    }

    @Override
    protected void renderBackground(SpruceGuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int borderColor = this.active ? 0xDF000000 : 0x5F000000;
        drawBorder(graphics.vanilla(), getX(), getY(), getWidth(), getHeight(), borderColor);
        graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1,
                this.active ? 0x7F000000 : 0x0D000000);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    @Override
    protected void renderWidget(SpruceGuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderWidget(graphics, mouseX, mouseY, delta);

        int margin = client.textRenderer.fontHeight / 2;
        int nameColor = this.active ? 0xFFFFFFFF : 0xFF808080;
        ClickableWidget.drawScrollableText(
                graphics.vanilla(), client.textRenderer,
                Text.of(entry.displayName),
                getX() + margin, getY() + margin,
                getX() + getWidth() - margin, getY() + margin + client.textRenderer.fontHeight,
                nameColor
        );

        entry.ensureTextureLoaded();

        int previewTop = getY() + margin + client.textRenderer.fontHeight + 2;
        int previewBottom = getY() + getHeight() - BUTTON_HEIGHT * 3 - BUTTON_MARGIN * 4;
        int previewLeft = getX() + 1;
        int previewRight = getX() + getWidth() - 1;

        if (entry.textureId != null) {
            int size = (int) ((previewBottom - previewTop) * 0.5f);
            SkinTextures skinTextures = new SkinTextures(
                    entry.textureId, null, null, null,
                    entry.skinType == SkinType.SLIM ? SkinTextures.Model.SLIM : SkinTextures.Model.WIDE,
                    true
            );
            SkinRenderer.renderPlayer(graphics.vanilla(), previewLeft, previewTop, previewRight, previewBottom, size, skinTextures);
        }
    }
}
