package nodebox.graphics;

import org.junit.Test;

import static org.junit.Assert.*;

public class StrokeStylerTest {

    @Test
    public void testDashedStroke() {
        Path line = new Path();
        line.line(0, 0, 200, 0);
        line.setStrokeWidth(2.0);

        Geometry dashed = StrokeStyler.apply(line, 10.0, 5.0, 0.0, "butt", "miter", false, false, 10.0);
        assertNotNull(dashed);
        assertFalse("Dashed stroke should produce paths", dashed.getPaths().isEmpty());
    }

    @Test
    public void testArrowheads() {
        Path line = new Path();
        line.line(0, 0, 200, 100);
        line.setStrokeWidth(2.0);

        Geometry withArrows = StrokeStyler.apply(line, 0.0, 0.0, 0.0, "round", "round", true, true, 12.0);
        assertNotNull(withArrows);
        // Base line path + start arrow + end arrow = 3 paths
        assertEquals(3, withArrows.getPaths().size());
    }
}
