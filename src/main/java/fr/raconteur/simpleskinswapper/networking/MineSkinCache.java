package fr.raconteur.simpleskinswapper.networking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import fr.raconteur.simpleskinswapper.SimpleSkinSwapper;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class MineSkinCache {
    private static final Path CACHE_FILE = FabricLoader.getInstance().getGameDir()
            .resolve("skins").resolve("cache.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static JsonObject cache = null;

    private static JsonObject load() {
        if (cache != null) return cache;
        try {
            if (Files.exists(CACHE_FILE)) {
                String content = Files.readString(CACHE_FILE);
                cache = GSON.fromJson(content, JsonObject.class);
            }
        } catch (Exception e) {
            SimpleSkinSwapper.LOGGER.warn("MineSkinCache: failed to load cache: {}", e.getMessage());
        }
        if (cache == null) cache = new JsonObject();
        return cache;
    }

    private static void save() {
        try {
            Files.createDirectories(CACHE_FILE.getParent());
            Files.writeString(CACHE_FILE, GSON.toJson(cache));
        } catch (IOException e) {
            SimpleSkinSwapper.LOGGER.warn("MineSkinCache: failed to save cache: {}", e.getMessage());
        }
    }

    public static String fileHash(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            SimpleSkinSwapper.LOGGER.warn("MineSkinCache: failed to hash file: {}", e.getMessage());
            return null;
        }
    }

    @Nullable
    public static Property get(String fileHash) {
        JsonObject data = load();
        if (!data.has(fileHash)) return null;
        try {
            JsonObject entry = data.getAsJsonObject(fileHash);
            String value = entry.get("texture").getAsString();
            String signature = entry.has("signature") && !entry.get("signature").isJsonNull()
                    ? entry.get("signature").getAsString() : null;
            SimpleSkinSwapper.LOGGER.info("MineSkinCache: cache hit for {}", fileHash.substring(0, 8));
            return new Property("textures", value, signature);
        } catch (Exception e) {
            SimpleSkinSwapper.LOGGER.warn("MineSkinCache: corrupted entry for {}: {}", fileHash, e.getMessage());
            return null;
        }
    }

    public static void put(String fileHash, Property property) {
        JsonObject data = load();
        JsonObject entry = new JsonObject();
        entry.addProperty("texture", property.value());
        if (property.hasSignature()) {
            entry.addProperty("signature", property.signature());
        } else {
            entry.add("signature", com.google.gson.JsonNull.INSTANCE);
        }
        data.add(fileHash, entry);
        save();
        SimpleSkinSwapper.LOGGER.info("MineSkinCache: cached entry for {}", fileHash.substring(0, 8));
    }
}
