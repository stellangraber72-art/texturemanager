package com.example.texturemanager;

import com.example.texturemanager.Categories.Category;
import com.example.texturemanager.PackScanner.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Oben Gruppen-Tabs, links Kategorien, rechts die Packs der Kategorie.
 * Nur Standard-Widgets (robust gegen API-Änderungen).
 */
public class TextureManagerScreen extends Screen {
    private final Screen parent;
    private final Map<String, List<Entry>> data;
    private final Map<String, String> pending = new LinkedHashMap<>();
    private int group = 0, catIndex = 0, catPage = 0, packPage = 0;
    private boolean applyToGroup = false;

    private record Row(Entry e, int x, int y, int w) {}
    private final List<Row> rows = new ArrayList<>();
    private String currentCatId = "";
    private boolean showBig = false;
    private int previewX, previewY, previewSize = 100;

    public TextureManagerScreen(Screen parent) {
        super(Component.literal("Texture Manager"));
        this.parent = parent;
        this.data = PackScanner.scan(Minecraft.getInstance().getResourcePackRepository());
        this.pending.putAll(TextureManagerMod.config().selection);
    }

    @Override
    protected void init() {
        rows.clear();
        addRenderableWidget(new StringWidget((width - font.width(title)) / 2, 6, font.width(title), 10, title, font));

        // --- Gruppen-Tabs ---
        int n = Categories.GROUPS.size();
        int tabW = (width - 20) / n;
        for (int g = 0; g < n; g++) {
            final int gi = g;
            Button t = Button.builder(Component.literal(Categories.GROUPS.get(g)), b -> {
                group = gi;
                catIndex = 0;
                catPage = 0;
                packPage = 0;
                rebuildWidgets();
            }).bounds(10 + g * tabW, 18, tabW - 2, 18).build();
            t.active = g != group;
            addRenderableWidget(t);
        }

        List<Category> cats = Categories.inGroup(Categories.GROUPS.get(group));
        catIndex = Math.max(0, Math.min(catIndex, cats.size() - 1));
        Category cat = cats.get(catIndex);

        int top = 42, bottom = height - 56;

        // --- Kategorien (links) ---
        int catPer = Math.max(1, (bottom - top) / 20);
        int catPages = (cats.size() + catPer - 1) / catPer;
        catPage = Math.max(0, Math.min(catPage, catPages - 1));
        int y = top;
        for (int i = catPage * catPer; i < Math.min(cats.size(), (catPage + 1) * catPer); i++) {
            final int idx = i;
            Category c = cats.get(i);
            String label = (pending.containsKey(c.id()) ? "* " : "") + c.label();
            Button b = Button.builder(Component.literal(label), btn -> {
                catIndex = idx;
                packPage = 0;
                rebuildWidgets();
            }).bounds(10, y, 160, 18).build();
            b.active = idx != catIndex;
            addRenderableWidget(b);
            y += 20;
        }
        if (catPages > 1) {
            Button up = Button.builder(Component.literal("^"), b -> { catPage--; rebuildWidgets(); })
                    .bounds(10, height - 52, 78, 20).build();
            Button down = Button.builder(Component.literal("v"), b -> { catPage++; rebuildWidgets(); })
                    .bounds(92, height - 52, 78, 20).build();
            up.active = catPage > 0;
            down.active = catPage < catPages - 1;
            addRenderableWidget(up);
            addRenderableWidget(down);
        }

        // --- Packs (rechts) ---
        int x = 180, w = width - 190;
        showBig = false;
        if (w - previewSize - 20 >= 160) {
            showBig = true;
            w = w - previewSize - 20;
            previewX = width - 10 - previewSize;
            previewY = top + 14;
            addRenderableWidget(new StringWidget(previewX, top, previewSize, 10, Component.literal("Vorschau"), font));
        }
        currentCatId = cat.id();
        List<Entry> entries = new ArrayList<>();
        entries.add(null); // null = "Automatisch"
        entries.addAll(data.get(cat.id()));

        addRenderableWidget(new StringWidget(x, top, w, 10,
                Component.literal(cat.label() + " - " + data.get(cat.id()).size() + " Pack(s)"), font));

        int packPer = Math.max(1, (bottom - top - 14) / 20);
        int packPages = (entries.size() + packPer - 1) / packPer;
        packPage = Math.max(0, Math.min(packPage, packPages - 1));
        y = top + 14;
        for (int i = packPage * packPer; i < Math.min(entries.size(), (packPage + 1) * packPer); i++) {
            Entry e = entries.get(i);
            boolean active = e == null ? !pending.containsKey(cat.id())
                    : e.packId().equals(pending.get(cat.id()));
            String name = e == null ? "Automatisch (normale Pack-Reihenfolge)" : e.title().getString();
            addRenderableWidget(Button.builder(Component.literal((active ? "[x] " : "[ ] ") + name),
                    btn -> {
                        choose(cat, e, cats);
                        rebuildWidgets();
                    }).bounds(x + 22, y, w - 22, 18).build());
            rows.add(new Row(e, x, y, w));
            y += 20;
        }

        if (packPages > 1) {
            Button prev = Button.builder(Component.literal("<"), b -> { packPage--; rebuildWidgets(); })
                    .bounds(x, height - 52, 40, 20).build();
            Button next = Button.builder(Component.literal(">"), b -> { packPage++; rebuildWidgets(); })
                    .bounds(x + 44, height - 52, 40, 20).build();
            prev.active = packPage > 0;
            next.active = packPage < packPages - 1;
            addRenderableWidget(prev);
            addRenderableWidget(next);
        }
        addRenderableWidget(Button.builder(
                Component.literal("Ganze Gruppe: " + (applyToGroup ? "AN" : "AUS")), b -> {
                    applyToGroup = !applyToGroup;
                    rebuildWidgets();
                }).bounds(x + 92, height - 52, 140, 20).build());

        // --- Aktionen (unten) ---
        int by = height - 28;
        addRenderableWidget(Button.builder(Component.literal("Anwenden"), b -> apply())
                .bounds(width / 2 - 155, by, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Alles zurücksetzen"), b -> {
            pending.clear();
            rebuildWidgets();
        }).bounds(width / 2 - 50, by, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Abbrechen"), b -> onClose())
                .bounds(width / 2 + 55, by, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        Entry hovered = null;
        for (Row r : rows) {
            PreviewCache.Preview pv = r.e() == null ? null : PreviewCache.get(r.e().packId(), currentCatId);
            drawPreview(g, pv, r.x(), r.y(), 18);
            if (mouseX >= r.x() && mouseX < r.x() + r.w() && mouseY >= r.y() && mouseY < r.y() + 18) {
                hovered = r.e();
            }
        }
        if (showBig) {
            String packId = hovered != null ? hovered.packId() : pending.get(currentCatId);
            PreviewCache.Preview pv = packId == null ? null : PreviewCache.get(packId, currentCatId);
            drawPreview(g, pv, previewX, previewY, previewSize);
        }
    }

    /** Zeichnet eine Textur skaliert in ein Quadrat. Hohe Streifen (Animationen) zeigen nur das erste Bild. */
    private void drawPreview(GuiGraphics g, PreviewCache.Preview p, int x, int y, int size) {
        g.fill(x, y, x + size, y + size, 0xFF202020);
        if (p == null) return;
        int rw = p.w();
        int rh = p.h() > p.w() ? p.w() : p.h();
        float scale = (float) size / Math.max(rw, rh);
        int dw = Math.max(1, Math.round(rw * scale));
        int dh = Math.max(1, Math.round(rh * scale));
        g.blit(RenderPipelines.GUI_TEXTURED, p.id(), x + (size - dw) / 2, y + (size - dh) / 2,
                0f, 0f, dw, dh, rw, rh, p.w(), p.h());
    }

    @Override
    public void removed() {
        PreviewCache.clear();
    }

    /**
     * Wählt ein Pack. Bei "Ganze Gruppe: AN" wird es für alle Kategorien der Gruppe gesetzt,
     * in denen das Pack tatsächlich Inhalte hat.
     */
    private void choose(Category cat, Entry e, List<Category> cats) {
        List<Category> targets = applyToGroup ? cats : List.of(cat);
        for (Category c : targets) {
            if (e == null) {
                pending.remove(c.id());
            } else if (data.get(c.id()).stream().anyMatch(x -> x.packId().equals(e.packId()))) {
                pending.put(c.id(), e.packId());
            }
        }
    }

    private void apply() {
        ModConfig cfg = TextureManagerMod.config();
        cfg.selection.clear();
        cfg.selection.putAll(pending);
        cfg.save();
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(parent);
        mc.reloadResourcePacks();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
