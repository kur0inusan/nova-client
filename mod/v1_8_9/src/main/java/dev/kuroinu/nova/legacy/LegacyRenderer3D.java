package dev.kuroinu.nova.legacy;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.kuroinu.nova.render.Renderer3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/**
 * {@link Renderer3D} for 1.8.9. {@link #prepare} must run inside the world pass (camera transform
 * applied) and be paired with {@link #finish}. Used only on the render thread.
 */
public final class LegacyRenderer3D implements Renderer3D {
    public static final LegacyRenderer3D INSTANCE = new LegacyRenderer3D();

    private final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    private final float[] right = new float[3];
    private double cameraX;
    private double cameraY;
    private double cameraZ;

    private LegacyRenderer3D() {
    }

    /** Captures the camera for this frame and sets up translucent, unlit, untextured drawing. */
    public void prepare(Entity camera, float tickDelta) {
        // Same interpolation vanilla uses to offset entity rendering from the camera.
        cameraX = camera.prevTickX + (camera.x - camera.prevTickX) * tickDelta;
        cameraY = camera.prevTickY + (camera.y - camera.prevTickY) * tickDelta;
        cameraZ = camera.prevTickZ + (camera.z - camera.prevTickZ) * tickDelta;

        // The view's right axis in world space is the first row of the modelview rotation.
        modelView.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        right[0] = modelView.get(0);
        right[1] = modelView.get(4);
        right[2] = modelView.get(8);

        MinecraftClient.getInstance().gameRenderer.disableLightmap();
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture();
        GlStateManager.disableLighting();
        GlStateManager.disableAlphaTest(); // keep faint gradient edges
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    public void finish() {
        GL11.glLineWidth(1f);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepthTest();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
        GlStateManager.popMatrix();
    }

    @Override
    public double cameraX() {
        return cameraX;
    }

    @Override
    public double cameraY() {
        return cameraY;
    }

    @Override
    public double cameraZ() {
        return cameraZ;
    }

    @Override
    public float[] cameraRight() {
        return right.clone();
    }

    @Override
    public void setDepthTest(boolean enabled) {
        if (enabled) {
            GlStateManager.enableDepthTest();
        } else {
            GlStateManager.disableDepthTest();
        }
    }

    @Override
    public void begin(Mode mode, float lineWidth) {
        if (mode == Mode.LINES) {
            GL11.glLineWidth(lineWidth);
        }
        Tessellator.getInstance().getBuffer()
                .begin(mode == Mode.LINES ? GL11.GL_LINES : GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
    }

    @Override
    public void vertex(float x, float y, float z, int argb) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.vertex(x, y, z)
                .color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF)
                .next();
    }

    @Override
    public void end() {
        Tessellator.getInstance().draw();
    }
}
