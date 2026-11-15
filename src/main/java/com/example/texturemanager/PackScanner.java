package com.example.texturemanager;

import com.example.texturemanager.Categories.Category;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.*;

/** Findet heraus, welches Pack welche Kategorien enthaelt (auch deaktivierte Packs). */
public final class PackScanner {
    public record Entry(String packId, Component title) {}

    public static Map<String, List<Entry>> scan(PackRepository repo) {
        Map<String, List<Entry>> result = new LinkedHashMap<>();
        for (Category c : Categories.ALL) result.put(c.id(), new ArrayList<>());

        for (Pack pack : repo.getAvailablePacks()) {
            Set<String> found = new HashSet<>();
            try (PackResources res = pack.open()) {
                for (String dir : Categories.DIRS) {
                    res.listResources(PackType.CLIENT_RESOURCES, "minecraft", dir, (id, supplier) -> {
                        Category c = Categories.match(id);
                        if (c != null) found.add(c.id());
                    });
                }
            } catch (Exception e) {
                TextureManagerMod.LOG.warn("Pack {} konnte nicht gescannt werden", pack.getId(), e);
                continue;
            }
            for (String cid : found) {
                result.get(cid).add(new Entry(pack.getId(), pack.getTitle()));
            }
        }

        // Vanilla zuerst, Rest alphabetisch
        Comparator<Entry> cmp = Comparator
                .comparing((Entry e) -> !e.packId().equals("vanilla"))
                .thenComparing(e -> e.title().getString().toLowerCase(Locale.ROOT));
        result.values().forEach(l -> l.sort(cmp));
        return result;
    }

    private PackScanner() {}
}
