package dev.kuroinu.nova.render;

/**
 * Immediate-mode drawing in the world, handed out with {@link dev.kuroinu.nova.event.Events.Render3D}.
 * Vertex coordinates are in blocks relative to the camera position (world position minus
 * {@link #cameraX()} etc.), which keeps float precision far from the origin.
 * Shapes are drawn translucent, unlit and without writing depth.
 */
public interface Renderer3D {
    enum Mode {
        TRIANGLES, LINES
    }

    double cameraX();

    double cameraY();

    double cameraZ();

    /** Unit vector pointing to the right of the view, in world space (for billboards). */
    float[] cameraRight();

    /** When false, shapes ignore the depth buffer and show through blocks and entities. */
    void setDepthTest(boolean enabled);

    /** Starts a batch; {@code lineWidth} (screen pixels) applies to {@link Mode#LINES}. */
    void begin(Mode mode, float lineWidth);

    void vertex(float x, float y, float z, int argb);

    void end();
}
