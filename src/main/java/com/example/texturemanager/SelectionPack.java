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
 * Virtuelles Pack mit hoechster Prioritaet. Fuer jede Datei einer Kategorie mit
 * gewaehltem Pack wird die Datei aus genau diesem Pack geliefert.
 */
public class SelectionPack implements PackResources {
    private final PackRepository repo;
    private final Map<String, String> selection;
    private final Map<String, PackResources> opened = new HashMap<>();
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

    private PackResources open(String packId) {
        return opened.computeIfAbsent(packId, id -> {
            Pack p = repo.getPack(id);
            return p == null ? null : p.open();
        });
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) { return null; }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
        if (type != PackType.CLIENT_RESOURCES) return null;
        Category c = Categories.match(id);
        if (c == null) return null;
        PackResources src = source(c.id());
        return src == null ? null : src.getResource(type, id);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES || !namespace.equals("minecraft")) return;
        for (String packId : new HashSet<>(selection.values())) {
            PackResources src = open(packId);
            if (src == null) continue;
            src.listResources(type, namespace, path, (id, supplier) -> {
                Category c = Categories.match(id);
                if (c != null && packId.equals(selection.get(c.id()))) output.accept(id, supplier);
            });
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
    public void close() {
        opened.values().forEach(r -> { try { r.close(); } catch (Exception ignored) {} });
        opened.clear();
    }
}
