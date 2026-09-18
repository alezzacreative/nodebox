package nodebox.client;

import nodebox.util.GPUUtils;
import org.junit.Test;

import java.util.prefs.Preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GPUAccelerationTest {

    @Test
    public void testDefaultGpuAccelerationIsEnabled() {
        assertTrue("DEFAULT_GPU_ACCELERATION must be true by default", Application.DEFAULT_GPU_ACCELERATION);
        assertTrue("GPU_ACCELERATION constant must be true by default", Application.GPU_ACCELERATION);
        assertEquals("Preference key must match", "NBGpuAcceleration", Application.PREFERENCE_GPU_ACCELERATION);

        Preferences preferences = Preferences.userNodeForPackage(Application.class);
        boolean enabled = preferences.getBoolean(Application.PREFERENCE_GPU_ACCELERATION, Application.DEFAULT_GPU_ACCELERATION);
        assertTrue("Preferences default must be true", enabled);
    }

    @Test
    public void testPipelinePropertiesApplication() {
        GPUUtils.applyGpuPipelineProperties(true);
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            assertEquals("true", System.getProperty("sun.java2d.d3d"));
            assertEquals("true", System.getProperty("sun.java2d.transaccel"));
            assertEquals("true", System.getProperty("sun.java2d.ddoffscreen"));
        } else if (osName.contains("mac")) {
            assertEquals("true", System.getProperty("sun.java2d.metal"));
            assertEquals("true", System.getProperty("sun.java2d.opengl"));
        }

        GPUUtils.applyGpuPipelineProperties(false);
        if (osName.contains("win")) {
            assertEquals("false", System.getProperty("sun.java2d.d3d"));
        } else if (osName.contains("mac")) {
            assertEquals("false", System.getProperty("sun.java2d.metal"));
            assertEquals("false", System.getProperty("sun.java2d.opengl"));
        }

        // Restore to true for subsequent tests and normal execution
        GPUUtils.applyGpuPipelineProperties(true);
    }

    @Test
    public void testPipelineDescription() {
        String description = GPUUtils.getPipelineDescription();
        assertNotNull("Pipeline description should not be null", description);
        assertTrue("Pipeline description should not be empty", description.trim().length() > 0);
    }

    @Test
    public void testViewerGpuToggle() {
        Viewer viewer = new Viewer();
        assertTrue("Viewer GPU acceleration should be enabled by default", viewer.isGpuAcceleration());

        viewer.setGpuAcceleration(false);
        assertEquals(false, viewer.isGpuAcceleration());

        viewer.setGpuAcceleration(true);
        assertEquals(true, viewer.isGpuAcceleration());
    }

    @Test
    public void testViewerThemeBackgroundColor() {
        Viewer viewer = new Viewer();

        nodebox.ui.Theme.setTheme(nodebox.ui.Theme.THEME_DARK);
        viewer.updateTheme();
        assertEquals(nodebox.ui.Theme.VIEWER_BACKGROUND_COLOR, viewer.getBackground());

        nodebox.ui.Theme.setTheme(nodebox.ui.Theme.THEME_LIGHT);
        viewer.updateTheme();
        assertEquals(nodebox.ui.Theme.VIEWER_BACKGROUND_COLOR, viewer.getBackground());
    }

}
