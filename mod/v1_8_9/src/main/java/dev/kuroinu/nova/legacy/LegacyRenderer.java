package dev.kuroinu.nova.legacy;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.kuroinu.nova.render.Renderer;
import dev.kuroinu.nova.render.font.Glyph;
import dev.kuroinu.nova.render.font.GlyphAtlas;
import dev.kuroinu.nova.render.font.NovaFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Window;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * {@link Renderer} for 1.8.9's fixed-function pipeline. Used only on the render thread.
 *
 * <p>Tracks the transform built with {@link #pushScale} (assumed to start from plain GUI
 * coordinates) so custom-font text can be rasterized at the exact physical pixel size and
 * snapped to the pixel grid.</p>
 */
public final class LegacyRenderer implements Renderer {
    public static final LegacyRenderer INSTANCE = new LegacyRenderer();

    private static final int CORNER_SEGMENTS = 10;

    /** {translateX, translateY, scale} from layout units to GUI coordinates. */
    private final Deque<float[]> transforms = new ArrayDeque<float[]>();
    private final Deque<float[]> scissors = new ArrayDeque<float[]>();
    private final Map<GlyphAtlas.Page, int[]> textures = new IdentityHashMap<GlyphAtlas.Page, int[]>();
    private final IntBuffer upload = BufferUtils.createIntBuffer(GlyphAtlas.PAGE_SIZE * GlyphAtlas.PAGE_SIZE);

    private LegacyRenderer() {
    }

    private static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    private static TextRenderer font() {
        return mc().textRenderer;
    }

    private static int guiScale() {
        return new Window(mc()).getScaleFactor();
    }

    private float[] transform() {
        float[] t = transforms.peek();
        return t != null ? t : new float[]{0, 0, 1};
    }

    // ------------------------------------------------------------------ shapes

    @Override
    public void rect(float x, float y, float width, float height, int argb) {
        if (isWhole(x) && isWhole(y) && isWhole(width) && isWhole(height)) {
            DrawableHelper.fill((int) x, (int) y, (int) (x + width), (int) (y + height), argb);
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(width, height, 1f);
        DrawableHelper.fill(0, 0, 1, 1, argb);
        GlStateManager.popMatrix();
    }

    @Override
    public void roundedRect(float x, float y, float width, float height, float radius, int argb) {
        radius = Math.min(radius, Math.min(width, height) / 2f);
        if (radius < 0.75f) {
            rect(x, y, width, height, argb);
            return;
        }
        beginColor(argb);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, VertexFormats.POSITION);
        buffer.vertex(x + width / 2f, y + height / 2f, 0).next();
        float[][] corners = corners(x, y, width, height, radius);
        for (float[] c : corners) {
            for (int i = 0; i <= CORNER_SEGMENTS; i++) {
                double a = Math.toRadians(c[2] + 90.0 * i / CORNER_SEGMENTS);
                buffer.vertex(c[0] + Math.cos(a) * radius, c[1] + Math.sin(a) * radius, 0).next();
            }
        }
        buffer.vertex(x + width - radius, y, 0).next();
        tessellator.draw();
        endColor();
    }

    @Override
    public void roundedOutline(float x, float y, float width, float height, float radius, float thickness, int argb) {
        radius = Math.min(radius, Math.min(width, height) / 2f);
        float inner = Math.max(0, radius - thickness);
        beginColor(argb);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, VertexFormats.POSITION);
        float[][] corners = corners(x, y, width, height, radius);
        double firstOx = 0, firstOy = 0, firstIx = 0, firstIy = 0;
        boolean first = true;
        for (float[] c : corners) {
            for (int i = 0; i <= CORNER_SEGMENTS; i++) {
                double a = Math.toRadians(c[2] + 90.0 * i / CORNER_SEGMENTS);
                double cos = Math.cos(a), sin = Math.sin(a);
                // Inner edge shares the corner center with a radius shrunk by the thickness (callers keep thickness <= radius).
                double ox = c[0] + cos * radius, oy = c[1] + sin * radius;
                double ix = c[0] + cos * inner, iy = c[1] + sin * inner;
                buffer.vertex(ox, oy, 0).next();
                buffer.vertex(ix, iy, 0).next();
                if (first) {
                    firstOx = ox;
                    firstOy = oy;
                    firstIx = ix;
                    firstIy = iy;
                    first = false;
                }
            }
        }
        buffer.vertex(firstOx, firstOy, 0).next();
        buffer.vertex(firstIx, firstIy, 0).next();
        tessellator.draw();
        endColor();
    }

    /** Corner arc centers clockwise from top-right with their start angles (screen Y points down). */
    private static float[][] corners(float x, float y, float w, float h, float r) {
        return new float[][]{
                {x + w - r, y + r, -90},
                {x + w - r, y + h - r, 0},
                {x + r, y + h - r, 90},
                {x + r, y + r, 180},
        };
    }

    private static void beginColor(int argb) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture();
        GlStateManager.disableCull();
        GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        setColor(argb);
    }

    private static void endColor() {
        GlStateManager.enableCull();
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void setColor(int argb) {
        GlStateManager.color(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f,
                ((argb >>> 24) & 0xFF) / 255f);
    }

    // ------------------------------------------------------------------ vanilla text

    @Override
    public void text(String text, float x, float y, int argb, boolean shadow) {
        GlStateManager.enableBlend();
        font().draw(text, x, y, argb, shadow);
    }

    @Override
    public int textWidth(String text) {
        return font().getStringWidth(text);
    }

    @Override
    public int fontHeight() {
        return font().fontHeight;
    }

    // ------------------------------------------------------------------ custom fonts

    private GlyphAtlas atlas(NovaFont font) {
        float pixelsPerUnit = transform()[2] * guiScale();
        return GlyphAtlas.get(font.face, Math.round(font.size * pixelsPerUnit));
    }

    @Override
    public float textWidth(NovaFont font, String text) {
        float pixelsPerUnit = transform()[2] * guiScale();
        return atlas(font).advance(text) / pixelsPerUnit;
    }

    @Override
    public float lineHeight(NovaFont font) {
        float pixelsPerUnit = transform()[2] * guiScale();
        return atlas(font).lineHeight() / pixelsPerUnit;
    }

    @Override
    public void text(NovaFont font, String text, float x, float y, int argb) {
        if (text.isEmpty() || (argb >>> 24) == 0) {
            return;
        }
        float[] t = transform();
        int gs = guiScale();
        GlyphAtlas atlas = atlas(font);

        // Work in absolute framebuffer pixels so glyph texels map 1:1 onto screen pixels.
        float penX = Math.round((t[0] + x * t[2]) * gs);
        float baseline = Math.round((t[1] + y * t[2]) * gs + atlas.ascent);

        GlStateManager.pushMatrix();
        GlStateManager.scale(1f / t[2], 1f / t[2], 1f);
        GlStateManager.translate(-t[0], -t[1], 0f);
        GlStateManager.scale(1f / gs, 1f / gs, 1f);

        GlStateManager.enableTexture();
        GlStateManager.enableBlend();
        GlStateManager.disableAlphaTest();
        GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        setColor(argb);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlyphAtlas.Page bound = null;
        float inv = 1f / GlyphAtlas.PAGE_SIZE;
        for (int i = 0; i < text.length(); i++) {
            Glyph g = atlas.glyph(text.charAt(i));
            if (!g.isEmpty()) {
                if (g.page != bound) {
                    if (bound != null) {
                        tessellator.draw();
                    }
                    bindPage(g.page);
                    bound = g.page;
                    buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);
                }
                float x0 = Math.round(penX + g.xOffset);
                float y0 = baseline + g.yOffset;
                float x1 = x0 + g.width;
                float y1 = y0 + g.height;
                float u0 = g.texX * inv, v0 = g.texY * inv;
                float u1 = (g.texX + g.width) * inv, v1 = (g.texY + g.height) * inv;
                buffer.vertex(x0, y0, 0).texture(u0, v0).next();
                buffer.vertex(x0, y1, 0).texture(u0, v1).next();
                buffer.vertex(x1, y1, 0).texture(u1, v1).next();
                buffer.vertex(x1, y0, 0).texture(u1, v0).next();
            }
            penX += g.advance;
        }
        if (bound != null) {
            tessellator.draw();
        }

        GlStateManager.enableAlphaTest();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }

    /** Binds a glyph page's GL texture, creating or re-uploading it when the page changed. */
    private void bindPage(GlyphAtlas.Page page) {
        int[] tex = textures.get(page); // {glId, uploadedVersion}
        if (tex == null) {
            tex = new int[]{GL11.glGenTextures(), -1};
            textures.put(page, tex);
            GlStateManager.bindTexture(tex[0]);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        } else {
            GlStateManager.bindTexture(tex[0]);
        }
        if (tex[1] != page.version) {
            upload.clear();
            upload.put(page.pixels);
            upload.flip();
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, GlyphAtlas.PAGE_SIZE, GlyphAtlas.PAGE_SIZE, 0,
                    GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, upload);
            tex[1] = page.version;
        }
    }

    // ------------------------------------------------------------------ transforms & clipping

    @Override
    public void pushScale(float x, float y, float scale) {
        float[] t = transform();
        transforms.push(new float[]{t[0] + x * t[2], t[1] + y * t[2], t[2] * scale});
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(scale, scale, 1f);
    }

    @Override
    public void pop() {
        transforms.poll();
        GlStateManager.popMatrix();
    }

    @Override
    public void pushScissor(float x, float y, float width, float height) {
        float[] top = scissors.peek();
        if (top != null) {
            float x2 = Math.min(x + width, top[0] + top[2]);
            float y2 = Math.min(y + height, top[1] + top[3]);
            x = Math.max(x, top[0]);
            y = Math.max(y, top[1]);
            width = Math.max(0, x2 - x);
            height = Math.max(0, y2 - y);
        }
        float[] rect = {x, y, width, height};
        scissors.push(rect);
        applyScissor(rect);
    }

    @Override
    public void popScissor() {
        scissors.pop();
        float[] top = scissors.peek();
        if (top == null) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            applyScissor(top);
        }
    }

    private static void applyScissor(float[] r) {
        int scale = guiScale();
        int windowHeight = mc().height;
        // glScissor works in framebuffer pixels with a bottom-left origin.
        int x = (int) Math.floor(r[0] * scale);
        int y = (int) Math.floor(windowHeight - (r[1] + r[3]) * scale);
        int w = (int) Math.ceil(r[2] * scale);
        int h = (int) Math.ceil(r[3] * scale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, Math.max(0, w), Math.max(0, h));
    }

    private static boolean isWhole(float v) {
        return v == (int) v;
    }
}
