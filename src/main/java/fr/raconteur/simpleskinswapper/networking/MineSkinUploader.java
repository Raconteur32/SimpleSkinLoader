package fr.raconteur.simpleskinswapper.networking;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import fr.raconteur.simpleskinswapper.SimpleSkinSwapper;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MineSkinUploader {
    private static final String PROXY_HOST = "sssmineskinsproxy.raconteur.fr:28433";
    private static final URI PROXY_URI = URI.create("ws://" + PROXY_HOST + "/skin-gateway");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /**
     * Uploads the skin file via WebSocket to the MineSkin proxy and returns
     * a texture Property (value + signature), or null on failure.
     */
    @Nullable
    public static Property upload(File skinFile, String variant) {
        String fileHash = MineSkinCache.fileHash(skinFile);
        if (fileHash != null) {
            Property cached = MineSkinCache.get(fileHash);
            if (cached != null) return cached;
        }

        try {
            byte[] fileBytes = Files.readAllBytes(skinFile.toPath());
            SimpleSkinSwapper.LOGGER.info("MineSkin: uploading {} ({} bytes)", skinFile.getName(), fileBytes.length);

            JsonObject req = new JsonObject();
            req.addProperty("type", "file");
            req.addProperty("model", variant);
            String jsonMessage = new Gson().toJson(req);

            LinkedBlockingQueue<Optional<Property>> channel = new LinkedBlockingQueue<>(1);

            HTTP_CLIENT.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(PROXY_URI, new UploadListener(fileBytes, jsonMessage, channel))
                    .exceptionally(e -> {
                        SimpleSkinSwapper.LOGGER.warn("MineSkin: connection failed: {}", e.getMessage());
                        channel.offer(Optional.empty());
                        return null;
                    });

            SimpleSkinSwapper.LOGGER.info("MineSkin: waiting for response (timeout 30s)...");
            Optional<Property> outcome = channel.poll(30, TimeUnit.SECONDS);
            if (outcome == null) {
                SimpleSkinSwapper.LOGGER.warn("MineSkin: timed out waiting for response");
                return null;
            }
            if (outcome.isEmpty()) return null;

            Property prop = outcome.get();
            if (fileHash != null) MineSkinCache.put(fileHash, prop);
            return prop;
        } catch (Exception e) {
            SimpleSkinSwapper.LOGGER.warn("MineSkin: upload failed: {}", e.getMessage());
            return null;
        }
    }

    private static final class UploadListener implements WebSocket.Listener {
        private final byte[] fileBytes;
        private final String jsonMessage;
        private final BlockingQueue<Optional<Property>> channel;
        private final Gson gson = new Gson();

        UploadListener(byte[] fileBytes, String jsonMessage, BlockingQueue<Optional<Property>> channel) {
            this.fileBytes = fileBytes;
            this.jsonMessage = jsonMessage;
            this.channel = channel;
        }

        @Override
        public void onOpen(WebSocket ws) {
            SimpleSkinSwapper.LOGGER.info("MineSkin: connected, sending skin data then JSON");
            ws.sendBinary(ByteBuffer.wrap(fileBytes), true)
              .thenRun(() -> ws.sendText(jsonMessage, true));
            ws.request(1);
        }

        @Override
        public CompletableFuture<?> onText(WebSocket ws, CharSequence data, boolean last) {
            SimpleSkinSwapper.LOGGER.info("MineSkin: received response: {}", data);
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            try {
                JsonObject body = gson.fromJson(data.toString(), JsonObject.class);
                String value = body.get("textureValue").getAsString();
                String sig = body.has("textureSignature") && !body.get("textureSignature").isJsonNull()
                        ? body.get("textureSignature").getAsString() : null;
                SimpleSkinSwapper.LOGGER.info("MineSkin: texture property parsed OK (signature: {})", sig != null);
                channel.offer(Optional.of(new Property("textures", value, sig)));
            } catch (Exception e) {
                SimpleSkinSwapper.LOGGER.warn("MineSkin: could not parse response: {}", e.getMessage());
                channel.offer(Optional.empty());
            }
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable err) {
            SimpleSkinSwapper.LOGGER.warn("MineSkin: WebSocket error: {}", err.getMessage());
            channel.offer(Optional.empty());
        }
    }
}
