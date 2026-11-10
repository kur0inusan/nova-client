package dev.kuroinu.nova.render.font;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Glyphs of one face at one pixel size, rasterized on demand with AWT into 512x512 ARGB pages
 * (white, coverage in alpha). Version renderers upload a page whenever its {@link Page#version}
 * changes and draw textured quads. Characters the face lacks (★, ←, CJK...) come from the
 * platform's "Dialog" font. Render-thread only.
 */
public final class GlyphAtlas {
    public static final int PAGE_SIZE = 512;
    private static final int PAD = 1;

    private static final Map<FontFace, Font> BASE_FONTS = new EnumMap<FontFace, Font>(FontFace.class);
    private static final Map<String, GlyphAtlas> CACHE = new HashMap<String, GlyphAtlas>();

    public static final class Page {
        public final int[] pixels = new int[PAGE_SIZE * PAGE_SIZE];
        /** Incremented on every change; renderers re-upload when it differs from what they uploaded. */
        public int version;
        int cursorX = PAD;
        int cursorY = PAD;
        int rowHeight;
    }

    public final int pixelSize;
    /** Pixels from the top of the line box to the baseline. */
    public final float ascent;
    public final float descent;
    private final Font font;
    private final Font fallback;
    private final FontRenderContext frc = new FontRenderContext(null, true, true);
    private final List<Page> pages = new ArrayList<Page>();
    private final Map<Integer, Glyph> glyphs = new HashMap<Integer, Glyph>();

    private GlyphAtlas(FontFace face, int pixelSize) {
        this.pixelSize = pixelSize;
        this.font = baseFont(face).deriveFont((float) pixelSize);
        this.fallback = new Font(Font.DIALOG, font.isBold() ? Font.BOLD : Font.PLAIN, pixelSize);
        LineMetrics lm = font.getLineMetrics("Hgy", frc);
        this.ascent = lm.getAscent();
        this.descent = lm.getDescent();
    }

    /** Atlas for a face at a physical pixel size (clamped to a sane range). */
    public static GlyphAtlas get(FontFace face, int pixelSize) {
        int px = Math.max(4, Math.min(160, pixelSize));
        String key = face.name() + ":" + px;
        GlyphAtlas atlas = CACHE.get(key);
        if (atlas == null) {
            atlas = new GlyphAtlas(face, px);
            CACHE.put(key, atlas);
        }
        return atlas;
    }

    private static synchronized Font baseFont(FontFace face) {
        Font f = BASE_FONTS.get(face);
        if (f == null) {
            InputStream in = GlyphAtlas.class.getResourceAsStream(face.resourcePath);
            try {
                if (in == null) {
                    throw new IllegalStateException("Missing font resource " + face.resourcePath);
                }
                f = Font.createFont(Font.TRUETYPE_FONT, in);
            } catch (Exception e) {
                f = new Font(Font.SANS_SERIF, Font.PLAIN, 12); // keep the game running with a system font
            } finally {
                try {
                    if (in != null) in.close();
                } catch (Exception ignored) {
                }
            }
            BASE_FONTS.put(face, f);
        }
        return f;
    }

    public float lineHeight() {
        return ascent + descent;
    }

    public float advance(String text) {
        float w = 0;
        for (int i = 0; i < text.length(); i++) {
            w += glyph(text.charAt(i)).advance;
        }
        return w;
    }

    public Glyph glyph(char c) {
        Integer key = (int) c;
        Glyph g = glyphs.get(key);
        if (g == null) {
            g = rasterize(c);
            glyphs.put(key, g);
        }
        return g;
    }

    private Glyph rasterize(char c) {
        Font f = font.canDisplay(c) ? font : fallback;
        GlyphVector gv = f.createGlyphVector(frc, String.valueOf(c));
        float advance = gv.getGlyphMetrics(0).getAdvanceX();
        Rectangle bounds = gv.getPixelBounds(frc, 0, 0);
        if (bounds.width <= 0 || bounds.height <= 0) {
            return new Glyph(null, 0, 0, 0, 0, 0, 0, advance);
        }
        int w = bounds.width + 2;
        int h = bounds.height + 2;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(java.awt.Color.WHITE);
        g.drawGlyphVector(gv, 1 - bounds.x, 1 - bounds.y);
        g.dispose();

        Page page = place(w, h);
        int px = page.cursorX;
        int py = page.cursorY;
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            img.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                int alpha = row[x] >>> 24;
                page.pixels[(py + y) * PAGE_SIZE + px + x] = (alpha << 24) | 0xFFFFFF;
            }
        }
        page.cursorX += w + PAD;
        page.rowHeight = Math.max(page.rowHeight, h);
        page.version++;
        return new Glyph(page, px, py, w, h, bounds.x - 1, bounds.y - 1, advance);
    }

    /** Finds room for a w*h glyph, starting a new row or page when needed. */
    private Page place(int w, int h) {
        Page page = pages.isEmpty() ? null : pages.get(pages.size() - 1);
        if (page != null && page.cursorX + w + PAD > PAGE_SIZE) {
            page.cursorX = PAD;
            page.cursorY += page.rowHeight + PAD;
            page.rowHeight = 0;
        }
        if (page == null || page.cursorY + h + PAD > PAGE_SIZE) {
            page = new Page();
            pages.add(page);
        }
        return page;
    }
}
