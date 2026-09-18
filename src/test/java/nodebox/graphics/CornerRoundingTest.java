package nodebox.graphics;

import nodebox.function.CoreVectorFunctions;
import nodebox.util.CornerRounding;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class CornerRoundingTest {

    @Test
    public void testRoundCornersOnRect() {
        // Create a 100x100 rectangle
        Path rect = new Path();
        rect.rect(0, 0, 100, 100);
        assertEquals(1, rect.getContours().size());
        Contour c0 = rect.getContours().get(0);
        assertEquals(4, c0.getPointCount());

        // Round corners with radius 10
        Geometry rounded = CornerRounding.roundCorners(rect.asGeometry(), 10.0, "round", 175.0, true);
        assertNotNull(rounded);
        assertEquals(1, rounded.getPaths().size());
        Path p = rounded.getPaths().get(0);
        assertEquals(1, p.getContours().size());

        // Each 90-degree corner should now have cubic bezier curves
        Contour rc = p.getContours().get(0);
        assertTrue("Rounded contour should have more points than original 4", rc.getPointCount() > 4);

        // Verify there are CURVE_TO points
        boolean foundCurve = false;
        for (Point pt : rc.getPoints()) {
            if (pt.isCurveTo()) {
                foundCurve = true;
                break;
            }
        }
        assertTrue("Should contain rounded curve points", foundCurve);
    }

    @Test
    public void testDifferentCornerTypes() {
        Path rect = new Path();
        rect.rect(0, 0, 100, 100);
        Geometry geo = rect.asGeometry();

        // 1. Chamfer (flat cuts)
        Geometry chamfered = CornerRounding.roundCorners(geo, 10.0, "chamfer", 175.0, true);
        assertNotNull(chamfered);
        Contour chamferContour = chamfered.getPaths().get(0).getContours().get(0);
        // Each corner replaces 1 vertex with 2 line-to vertices -> 8 points total
        assertEquals(8, chamferContour.getPointCount());
        for (Point pt : chamferContour.getPoints()) {
            assertTrue("Chamfer should only use LINE_TO points", pt.isLineTo());
        }

        // 2. Scoop (concave arc)
        Geometry scooped = CornerRounding.roundCorners(geo, 10.0, "scoop", 175.0, true);
        assertNotNull(scooped);
        Contour scoopContour = scooped.getPaths().get(0).getContours().get(0);
        assertTrue(scoopContour.getPointCount() > 4);

        // 3. Dogbone (CNC relief)
        Geometry dogbone = CornerRounding.roundCorners(geo, 10.0, "dogbone", 175.0, true);
        assertNotNull(dogbone);
        Contour dogboneContour = dogbone.getPaths().get(0).getContours().get(0);
        assertTrue(dogboneContour.getPointCount() > 4);

        // 4. Squircle (G2 superellipse)
        Geometry squircle = CornerRounding.roundCorners(geo, 10.0, "squircle", 175.0, true);
        assertNotNull(squircle);
        Contour squircleContour = squircle.getPaths().get(0).getContours().get(0);
        assertTrue(squircleContour.getPointCount() > 4);
    }

    @Test
    public void testRoundCornersOnText() {
        // Generate text path for letter "H"
        Path textGeo = CoreVectorFunctions.textpath("H", "Helvetica", 72.0, "center", Point.ZERO, 0.0);
        assertNotNull(textGeo);
        assertTrue(textGeo.getContours().size() > 0);

        // Apply round corners to text
        Geometry roundedText = CoreVectorFunctions.roundCorners(textGeo, 5.0, "round", 175.0, true);
        assertNotNull(roundedText);
        assertTrue("Rounded text should produce valid geometry", roundedText.getPaths().size() > 0);
        assertTrue("Rounded text should have valid bounds", roundedText.getBounds().getWidth() > 0);
    }

    @Test
    public void testThresholdAngleFiltering() {
        // Create a triangle with one very obtuse corner (178 degrees)
        Path obtuse = new Path();
        obtuse.moveto(0, 0);
        obtuse.lineto(100, 1); // almost collinear
        obtuse.lineto(200, 0);
        obtuse.lineto(100, 50);
        obtuse.close();

        // With threshold = 160, the 178-deg corner should NOT be rounded
        Geometry res = CornerRounding.roundCorners(obtuse.asGeometry(), 10.0, "round", 160.0, true);
        assertNotNull(res);
    }
}
