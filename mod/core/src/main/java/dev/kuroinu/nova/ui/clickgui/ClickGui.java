package dev.kuroinu.nova.ui.clickgui;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.hud.HudEditor;
import dev.kuroinu.nova.module.Category;
import dev.kuroinu.nova.module.Module;
import dev.kuroinu.nova.module.impl.ClickGuiSettings;
import dev.kuroinu.nova.render.AlphaRenderer;
import dev.kuroinu.nova.render.Colors;
import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.render.font.NovaFont;
import dev.kuroinu.nova.setting.BooleanSetting;
import dev.kuroinu.nova.setting.ColorSetting;
import dev.kuroinu.nova.setting.ModeSetting;
import dev.kuroinu.nova.setting.NumberSetting;
import dev.kuroinu.nova.setting.Setting;
import dev.kuroinu.nova.setting.TextSetting;
import dev.kuroinu.nova.ui.Animation;
import dev.kuroinu.nova.ui.Easing;
import dev.kuroinu.nova.ui.NovaScreen;
import dev.kuroinu.nova.ui.Theme;
import dev.kuroinu.nova.ui.UiKey;
import dev.kuroinu.nova.ui.UiScale;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Nova menu, implementing "Layout B · Window" from the design handoff (docs/design).
 * Every module toggle, setting, favorite and bind is edited here.
 *
 * <p>Laid out in design units (handoff CSS px; see {@link UiScale}). Immediate-mode: each frame
 * draws the UI and records clickable regions; clicks are resolved against the last frame's
 * regions, so layout and hit-testing can never disagree.</p>
 *
 * <p>Motion: open/close fade + scale, staggered card entrance, settings-page slide, sliding nav
 * highlight, hover fades, expanding dropdowns/palettes, accent blending on theme change.</p>
 */
public final class ClickGui extends NovaScreen {
    // Fonts (handoff typography)
    private static final NovaFont LOGO_BOLD = NovaFont.bold(18);
    private static final NovaFont LOGO_REGULAR = NovaFont.regular(18);
    private static final NovaFont INPUT = NovaFont.regular(13);
    private static final NovaFont NAV = NovaFont.medium(14);
    private static final NovaFont MONO_10 = NovaFont.mono(10);
    private static final NovaFont MONO_11 = NovaFont.mono(11);
    private static final NovaFont MONO_12 = NovaFont.mono(12);
    private static final NovaFont MONO_13 = NovaFont.mono(13);
    private static final NovaFont BUTTON = NovaFont.semibold(14);
    private static final NovaFont TITLE = NovaFont.semibold(22);
    private static final NovaFont SUB = NovaFont.regular(13);
    private static final NovaFont CARD_NAME = NovaFont.semibold(15);
    private static final NovaFont DESC = NovaFont.regular(12);
    private static final NovaFont SMALL_BUTTON = NovaFont.regular(12);
    private static final NovaFont PAGE_NAME = NovaFont.semibold(20);
    private static final NovaFont BODY = NovaFont.regular(14);
    private static final NovaFont STAR = NovaFont.regular(14);
    private static final NovaFont STAR_LARGE = NovaFont.regular(16);
    private static final NovaFont CARET = NovaFont.regular(10);

    private static final float SIDEBAR_W = 220;
    private static final float CARD_MIN_W = 200;
    private static final float GAP = 10;
    private static final float DESC_LINE = 12 * 1.4f;
    private static final float LIST_MAX_W = 560;
    private static final float NAV_H = 36;
    private static final float ROW_H = 58;
    private static final float ROW_GAP = 8;
    private static final NovaFont ROW_NAME = NovaFont.semibold(16);
    private static final NovaFont ROW_DESC = NovaFont.regular(13);
    private static final NovaFont ROW_BADGE = NovaFont.bold(15);
    private static final NovaFont ROW_GEAR = NovaFont.regular(17);

    // Motion timings (ms)
    private static final long VIEW_MS = 180;
    private static final long CARD_MS = 170;
    private static final long CARD_STAGGER_MS = 22;
    private static final float HOVER_S = 0.08f;

    private interface Action {
        void run(int button);
    }

    private static final class Hit {
        final float x, y, w, h;
        final Action action;

        Hit(float x, float y, float w, float h, Action action) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.action = action;
        }

        boolean contains(float px, float py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private static final class NavEntry {
        final String key, name;
        final int count;
        final boolean active;
        final Runnable select;

        NavEntry(String key, String name, int count, boolean active, Runnable select) {
            this.key = key;
            this.name = name;
            this.count = count;
            this.active = active;
            this.select = select;
        }
    }

    private final Map<String, Animation> anims = new HashMap<String, Animation>();
    private final Animation openAnim = new Animation(0.14f, 0f);
    private final Animation scrollAnim = new Animation(0.08f, 0f);
    private final Animation accentAnim = new Animation(0.2f, 1f);
    private final List<Hit> hits = new ArrayList<Hit>();
    private final Deque<float[]> clips = new ArrayDeque<float[]>();

    /** Category shown in the grid (ignored while {@link #favorites}). */
    private Category category = Category.HUD;
    private boolean favorites;
    private String search = "";
    private boolean searchFocused;
    private Module page;
    private float scroll;
    /** Largest valid scroll offset, measured in the last frame. */
    private float maxScroll;

    private Setting<?> openDropdown;
    private Setting<?> dropdownBeforeClick;
    private Module listening;
    private Module listeningBeforeClick;
    private TextSetting focusedText;
    private NumberSetting dragNumber;
    private float dragTrackX, dragTrackW;

    private boolean closing;
    private Runnable afterClose;
    private String viewKey = "";
    private long viewChangedAt;
    private int viewDirection;

    /** Current frame transform: GUI coords per unit, and the open-animation scale about the center. */
    private float unit = 1f, scale = 1f, originX, originY;
    private float appliedScale = 1f;
    private float mouseX, mouseY;
    private int accent, accentSoft, accentFrom, accentTo;

    @Override
    public void onOpen() {
        closing = false;
        afterClose = null;
        listening = null;
        openDropdown = null;
        focusedText = null;
        openAnim.snap(0f);
        viewChangedAt = System.currentTimeMillis();
        accentTo = Theme.accent();
        accentFrom = accentTo;
        accentAnim.snap(1f);
        NovaClient.get().toasts().setDrawnByScreen(true);
    }

    @Override
    public void onClose() {
        dragNumber = null;
        listening = null;
        openDropdown = null;
        focusedText = null;
        searchFocused = false;
        NovaClient.get().toasts().setDrawnByScreen(false);
        NovaClient.get().config().save();
    }

    /** Animates out, then returns to the game (or runs {@code then}, e.g. opening the HUD editor). */
    private void animateClose(Runnable then) {
        if (!closing) {
            closing = true;
            afterClose = then;
        }
    }

    @Override
    public void close() {
        animateClose(null);
    }

    // ================================================================== frame

    @Override
    protected void render(Renderer base, int guiMouseX, int guiMouseY, float delta) {
        float t = openAnim.update(closing ? 0f : 1f);
        if (closing && t < 0.02f) {
            Runnable then = afterClose;
            closing = false;
            if (then != null) then.run();
            else NovaClient.get().platform().openScreen(getParent());
            return;
        }
        float e = Easing.outCubic(t);
        updateAccent();
        hits.clear();
        clips.clear();

        // Opened from a Nova screen (e.g. the main menu): draw it behind, its foreground fading out as we fade in.
        NovaScreen parent = getParent();
        if (parent != null) {
            parent.init(width, height);
            parent.renderAsBackdrop(base, 1f - e, delta);
        }

        AlphaRenderer r = new AlphaRenderer(base);
        r.setAlpha(e);
        r.rect(0, 0, width, height, Theme.OVERLAY);
        ClickGuiSettings gui = NovaClient.get().modules().get(ClickGuiSettings.class);
        // Applied on release: resizing the menu under the cursor mid-drag would make the slider fight itself.
        if (gui == null || dragNumber != gui.scale) {
            appliedScale = gui == null ? 1f : gui.scale.floatValue();
        }
        float[] units = UiScale.push(base, width, height, appliedScale);
        try {
            float sw = units[0], sh = units[1];
            unit = units[2];
            scale = 0.96f + 0.04f * e;
            originX = sw / 2f * (1 - scale);
            originY = sh / 2f * (1 - scale);
            mouseX = (guiMouseX / unit - originX) / scale;
            mouseY = (guiMouseY / unit - originY) / scale;
            updateDrag();
            trackView();

            float ww = Math.round(Math.min(940, sw * 0.94f));
            float wh = Math.round(Math.min(600, sh * 0.88f));
            float wx = Math.round((sw - ww) / 2f);
            float wy = Math.round((sh - wh) / 2f);

            r.pushScale(originX, originY, scale);
            try {
                drawShadow(r, wx, wy, ww, wh);
                r.roundedRect(wx, wy, ww, wh, 16, Theme.WINDOW);
                drawSidebar(r, wx, wy, wh);
                if (page == null) {
                    drawGrid(r, wx + SIDEBAR_W, wy, ww - SIDEBAR_W, wh);
                } else {
                    drawPage(r, page, wx + SIDEBAR_W, wy, ww - SIDEBAR_W, wh);
                }
                r.roundedOutline(wx, wy, ww, wh, 16, 1, Theme.BORDER_8);
            } finally {
                r.pop();
            }
            NovaClient.get().toasts().render(base, sw, sh);
        } finally {
            base.pop();
        }
    }

    /** Blends to the new accent when the theme changes. */
    private void updateAccent() {
        int target = Theme.accent();
        if (target != accentTo) {
            accentFrom = accent;
            accentTo = target;
            accentAnim.snap(0f);
        }
        accent = Colors.lerp(accentFrom, accentTo, accentAnim.update(1f));
        accentSoft = (accent & 0x00FFFFFF) | 0x26000000;
    }

    /** Restarts view transitions when the shown view changes. */
    private void trackView() {
        String key = page != null ? "page:" + page.getId()
                : !search.isEmpty() ? "search" : favorites ? "fav" : "cat:" + category;
        if (!key.equals(viewKey)) {
            boolean wasPage = viewKey.startsWith("page:"), isPage = key.startsWith("page:");
            viewDirection = isPage && !wasPage ? 1 : (wasPage && !isPage ? -1 : 0);
            viewKey = key;
            viewChangedAt = System.currentTimeMillis();
        }
    }

    /** Approximates the window's "0 30px 80px rgba(0,0,0,.5)" shadow with stacked translucent layers. */
    private static void drawShadow(Renderer r, float x, float y, float w, float h) {
        int layers = 8;
        for (int i = layers; i >= 1; i--) {
            float spread = i * 8f;
            float a = 0.5f * (1f - (float) i / (layers + 1)) / layers * 1.6f;
            r.roundedRect(x - spread + 4, y - spread + 30f * i / layers, w + spread * 2 - 8, h + spread * 2 - 16,
                    16 + spread, Colors.argb(Math.round(a * 255), 0, 0, 0));
        }
    }

    // ================================================================== sidebar

    private void drawSidebar(Renderer r, float wx, float wy, float wh) {
        pushClip(r, wx, wy, SIDEBAR_W, wh);
        r.roundedRect(wx, wy, SIDEBAR_W + 20, wh, 16, Theme.SIDEBAR);
        popClip(r);
        r.rect(wx + SIDEBAR_W - 1, wy, 1, wh, Theme.BORDER_6);

        float x = wx + 14, w = SIDEBAR_W - 28;
        float y = wy + 20;

        // Logo
        float logoH = r.lineHeight(LOGO_BOLD);
        float cy = y + logoH / 2f;
        r.diamond(x + 12, cy, 12, accent);
        float tx = x + 6 + 12 + 10;
        r.textCenteredV(LOGO_BOLD, "Nova", tx, cy, Theme.TEXT);
        r.textCenteredV(LOGO_REGULAR, "Client", tx + r.textWidth(LOGO_BOLD, "Nova") + 10, cy, Theme.MUTED);
        y += logoH + 18;

        drawSearch(r, x, y, w);
        y += 34 + 18;
        drawNav(r, x, y, w);

        // Bottom: Edit HUD, theme dots, version
        float bottom = wy + wh - 16;
        float dotsCy = bottom - 7;
        float btnY = bottom - 14 - 18 - 38;
        float hv = hover("edithud", x, btnY, w, 38);
        r.roundedRect(x, btnY, w, 38, 8, Colors.lerp(accent, Colors.lerp(accent, 0xFFFFFFFF, 0.12f), hv));
        r.textCentered(BUTTON, "Edit HUD", x + w / 2f, btnY + 19, Theme.ON_ACCENT);
        addHit(x, btnY, w, 38, b -> animateClose(() ->
                NovaClient.get().platform().openScreen(new HudEditor().withParent(this))));

        final ClickGuiSettings gui = NovaClient.get().modules().get(ClickGuiSettings.class);
        float dx = x + 4 + 7;
        for (final Theme.Preset p : Theme.Preset.values()) {
            float sel = anim("theme:" + p, 0.12f, Theme.preset() == p).update(Theme.preset() == p ? 1f : 0f);
            float dh = hover("dot:" + p, dx - 9, dotsCy - 9, 18, 18);
            r.circle(dx, dotsCy, 7 + dh * 0.8f, p.accent);
            if (sel > 0.01f) {
                r.roundedOutline(dx - 11, dotsCy - 11, 22, 22, 11, 2, Colors.withAlpha(Theme.KNOB, (int) (255 * sel)));
            }
            addHit(dx - 9, dotsCy - 9, 18, 18, b -> {
                if (gui != null) gui.theme.set(p.displayName);
            });
            dx += 14 + 7;
        }
        String version = "v1.0";
        r.textCenteredV(MONO_10, version, x + w - 4 - r.textWidth(MONO_10, version), dotsCy, Theme.DISABLED);
    }

    private void drawSearch(Renderer r, float x, float y, float w) {
        float h = 34;
        float focus = anim("searchfocus", HOVER_S, searchFocused).update(searchFocused ? 1f : 0f);
        float hv = hover("search", x, y, w, h);
        r.roundedRect(x, y, w, h, 8, Theme.INPUT);
        int border = Colors.lerp(Colors.lerp(Theme.BORDER_8, 0x24FFFFFF, hv), Colors.withAlpha(accent, 0x90), focus);
        r.roundedOutline(x, y, w, h, 8, 1, border);
        float cy = y + h / 2f;
        boolean caret = searchFocused && (System.currentTimeMillis() / 530) % 2 == 0;
        if (search.isEmpty()) {
            r.textCenteredV(INPUT, "Search…", x + 12, cy, Theme.MUTED_2);
            if (caret) r.rect(x + 12, cy - 7, 1, 14, Theme.TEXT);
        } else {
            String shown = search;
            while (r.textWidth(INPUT, shown) > w - 28 && shown.length() > 1) shown = shown.substring(1);
            r.textCenteredV(INPUT, shown, x + 12, cy, Theme.TEXT);
            if (caret) r.rect(x + 12 + r.textWidth(INPUT, shown) + 1, cy - 7, 1, 14, Theme.TEXT);
        }
        addHit(x, y, w, h, b -> {
            searchFocused = true;
            if (b == 1) setSearch("");
        });
    }

    private void drawNav(Renderer r, float x, float y, float w) {
        boolean searching = !search.isEmpty();
        List<NavEntry> entries = new ArrayList<NavEntry>();
        entries.add(new NavEntry("fav", "Favorites", countFavorites(), !searching && page == null && favorites, () -> {
            favorites = true;
        }));
        for (final Category c : Category.values()) {
            int count = NovaClient.get().modules().getModules(c).size();
            if (count == 0) continue;
            boolean active = !searching && (page == null ? !favorites && category == c : page.getCategory() == c);
            entries.add(new NavEntry("cat:" + c, c.getDisplayName(), count, active, () -> {
                favorites = false;
                category = c;
            }));
        }

        // Sliding highlight behind the active item.
        Float activeY = null;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).active) activeY = y + i * (NAV_H + 2);
        }
        Animation hy = anims.get("navY");
        if (hy == null) {
            hy = new Animation(0.1f, activeY != null ? activeY : y);
            anims.put("navY", hy);
        }
        float highlightY = activeY != null ? hy.update(activeY) : hy.get();
        float highlightA = anim("navA", 0.1f, activeY != null).update(activeY != null ? 1f : 0f);
        if (highlightA > 0.01f) {
            r.roundedRect(x, highlightY, w, NAV_H, 8, Colors.withAlpha(accentSoft, (int) (0x26 * highlightA)));
        }

        for (final NavEntry n : entries) {
            float hv = hover("nav:" + n.key, x, y, w, NAV_H);
            float act = anim("navact:" + n.key, 0.1f, n.active).update(n.active ? 1f : 0f);
            if (hv > 0.01f && !n.active) {
                r.roundedRect(x, y, w, NAV_H, 8, Colors.withAlpha(Theme.BORDER_4, (int) (0x0A * hv)));
            }
            float cy = y + NAV_H / 2f;
            r.textCenteredV(NAV, n.name, x + 10, cy, Colors.lerp(Colors.lerp(Theme.TEXT_2, Theme.TEXT, hv * 0.6f), accent, act));
            String c = String.valueOf(n.count);
            r.textCenteredV(MONO_11, c, x + w - 10 - r.textWidth(MONO_11, c), cy, Theme.MUTED_2);
            addHit(x, y, w, NAV_H, b -> {
                n.select.run();
                page = null;
                setSearch("");
                resetScroll();
            });
            y += NAV_H + 2;
        }
    }

    // ================================================================== grid view

    private void drawGrid(Renderer base, float mx, float wy, float mw, float wh) {
        List<Module> mods = visibleModules();
        boolean searching = !search.isEmpty();
        String title = searching ? "Search" : (favorites ? "Favorites" : category.getDisplayName());
        String sub = searching
                ? mods.size() + " result" + (mods.size() == 1 ? "" : "s") + " for “" + search + "”"
                : mods.size() + " module" + (mods.size() == 1 ? "" : "s");

        float vt = Easing.since(viewChangedAt, 0, VIEW_MS);
        AlphaRenderer header = new AlphaRenderer(base);
        header.setAlpha(vt);
        float shift = viewDirection < 0 ? (1 - vt) * -16 : 0;
        float titleLh = base.lineHeight(TITLE);
        float tx = mx + 26 + shift, ty = wy + 22;
        header.text(TITLE, title, tx, ty, Theme.TEXT);
        float subY = ty + titleLh * 0.78f - base.lineHeight(SUB) * 0.78f; // baseline-align with the title
        header.text(SUB, sub, tx + base.textWidth(TITLE, title) + 12, subY, Theme.MUTED_2);
        float headerH = 22 + titleLh + 14;

        float viewY = wy + headerH;
        float viewH = wh - headerH;
        float x = mx + 26 + shift, w = mw - 52;
        pushClip(base, mx, viewY, mw, viewH);
        float offset = scrollAnim.update(scroll);
        float contentH;
        try {
            ClickGuiSettings gui = NovaClient.get().modules().get(ClickGuiSettings.class);
            boolean grid = gui != null && gui.layout.is("Grid");
            contentH = (grid ? drawCards(base, mods, x, viewY - offset, w)
                    : drawRows(base, mods, x, viewY - offset, w)) + 26;
        } finally {
            popClip(base);
        }
        finishScroll(base, mx + mw - 6, viewY, viewH, contentH, offset);
    }

    private float drawCards(Renderer r, List<Module> mods, float x, float y, float w) {
        if (mods.isEmpty()) {
            String text = !search.isEmpty() ? "No modules match “" + search + "”"
                    : favorites ? "Star a module to pin it here" : "No modules";
            AlphaRenderer fade = new AlphaRenderer(r);
            fade.setAlpha(Easing.since(viewChangedAt, 0, VIEW_MS));
            fade.text(BODY, text, x, y + 40, Theme.MUTED_2);
            return 40 + r.lineHeight(BODY) + 40;
        }
        int cols = Math.max(1, (int) Math.floor((w + GAP) / (CARD_MIN_W + GAP)));
        float cw = (w - GAP * (cols - 1)) / cols;
        float top = y;
        int index = 0;
        for (int start = 0; start < mods.size(); start += cols) {
            float rowH = 0;
            List<List<String>> descs = new ArrayList<List<String>>();
            for (int i = start; i < Math.min(mods.size(), start + cols); i++) {
                List<String> lines = wrap(r, DESC, mods.get(i).getDescription(), cw - 28);
                descs.add(lines);
                rowH = Math.max(rowH, cardHeight(lines.size()));
            }
            for (int i = start; i < Math.min(mods.size(), start + cols); i++) {
                int col = i - start;
                // Staggered entrance: each card fades in and rises 8 units, slightly after the previous one.
                float ct = Easing.since(viewChangedAt, index++ * CARD_STAGGER_MS, CARD_MS);
                AlphaRenderer card = new AlphaRenderer(r);
                card.setAlpha(ct);
                drawCard(card, mods.get(i), descs.get(col), x + col * (cw + GAP), y + (1 - ct) * 8, cw, rowH);
            }
            y += rowH + GAP;
        }
        return y - GAP - top;
    }

    // ---------------------------------------------------------------- list layout

    private float drawRows(Renderer r, List<Module> mods, float x, float y, float w) {
        if (mods.isEmpty()) {
            return drawCards(r, mods, x, y, w); // shared empty state
        }
        float top = y;
        int index = 0;
        for (Module m : mods) {
            float ct = Easing.since(viewChangedAt, index++ * CARD_STAGGER_MS, CARD_MS);
            AlphaRenderer row = new AlphaRenderer(r);
            row.setAlpha(ct);
            drawRow(row, m, x + (1 - ct) * 10, y, w);
            y += ROW_H + ROW_GAP;
        }
        return y - ROW_GAP - top;
    }

    /** A full-width module row: initial badge, name + inline description, then bind, star, gear, switch. */
    private void drawRow(Renderer r, final Module m, float x, float y, float w) {
        float h = ROW_H;
        boolean on = m.isToggleable() && m.isEnabled();
        float hv = hover("row:" + m.getId(), x, y, w, h);
        float t = anim("card:" + m.getId(), 0.15f, on).update(on ? 1f : 0f);
        r.roundedRect(x, y, w, h, 10, Colors.lerp(Colors.lerp(Theme.CARD, 0xE6161620, hv), Theme.CARD_ON, t));
        r.roundedOutline(x, y, w, h, 10, 1, Colors.lerp(Colors.lerp(Theme.BORDER_6, 0x1FFFFFFF, hv), accent, t));
        addHit(x, y, w, h, b -> {
            if (b == 0 && m.isToggleable()) {
                NovaClient.get().toggleByUser(m);
            } else {
                openPage(m);
            }
        });

        float cy = y + h / 2f;
        float ix = x + 16;
        // Badge with the module's initial.
        r.roundedRect(ix, cy - 17, 34, 34, 8, Colors.lerp(0xFF1A1B22, accentSoft, t));
        r.textCentered(ROW_BADGE, m.getName().substring(0, 1).toUpperCase(Locale.ROOT), ix + 17, cy,
                Colors.lerp(Theme.MUTED, accent, t));

        // Right-side controls, laid out right to left.
        float right = x + w - 16;
        if (m.isToggleable()) {
            drawSwitch(r, "row:" + m.getId(), m.isEnabled(), right - 34, cy - 10, 34, 20, 14);
            right -= 34 + 12;
        }
        float gh = hover("gear:" + m.getId(), right - 30, cy - 15, 30, 30);
        if (gh > 0.01f) r.roundedRect(right - 30, cy - 15, 30, 30, 8, Colors.withAlpha(Theme.BORDER_6, (int) (0x14 * gh)));
        r.textCentered(ROW_GEAR, "⚙", right - 15, cy, Colors.lerp(Theme.MUTED, Theme.TEXT, gh));
        addHit(right - 30, cy - 15, 30, 30, b -> openPage(m));
        right -= 30 + 4;
        drawStar(r, m, right - 28, cy - 14, 28, STAR_LARGE);
        right -= 28 + 8;
        if (m.getBindKey() != Module.NO_KEY) {
            String bind = NovaClient.get().platform().getKeyName(m.getBindKey()).toUpperCase(Locale.ROOT);
            float bw = r.textWidth(MONO_11, bind) + 14;
            r.roundedRect(right - bw, cy - 11, bw, 22, 6, 0xFF1A1B22);
            r.textCentered(MONO_11, bind, right - bw / 2f, cy, Theme.MUTED);
            right -= bw + 10;
        }

        // Name, then the description inline in the remaining space.
        float tx = ix + 34 + 14;
        float avail = right - tx;
        String name = r.trimToWidth(ROW_NAME, m.getName(), avail);
        r.textCenteredV(ROW_NAME, name, tx, cy, Theme.TEXT);
        float nameW = r.textWidth(ROW_NAME, name) + 12;
        if (avail - nameW > 40) {
            // Nudge down so the smaller description shares the name's baseline.
            float baselineShift = (r.lineHeight(ROW_NAME) - r.lineHeight(ROW_DESC)) * 0.35f;
            r.textCenteredV(ROW_DESC, r.trimToWidth(ROW_DESC, m.getDescription(), avail - nameW),
                    tx + nameW, cy + baselineShift, Theme.MUTED);
        }
    }

    private static float cardHeight(int descLines) {
        return 14 + 22 + 8 + Math.max(34, descLines * DESC_LINE) + 8 + 26 + 14;
    }

    private void drawCard(Renderer r, final Module m, List<String> desc, float x, float y, float w, float h) {
        boolean on = m.isToggleable() && m.isEnabled();
        float hv = hover("card:" + m.getId(), x, y, w, h);
        float t = anim("card:" + m.getId(), 0.15f, on).update(on ? 1f : 0f);
        r.roundedRect(x, y, w, h, 10, Colors.lerp(Colors.lerp(Theme.CARD, 0xE6161620, hv), Theme.CARD_ON, t));
        r.roundedOutline(x, y, w, h, 10, 1, Colors.lerp(Colors.lerp(Theme.BORDER_6, 0x1FFFFFFF, hv), accent, t));

        addHit(x, y, w, h, b -> {
            if (b == 0 && m.isToggleable()) {
                NovaClient.get().toggleByUser(m);
            } else {
                openPage(m);
            }
        });

        float ix = x + 14, iw = w - 28;
        r.textCenteredV(CARD_NAME, r.trimToWidth(CARD_NAME, m.getName(), iw - 30), ix, y + 14 + 11, Theme.TEXT);
        drawStar(r, m, ix + iw - 22, y + 14, 22, STAR);

        float dy = y + 14 + 22 + 8;
        for (String line : desc) {
            r.text(DESC, line, ix, dy + (DESC_LINE - r.lineHeight(DESC)) / 2f, Theme.MUTED);
            dy += DESC_LINE;
        }

        float by = y + h - 14 - 26;
        String label = "Settings";
        float bw = r.textWidth(SMALL_BUTTON, label) + 20;
        float bh = hover("set:" + m.getId(), ix, by, bw, 26);
        if (bh > 0.01f) r.roundedRect(ix, by, bw, 26, 6, Colors.withAlpha(Theme.BORDER_6, (int) (0x0F * bh)));
        r.roundedOutline(ix, by, bw, 26, 6, 1, Colors.lerp(Theme.BORDER_8, 0x2EFFFFFF, bh));
        r.textCentered(SMALL_BUTTON, label, ix + bw / 2f, by + 13, Colors.lerp(Theme.TEXT_3, Theme.TEXT, bh));
        addHit(ix, by, bw, 26, b -> openPage(m));

        if (m.isToggleable()) {
            drawSwitch(r, "card:" + m.getId(), m.isEnabled(), ix + iw - 34, by + 3, 34, 20, 14);
        }
    }

    private void drawStar(Renderer r, final Module m, float x, float y, float size, NovaFont font) {
        boolean fav = m.isFavorite();
        float hv = hover("star:" + m.getId() + size, x, y, size, size);
        float f = anim("fav:" + m.getId(), 0.12f, fav).update(fav ? 1f : 0f);
        int color = Colors.lerp(Colors.lerp(Theme.DISABLED, Theme.TEXT_3, hv), accent, f);
        r.textCentered(font, f > 0.5f ? "★" : "☆", x + size / 2f, y + size / 2f, color);
        addHit(x, y, size, size, b -> m.setFavorite(!m.isFavorite()));
    }

    // ================================================================== settings page

    private void drawPage(Renderer base, final Module m, float mx, float wy, float mw, float wh) {
        // Slide in from the right (and fade) when the page opens.
        float vt = Easing.since(viewChangedAt, 0, VIEW_MS);
        AlphaRenderer r = new AlphaRenderer(base);
        r.setAlpha(vt);
        float dx = viewDirection > 0 ? (1 - vt) * 20 : 0;

        float headerH = 18 + 40 + 18;
        float cy = wy + 18 + 20;
        float x = mx + 26 + dx;
        String back = "← " + m.getCategory().getDisplayName();
        float bw = r.textWidth(SUB, back) + 24;
        float bh = hover("back", x, cy - 15, bw, 30);
        if (bh > 0.01f) r.roundedRect(x, cy - 15, bw, 30, 6, Colors.withAlpha(Theme.BORDER_6, (int) (0x0F * bh)));
        r.roundedOutline(x, cy - 15, bw, 30, 6, 1, Colors.lerp(Theme.BORDER_8, 0x2EFFFFFF, bh));
        r.textCentered(SUB, back, x + bw / 2f, cy, Colors.lerp(Theme.TEXT_2, Theme.TEXT, bh));
        addHit(x, cy - 15, bw, 30, b -> closePage());

        float right = mx + mw - 26 + dx;
        if (m.isToggleable()) {
            drawSwitch(r, "page:" + m.getId(), m.isEnabled(), right - 40, cy - 11, 40, 22, 16);
            addHit(right - 40, cy - 11, 40, 22, b -> NovaClient.get().toggleByUser(m));
            right -= 40 + 14;
        }
        drawStar(r, m, right - 30, cy - 15, 30, STAR_LARGE);
        right -= 30 + 14;

        float nx = x + bw + 14;
        float nameLh = r.lineHeight(PAGE_NAME), descLh = r.lineHeight(DESC);
        float blockTop = cy - (nameLh + descLh) / 2f;
        r.text(PAGE_NAME, r.trimToWidth(PAGE_NAME, m.getName(), right - nx), nx, blockTop, Theme.TEXT);
        r.text(DESC, r.trimToWidth(DESC, m.getDescription(), right - nx), nx, blockTop + nameLh, Theme.MUTED_2);
        base.rect(mx, wy + headerH - 1, mw, 1, Theme.BORDER_6);

        float viewY = wy + headerH;
        float viewH = wh - headerH;
        float lw = Math.min(LIST_MAX_W, mw - 52);
        pushClip(r, mx, viewY, mw, viewH);
        float offset = scrollAnim.update(scroll);
        float y = viewY + 8 - offset;
        float top = y;
        try {
            for (Setting<?> s : m.getSettings()) {
                y = drawSettingRow(r, s, x, y, lw);
            }
            if (m.isToggleable()) {
                y = drawKeybindRow(r, m, x, y, lw);
            }
            if (m.getSettings().isEmpty() && !m.isToggleable()) {
                r.text(BODY, "No settings", x, y + 14, Theme.MUTED_2);
                y += 14 + r.lineHeight(BODY) + 14;
            }
        } finally {
            popClip(r);
        }
        finishScroll(base, mx + mw - 6, viewY, viewH, y - top + 8 + 26, offset);
    }

    /** Draws one setting row (padding 14 0, bottom divider) and returns the next row's y. */
    private float drawSettingRow(Renderer r, Setting<?> s, float x, float y, float w) {
        float contentY = y + 14;
        float contentH;
        if (s instanceof BooleanSetting) {
            contentH = drawToggle(r, (BooleanSetting) s, x, contentY, w);
        } else if (s instanceof NumberSetting) {
            contentH = drawSlider(r, (NumberSetting) s, x, contentY, w);
        } else if (s instanceof ModeSetting) {
            contentH = drawMode(r, (ModeSetting) s, x, contentY, w);
        } else if (s instanceof ColorSetting) {
            contentH = drawColor(r, (ColorSetting) s, x, contentY, w);
        } else if (s instanceof TextSetting) {
            contentH = drawTextField(r, (TextSetting) s, x, contentY, w);
        } else {
            return y;
        }
        float bottom = contentY + contentH + 14;
        r.rect(x, bottom - 1, w, 1, Theme.BORDER_5);
        return bottom;
    }

    private float drawToggle(Renderer r, final BooleanSetting s, float x, float y, float w) {
        float h = 20;
        r.textCenteredV(BODY, s.getName(), x, y + h / 2f, Theme.TEXT);
        drawSwitch(r, key(s), s.get(), x + w - 34, y, 34, 20, 14);
        addHit(x, y - 14, w, h + 28, b -> s.toggle());
        return h;
    }

    private float drawSlider(Renderer r, final NumberSetting s, float x, float y, float w) {
        float labelH = r.lineHeight(BODY);
        r.text(BODY, s.getName(), x, y, Theme.TEXT);
        String value = formatNumber(s);
        r.textCenteredV(MONO_12, value, x + w - r.textWidth(MONO_12, value), y + labelH / 2f, accent);

        final float trackY = y + labelH + 8;
        float target = (float) ((s.get() - s.getMin()) / (s.getMax() - s.getMin()));
        Animation fa = anim("frac:" + key(s), 0.06f, target);
        if (dragNumber == s) {
            fa.snap(target); // follow the cursor exactly while dragging; ease only on resets/external changes
        }
        float frac = fa.update(target);
        float tcy = trackY + 9;
        float hv = Math.max(hover("slider:" + key(s), x - 7, trackY, w + 14, 18), dragNumber == s ? 1f : 0f);
        r.roundedRect(x, tcy - 2.5f, w, 5, 2.5f, Theme.TRACK);
        r.roundedRect(x, tcy - 2.5f, Math.max(5, w * frac), 5, 2.5f, accent);
        r.circle(x + w * frac, tcy, 7 + hv * 1.5f, Theme.KNOB);
        final float tx = x, tw = w;
        addHit(x - 7, trackY, w + 14, 18, b -> {
            dragNumber = s;
            dragTrackX = tx;
            dragTrackW = tw;
            updateDrag();
        });
        return labelH + 8 + 18;
    }

    private float drawMode(Renderer r, final ModeSetting s, float x, float y, float w) {
        float h = 30;
        float cy = y + h / 2f;
        r.textCenteredV(BODY, s.getName(), x, cy, Theme.TEXT);
        boolean open = openDropdown == s;
        String value = s.get();
        String caret = open ? "▲" : "▼";
        float bw = 12 + r.textWidth(SUB, value) + 10 + r.textWidth(CARET, caret) + 12;
        float bx = x + w - bw;
        float hv = hover("mode:" + key(s), bx, y, bw, h);
        r.roundedRect(bx, y, bw, h, 6, Colors.lerp(Theme.INPUT, 0xFF17171D, hv));
        r.roundedOutline(bx, y, bw, h, 6, 1, open ? Colors.withAlpha(accent, 0x90) : Colors.lerp(Theme.BORDER_8, 0x2EFFFFFF, hv));
        r.textCenteredV(SUB, value, bx + 12, cy, Theme.TEXT);
        r.textCenteredV(CARET, caret, bx + bw - 12 - r.textWidth(CARET, caret), cy, Theme.MUTED_2);
        addHit(bx, y, bw, h, b -> openDropdown = dropdownBeforeClick == s ? null : s);

        // Option chips, wrapped and right-aligned; the area expands/collapses smoothly.
        float ex = anim("expand:" + key(s), 0.12f, open).update(open ? 1f : 0f);
        if (ex < 0.01f) {
            return h;
        }
        float chipH = 28, gap = 6;
        List<List<String>> rows = new ArrayList<List<String>>();
        List<String> row = new ArrayList<String>();
        float rowW = 0;
        for (String o : s.getModes()) {
            float cw = r.textWidth(SUB, o) + 24;
            if (!row.isEmpty() && rowW + gap + cw > w) {
                rows.add(row);
                row = new ArrayList<String>();
                rowW = 0;
            }
            rowW += (row.isEmpty() ? 0 : gap) + cw;
            row.add(o);
        }
        rows.add(row);
        float fullH = 8 + rows.size() * (chipH + gap) - gap;
        float shownH = fullH * Easing.outCubic(ex);
        AlphaRenderer chips = new AlphaRenderer(r);
        chips.setAlpha(ex);
        pushClip(r, x, y + h, w, shownH);
        try {
            float cy2 = y + h + 8 - (1 - Easing.outCubic(ex)) * 6;
            for (List<String> rr : rows) {
                float total = 0;
                for (String o : rr) total += r.textWidth(SUB, o) + 24;
                total += gap * (rr.size() - 1);
                float cx = x + w - total;
                for (final String o : rr) {
                    float cw = r.textWidth(SUB, o) + 24;
                    boolean selected = o.equals(value);
                    float chv = hover("chip:" + key(s) + o, cx, cy2, cw, chipH);
                    int bg = selected ? accentSoft : Colors.lerp(Theme.CHIP, 0xFF1D1D24, chv);
                    chips.roundedRect(cx, cy2, cw, chipH, 6, bg);
                    chips.textCentered(SUB, o, cx + cw / 2f, cy2 + chipH / 2f, selected ? accent : Theme.TEXT_2);
                    if (open) addHit(cx, cy2, cw, chipH, b -> s.set(o));
                    cx += cw + gap;
                }
                cy2 += chipH + gap;
            }
        } finally {
            popClip(r);
        }
        return h + shownH;
    }

    private float drawColor(Renderer r, final ColorSetting s, float x, float y, float w) {
        float h = 28;
        r.textCenteredV(BODY, s.getName(), x, y + h / 2f, Theme.TEXT);
        float sx = x + w - 28;
        float hv = hover("swatch:" + key(s), sx, y, 28, 28);
        r.roundedRect(sx, y, 28, 28, 6, s.argb());
        r.roundedOutline(sx, y, 28, 28, 6, 1, Colors.lerp(Theme.BORDER_20, 0x66FFFFFF, hv));
        addHit(sx, y, 28, 28, b -> openDropdown = dropdownBeforeClick == s ? null : s);

        boolean open = openDropdown == s;
        float ex = anim("expand:" + key(s), 0.12f, open).update(open ? 1f : 0f);
        if (ex < 0.01f) {
            return h;
        }
        float size = 22, gap = 8;
        float fullH = 8 + size + 4;
        float shownH = fullH * Easing.outCubic(ex);
        int n = Theme.PALETTE.length;
        float px = x + w - (n * size + (n - 1) * gap);
        float py = y + h + 8;
        AlphaRenderer sw = new AlphaRenderer(r);
        sw.setAlpha(ex);
        pushClip(r, x - 4, y + h, w + 8, shownH);
        try {
            for (int i = 0; i < n; i++) {
                final int c = Theme.PALETTE[i];
                // Swatches pop in left to right.
                float pop = Easing.outCubic(Easing.clamp01(ex * 1.6f - i * 0.06f));
                float ccx = px + size / 2f, ccy = py + size / 2f;
                float dh = hover("pal:" + key(s) + i, px, py, size, size);
                sw.circle(ccx, ccy, size / 2f * (0.6f + 0.4f * pop) + dh, c);
                if ((s.argb() & 0xFFFFFF) == (c & 0xFFFFFF)) {
                    sw.roundedOutline(ccx - size / 2f - 4, ccy - size / 2f - 4, size + 8, size + 8, size / 2f + 4, 2, Theme.KNOB);
                }
                // Keep the setting's own alpha so translucent colors (e.g. backgrounds) stay translucent.
                if (open) addHit(px, py, size, size, b -> s.set((s.argb() & 0xFF000000) | (c & 0xFFFFFF)));
                px += size + gap;
            }
        } finally {
            popClip(r);
        }
        return h + shownH;
    }

    private float drawTextField(Renderer r, final TextSetting s, float x, float y, float w) {
        float labelH = r.lineHeight(BODY);
        r.text(BODY, s.getName(), x, y, Theme.TEXT);
        float fy = y + labelH + 8, fh = 34;
        boolean focused = focusedText == s;
        float focus = anim("tf:" + key(s), HOVER_S, focused).update(focused ? 1f : 0f);
        float hv = hover("tfh:" + key(s), x, fy, w, fh);
        r.roundedRect(x, fy, w, fh, 6, Theme.INPUT);
        r.roundedOutline(x, fy, w, fh, 6, 1,
                Colors.lerp(Colors.lerp(Theme.BORDER_8, 0x24FFFFFF, hv), Colors.withAlpha(accent, 0x90), focus));
        String text = s.get();
        while (r.textWidth(MONO_13, text) > w - 28 && text.length() > 1) text = text.substring(1);
        float cy = fy + fh / 2f;
        r.textCenteredV(MONO_13, text, x + 12, cy, Theme.TEXT);
        if (focused && (System.currentTimeMillis() / 530) % 2 == 0) {
            r.rect(x + 12 + r.textWidth(MONO_13, text) + 1, cy - 7, 1, 14, Theme.TEXT);
        }
        addHit(x, fy, w, fh, b -> {
            focusedText = s;
            if (b == 1) s.set("");
        });
        return labelH + 8 + fh;
    }

    private float drawKeybindRow(Renderer r, final Module m, float x, float y, float w) {
        float contentY = y + 14;
        float h = 30;
        float cy = contentY + h / 2f;
        r.textCenteredV(BODY, "Keybind", x, cy, Theme.TEXT);
        boolean isListening = listening == m;
        float lt = anim("listen:" + m.getId(), 0.1f, isListening).update(isListening ? 1f : 0f);
        String text = isListening ? "PRESS…" : m.getBindKey() == Module.NO_KEY ? "NONE"
                : NovaClient.get().platform().getKeyName(m.getBindKey()).toUpperCase(Locale.ROOT);
        float bw = Math.max(64, r.textWidth(MONO_12, text) + 24);
        float bx = x + w - bw;
        float hv = hover("bind:" + m.getId(), bx, contentY, bw, h);
        if (lt > 0.01f) r.roundedRect(bx, contentY, bw, h, 6, Colors.withAlpha(accentSoft, (int) (0x26 * lt)));
        r.roundedOutline(bx, contentY, bw, h, 6, 1, Colors.lerp(Colors.lerp(Theme.BORDER_8, 0x2EFFFFFF, hv), accent, lt));
        r.textCentered(MONO_12, text, bx + bw / 2f, cy, Colors.lerp(Theme.TEXT_3, accent, lt));
        addHit(bx, contentY, bw, h, b -> {
            if (b == 1) {
                m.setBindKey(Module.NO_KEY);
                listening = null;
            } else {
                listening = listeningBeforeClick == m ? null : m;
            }
        });
        float bottom = contentY + h + 14;
        r.rect(x, bottom - 1, w, 1, Theme.BORDER_5);
        return bottom;
    }

    // ================================================================== shared widgets

    private void drawSwitch(Renderer r, String key, boolean on, float x, float y, float w, float h, float knob) {
        float t = anim("switch:" + key, 0.15f, on).update(on ? 1f : 0f);
        r.roundedRect(x, y, w, h, h / 2f, Colors.lerp(Theme.SWITCH_OFF, accent, t));
        float inset = (h - knob) / 2f;
        float kx = x + inset + t * (w - knob - inset * 2);
        r.circle(kx + knob / 2f, y + h / 2f, knob / 2f, Theme.KNOB);
    }

    private void finishScroll(Renderer r, float barX, float viewY, float viewH, float contentH, float offset) {
        float max = Math.max(0, contentH - viewH);
        maxScroll = max;
        if (scroll > max) scroll = max;
        if (scroll < 0) scroll = 0;
        if (scrollAnim.get() > max) scrollAnim.snap(max); // content shrank (e.g. dropdown closed)
        float show = anim("scrollbar", 0.12f, false).update(contentH > viewH + 0.5f ? 1f : 0f);
        if (show > 0.01f) {
            float barH = Math.max(24, viewH * viewH / Math.max(contentH, viewH));
            float barY = viewY + (viewH - barH) * (max == 0 ? 0 : Math.min(1, offset / max));
            r.roundedRect(barX, barY + 4, 3, barH - 8, 1.5f, Colors.withAlpha(0xFFFFFFFF, (int) (0x26 * show)));
        }
    }

    // ================================================================== data

    private List<Module> visibleModules() {
        List<Module> out = new ArrayList<Module>();
        String q = search.toLowerCase(Locale.ROOT);
        for (Module m : NovaClient.get().modules().getModules()) {
            if (!q.isEmpty()) {
                if (m.getName().toLowerCase(Locale.ROOT).contains(q)
                        || m.getDescription().toLowerCase(Locale.ROOT).contains(q)
                        || m.getCategory().getDisplayName().toLowerCase(Locale.ROOT).contains(q)) {
                    out.add(m);
                }
            } else if (favorites ? m.isFavorite() : m.getCategory() == category) {
                out.add(m);
            }
        }
        return out;
    }

    private int countFavorites() {
        int n = 0;
        for (Module m : NovaClient.get().modules().getModules()) {
            if (m.isFavorite()) n++;
        }
        return n;
    }

    private static List<String> wrap(Renderer r, NovaFont font, String text, float maxW) {
        List<String> lines = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && r.textWidth(font, candidate) > maxW) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    static String formatNumber(NumberSetting s) {
        double v = s.get();
        String text = s.getStep() < 1 ? String.format(Locale.ROOT, "%.1f", v) : String.valueOf(Math.round(v));
        return text + s.getUnit();
    }

    // ================================================================== input

    @Override
    public boolean mouseClicked(double gx, double gy, int button) {
        if (closing) return true;
        float x = (float) ((gx / unit - originX) / scale), y = (float) ((gy / unit - originY) / scale);
        searchFocused = false;
        focusedText = null;
        dropdownBeforeClick = openDropdown;
        openDropdown = null;
        listeningBeforeClick = listening;
        listening = null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (hit.contains(x, y)) {
                hit.action.run(button);
                return true;
            }
        }
        return true; // background click: dropdowns closed / listening cancelled above
    }

    @Override
    public void mouseReleased(double x, double y, int button) {
        dragNumber = null;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double amount) {
        // Clamp at input time: an out-of-range target made the eased scroll overshoot and snap back at the ends.
        if (!closing) scroll = Math.max(0, Math.min(maxScroll, scroll - (float) Math.signum(amount) * 48));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, UiKey key) {
        if (closing) return true;
        if (listening != null) {
            if (key == UiKey.ESCAPE) {
                listening.setBindKey(Module.NO_KEY);
            } else if (keyCode > 0) {
                listening.setBindKey(keyCode);
            } else {
                return true; // unidentifiable key (e.g. modifiers on 26.x): keep listening
            }
            listening = null;
            return true;
        }
        if (key == UiKey.ESCAPE && openDropdown != null) {
            openDropdown = null;
            return true;
        }
        if (focusedText != null) {
            if (key == UiKey.ESCAPE || key == UiKey.ENTER) {
                focusedText = null;
            } else if (key == UiKey.BACKSPACE && !focusedText.get().isEmpty()) {
                String v = focusedText.get();
                focusedText.set(v.substring(0, v.length() - 1));
            }
            return key != UiKey.OTHER;
        }
        if (key == UiKey.BACKSPACE && !search.isEmpty() && (searchFocused || page == null)) {
            setSearch(search.substring(0, search.length() - 1));
            return true;
        }
        return false; // unhandled ESC closes the menu (animated)
    }

    @Override
    public void charTyped(char c) {
        if (closing || listening != null || c < 32 || c == 127) {
            return;
        }
        if (focusedText != null) {
            focusedText.set(focusedText.get() + c);
            return;
        }
        if (search.length() < 32) {
            searchFocused = true;
            setSearch(search + c);
        }
    }

    // ================================================================== helpers

    private void updateDrag() {
        if (dragNumber != null && dragTrackW > 0) {
            double f = Math.max(0, Math.min(1, (mouseX - dragTrackX) / dragTrackW));
            dragNumber.set(dragNumber.getMin() + f * (dragNumber.getMax() - dragNumber.getMin()));
        }
    }

    private void setSearch(String value) {
        if (!value.equals(search)) {
            search = value;
            if (!value.isEmpty()) page = null;
            resetScroll();
        }
    }

    private void openPage(Module m) {
        page = m;
        openDropdown = null;
        focusedText = null;
        resetScroll();
    }

    private void closePage() {
        if (page != null && search.isEmpty() && !favorites) {
            category = page.getCategory();
        }
        page = null;
        openDropdown = null;
        focusedText = null;
        resetScroll();
    }

    private void resetScroll() {
        scroll = 0;
        scrollAnim.snap(0);
    }

    private void addHit(float x, float y, float w, float h, Action action) {
        float[] clip = clips.peek();
        if (clip != null) {
            float x2 = Math.min(x + w, clip[0] + clip[2]);
            float y2 = Math.min(y + h, clip[1] + clip[3]);
            x = Math.max(x, clip[0]);
            y = Math.max(y, clip[1]);
            w = x2 - x;
            h = y2 - y;
            if (w <= 0 || h <= 0) return;
        }
        hits.add(new Hit(x, y, w, h, action));
    }

    private boolean hovered(float x, float y, float w, float h) {
        if (closing) return false;
        float[] clip = clips.peek();
        if (clip != null && (mouseX < clip[0] || mouseX >= clip[0] + clip[2]
                || mouseY < clip[1] || mouseY >= clip[1] + clip[3])) {
            return false;
        }
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /** Hover amount in [0,1] that fades in/out instead of switching instantly. */
    private float hover(String key, float x, float y, float w, float h) {
        boolean over = hovered(x, y, w, h);
        return anim("hover:" + key, HOVER_S, false).update(over ? 1f : 0f);
    }

    /**
     * Pushes a clip in local units (intersected with the enclosing clip); the renderer scissor gets
     * the equivalent GUI-coordinate rectangle, including the open-animation transform.
     */
    private void pushClip(Renderer r, float x, float y, float w, float h) {
        float[] top = clips.peek();
        if (top != null) {
            float x2 = Math.min(x + w, top[0] + top[2]);
            float y2 = Math.min(y + h, top[1] + top[3]);
            x = Math.max(x, top[0]);
            y = Math.max(y, top[1]);
            w = Math.max(0, x2 - x);
            h = Math.max(0, y2 - y);
        }
        clips.push(new float[]{x, y, w, h});
        r.pushScissor((originX + x * scale) * unit, (originY + y * scale) * unit, w * scale * unit, h * scale * unit);
    }

    private void popClip(Renderer r) {
        clips.pop();
        r.popScissor();
    }

    /** Per-widget animation, created at its current state so nothing animates on first show. */
    private Animation anim(String key, float duration, boolean initiallyOn) {
        return anim(key, duration, initiallyOn ? 1f : 0f);
    }

    private Animation anim(String key, float duration, float initial) {
        Animation a = anims.get(key);
        if (a == null) {
            a = new Animation(duration, initial);
            anims.put(key, a);
        }
        return a;
    }

    private static String key(Setting<?> s) {
        return Integer.toHexString(System.identityHashCode(s));
    }
}
