package nodebox.util;

import nodebox.ui.Platform;

import java.awt.*;
import java.awt.ImageCapabilities;
import java.awt.image.VolatileImage;

/**
 * Utility class for GPU hardware acceleration detection, pipeline inspection,
 * and VRAM-backed VolatileImage surface creation.
 */
public final class GPUUtils {

    private GPUUtils() {
    }

    /**
     * Apply low-level Java2D hardware pipeline system properties.
     * Must be invoked as early as possible during application launch.
     *
     * @param enableGpu true to enable GPU hardware acceleration pipelines; false for software fallback.
     */
    public static void applyGpuPipelineProperties(boolean enableGpu) {
        if (enableGpu) {
            if (Platform.onWindows()) {
                System.setProperty("sun.java2d.d3d", "true");
                System.setProperty("sun.java2d.transaccel", "true");
                System.setProperty("sun.java2d.ddoffscreen", "true");
            } else if (Platform.onMac()) {
                System.setProperty("sun.java2d.metal", "true");
                System.setProperty("sun.java2d.opengl", "true");
            } else {
                System.setProperty("sun.java2d.opengl", "true");
            }
            System.setProperty("sun.java2d.pmoffscreen", "true");
            System.setProperty("sun.java2d.noddraw", "false");
        } else {
            System.setProperty("sun.java2d.d3d", "false");
            System.setProperty("sun.java2d.opengl", "false");
            System.setProperty("sun.java2d.metal", "false");
            System.setProperty("sun.java2d.noddraw", "true");
        }
    }

    /**
     * Detects human-readable pipeline description and acceleration status.
     *
     * @return a descriptive string of the current graphics pipeline.
     */
    public static String getPipelineDescription() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            if (ge.isHeadlessInstance()) {
                return "Headless (No Display Device)";
            }
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();

            StringBuilder sb = new StringBuilder();
            if (Platform.onWindows()) {
                String d3d = System.getProperty("sun.java2d.d3d", "true");
                if ("true".equalsIgnoreCase(d3d)) {
                    sb.append("Direct3D Hardware Accelerated");
                } else {
                    sb.append("Software (GDI / CPU Rasterizer)");
                }
            } else if (Platform.onMac()) {
                String metal = System.getProperty("sun.java2d.metal", "true");
                if ("true".equalsIgnoreCase(metal)) {
                    sb.append("Metal Hardware Accelerated");
                } else {
                    sb.append("OpenGL Hardware Accelerated");
                }
            } else {
                String ogl = System.getProperty("sun.java2d.opengl", "false");
                if ("true".equalsIgnoreCase(ogl)) {
                    sb.append("OpenGL Hardware Accelerated");
                } else {
                    sb.append("Software (X11 / CPU Rasterizer)");
                }
            }

            // Check if offscreen surfaces are accelerated on this configuration
            ImageCapabilities caps = gc.getImageCapabilities();
            if (caps.isAccelerated()) {
                sb.append(" [VRAM Ready]");
            }
            return sb.toString();
        } catch (Throwable t) {
            return "Standard Java2D (Marlin CPU Rasterizer)";
        }
    }

    /**
     * Creates a VRAM-backed VolatileImage compatible with the given GraphicsConfiguration.
     *
     * @param gc     the target GraphicsConfiguration.
     * @param width  surface width in pixels (must be &gt; 0).
     * @param height surface height in pixels (must be &gt; 0).
     * @return a new VolatileImage in GPU memory, or null if creation failed.
     */
    public static VolatileImage createAcceleratedSurface(GraphicsConfiguration gc, int width, int height) {
        if (gc == null || width <= 0 || height <= 0) return null;
        try {
            return gc.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
        } catch (Throwable t) {
            try {
                return gc.createCompatibleVolatileImage(width, height);
            } catch (Throwable t2) {
                return null;
            }
        }
    }

    /**
     * Checks whether an image is hardware-accelerated in video memory.
     *
     * @param img the image to inspect.
     * @param gc  the graphics configuration.
     * @return true if stored in GPU VRAM, false otherwise.
     */
    public static boolean isAccelerated(Image img, GraphicsConfiguration gc) {
        if (img == null || gc == null) return false;
        try {
            ImageCapabilities caps = img.getCapabilities(gc);
            return caps != null && caps.isAccelerated();
        } catch (Throwable t) {
            return false;
        }
    }
}
