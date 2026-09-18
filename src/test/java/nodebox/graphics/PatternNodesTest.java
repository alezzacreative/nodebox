package nodebox.graphics;

import org.junit.Test;

import static org.junit.Assert.*;

public class PatternNodesTest {

    @Test
    public void testHexGrid() {
        // Flat orientation
        Geometry flatGeom = PatternNodes.hexGrid(5, 5, 40.0, 0.0, "flat", Point.ZERO);
        assertNotNull(flatGeom);
        assertEquals(25, flatGeom.size());

        // Pointy orientation
        Geometry pointyGeom = PatternNodes.hexGrid(4, 6, 50.0, 5.0, "pointy", new Point(10, -10));
        assertNotNull(pointyGeom);
        assertEquals(24, pointyGeom.size());
    }

    @Test
    public void testStripes() {
        // Linear stripes
        Geometry linear = PatternNodes.stripes(400, 300, 10, 0.5, 45, "linear");
        assertNotNull(linear);
        assertEquals(10, linear.size());

        // Radial stripes
        Geometry radial = PatternNodes.stripes(300, 300, 12, 0.5, 0, "radial");
        assertNotNull(radial);
        assertEquals(12, radial.size());

        // Checkerboard
        Geometry checker = PatternNodes.stripes(200, 200, 4, 0.8, 0, "checker");
        assertNotNull(checker);
        assertTrue(checker.size() > 0);
    }

    @Test
    public void testKaleidoscope() {
        Path star = new Path();
        star.rect(10, 10, 30, 30);

        // 6-fold mirror kaleidoscope
        Geometry result = PatternNodes.kaleidoscope(star, 6, Point.ZERO, true);
        assertNotNull(result);
        assertEquals(6, result.size());

        // 4-fold non-mirror kaleidoscope
        Geometry noMirror = PatternNodes.kaleidoscope(star, 4, new Point(5, 5), false);
        assertNotNull(noMirror);
        assertEquals(4, noMirror.size());
    }

    @Test
    public void testSeamlessTile() {
        // A shape that overlaps the right edge of a 200x200 tile (centered at 0, so bounds is -100..100)
        Path r = new Path();
        r.rect(90, 0, 40, 40);

        Geometry tiled = PatternNodes.seamlessTile(r, 200, 200, false, Point.ZERO);
        assertNotNull(tiled);
        // Original + wrapped clone across left border
        assertTrue("Tile wrapping should produce repeated geometries across borders", tiled.size() > 1);

        // Tiling with clipping
        Geometry clipped = PatternNodes.seamlessTile(r, 200, 200, true, Point.ZERO);
        assertNotNull(clipped);
    }

    @Test
    public void testWaveWarp() {
        Path line = new Path();
        line.line(0, 0, 200, 0);

        // Sine wave warp
        Geometry warped = PatternNodes.waveWarp(line, "horizontal", "sine", 15.0, 0.05, 90.0);
        assertNotNull(warped);
        assertTrue(warped.size() > 0);

        // Triangle wave warp
        Geometry triWarp = PatternNodes.waveWarp(line, "vertical", "triangle", 20.0, 0.02, 0.0);
        assertNotNull(triWarp);

        // Radial ripples
        Geometry radialWarp = PatternNodes.waveWarp(line, "radial", "sine", 10.0, 0.03, 0.0);
        assertNotNull(radialWarp);
    }

    @Test
    public void testBulgePinch() {
        Path gridShape = new Path();
        gridShape.rect(0, 0, 100, 100);

        // Bulge (positive strength)
        Geometry bulged = PatternNodes.bulgePinch(gridShape, Point.ZERO, 80.0, 0.8);
        assertNotNull(bulged);

        // Pinch (negative strength)
        Geometry pinched = PatternNodes.bulgePinch(gridShape, Point.ZERO, 80.0, -0.6);
        assertNotNull(pinched);
    }
}
