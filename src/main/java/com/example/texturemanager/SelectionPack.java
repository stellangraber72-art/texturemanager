package com.example.texturemanager;

import com.example.texturemanager.Categories.Category;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Virtuelles Pack mit höchster Priorität. Für jede Datei einer Kategorie mit
 * gewähltem Pack wird die Datei aus genau diesem Pack geliefert.
 *
 * Wichtig: Minecraft lädt Ressourcen auf mehreren Threads gleichzeitig. Deshalb ist
 * das Öffnen der Packs synchronisiert, und nichts hier darf eine Exception nach außen werfen
 * (sonst bricht der ganze Reload ab: "Resource pack loading failed").
 */
public class SelectionPack implements PackResources {
    private final PackRepository repo;
    private final Map<String, String> selection;
    private final Map<String, PackResources> opened = new HashMap<>();
    private final Set<String> failed = new HashSet<>();
    private boolean closed = false;
    private final PackLocationInfo info = new PackLocationInfo(
            "texturemanager", Component.literal("Texture Manager"), PackSource.BUILT_IN, Optional.empty());

    public SelectionPack(PackRepository repo, Map<String, String> selection) {
        this.repo = repo;
        this.selection = selection;
    }

    private PackResources source(String categoryId) {
        String packId = selection.get(categoryId);
        return packId == null ? null : open(packId);
    }

    private synchronized PackResources open(String packId) {
        if (closed || failed.contains(packId)) return null;
        PackResources r = opened.get(packId);
        if (r != null) return r;
        try {
            Pack p = repo.getPack(packId);
            if (p == null) {
                failed.add(packId);
                TextureManagerMod.LOG.warn("Gewähltes Pack nicht gefunden (wird ignoriert): {}", packId);
                return null;
            }
            r = p.open();
            opened.put(packId, r);
            return r;
        } catch (Exception e) {
            failed.add(packId);
            TextureManagerMod.LOG.warn("Pack konnte nicht geöffnet werden (wird ignoriert): {}", packId, e);
            return null;
        }
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) { return null; }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
        try {
            if (type != PackType.CLIENT_RESOURCES) return null;
            Category c = Categories.match(id);
            if (c == null) return null;
            PackResources src = source(c.id());
            return src == null ? null : src.getResource(type, id);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES || !namespace.equals("minecraft")) return;
        for (String packId : new HashSet<>(selection.values())) {
            try {
                PackResources src = open(packId);
                if (src == null) continue;
                src.listResources(type, namespace, path, (id, supplier) -> {
                    Category c = Categories.match(id);
                    if (c != null && packId.equals(selection.get(c.id()))) output.accept(id, supplier);
                });
            } catch (Exception e) {
                TextureManagerMod.LOG.warn("Auflisten fehlgeschlagen für Pack {}", packId, e);
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of("minecraft") : Set.of();
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionType<T> type) { return null; }

    @Override
    public PackLocationInfo location() { return info; }

    @Override
    public synchronized void close() {
        closed = true;
        opened.values().forEach(r -> { try { r.close(); } catch (Exception ignored) {} });
        opened.clear();
    }
}
