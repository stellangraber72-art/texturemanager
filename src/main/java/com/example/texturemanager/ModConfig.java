package com.example.texturemanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Speichert Kategorie-ID -> Pack-ID (z. B. "anchor" -> "file/MeinPack.zip"). */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Map<String, String> selection = new LinkedHashMap<>();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("texturemanager.json");
    }

    public static ModConfig load() {
        try (Reader r = Files.newBufferedReader(path())) {
            ModConfig c = GSON.fromJson(r, ModConfig.class);
            if (c != null) {
                if (c.selection == null) c.selection = new LinkedHashMap<>();
                return c;
            }
        } catch (NoSuchFileException ignored) {
        } catch (Exception e) {
            TextureManagerMod.LOG.error("Config konnte nicht geladen werden", e);
        }
        return new ModConfig();
    }

    public void save() {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(this));
        } catch (IOException e) {
            TextureManagerMod.LOG.error("Config konnte nicht gespeichert werden", e);
        }
    }
}
