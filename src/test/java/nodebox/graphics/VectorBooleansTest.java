package nodebox.graphics;

import org.junit.Test;

import static org.junit.Assert.*;

public class VectorBooleansTest {

    @Test
    public void testUnion() {
        Path r1 = new Path();
        r1.rect(0, 0, 100, 100);
        Path r2 = new Path();
        r2.rect(50, 0, 100, 100);

        IGeometry union = VectorBooleans.combine(r1, r2, "union");
        assertNotNull(union);
        Rect b = union.getBounds();
        assertEquals("Union width spans from 0 to 150 = 150", 150.0, b.getWidth(), 0.001);
        assertEquals("Union height is 100", 100.0, b.getHeight(), 0.001);
    }

    @Test
    public void testDifference() {
        Path r1 = new Path();
        r1.rect(0, 0, 100, 100);
        Path cutter = new Path();
        cutter.rect(50, 0, 100, 100);

        IGeometry diff = VectorBooleans.combine(r1, cutter, "difference");
        assertNotNull(diff);
        Rect b = diff.getBounds();
        assertEquals("Remaining width should be 50", 50.0, b.getWidth(), 0.001);
    }

    @Test
    public void testIntersection() {
        Path r1 = new Path();
        r1.rect(0, 0, 100, 100);
        Path r2 = new Path();
        r2.rect(50, 0, 100, 100);

        IGeometry inter = VectorBooleans.combine(r1, r2, "intersection");
        assertNotNull(inter);
        Rect b = inter.getBounds();
        assertEquals("Intersection overlap width is 50", 50.0, b.getWidth(), 0.001);
        assertEquals("Intersection x starts at 0", 0.0, b.getX(), 0.001);
    }

    @Test
    public void testXor() {
        Path r1 = new Path();
        r1.rect(0, 0, 100, 100);
        Path r2 = new Path();
        r2.rect(50, 0, 100, 100);

        IGeometry xor = VectorBooleans.combine(r1, r2, "xor");
        assertNotNull(xor);
        Rect b = xor.getBounds();
        assertEquals("XOR bounding width is 150", 150.0, b.getWidth(), 0.001);
    }
}
