package fr.raconteur.simpleskinswapper.gui;

import fr.raconteur.simpleskinswapper.SimpleSkinSwapper;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public class SkinShuffleImporter {
    private static final Path SOURCE_DIR = FabricLoader.getInstance().getGameDir()
            .resolve("config").resolve("skinshuffle").resolve("skins");
    private static final Path MARKER_FILE = SOURCE_DIR.resolve(".sssimported");
    private static final Path TARGET_DIR = FabricLoader.getInstance().getGameDir()
            .resolve("skins");

    public static void importIfNeeded() {
        if (!Files.isDirectory(SOURCE_DIR) || Files.exists(MARKER_FILE)) return;

        SimpleSkinSwapper.LOGGER.info("SkinShuffleImporter: importing skins from {}", SOURCE_DIR);

        try {
            Files.createDirectories(TARGET_DIR);
            int count = 0;

            try (Stream<Path> files = Files.list(SOURCE_DIR)) {
                for (Path file : files.toList()) {
                    if (!file.getFileName().toString().toLowerCase().endsWith(".png")) continue;
                    Path dest = TARGET_DIR.resolve(file.getFileName());
                    if (!Files.exists(dest)) {
                        Files.copy(file, dest, StandardCopyOption.COPY_ATTRIBUTES);
                        count++;
                    }
                }
            }

            Files.createFile(MARKER_FILE);
            SimpleSkinSwapper.LOGGER.info("SkinShuffleImporter: imported {} skin(s).", count);
        } catch (IOException e) {
            SimpleSkinSwapper.LOGGER.warn("SkinShuffleImporter: import failed: {}", e.getMessage());
        }
    }
}
