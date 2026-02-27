package fr.raconteur.simpleskinswapper.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.render.SpruceGuiGraphics;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import fr.raconteur.simpleskinswapper.SimpleSkinSwapperClient;
import fr.raconteur.simpleskinswapper.changeskin.SkinChange;
import fr.raconteur.simpleskinswapper.changeskin.SkinSwapperState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.special.EntityGuiElementRenderState;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.EntityPose;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SkinCard extends SpruceContainerWidget {

    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_MARGIN = 4;

    private final SkinEntry entry;
    private final SkinCarouselScreen parent;
    private Position position;

    public SkinCard(SkinCarouselScreen parent, SkinEntry entry, int width, int height) {
        super(Position.of(0, 0), width, height);
        this.parent = parent;
        this.entry = entry;
        this.position = Position.of(0, 0);

        SpruceButtonWidget applyButton = new SpruceButtonWidget(
                Position.of(BUTTON_MARGIN, height - BUTTON_HEIGHT - BUTTON_MARGIN),
                width - BUTTON_MARGIN * 2, BUTTON_HEIGHT,
                Text.translatable("simpleskinswapper.screen.carousel.apply"),
                button -> applySkin()
        );
        addChild(applyButton);
    }

    private void applySkin() {
        if (!SkinSwapperState.beginSwap()) return;
        showOverlay(Text.translatable("simpleskinswapper.message.applying"));
        SkinChange.changeSkin(entry.file, entry.skinType,
                () -> showOverlay(Text.translatable("simpleskinswapper.message.success")),
                err -> showOverlay(Text.translatable("simpleskinswapper.message.error", err))
        );
        parent.close();
    }

    private void showOverlay(Text text) {
        if (client.player != null) {
            client.player.sendMessage(text, true);
        }
    }

    public void overridePosition(Position newPosition) {
        this.position = newPosition;
    }

    @Override
    public int getX() {
        return this.position.getX();
    }

    @Override
    public int getY() {
        return this.position.getY();
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

        // Draw skin name at top
        int margin = client.textRenderer.fontHeight / 2;
        int nameColor = this.active ? 0xFFFFFFFF : 0xFF808080;
        ClickableWidget.drawScrollableText(
                graphics.vanilla(), client.textRenderer,
                Text.of(entry.displayName),
                getX() + margin, getY() + margin,
                getX() + getWidth() - margin, getY() + margin + client.textRenderer.fontHeight,
                nameColor
        );

        // Load texture if not yet loaded
        entry.ensureTextureLoaded();

        // 3D skin preview in the center area (below name, above button)
        int previewTop = getY() + margin + client.textRenderer.fontHeight + 2;
        int previewBottom = getY() + getHeight() - BUTTON_HEIGHT - BUTTON_MARGIN * 2;
        int previewLeft = getX() + 1;
        int previewRight = getX() + getWidth() - 1;

        if (entry.textureId != null) {
            int previewHeight = previewBottom - previewTop;
            int size = (int) (previewHeight * 0.5f);
            renderSkin3D(graphics.vanilla(), previewLeft, previewTop, previewRight, previewBottom, size);
        }
    }

    private void renderSkin3D(DrawContext context, int x1, int y1, int x2, int y2, int size) {
        SkinTextures skinTextures = new SkinTextures(
                entry.textureId, null, null, null,
                entry.skinType == SkinType.SLIM ? SkinTextures.Model.SLIM : SkinTextures.Model.WIDE,
                true
        );

        PlayerEntityRenderState renderState = new PlayerEntityRenderState();
        renderState.age = SimpleSkinSwapperClient.TOTAL_TICK_DELTA;
        renderState.width = 0.6F;
        renderState.height = 1.8F;
        renderState.standingEyeHeight = 1.62F;
        renderState.invisible = false;
        renderState.sneaking = false;
        renderState.onFire = false;
        renderState.bodyYaw = 180.0f;
        renderState.relativeHeadYaw = 0.0f;
        renderState.pitch = 0.0f;
        renderState.deathTime = 0.0F;

        float animTime = SimpleSkinSwapperClient.TOTAL_TICK_DELTA * 0.067F;
        renderState.limbSwingAnimationProgress = MathHelper.sin(animTime) * 0.05F;
        renderState.limbSwingAmplitude = 0.1F;

        renderState.baseScale = 1.0F;
        renderState.ageScale = 1.0F;
        renderState.flipUpsideDown = false;
        renderState.shaking = false;
        renderState.baby = false;
        renderState.touchingWater = false;
        renderState.usingRiptide = false;
        renderState.hurt = false;
        renderState.invisibleToPlayer = false;
        renderState.hasOutline = false;
        renderState.sleepingDirection = null;
        renderState.customName = null;
        renderState.pose = EntityPose.STANDING;
        renderState.leaningPitch = 0.0F;
        renderState.handSwingProgress = 0.0F;
        renderState.limbAmplitudeInverse = 1.0F;
        renderState.crossbowPullTime = 0.0F;
        renderState.itemUseTime = 0;
        renderState.isInSneakingPose = false;
        renderState.isGliding = false;
        renderState.isSwimming = false;
        renderState.hasVehicle = false;
        renderState.isUsingItem = false;
        renderState.leftWingPitch = 0.0F;
        renderState.leftWingYaw = 0.0F;
        renderState.leftWingRoll = 0.0F;
        renderState.skinTextures = skinTextures;
        renderState.name = entry.displayName;
        renderState.spectator = false;
        renderState.stuckArrowCount = 0;
        renderState.stingerCount = 0;
        renderState.itemUseTimeLeft = 0;
        renderState.handSwinging = false;
        renderState.glidingTicks = 0.0F;
        renderState.applyFlyingRotation = false;
        renderState.flyingRotation = 0.0F;
        renderState.hatVisible = true;
        renderState.jacketVisible = true;
        renderState.leftPantsLegVisible = true;
        renderState.rightPantsLegVisible = true;
        renderState.leftSleeveVisible = true;
        renderState.rightSleeveVisible = true;
        renderState.capeVisible = false;
        renderState.playerName = null;
        renderState.leftShoulderParrotVariant = null;
        renderState.rightShoulderParrotVariant = null;
        renderState.id = 0;

        Quaternionf baseRotation = new Quaternionf().rotationZ((float) Math.PI);
        Vector3f entityPosition = new Vector3f(0.0F, 1.1F, 0.0F);

        context.enableScissor(x1, y1, x2, y2);
        EntityGuiElementRenderState state = new EntityGuiElementRenderState(
                renderState, entityPosition, baseRotation, null,
                x1, y1, x2, y2, (float) size, context.scissorStack.peekLast()
        );
        context.state.addSpecialElement(state);
        context.disableScissor();
    }
}
