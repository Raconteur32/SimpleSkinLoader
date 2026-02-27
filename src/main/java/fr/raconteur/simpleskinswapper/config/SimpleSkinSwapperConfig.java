package fr.raconteur.simpleskinswapper.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.raconteur.simpleskinswapper.SimpleSkinSwapper;
import net.minecraft.client.MinecraftClient;

import java.io.*;

public class SimpleSkinSwapperConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile SimpleSkinSwapperConfig instance;

    // Config fields
    public boolean serverCommandEnabled = false;
    public String serverCommand = "";

    public static SimpleSkinSwapperConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static void save() {
        if (instance == null) return;
        try {
            File configFile = getConfigFile();
            configFile.getParentFile().mkdirs();
            try (Writer writer = new FileWriter(configFile)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            SimpleSkinSwapper.LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }

    private static SimpleSkinSwapperConfig load() {
        File configFile = getConfigFile();
        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile)) {
                return GSON.fromJson(reader, SimpleSkinSwapperConfig.class);
            } catch (IOException e) {
                SimpleSkinSwapper.LOGGER.error("Failed to load config: {}", e.getMessage());
            }
        }
        return new SimpleSkinSwapperConfig();
    }

    private static File getConfigFile() {
        return MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("config")
                .resolve("simpleskinswapper.json")
                .toFile();
    }
}
