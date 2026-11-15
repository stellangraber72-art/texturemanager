package com.example.texturemanager;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Lädt pro (Pack, Kategorie) eine Vorschau-Textur und merkt sie sich, bis die GUI schließt. */
public final class PreviewCache {
    public record Preview(Identifier id, int w, int h) {}

    private static final Map<String, Optional<Preview>> CACHE = new HashMap<>();
    private static int counter = 0;

    public static Preview get(String packId, String categoryId) {
        String key = packId + "|" + categoryId;
        Optional<Preview> cached = CACHE.get(key);
        if (cached == null) {
            cached = Optional.ofNullable(load(packId, categoryId));
            CACHE.put(key, cached);
        }
        return cached.orElse(null);
    }

    private static Preview load(String packId, String categoryId) {
        Minecraft mc = Minecraft.getInstance();
        Pack pack = mc.getResourcePackRepository().getPack(packId);
        if (pack == null) return null;
        try (PackResources res = pack.open()) {
            for (String path : Categories.previewPaths(categoryId)) {
                IoSupplier<InputStream> sup = res.getResource(PackType.CLIENT_RESOURCES,
                        Identifier.fromNamespaceAndPath("minecraft", path));
                if (sup == null) continue;
                try (InputStream in = sup.get()) {
                    NativeImage img = NativeImage.read(in);
                    int w = img.getWidth(), h = img.getHeight();
                    Identifier id = Identifier.fromNamespaceAndPath(TextureManagerMod.MOD_ID, "preview/" + (counter++));
                    mc.getTextureManager().register(id, new DynamicTexture(() -> "texturemanager preview", img));
                    return new Preview(id, w, h);
                }
            }
        } catch (Exception e) {
            TextureManagerMod.LOG.warn("Vorschau für {} / {} fehlgeschlagen", packId, categoryId, e);
        }
        return null;
    }

    public static void clear() {
        Minecraft mc = Minecraft.getInstance();
        for (Optional<Preview> o : CACHE.values()) {
            o.ifPresent(p -> mc.getTextureManager().release(p.id()));
        }
        CACHE.clear();
    }

    private PreviewCache() {}
}
