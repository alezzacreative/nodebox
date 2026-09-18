package nodebox.client;

import com.google.common.collect.ImmutableList;
import nodebox.graphics.Path;
import nodebox.graphics.Point;
import nodebox.graphics.Rect;
import org.junit.Test;

import java.awt.event.MouseEvent;

import static org.junit.Assert.*;

public class ViewerSelectionTest {

    @Test
    public void testOutputGeometryBoundsCalculation() {
        Viewer viewer = new Viewer();
        assertNull("Empty viewer should have null geometry bounds", viewer.getOutputGeometryBounds());

        Path rectPath = new Path();
        rectPath.rect(-50, -30, 100, 60);

        viewer.setOutputValues(ImmutableList.of(rectPath.asGeometry()));
        Rect bounds = viewer.getOutputGeometryBounds();
        assertNotNull("Bounds should not be null when geometry is present", bounds);
        assertEquals(-100.0, bounds.getX(), 0.001);
        assertEquals(-60.0, bounds.getY(), 0.001);
        assertEquals(100.0, bounds.getWidth(), 0.001);
        assertEquals(60.0, bounds.getHeight(), 0.001);
    }

    @Test
    public void testSelectionGizmoVisibilityToggle() {
        Viewer viewer = new Viewer();
        assertTrue("Selection gizmo is shown by default", viewer.isShowSelectionGizmo());

        viewer.setShowSelectionGizmo(false);
        assertFalse("Selection gizmo can be toggled off", viewer.isShowSelectionGizmo());

        viewer.setShowSelectionGizmo(true);
        assertTrue("Selection gizmo can be toggled on", viewer.isShowSelectionGizmo());
    }

    @Test
    public void testEmptySpaceClickDeselects() {
        Viewer viewer = new Viewer();
        Path rectPath = new Path();
        rectPath.rect(0, 0, 50, 50);
        viewer.setOutputValues(ImmutableList.of(rectPath.asGeometry()));
        viewer.setSize(600, 600);

        // Simulate click far outside shape bounds (e.g. at 500, 500)
        MouseEvent clickOutside = new MouseEvent(
                viewer, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                MouseEvent.BUTTON1_DOWN_MASK, 500, 500, 1, false, MouseEvent.BUTTON1);

        viewer.mousePressed(clickOutside);
        assertFalse("Clicking on empty canvas space should hide selection gizmo", viewer.isShowSelectionGizmo());
    }
}