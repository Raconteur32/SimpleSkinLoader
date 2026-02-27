package fr.raconteur.simpleskinswapper.changeskin;

import fr.raconteur.simpleskinswapper.SimpleSkinSwapper;
import fr.raconteur.simpleskinswapper.gui.SkinType;
import net.minecraft.client.MinecraftClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.util.function.Consumer;

public class SkinChange {

    /**
     * Apply a skin. Uploads to Mojang if a valid access token is available,
     * then sends the configured server command (or notifies to reconnect if not set).
     *
     * Callbacks always execute on the render thread.
     *
     * @param skinFile  The PNG file to apply.
     * @param skinType  CLASSIC or SLIM.
     * @param onSuccess Called when done.
     * @param onError   Unused, kept for API compatibility.
     */
    public static void changeSkin(File skinFile, SkinType skinType,
                                   Runnable onSuccess, Consumer<String> onError) {
        MinecraftClient client = MinecraftClient.getInstance();

        String accessToken = getAccessToken(client);
        if (accessToken == null || accessToken.equals("0")) {
            SimpleSkinSwapper.LOGGER.warn("No valid access token; skipping Mojang upload.");
            client.execute(onSuccess);
            SkinChangeManager.sendServerCommandIfNeeded();
            return;
        }

        Thread thread = new Thread(() -> {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost post = new HttpPost("https://api.minecraftservices.com/minecraft/profile/skins");
                post.addHeader("Authorization", "Bearer " + accessToken);

                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.addTextBody("variant", skinType.getMojangVariant());
                builder.addPart("file", new FileBody(skinFile));
                post.setEntity(builder.build());

                HttpResponse response = httpClient.execute(post);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != 200) {
                    SimpleSkinSwapper.LOGGER.warn("Skin upload failed (HTTP {}).", statusCode);
                }
            } catch (Exception e) {
                SimpleSkinSwapper.LOGGER.warn("Skin upload error: {}", e.getMessage());
            }

            client.execute(onSuccess);
            SkinChangeManager.sendServerCommandIfNeeded();

        }, "SimpleSkinSwapper-SkinUpload");
        thread.setDaemon(true);
        thread.start();
    }

    private static String getAccessToken(MinecraftClient client) {
        if (client.getSession() == null) return null;
        return client.getSession().getAccessToken();
    }
}
