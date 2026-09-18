package nodebox.function;

import nodebox.graphics.Geometry;
import nodebox.graphics.Path;
import nodebox.graphics.Point;
import nodebox.util.Duplicator;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DuplicatorTest {

    @Test
    public void testLinearDuplication() {
        Path rect = new Path();
        rect.rect(0, 0, 20, 20);

        List<Geometry> clones = Duplicator.duplicate(rect.asGeometry(), "linear",
                5, 50.0, 0.0,
                3, 3, 60.0, 60.0, 0.0,
                100.0, 0.0, 360.0, true,
                3.0, 137.5, null,
                0.0, 0.0, 1.0,
                0.0, 0.0, 0.0, 1);

        assertEquals("Should produce 5 linear clones", 5, clones.size());
        // Verify spacing along X axis
        assertEquals(-100.0, clones.get(0).getBounds().x + clones.get(0).getBounds().width / 2.0, 1e-3);
        assertEquals(0.0, clones.get(2).getBounds().x + clones.get(2).getBounds().width / 2.0, 1e-3);
        assertEquals(100.0, clones.get(4).getBounds().x + clones.get(4).getBounds().width / 2.0, 1e-3);
    }

    @Test
    public void testGridDuplication() {
        Path rect = new Path();
        rect.rect(0, 0, 10, 10);

        List<Geometry> clones = Duplicator.duplicate(rect.asGeometry(), "grid",
                1, 50.0, 0.0,
                3, 4, 40.0, 50.0, 0.5,
                100.0, 0.0, 360.0, true,
                3.0, 137.5, null,
                0.0, 0.0, 1.0,
                0.0, 0.0, 0.0, 1);

        assertEquals("Should produce 3x4 = 12 clones", 12, clones.size());
    }

    @Test
    public void testRadialDuplication() {
        Path rect = new Path();
        rect.rect(0, 0, 10, 10);

        List<Geometry> clones = Duplicator.duplicate(rect.asGeometry(), "radial",
                8, 50.0, 0.0,
                3, 3, 60.0, 60.0, 0.0,
                100.0, 0.0, 360.0, true,
                3.0, 137.5, null,
                0.0, 0.0, 1.0,
                0.0, 0.0, 0.0, 1);

        assertEquals("Should produce 8 radial clones", 8, clones.size());
    }

    @Test
    public void testSpiralDuplication() {
        Path circle = new Path();
        circle.ellipse(0, 0, 10, 10);

        List<Geometry> clones = Duplicator.duplicate(circle.asGeometry(), "spiral",
                30, 50.0, 0.0,
                3, 3, 60.0, 60.0, 0.0,
                100.0, 0.0, 360.0, true,
                2.5, 137.5, null,
                0.0, 0.0, 1.0,
                0.0, 0.0, 0.0, 1);

        assertEquals("Should produce 30 spiral clones", 30, clones.size());
    }

    @Test
    public void testStepTransformsAndJitter() {
        Path rect = new Path();
        rect.rect(0, 0, 20, 20);

        // Step rotation = 15 deg, Step scale = 10%, Jitter pos = 5
        List<Geometry> clones = CoreVectorFunctions.duplicator(rect.asGeometry(), "linear",
                6, 30.0, 0.0,
                3, 3, 60.0, 60.0, 0.0,
                100.0, 0.0, 360.0, false,
                3.0, 137.5,
                15.0, 10.0, 0.5,
                5.0, 10.0, 5.0, 42);

        assertEquals(6, clones.size());
        assertNotNull(clones.get(0));
        assertNotNull(clones.get(5));
    }

    @Test
    public void testDirectNdbxDuplicatorSignature() {
        Path rect = new Path();
        rect.rect(0, 0, 30, 30);

        // Call 22-argument duplicator directly mapped to corevector.ndbx
        List<Geometry> clones = CoreVectorFunctions.duplicator(rect.asGeometry(), "grid",
                9, 50.0, 0.0,
                3, 3, 50.0, 50.0, 0.0,
                100.0, 0.0, 360.0, true,
                3.0, 137.5,
                0.0, 0.0, 1.0,
                0.0, 0.0, 0.0, 1);

        assertEquals("Should produce 9 grid clones", 9, clones.size());
        assertNotNull(clones.get(0));
    }
}
