package fr.raconteur.simpleskinswapper.gui;

import fr.raconteur.simpleskinswapper.SimpleSkinSwapperClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.special.EntityGuiElementRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SkinRenderer {

    // GUI Y goes down, 3D Y goes up → flip 180° around Z to avoid upside-down rendering.
    private static final Quaternionf BASE_ROTATION = new Quaternionf().rotationZ((float) Math.PI);

    // Player model origin is at the feet; offset by 1.1 to center the model vertically in the preview area.
    private static final Vector3f MODEL_OFFSET = new Vector3f(0.0F, 1.1F, 0.0F);

    public static void renderPlayer(DrawContext context, int x1, int y1, int x2, int y2, int size, SkinTextures skin) {
        PlayerEntityRenderState state = buildRenderState(skin);
        context.enableScissor(x1, y1, x2, y2);
        EntityGuiElementRenderState element = new EntityGuiElementRenderState(
                state, MODEL_OFFSET, BASE_ROTATION, null,
                x1, y1, x2, y2, (float) size, context.scissorStack.peekLast()
        );
        context.state.addSpecialElement(element);
        context.disableScissor();
    }

    private static PlayerEntityRenderState buildRenderState(SkinTextures skin) {
        PlayerEntityRenderState s = new PlayerEntityRenderState();

        // Identity
        s.width = 0.6F;
        s.height = 1.8F;
        s.standingEyeHeight = 1.62F;
        s.baseScale = 1.0F;
        s.ageScale = 1.0F;
        s.age = SimpleSkinSwapperClient.TOTAL_TICK_DELTA;
        s.id = 0;

        // Pose
        s.pose = EntityPose.STANDING;
        s.bodyYaw = 180.0F;
        s.relativeHeadYaw = 0.0F;
        s.pitch = 0.0F;

        // Idle arm animation
        float t = SimpleSkinSwapperClient.TOTAL_TICK_DELTA * 0.067F;
        s.limbSwingAnimationProgress = MathHelper.sin(t) * 0.05F;
        s.limbSwingAmplitude = 0.1F;
        s.handSwingProgress = 0.0F;
        s.handSwinging = false;
        s.leaningPitch = 0.0F;
        s.limbAmplitudeInverse = 1.0F;

        // Skin
        s.skinTextures = skin;
        s.name = "";
        s.playerName = null;
        s.spectator = false;
        s.hatVisible = true;
        s.jacketVisible = true;
        s.leftSleeveVisible = true;
        s.rightSleeveVisible = true;
        s.leftPantsLegVisible = true;
        s.rightPantsLegVisible = true;
        s.capeVisible = false;
        s.leftShoulderParrotVariant = null;
        s.rightShoulderParrotVariant = null;

        // Inactive
        s.invisible = false;
        s.invisibleToPlayer = false;
        s.onFire = false;
        s.hurt = false;
        s.deathTime = 0.0F;
        s.sneaking = false;
        s.isInSneakingPose = false;
        s.baby = false;
        s.flipUpsideDown = false;
        s.shaking = false;
        s.hasOutline = false;
        s.customName = null;
        s.sleepingDirection = null;
        s.touchingWater = false;
        s.usingRiptide = false;
        s.hasVehicle = false;
        s.isUsingItem = false;
        s.isGliding = false;
        s.glidingTicks = 0.0F;
        s.applyFlyingRotation = false;
        s.flyingRotation = 0.0F;
        s.isSwimming = false;
        s.crossbowPullTime = 0.0F;
        s.itemUseTime = 0;
        s.itemUseTimeLeft = 0;
        s.stuckArrowCount = 0;
        s.stingerCount = 0;
        s.leftWingPitch = 0.0F;
        s.leftWingYaw = 0.0F;
        s.leftWingRoll = 0.0F;

        return s;
    }
}
