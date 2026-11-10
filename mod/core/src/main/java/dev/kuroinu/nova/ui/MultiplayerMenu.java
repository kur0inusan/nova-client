package dev.kuroinu.nova.ui;

import dev.kuroinu.nova.NovaClient;
import dev.kuroinu.nova.module.impl.ClickGuiSettings;
import dev.kuroinu.nova.platform.ServerBrowser;
import dev.kuroinu.nova.render.AlphaRenderer;
import dev.kuroinu.nova.render.Colors;
import dev.kuroinu.nova.render.FormattedText;
import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.render.font.NovaFont;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Nova's server list, in the ClickGUI window style: favicon, name, colored MOTD, player count and
 * a 5-bar ping indicator per server; hover reveals reorder/edit/delete; add, edit, direct connect
 * and delete confirmation are in-screen dialogs.
 */
public final class MultiplayerMenu extends NovaScreen {
    private static final NovaFont TITLE = NovaFont.semibold(22);
    private static final NovaFont SUB = NovaFont.regular(13);
    private static final NovaFont BUTTON = NovaFont.semibold(13);
    private static final NovaFont NAME = NovaFont.semibold(16);
    private static final NovaFont MOTD = NovaFont.regular(13);
    private static final NovaFont MOTD_BOLD = NovaFont.semibold(13);
    private static final NovaFont MONO_11 = NovaFont.mono(11);
    private static final NovaFont MONO_12 = NovaFont.monoSemibold(12);
    private static final NovaFont ICON = NovaFont.regular(15);
    private static final NovaFont INITIAL = NovaFont.bold(20);
    private static final NovaFont MODAL_TITLE = NovaFont.semibold(18);
    private static final NovaFont LABEL = NovaFont.regular(13);
    private static final NovaFont BODY = NovaFont.regular(14);
    private static final NovaFont FIELD = NovaFont.mono(13);

    private static final float ROW_H = 72;
    private static final float ROW_GAP = 8;
    private static final float FOOTER_H = 68;
    private static final float RIGHT_COL_W = 150;
    private static final int GOOD = 0xFF8EDFA6, FAIR = 0xFFE4C95B, POOR = 0xFFE49900, BAD = 0xFFFF7C80;
    private static final int DANGER = 0xFFFF7C80;

    private enum Modal {ADD, EDIT, DIRECT, DELETE}

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

    private final ServerBrowser browser = NovaClient.get().platform().serverBrowser();
    private final List<Hit> hits = new ArrayList<Hit>();
    private final List<Hit> modalHits = new ArrayList<Hit>();
    private final Map<String, Animation> anims = new HashMap<String, Animation>();
    private final Animation openAnim = new Animation(0.14f, 0f);
    private final Animation modalAnim = new Animation(0.1f, 0f);
    private final Animation scrollAnim = new Animation(0.08f, 0f);

    private final TextField nameField = new TextField(FIELD, "Minecraft Server", 48);
    private final TextField addressField = new TextField(FIELD, "play.example.net", 128);

    private int selected = -1;
    private int lastClickIndex = -1;
    private long lastClickTime;
    private float scroll, maxScroll;
    private long listShownAt;

    private Modal modal;
    private Modal closingModal;
    private int modalIndex = -1;

    private boolean closing;
    private Runnable afterClose;

    private float unit = 1f, scale = 1f, originX, originY;
    private float mouseX, mouseY, screenMouseX, screenMouseY;
    private int accent;

    // ================================================================== lifecycle

    @Override
    public void onOpen() {
        openAnim.snap(0f);
        start();
    }

    @Override
    public void onResume() {
        openAnim.snap(0f); // back from a failed/cancelled connection: fade in again
        closing = false;
        start();
    }

    private void start() {
        listShownAt = System.currentTimeMillis();
        if (browser != null) browser.open();
    }

    @Override
    public void onClose() {
        if (browser != null) browser.close();
    }

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
        NovaScreen parent = getParent();
        if (parent != null) {
            parent.init(width, height);
            parent.renderAsBackdrop(base, 0f, delta); // main menu background only
        }
        float t = openAnim.update(closing ? 0f : 1f);
        if (closing && t < 0.02f) {
            Runnable then = afterClose;
            closing = false;
            if (then != null) then.run();
            else NovaClient.get().platform().openScreen(parent);
            return;
        }
        if (browser != null) browser.tick();
        accent = Theme.accent();
        hits.clear();
        modalHits.clear();
        float e = Easing.outCubic(t);

        AlphaRenderer r = new AlphaRenderer(base);
        r.setAlpha(e);
        r.rect(0, 0, width, height, Theme.OVERLAY);
        ClickGuiSettings gui = NovaClient.get().modules().get(ClickGuiSettings.class);
        float[] units = UiScale.push(base, width, height, gui == null ? 1f : gui.scale.floatValue());
        try {
            float sw = units[0], sh = units[1];
            unit = units[2];
            scale = 0.96f + 0.04f * e;
            originX = sw / 2f * (1 - scale);
            originY = sh / 2f * (1 - scale);
            screenMouseX = guiMouseX / unit;
            screenMouseY = guiMouseY / unit;
            boolean modalOpen = modal != null;
            mouseX = modalOpen ? -1e5f : (screenMouseX - originX) / scale;
            mouseY = modalOpen ? -1e5f : (screenMouseY - originY) / scale;

            float ww = Math.round(Math.min(940, sw * 0.94f));
            float wh = Math.round(Math.min(600, sh * 0.88f));
            float wx = Math.round((sw - ww) / 2f);
            float wy = Math.round((sh - wh) / 2f);

            r.pushScale(originX, originY, scale);
            try {
                drawShadow(r, wx, wy, ww, wh);
                r.roundedRect(wx, wy, ww, wh, 16, Theme.WINDOW);
                drawHeader(r, wx, wy, ww);
                float headerH = 22 + r.lineHeight(TITLE) + 18;
                drawList(r, wx, wy + headerH, ww, wh - headerH - FOOTER_H);
                drawFooter(r, wx, wy + wh - FOOTER_H, ww);
                r.roundedOutline(wx, wy, ww, wh, 16, 1, Theme.BORDER_8);
            } finally {
                r.pop();
            }
            drawModal(base, sw, sh);
        } finally {
            base.pop();
        }
    }

    private static void drawShadow(Renderer r, float x, float y, float w, float h) {
        for (int i = 8; i >= 1; i--) {
            float spread = i * 8f;
            float a = 0.5f * (1f - i / 9f) / 8 * 1.6f;
            r.roundedRect(x - spread + 4, y - spread + 30f * i / 8, w + spread * 2 - 8, h + spread * 2 - 16,
                    16 + spread, Colors.argb(Math.round(a * 255), 0, 0, 0));
        }
    }

    // ================================================================== header & footer

    private void drawHeader(Renderer r, float wx, float wy, float ww) {
        float x = wx + 26, y = wy + 22;
        r.text(TITLE, "Multiplayer", x, y, Theme.TEXT);
        int count = browser == null ? 0 : browser.servers().size();
        String sub = count + " server" + (count == 1 ? "" : "s");
        float titleLh = r.lineHeight(TITLE);
        r.text(SUB, sub, x + r.textWidth(TITLE, "Multiplayer") + 12, y + titleLh * 0.78f - r.lineHeight(SUB) * 0.78f, Theme.MUTED_2);

        if (browser == null) return;
        float bh = 34, by = y + titleLh / 2f - bh / 2f;
        float right = wx + ww - 26;
        float addW = r.textWidth(BUTTON, "Add Server") + 28;
        button(r, "add", "Add Server", true, right - addW, by, addW, bh, b -> openModal(Modal.ADD, -1));
        right -= addW + 8;
        float dcW = r.textWidth(BUTTON, "Direct Connect") + 28;
        button(r, "direct", "Direct Connect", false, right - dcW, by, dcW, bh, b -> openModal(Modal.DIRECT, -1));
        right -= dcW + 8;
        iconButton(r, "refresh", "↻", right - bh, by, bh, Theme.TEXT_2, b -> {
            browser.refresh();
            listShownAt = System.currentTimeMillis();
        });
    }

    private void drawFooter(Renderer r, float wx, float fy, float ww) {
        r.rect(wx, fy, ww, 1, Theme.BORDER_6);
        float bh = 38, by = fy + (FOOTER_H - bh) / 2f;
        float backW = r.textWidth(BUTTON, "← Back") + 28;
        button(r, "back", "← Back", false, wx + 26, by, backW, bh, b -> close());

        boolean canJoin = browser != null && selected >= 0 && selected < browser.servers().size();
        float joinW = r.textWidth(BUTTON, "Join Server") + 40;
        AlphaRenderer dim = new AlphaRenderer(r);
        dim.setAlpha(canJoin ? 1f : 0.4f);
        button(dim, "join", "Join Server", true, wx + ww - 26 - joinW, by, joinW, bh, canJoin ? b -> join(selected) : null);

        if (canJoin) {
            String addr = browser.servers().get(selected).address;
            r.textCenteredV(MONO_11, addr, wx + 26 + backW + 16, fy + FOOTER_H / 2f, Theme.MUTED_2);
        }
    }

    // ================================================================== list

    private void drawList(Renderer r, float wx, float viewY, float ww, float viewH) {
        float x = wx + 26, w = ww - 52;
        if (browser == null) {
            r.text(BODY, "The server list isn't available on this Minecraft version yet.", x, viewY + 30, Theme.MUTED_2);
            return;
        }
        List<ServerBrowser.Server> servers = browser.servers();
        if (selected >= servers.size()) selected = -1;
        pushClip(r, wx, viewY, ww, viewH);
        float offset = scrollAnim.update(scroll);
        float y = viewY + 4 - offset;
        float top = y;
        try {
            if (servers.isEmpty()) {
                r.text(BODY, "No servers yet. Add one, or use Direct Connect.", x, viewY + 30, Theme.MUTED_2);
            }
            for (int i = 0; i < servers.size(); i++) {
                float ct = Easing.since(listShownAt, i * 30L, 200);
                AlphaRenderer row = new AlphaRenderer(r);
                row.setAlpha(ct);
                drawRow(row, i, servers.get(i), servers.size(), x, y + (1 - ct) * 8, w, viewY, viewH);
                y += ROW_H + ROW_GAP;
            }
        } finally {
            popClip(r);
        }
        float contentH = y - top + 22;
        maxScroll = Math.max(0, contentH - viewH);
        if (scroll > maxScroll) scroll = maxScroll;
        if (scrollAnim.get() > maxScroll) scrollAnim.snap(maxScroll);
        if (contentH > viewH + 0.5f) {
            float barH = Math.max(24, viewH * viewH / contentH);
            float barY = viewY + (viewH - barH) * (maxScroll == 0 ? 0 : Math.min(1, offset / maxScroll));
            r.roundedRect(wx + ww - 6, barY + 4, 3, barH - 8, 1.5f, 0x26FFFFFF);
        }
    }

    private void drawRow(Renderer r, final int index, ServerBrowser.Server s, int count, float x, float y, float w,
                         float viewY, float viewH) {
        boolean sel = index == selected;
        boolean over = hovered(x, y, w, ROW_H) && y + ROW_H > viewY && y < viewY + viewH;
        float hv = anim("row:" + index, 0.08f).update(over ? 1f : 0f);
        float st = anim("sel:" + index, 0.12f).update(sel ? 1f : 0f);
        r.roundedRect(x, y, w, ROW_H, 12, Colors.lerp(Colors.lerp(Theme.CARD, 0xE6161620, hv), Theme.CARD_ON, st));
        r.roundedOutline(x, y, w, ROW_H, 12, 1, Colors.lerp(Colors.lerp(Theme.BORDER_6, 0x1FFFFFFF, hv), accent, st));
        addHit(x, y, w, ROW_H, b -> {
            long now = System.currentTimeMillis();
            if (b == 0 && index == lastClickIndex && now - lastClickTime < 350) {
                join(index); // double click
            }
            selected = index;
            lastClickIndex = index;
            lastClickTime = now;
        });

        // Favicon or placeholder initial.
        float ix = x + 12, iy = y + 12, is = 48;
        r.roundedRect(ix, iy, is, is, 8, 0xFF1A1B22);
        if (!browser.drawIcon(index, ix, iy, is)) {
            String plain = FormattedText.strip(s.name).trim();
            r.textCentered(INITIAL, plain.isEmpty() ? "?" : plain.substring(0, 1).toUpperCase(Locale.ROOT),
                    ix + is / 2f, iy + is / 2f, Theme.MUTED);
        }

        // Name + MOTD, clipped to the text column.
        float tx = ix + is + 14;
        float rightX = x + w - 14 - RIGHT_COL_W;
        pushClip(r, tx, y, rightX - 10 - tx, ROW_H);
        try {
            float nameLh = r.lineHeight(NAME), motdLh = r.lineHeight(MOTD);
            float ty = y + (ROW_H - nameLh - motdLh * 2) / 2f;
            FormattedText.draw(r, NAME, NAME, s.name, tx, ty, Theme.TEXT);
            String[] lines = (s.motd == null ? "" : s.motd).split("\n");
            for (int i = 0; i < Math.min(2, lines.length); i++) {
                FormattedText.draw(r, MOTD, MOTD_BOLD, lines[i], tx, ty + nameLh + i * motdLh, Theme.MUTED);
            }
        } finally {
            popClip(r);
        }

        // Right column: status (players + ping) crossfades to row actions on hover.
        float actions = anim("actions:" + index, 0.1f).update(over ? 1f : 0f);
        float cy = y + ROW_H / 2f;
        if (actions < 0.99f) {
            AlphaRenderer status = new AlphaRenderer(r);
            status.setAlpha(1 - actions);
            drawStatus(status, s, x + w - 14, y);
        }
        if (actions > 0.01f) {
            AlphaRenderer act = new AlphaRenderer(r);
            act.setAlpha(actions);
            float bx = x + w - 14 - 30;
            iconButton(act, "del:" + index, "✕", bx, cy - 15, 30, DANGER, over ? b -> openModal(Modal.DELETE, index) : null);
            bx -= 34;
            iconButton(act, "edit:" + index, "✎", bx, cy - 15, 30, Theme.TEXT_2, over ? b -> openModal(Modal.EDIT, index) : null);
            bx -= 34;
            if (index < count - 1) {
                iconButton(act, "down:" + index, "▼", bx, cy - 15, 30, Theme.TEXT_2, over ? b -> move(index, index + 1) : null);
            }
            bx -= 34;
            if (index > 0) {
                iconButton(act, "up:" + index, "▲", bx, cy - 15, 30, Theme.TEXT_2, over ? b -> move(index, index - 1) : null);
            }
        }
    }

    /** Player count (top) and 5-bar ping with latency/status text (bottom), right-aligned at {@code right}. */
    private void drawStatus(Renderer r, ServerBrowser.Server s, float right, float y) {
        float topCy = y + 24, bottomCy = y + ROW_H - 24;
        if (s.status == ServerBrowser.Status.ONLINE || s.status == ServerBrowser.Status.OUTDATED) {
            String players = s.players == null ? "" : s.players;
            float pw = FormattedText.width(r, MONO_12, MONO_12, players);
            FormattedText.draw(r, MONO_12, MONO_12, players, right - pw, topCy - r.lineHeight(MONO_12) / 2f, Theme.TEXT_2);
        }

        float barsW = 5 * 3 + 4 * 2;
        float bx = right - barsW, baseY = bottomCy + 6;
        float time = (System.nanoTime() % 100_000_000_000L) / 1e9f;
        int bars = 0, color = Theme.DISABLED;
        String label;
        int labelColor = Theme.MUTED_2;
        switch (s.status) {
            case PINGING:
                label = "Pinging…";
                break;
            case UNREACHABLE:
                label = "Offline";
                labelColor = BAD;
                break;
            case OUTDATED:
                label = FormattedText.strip(s.version == null ? "Incompatible" : s.version);
                labelColor = BAD;
                break;
            default:
                long p = s.ping;
                bars = p < 150 ? 5 : p < 300 ? 4 : p < 600 ? 3 : p < 1000 ? 2 : 1;
                color = p < 150 ? GOOD : p < 300 ? FAIR : p < 600 ? POOR : BAD;
                label = p + " ms";
                break;
        }
        for (int i = 0; i < 5; i++) {
            float h = 4 + i * 2.5f;
            int c;
            if (s.status == ServerBrowser.Status.PINGING) {
                float wave = 0.5f + 0.5f * (float) Math.sin(time * 7 - i * 0.9f); // ripple while pinging
                c = Colors.withAlpha(Theme.MUTED, Math.round(255 * (0.25f + 0.6f * wave)));
            } else {
                c = i < bars ? color : 0x33FFFFFF;
            }
            r.roundedRect(bx + i * 5, baseY - h, 3, h, 1, c);
        }
        float lw = r.textWidth(MONO_11, label);
        r.textCenteredV(MONO_11, label, bx - 10 - lw, bottomCy, labelColor);
    }

    // ================================================================== modal dialogs

    private void openModal(Modal kind, int index) {
        modal = kind;
        modalIndex = index;
        modalAnim.snap(0f);
        nameField.setFocused(false);
        addressField.setFocused(false);
        if (kind == Modal.ADD) {
            nameField.set("");
            addressField.set("");
            nameField.setFocused(true);
        } else if (kind == Modal.EDIT) {
            ServerBrowser.Server s = browser.servers().get(index);
            nameField.set(FormattedText.strip(s.name));
            addressField.set(s.address);
            nameField.setFocused(true);
        } else if (kind == Modal.DIRECT) {
            addressField.set("");
            addressField.setFocused(true);
        }
    }

    private void closeModal() {
        closingModal = modal;
        modal = null;
    }

    private void drawModal(Renderer base, float sw, float sh) {
        Modal kind = modal != null ? modal : closingModal;
        float t = modalAnim.update(modal != null ? 1f : 0f);
        if (kind == null || t < 0.01f) {
            closingModal = null;
            return;
        }
        float e = Easing.outCubic(t);
        AlphaRenderer r = new AlphaRenderer(base);
        r.setAlpha(e);
        r.rect(0, 0, sw, sh, 0x8C000000);

        float w = 420, pad = 24;
        float fieldH = 38, labelH = r.lineHeight(LABEL);
        float fieldBlock = labelH + 6 + fieldH + 14;
        float contentH;
        String title;
        switch (kind) {
            case ADD: title = "Add Server"; contentH = fieldBlock * 2; break;
            case EDIT: title = "Edit Server"; contentH = fieldBlock * 2; break;
            case DIRECT: title = "Direct Connect"; contentH = fieldBlock; break;
            default: title = "Delete server?"; contentH = r.lineHeight(BODY) * 2 + 14; break;
        }
        float h = pad + r.lineHeight(MODAL_TITLE) + 18 + contentH + 8 + 38 + pad;
        float s = 0.96f + 0.04f * e;
        float x = (sw - w) / 2f, y = (sh - h) / 2f;
        r.pushScale(sw / 2f * (1 - s), sh / 2f * (1 - s), s);
        try {
            float mx = (screenMouseX - sw / 2f * (1 - s)) / s, my = (screenMouseY - sh / 2f * (1 - s)) / s;
            r.roundedRect(x, y, w, h, 16, 0xF50E0F14);
            r.roundedOutline(x, y, w, h, 16, 1, Theme.BORDER_8);
            float cy = y + pad;
            r.text(MODAL_TITLE, title, x + pad, cy, Theme.TEXT);
            cy += r.lineHeight(MODAL_TITLE) + 18;
            float fw = w - pad * 2;

            boolean canSubmit;
            String submit;
            boolean danger = false;
            if (kind == Modal.DELETE) {
                String name = modalIndex >= 0 && modalIndex < browser.servers().size()
                        ? FormattedText.strip(browser.servers().get(modalIndex).name) : "";
                r.text(BODY, r.trimToWidth(BODY, "“" + name + "”", fw), x + pad, cy, Theme.TEXT);
                r.text(BODY, "will be removed from your server list.", x + pad, cy + r.lineHeight(BODY), Theme.MUTED);
                canSubmit = true;
                submit = "Delete";
                danger = true;
            } else {
                if (kind != Modal.DIRECT) {
                    cy = field(r, "Server Name", nameField, x + pad, cy, fw, fieldH, mx, my);
                }
                cy = field(r, "Server Address", addressField, x + pad, cy, fw, fieldH, mx, my);
                canSubmit = !addressField.get().trim().isEmpty();
                submit = kind == Modal.DIRECT ? "Join" : kind == Modal.ADD ? "Add" : "Save";
            }

            float by = y + h - pad - 38;
            float sw2 = r.textWidth(BUTTON, submit) + 36;
            float sx = x + w - pad - sw2;
            AlphaRenderer dim = new AlphaRenderer(r);
            dim.setAlpha(canSubmit ? 1f : 0.4f);
            boolean sOver = canSubmit && mx >= sx && mx < sx + sw2 && my >= by && my < by + 38;
            float sh2 = anim("msubmit", 0.08f).update(sOver ? 1f : 0f);
            int bg = danger ? DANGER : accent;
            dim.roundedRect(sx, by, sw2, 38, 8, Colors.lerp(bg, Colors.lerp(bg, 0xFFFFFFFF, 0.12f), sh2));
            dim.textCentered(BUTTON, submit, sx + sw2 / 2f, by + 19, Theme.ON_ACCENT);
            if (canSubmit) modalHits.add(new Hit(sx, by, sw2, 38, b -> submitModal()));

            float cw = r.textWidth(BUTTON, "Cancel") + 28;
            float cx = sx - 8 - cw;
            boolean cOver = mx >= cx && mx < cx + cw && my >= by && my < by + 38;
            float ch = anim("mcancel", 0.08f).update(cOver ? 1f : 0f);
            if (ch > 0.01f) r.roundedRect(cx, by, cw, 38, 8, Colors.withAlpha(Theme.BORDER_6, Math.round(0x0F * ch)));
            r.roundedOutline(cx, by, cw, 38, 8, 1, Colors.lerp(Theme.BORDER_8, 0x2EFFFFFF, ch));
            r.textCentered(BUTTON, "Cancel", cx + cw / 2f, by + 19, Theme.TEXT_2);
            modalHits.add(new Hit(cx, by, cw, 38, b -> closeModal()));
            // Modal hits are in this (scaled) space; store the transform so clicks map correctly.
            modalScale = s;
            modalOriginX = sw / 2f * (1 - s);
            modalOriginY = sh / 2f * (1 - s);
        } finally {
            r.pop();
        }
    }

    private float modalScale = 1f, modalOriginX, modalOriginY;

    private float field(Renderer r, String label, final TextField f, float x, float y, float w, float h, float mx, float my) {
        r.text(LABEL, label, x, y, Theme.MUTED);
        float fy = y + r.lineHeight(LABEL) + 6;
        boolean over = mx >= x && mx < x + w && my >= fy && my < fy + h;
        f.draw(r, x, fy, w, h, over, accent);
        modalHits.add(new Hit(x, fy, w, h, b -> {
            nameField.setFocused(f == nameField);
            addressField.setFocused(f == addressField);
        }));
        return fy + h + 14;
    }

    private void submitModal() {
        if (modal == null) return;
        String name = nameField.get().trim().isEmpty() ? "Minecraft Server" : nameField.get().trim();
        String address = addressField.get().trim();
        switch (modal) {
            case ADD:
                if (address.isEmpty()) return;
                browser.add(name, address);
                selected = browser.servers().size() - 1;
                break;
            case EDIT:
                if (address.isEmpty()) return;
                browser.edit(modalIndex, name, address);
                break;
            case DIRECT:
                if (address.isEmpty()) return;
                closeModal();
                animateClose(() -> browser.directConnect(address));
                return;
            default:
                browser.remove(modalIndex);
                if (selected == modalIndex) selected = -1;
                else if (selected > modalIndex) selected--;
                break;
        }
        closeModal();
    }

    // ================================================================== actions

    private void join(final int index) {
        if (browser == null || index < 0 || index >= browser.servers().size()) return;
        animateClose(() -> browser.join(index));
    }

    private void move(int from, int to) {
        browser.swap(from, to);
        if (selected == from) selected = to;
        else if (selected == to) selected = from;
        // Reset per-row hover/selection animations for the two rows that swapped places.
        anims.remove("row:" + from);
        anims.remove("row:" + to);
        anims.remove("sel:" + from);
        anims.remove("sel:" + to);
    }

    // ================================================================== input

    @Override
    public boolean mouseClicked(double gx, double gy, int button) {
        if (closing) return true;
        if (modal != null) {
            float sx = (float) gx / unit, sy = (float) gy / unit;
            float x = (sx - modalOriginX) / modalScale, y = (sy - modalOriginY) / modalScale;
            nameField.setFocused(false);
            addressField.setFocused(false);
            for (int i = modalHits.size() - 1; i >= 0; i--) {
                if (modalHits.get(i).contains(x, y)) {
                    NovaClient.get().platform().playClickSound();
                    modalHits.get(i).action.run(button);
                    return true;
                }
            }
            return true;
        }
        float x = (float) ((gx / unit - originX) / scale), y = (float) ((gy / unit - originY) / scale);
        for (int i = hits.size() - 1; i >= 0; i--) {
            if (hits.get(i).contains(x, y)) {
                hits.get(i).action.run(button);
                return true;
            }
        }
        selected = -1;
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double amount) {
        if (!closing && modal == null) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (float) Math.signum(amount) * 48));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, UiKey key) {
        if (closing) return true;
        if (modal != null) {
            TextField focused = nameField.isFocused() ? nameField : addressField.isFocused() ? addressField : null;
            if (key == UiKey.ESCAPE) {
                closeModal();
            } else if (key == UiKey.ENTER) {
                submitModal();
            } else if (key == UiKey.TAB && modal != Modal.DIRECT && modal != Modal.DELETE) {
                boolean toAddress = !addressField.isFocused();
                nameField.setFocused(!toAddress);
                addressField.setFocused(toAddress);
            } else if (focused != null) {
                focused.key(key);
            }
            return key != UiKey.OTHER;
        }
        if (key == UiKey.ENTER && selected >= 0) {
            join(selected);
            return true;
        }
        if (key == UiKey.DELETE && selected >= 0) {
            openModal(Modal.DELETE, selected);
            return true;
        }
        return false; // ESC: animated close back to the main menu
    }

    @Override
    public void charTyped(char c) {
        if (modal == null) return;
        if (nameField.isFocused()) nameField.type(c);
        else if (addressField.isFocused()) addressField.type(c);
    }

    // ================================================================== widgets & helpers

    private void button(Renderer r, String key, String label, boolean primary, float x, float y, float w, float h, Action action) {
        boolean over = action != null && hovered(x, y, w, h);
        float hv = anim("btn:" + key, 0.08f).update(over ? 1f : 0f);
        if (primary) {
            r.roundedRect(x, y, w, h, 8, Colors.lerp(accent, Colors.lerp(accent, 0xFFFFFFFF, 0.12f), hv));
            r.textCentered(BUTTON, label, x + w / 2f, y + h / 2f, Theme.ON_ACCENT);
        } else {
            r.roundedRect(x, y, w, h, 8, Colors.lerp(Theme.INPUT, 0xFF1A1B22, hv));
            r.roundedOutline(x, y, w, h, 8, 1, Colors.lerp(Theme.BORDER_8, 0x2EFFFFFF, hv));
            r.textCentered(BUTTON, label, x + w / 2f, y + h / 2f, Colors.lerp(Theme.TEXT_2, Theme.TEXT, hv));
        }
        if (action != null) addHit(x, y, w, h, wrapClick(action));
    }

    private void iconButton(Renderer r, String key, String glyph, float x, float y, float size, int color, Action action) {
        boolean over = action != null && hovered(x, y, size, size);
        float hv = anim("ico:" + key, 0.08f).update(over ? 1f : 0f);
        r.roundedRect(x, y, size, size, 8, Colors.lerp(0x00FFFFFF, 0x1AFFFFFF, hv));
        r.roundedOutline(x, y, size, size, 8, 1, Colors.lerp(Theme.BORDER_6, 0x2EFFFFFF, hv));
        r.textCentered(ICON, glyph, x + size / 2f, y + size / 2f, Colors.lerp(color, Theme.TEXT, color == DANGER ? 0 : hv));
        if (action != null) addHit(x, y, size, size, wrapClick(action));
    }

    private static Action wrapClick(final Action action) {
        return b -> {
            NovaClient.get().platform().playClickSound();
            action.run(b);
        };
    }

    private final java.util.Deque<float[]> clips = new java.util.ArrayDeque<float[]>();

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
        if (clip != null && (mouseX < clip[0] || mouseX >= clip[0] + clip[2] || mouseY < clip[1] || mouseY >= clip[1] + clip[3])) {
            return false;
        }
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private Animation anim(String key, float duration) {
        Animation a = anims.get(key);
        if (a == null) {
            a = new Animation(duration, 0f);
            anims.put(key, a);
        }
        return a;
    }
}
