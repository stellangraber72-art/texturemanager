package com.example.texturemanager;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Kategorien = Regeln, welche Dateien (assets/minecraft/...) zu welchem Thema gehören.
 * Die Reihenfolge in ALL ist wichtig: die ERSTE passende Kategorie gewinnt.
 * Neue Kategorie? Unten im static-Block eine Zeile mit add(...) ergänzen.
 */
public final class Categories {
    public record Category(String id, String label, String group, Predicate<String> matches) {}

    public static final String G_WEAPONS = "Waffen";
    public static final String G_ARMOR = "Rüstung";
    public static final String G_ITEMS = "PvP-Items";
    public static final String G_BLOCKS = "Blöcke";
    public static final String G_WORLD = "Welt & UI";
    public static final List<String> GROUPS = List.of(G_WEAPONS, G_ARMOR, G_ITEMS, G_BLOCKS, G_WORLD);

    /** Nur diese Ordner (unter assets/minecraft) werden betrachtet. */
    public static final List<String> DIRS = List.of("textures", "models", "items", "blockstates", "equipment");

    public static final List<Category> ALL;

    static {
        List<Category> l = new ArrayList<>();

        // ---------- Welt & UI (pfadbasiert, deshalb zuerst) ----------
        add(l, "sky", "Sky / Sun / Moon", G_WORLD, p -> p.startsWith("textures/environment/"));
        add(l, "particles", "Particles", G_WORLD, p -> p.startsWith("textures/particle/"));
        add(l, "hud", "HUD (Hotbar, Hearts, Crosshair)", G_WORLD, p -> p.startsWith("textures/gui/sprites/hud/"));
        add(l, "inventory", "Inventory / Container GUI", G_WORLD,
                p -> p.startsWith("textures/gui/container/") || p.startsWith("textures/gui/sprites/container/"));
        add(l, "glint", "Enchant Glint", G_WORLD, starts("enchanted_glint"));
        add(l, "fire", "Fire", G_WORLD, p -> base(p).matches("(soul_)?fire_[01]"));
        add(l, "water", "Water", G_WORLD, is("water_still", "water_flow", "water_overlay"));
        add(l, "lava", "Lava", G_WORLD, is("lava_still", "lava_flow"));

        // ---------- Waffen ----------
        for (String type : List.of("sword", "axe", "spear")) {
            for (String mat : List.of("netherite", "diamond", "iron")) {
                add(l, mat + "_" + type, cap(mat) + " " + cap(type), G_WEAPONS, is(mat + "_" + type));
            }
            add(l, "other_" + type, "Other " + cap(type) + "s", G_WEAPONS, ends("_" + type));
        }
        add(l, "pickaxe", "Pickaxes", G_WEAPONS, ends("_pickaxe"));
        add(l, "other_tools", "Shovels / Hoes", G_WEAPONS, ends("_shovel", "_hoe"));
        add(l, "mace", "Mace", G_WEAPONS, p -> base(p).equals("mace") || base(p).startsWith("mace_"));
        add(l, "bow", "Bow", G_WEAPONS, p -> base(p).equals("bow") || base(p).startsWith("bow_pulling"));
        add(l, "crossbow", "Crossbow", G_WEAPONS, starts("crossbow"));
        add(l, "trident", "Trident", G_WEAPONS, starts("trident"));
        add(l, "arrow", "Arrows", G_WEAPONS, p -> base(p).equals("arrow") || base(p).equals("spectral_arrow")
                || base(p).startsWith("tipped_arrow"));
        add(l, "wind_charge", "Wind Charge", G_WEAPONS, starts("wind_charge"));
        add(l, "shield", "Shield", G_WEAPONS, starts("shield"));
        add(l, "fishing_rod", "Fishing Rod", G_WEAPONS, starts("fishing_rod"));
        add(l, "flint", "Flint & Steel", G_WEAPONS, is("flint_and_steel"));

        // ---------- Rüstung ----------
        add(l, "armor_netherite", "Netherite Armor", G_ARMOR, armor("netherite"));
        add(l, "armor_diamond", "Diamond Armor", G_ARMOR, armor("diamond"));
        add(l, "armor_iron", "Iron Armor", G_ARMOR, armor("iron"));
        add(l, "armor_other", "Other Armor (Leather, Chain, Gold, Copper, Turtle)", G_ARMOR,
                armor("leather", "chainmail", "golden", "gold", "copper", "turtle"));
        add(l, "elytra", "Elytra", G_ARMOR, starts("elytra"));

        // ---------- PvP-Items ----------
        add(l, "totem", "Totem of Undying", G_ITEMS, starts("totem_of_undying"));
        add(l, "gapple", "Golden Apples", G_ITEMS, is("golden_apple", "enchanted_golden_apple"));
        add(l, "pearl", "Ender Pearl", G_ITEMS, is("ender_pearl"));
        add(l, "potion", "Potions (Splash, Lingering)", G_ITEMS, p -> base(p).contains("potion"));
        add(l, "xp_bottle", "XP Bottle", G_ITEMS, is("experience_bottle"));
        add(l, "firework", "Firework Rocket", G_ITEMS, starts("firework"));
        add(l, "buckets", "Buckets", G_ITEMS, p -> base(p).equals("bucket") || base(p).endsWith("_bucket"));
        add(l, "food", "Food (Steak, Golden Carrot, ...)", G_ITEMS, is("cooked_beef", "cooked_porkchop",
                "cooked_chicken", "cooked_mutton", "cooked_salmon", "cooked_cod", "cooked_rabbit", "golden_carrot",
                "bread", "baked_potato", "apple", "carrot", "chorus_fruit", "pumpkin_pie"));

        // ---------- Blöcke ----------
        add(l, "obsidian", "Obsidian", G_BLOCKS, is("obsidian", "crying_obsidian"));
        add(l, "crystal", "End Crystal", G_BLOCKS, starts("end_crystal"));
        add(l, "anchor", "Respawn Anchor", G_BLOCKS, starts("respawn_anchor"));
        add(l, "glowstone", "Glowstone", G_BLOCKS, is("glowstone"));
        add(l, "cobweb", "Cobweb", G_BLOCKS, is("cobweb"));
        add(l, "tnt", "TNT", G_BLOCKS, starts("tnt"));
        add(l, "bed", "Beds", G_BLOCKS, p -> p.contains("entity/bed/") || base(p).endsWith("_bed"));
        add(l, "stone", "Stone / Cobblestone", G_BLOCKS, is("stone", "cobblestone", "smooth_stone"));
        add(l, "deepslate", "Deepslate", G_BLOCKS, p -> base(p).contains("deepslate"));
        add(l, "grass", "Grass Block", G_BLOCKS, starts("grass_block"));
        add(l, "snow", "Snow", G_BLOCKS, is("snow", "snow_block"));

        ALL = List.copyOf(l);
    }

    // ---------- Helfer ----------

    private static void add(List<Category> l, String id, String label, String group, Predicate<String> m) {
        l.add(new Category(id, label, group, m));
    }

    private static String cap(String s) {
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    /** Dateiname ohne Ordner und Endungen ("a/b/foo.png.mcmeta" -> "foo"). */
    static String base(String path) {
        String f = path.substring(path.lastIndexOf('/') + 1);
        int dot = f.indexOf('.');
        return dot < 0 ? f : f.substring(0, dot);
    }

    private static Predicate<String> is(String... names) {
        Set<String> s = Set.of(names);
        return p -> s.contains(base(p));
    }

    private static Predicate<String> starts(String... prefixes) {
        return p -> {
            String b = base(p);
            for (String x : prefixes) if (b.startsWith(x)) return true;
            return false;
        };
    }

    private static Predicate<String> ends(String... suffixes) {
        return p -> {
            String b = base(p);
            for (String x : suffixes) if (b.endsWith(x)) return true;
            return false;
        };
    }

    private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};

    /** Rüstungsteile (Items/Models) plus die Trage-Texturen/Equipment-Definitionen eines Materials. */
    private static Predicate<String> armor(String... materials) {
        return p -> {
            String b = base(p);
            boolean worn = p.startsWith("equipment/") || p.contains("/humanoid");
            for (String m : materials) {
                if (worn && b.startsWith(m)) return true;
                for (String piece : PIECES) if (b.startsWith(m + "_" + piece)) return true;
            }
            return false;
        };
    }

    public static List<Category> inGroup(String group) {
        return ALL.stream().filter(c -> c.group().equals(group)).toList();
    }

    // ---------- Zuordnung ----------

    private static final Map<String, Optional<Category>> CACHE = new ConcurrentHashMap<>();

    /** Liefert die Kategorie einer Datei oder null. */
    public static Category match(Identifier id) {
        if (!id.getNamespace().equals("minecraft")) return null;
        return CACHE.computeIfAbsent(id.getPath(), Categories::compute).orElse(null);
    }

    private static Optional<Category> compute(String p) {
        boolean inDir = false;
        for (String d : DIRS) {
            if (p.startsWith(d + "/")) { inDir = true; break; }
        }
        if (!inDir) return Optional.empty();
        for (Category c : ALL) {
            if (c.matches().test(p)) return Optional.of(c);
        }
        return Optional.empty();
    }

    private Categories() {}
}
