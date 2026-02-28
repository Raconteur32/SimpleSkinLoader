package fr.raconteur.simpleskinswapper;

import fr.raconteur.simpleskinswapper.gui.SkinCarouselScreen;
import fr.raconteur.simpleskinswapper.gui.SkinWheelScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SimpleSkinSwapperClient implements ClientModInitializer {

    public static KeyBinding openCarouselKey;
    public static KeyBinding openWheelKey;
    /** Accumulated tick delta for animations (incremented each game tick). */
    public static float TOTAL_TICK_DELTA = 0;

    @Override
    public void onInitializeClient() {
        openCarouselKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simpleskinswapper.open_carousel",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "simpleskinswapper.title"
        ));

        openWheelKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simpleskinswapper.open_wheel",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "simpleskinswapper.title"
        ));

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            TOTAL_TICK_DELTA++;
            if (openCarouselKey.wasPressed()) {
                client.setScreen(new SkinCarouselScreen(client.currentScreen));
            }
            if (openWheelKey.wasPressed()) {
                client.setScreen(new SkinWheelScreen(client.currentScreen));
            }
        });
    }
}
