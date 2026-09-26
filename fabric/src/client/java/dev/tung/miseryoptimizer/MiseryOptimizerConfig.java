package dev.tung.miseryoptimizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MiseryOptimizerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("Misery Optimizer");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tung.json");

    boolean crystals = true;
    boolean crystalEffects = true;
    boolean crystalPlacement = true;
    boolean anchors = true;
    boolean pearls = true;
    boolean terrainPreview = true;
    boolean slotSync = true;
    boolean blockCorrections = true;
    boolean foodTiming = true;
    int maxPreviewBlocks = 512;
    int extraGraceMillis = 0;
    int pearlHandoffTicks = 6;

    static MiseryOptimizerConfig load() {
        MiseryOptimizerConfig config = new MiseryOptimizerConfig();
        if (!Files.isRegularFile(FILE)) return config;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            config.crystals = bool(json, "crystals", true);
            config.crystalEffects = bool(json, "crystalEffects", true);
            config.crystalPlacement = bool(json, "crystalPlacement", true);
            config.anchors = bool(json, "anchors", true);
            config.pearls = bool(json, "pearls", true);
            config.terrainPreview = bool(json, "terrainPreview", true);
            config.slotSync = bool(json, "slotSync", true);
            config.blockCorrections = bool(json, "blockCorrections", true);
            config.foodTiming = bool(json, "foodTiming", true);
            config.maxPreviewBlocks = choice(json, "maxPreviewBlocks", 512, new int[] {128, 256, 512, 768});
            config.extraGraceMillis = choice(json, "extraGraceMillis", 0, new int[] {0, 500, 1000, 2000});
            config.pearlHandoffTicks = choice(json, "pearlHandoffTicks", 6, new int[] {0, 3, 6, 9});
        } catch (Exception exception) {
            LOGGER.warn("Could not read {}; using defaults", FILE, exception);
        }
        return config;
    }

    void save() {
        JsonObject json = new JsonObject();
        json.addProperty("crystals", crystals);
        json.addProperty("crystalEffects", crystalEffects);
        json.addProperty("crystalPlacement", crystalPlacement);
        json.addProperty("anchors", anchors);
        json.addProperty("pearls", pearls);
        json.addProperty("terrainPreview", terrainPreview);
        json.addProperty("slotSync", slotSync);
        json.addProperty("blockCorrections", blockCorrections);
        json.addProperty("foodTiming", foodTiming);
        json.addProperty("maxPreviewBlocks", maxPreviewBlocks);
        json.addProperty("extraGraceMillis", extraGraceMillis);
        json.addProperty("pearlHandoffTicks", pearlHandoffTicks);
        try {
            Files.createDirectories(FILE.getParent());
            Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(json));
            try {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not save {}", FILE, exception);
        }
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive()
            ? json.get(key).getAsBoolean() : fallback;
    }

    private static int choice(JsonObject json, String key, int fallback, int[] allowed) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) return fallback;
        int value = json.get(key).getAsInt();
        for (int candidate : allowed) if (candidate == value) return value;
        return fallback;
    }
}
