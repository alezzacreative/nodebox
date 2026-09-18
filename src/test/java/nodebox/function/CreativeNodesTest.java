package nodebox.function;

import com.google.common.collect.ImmutableList;
import nodebox.graphics.Color;
import nodebox.graphics.Geometry;
import nodebox.graphics.Path;
import nodebox.graphics.Point;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class CreativeNodesTest {

    @Test
    public void testMathNoise() {
        double val = MathFunctions.noise(1.5, 2.5, 0.0, "simplex", 4, 0.5);
        assertTrue(val >= -1.0 && val <= 1.0);

        double valWorley = MathFunctions.noise(1.5, 2.5, 0.0, "worley", 1, 0.5);
        assertTrue(valWorley >= 0.0 && valWorley <= 1.0);
    }

    @Test
    public void testMathSpring() {
        // Step forward from 0 towards 100
        double next = MathFunctions.spring(0.0, 100.0, 0.0, 150.0, 15.0, 1.0);
        assertTrue(next > 0.0);
        assertTrue(next <= 100.0);
    }

    @Test
    public void testColorGradient() {
        Color blue = new Color(0, 0, 1, 1);
        Color yellow = new Color(1, 1, 0, 1);
        List<Color> stops = ImmutableList.of(blue, yellow);

        // Start and end positions
        Color c0 = ColorFunctions.gradient(stops, 0.0, "oklab");
        assertEquals(blue.getRed(), c0.getRed(), 0.01);
        assertEquals(blue.getBlue(), c0.getBlue(), 0.01);

        Color c1 = ColorFunctions.gradient(stops, 1.0, "oklab");
        assertEquals(yellow.getRed(), c1.getRed(), 0.01);
        assertEquals(yellow.getGreen(), c1.getGreen(), 0.01);

        // Midpoint in Oklab: perceptually vibrant, not muddy gray
        Color midOklab = ColorFunctions.gradient(stops, 0.5, "oklab");
        assertNotNull(midOklab);
        assertTrue(midOklab.getGreen() > 0.3);

        // RGB midpoint
        Color midRgb = ColorFunctions.gradient(stops, 0.5, "rgb");
        assertEquals(0.5, midRgb.getRed(), 0.01);
        assertEquals(0.5, midRgb.getBlue(), 0.01);

        // HSB midpoint
        Color midHsb = ColorFunctions.gradient(stops, 0.5, "hsb");
        assertNotNull(midHsb);
    }

    @Test
    public void testFlowField() {
        List<Point> seeds = ImmutableList.of(new Point(0, 0), new Point(50, 50));
        List<Path> streamlines = CoreVectorFunctions.flowField(seeds, Point.ZERO, 20, 5.0, 0.01, 360.0, 42);
        assertNotNull(streamlines);
        assertEquals(2, streamlines.size());
        for (Path p : streamlines) {
            assertTrue(p.getPointCount() >= 20);
        }
    }

    @Test
    public void testLSystem() {
        // Koch curve: F -> F+F-F-F+F
        Path koch = CoreVectorFunctions.lsystem("F", "F=F+F-F-F+F", 2, 90.0, 10.0, 1.0, Point.ZERO);
        assertNotNull(koch);
        assertTrue("Koch curve should have multiple line segments", koch.getPointCount() > 5);

        // Branching plant
        Path plant = CoreVectorFunctions.lsystem("X", "X=F+[[X]-X]-F[-FX]+X;F=FF", 2, 25.0, 5.0, 0.5, Point.ZERO);
        assertNotNull(plant);
        assertTrue("Branching plant should create multiple segments", plant.getPointCount() > 10);
    }

    @Test
    public void testSampleImage() throws Exception {
        // Create a temporary 100x100 RGB image with a red circle in center
        BufferedImage testImg = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = testImg.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, 100, 100);
        g.setColor(java.awt.Color.RED);
        g.fillOval(25, 25, 50, 50);
        g.dispose();

        File tmpFile = File.createTempFile("nodebox_test_sample", ".png");
        tmpFile.deleteOnExit();
        ImageIO.write(testImg, "PNG", tmpFile);

        List<Point> pts = ImmutableList.of(
                new Point(0, 0),       // Center -> Red circle
                new Point(45, 45)      // Corner -> White background
        );

        // Center origin test
        List<?> colors = DataFunctions.sampleImage(tmpFile.getAbsolutePath(), pts, "color", "center");
        assertEquals(2, colors.size());
        Color cCenter = (Color) colors.get(0);
        Color cCorner = (Color) colors.get(1);

        assertTrue("Center should be red", cCenter.getRed() > 0.8 && cCenter.getGreen() < 0.2);
        assertTrue("Corner should be white", cCorner.getRed() > 0.8 && cCorner.getGreen() > 0.8);

        // Brightness channel test
        List<?> brightness = DataFunctions.sampleImage(tmpFile.getAbsolutePath(), pts, "brightness", "center");
        assertEquals(2, brightness.size());
        double bCenter = ((Number) brightness.get(0)).doubleValue();
        double bCorner = ((Number) brightness.get(1)).doubleValue();
        assertTrue("Corner white brightness should be higher than red", bCorner > bCenter);
    }

    @Test
    public void testAttractor() {
        Point center = new Point(0, 0);
        Point p = new Point(100, 0);
        // Positive force attracts towards center
        Point attracted = (Point) CoreVectorFunctions.attractor(p, center, 40.0, 200.0, "linear");
        assertNotNull(attracted);
        assertTrue("Attracted point should be closer to center", attracted.x < 100.0 && attracted.x > 0.0);

        // Negative force repels away from center
        Point repelled = (Point) CoreVectorFunctions.attractor(p, center, -40.0, 200.0, "linear");
        assertNotNull(repelled);
        assertTrue("Repelled point should be further from center", repelled.x > 100.0);

        // Vortex swirls perpendicular to radius
        Point vortex = (Point) CoreVectorFunctions.attractor(p, center, 30.0, 200.0, "vortex");
        assertNotNull(vortex);
        assertTrue("Vortex should displace along Y tangent", Math.abs(vortex.y) > 0.1);
    }

    @Test
    public void testOffsetPath() {
        Path rect = new Path();
        rect.rect(0, 0, 50, 50);
        Path expanded = CoreVectorFunctions.offsetPath(rect, 10.0, "round");
        assertNotNull(expanded);
        assertTrue(expanded.getPointCount() > 0);
    }

    @Test
    public void testBarchart() {
        List<Double> values = ImmutableList.of(10.0, 50.0, 25.0, 90.0);
        Geometry bars = DataFunctions.barchart(values, Point.ZERO, 400.0, 200.0, 5.0, "vertical");
        assertNotNull(bars);
        assertEquals(4, bars.getPaths().size());
    }

    @Test
    public void testDonutChart() {
        List<Double> values = ImmutableList.of(30.0, 50.0, 20.0);
        List<Path> wedges = DataFunctions.donut(values, Point.ZERO, 40.0, 100.0, 0.0);
        assertNotNull(wedges);
        assertEquals(3, wedges.size());
    }
}
