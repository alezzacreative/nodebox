package nodebox.graphics;

import org.junit.Test;

import static org.junit.Assert.*;

public class ShapeMorpherTest {

    @Test
    public void testMorph() {
        Path square = new Path();
        square.rect(0, 0, 100, 100);

        Path circle = new Path();
        circle.ellipse(0, 0, 100, 100);

        // At progress 0.0 -> square
        Path p0 = ShapeMorpher.morph(square, circle, 0.0, 60);
        assertNotNull(p0);

        // At progress 1.0 -> circle
        Path p1 = ShapeMorpher.morph(square, circle, 1.0, 60);
        assertNotNull(p1);

        // At progress 0.5 -> intermediate
        Path pMid = ShapeMorpher.morph(square, circle, 0.5, 60);
        assertNotNull(pMid);
        assertTrue("Morphed path should have vertices", pMid.getPointCount() >= 50);

        Rect b = pMid.getBounds();
        assertFalse(Double.isNaN(b.getX()));
        assertFalse(Double.isNaN(b.getY()));
        assertTrue("Bounding box should be positive", b.getWidth() > 0 && b.getHeight() > 0);
    }
}
