package fr.raconteur.simpleskinswapper.changeskin;

import fr.raconteur.simpleskinswapper.config.SimpleSkinSwapperConfig;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SkinChangeManager {

    /**
     * The "textures" property value recorded when the server command was sent.
     * Used to detect whether the server has actually applied the new skin.
     * Null when no command is pending.
     */
    public static volatile String pendingCommandTextureValue = null;

    /**
     * The current attempt index (0 = first send, 1 = first retry, 2 = second retry).
     * Read by the mixin to compute the next attempt number.
     */
    public static volatile int commandAttempt = 0;

    /**
     * Signal set to true by the mixin as soon as a response packet is received,
     * regardless of whether the skin changed or not.
     * Each command send replaces this with a fresh AtomicBoolean(false).
     * The timeout closure captures the reference at send time, so an old timeout
     * can never fire on a newer attempt's WAITING state.
     */
    public static volatile AtomicBoolean commandResponseSignal = new AtomicBoolean(false);

    /** Maximum number of attempts (0, 1, 2). Beyond this, we give up. */
    private static final int MAX_ATTEMPTS = 3;

    /** Delays in seconds before attempt index 1 and 2. */
    private static final long[] RETRY_DELAYS_SECONDS = {5, 20};

    // -----------------------------------------------------------------------
    // Server command
    // -----------------------------------------------------------------------

    public static void sendServerCommandIfNeeded() {
        sendServerCommandIfNeeded(0);
    }

    /**
     * Sends the configured server command, or notifies the player to reconnect
     * if no command is configured.
     *
     * Attempt 0 fires immediately. Attempt 1 fires after 5 s. Attempt 2 fires
     * after 20 s. Attempt 3 (and beyond) abandons the swap entirely.
     *
     * Requires state SWAP_IN_PROGRESS (via beginCommand) for attempts 0–2.
     * For the abandon path the state is transitioned directly to READY_FOR_SWAP.
     *
     * @param attempt 0-based attempt index
     */
    public static void sendServerCommandIfNeeded(int attempt) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (attempt >= MAX_ATTEMPTS) {
            // All retries exhausted — give up and tell the player
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.translatable("simpleskinswapper.message.command_give_up"), false);
                }
            });
            SkinSwapperState.endSwap();
            return;
        }

        if (!SkinSwapperState.beginCommand()) return;
        commandAttempt = attempt;

        SimpleSkinSwapperConfig config = SimpleSkinSwapperConfig.get();

        if (!config.serverCommandEnabled || config.serverCommand == null || config.serverCommand.isBlank()) {
            // No command configured: ask the player to reconnect
            if (client.getNetworkHandler() != null) {
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(
                                Text.translatable("simpleskinswapper.message.reconnect"), false);
                    }
                });
            }
            SkinSwapperState.endSwap();
            return;
        }

        if (client.getNetworkHandler() == null) {
            SkinSwapperState.endSwap();
            return;
        }

        final String cmd = config.serverCommand.trim();

        client.execute(() -> {
            // Record the current texture value before sending the command
            if (client.player != null) {
                pendingCommandTextureValue = null;
                for (PlayerListEntry listEntry : client.getNetworkHandler().getPlayerList()) {
                    if (listEntry.getProfile().getId().equals(client.player.getUuid())) {
                        Property textures = listEntry.getProfile().getProperties()
                                .get("textures").stream().findFirst().orElse(null);
                        pendingCommandTextureValue = textures != null ? textures.value() : null;
                        break;
                    }
                }
            }

            // Notify the player
            if (client.player != null) {
                Text message;
                if (attempt == 0) {
                    message = Text.translatable("simpleskinswapper.message.command_pending");
                } else {
                    long delaySeconds = RETRY_DELAYS_SECONDS[attempt - 1];
                    message = Text.translatable("simpleskinswapper.message.command_retry",
                            delaySeconds, attempt + 1, MAX_ATTEMPTS);
                }
                client.player.sendMessage(message, false);
            }

            Runnable sendCmd = () -> client.execute(() -> {
                if (client.getNetworkHandler() == null) {
                    SkinSwapperState.endSwap();
                    return;
                }

                // Fresh signal for this specific send — captured in the timeout closure
                AtomicBoolean signal = new AtomicBoolean(false);
                commandResponseSignal = signal;

                SkinSwapperState.waitForCommandResult();
                if (cmd.startsWith("/")) {
                    client.getNetworkHandler().sendChatCommand(cmd.substring(1));
                } else {
                    client.getNetworkHandler().sendChatMessage(cmd);
                }

                // Timeout: if no response is received within 5 s for THIS send, give up
                CompletableFuture.runAsync(() -> client.execute(() -> {
                    if (!signal.get()) {
                        pendingCommandTextureValue = null;
                        SkinSwapperState.endSwap();
                        if (client.player != null) {
                            client.player.sendMessage(
                                    Text.translatable("simpleskinswapper.message.command_timeout"), false);
                        }
                    }
                }), CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS));
            });

            if (attempt == 0) {
                sendCmd.run();
            } else {
                long delaySeconds = RETRY_DELAYS_SECONDS[attempt - 1];
                CompletableFuture.runAsync(sendCmd,
                        CompletableFuture.delayedExecutor(delaySeconds, TimeUnit.SECONDS));
            }
        });
    }
}
