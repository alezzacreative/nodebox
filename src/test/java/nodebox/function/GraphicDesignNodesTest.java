package nodebox.function;

import com.google.common.collect.ImmutableList;
import nodebox.graphics.*;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import nodebox.node.NodeLibrary;
import nodebox.node.NodeRepository;

public class GraphicDesignNodesTest {

    @Test
    public void testTypeset() {
        String copy = "The quick brown fox jumps over the lazy dog.\nNodeBox procedural typography layout engine.";
        Geometry singleCol = CoreVectorFunctions.typeset(copy, "Helvetica", 16.0, "LEFT", new Point(0, 0), 300.0, 0.0, 1.4, 0.0, 1, 20.0);
        assertNotNull(singleCol);
        assertFalse("Typeset geometry should contain paths", singleCol.getPaths().isEmpty());

        // Multi-column
        Geometry twoCols = CoreVectorFunctions.typeset(copy, "Helvetica", 14.0, "CENTER", new Point(0, 0), 400.0, 50.0, 1.3, 5.0, 2, 20.0);
        assertNotNull(twoCols);
        assertFalse(twoCols.getPaths().isEmpty());
    }

    @Test
    public void testFitText() {
        Path fitted = CoreVectorFunctions.fitText("GRAPHIC DESIGN", "Helvetica", new Point(0, 0), 200.0, 50.0, 8.0, 120.0, "CENTER");
        assertNotNull(fitted);
        Rect bounds = fitted.getBounds();
        assertTrue("Fitted text width must fit target box", bounds.getWidth() <= 210.0);
        assertTrue("Fitted text height must fit target box", bounds.getHeight() <= 55.0);
    }

    @Test
    public void testTextOnPath() {
        Path circle = new Path();
        circle.ellipse(0, 0, 300, 300);

        Path onPath = CoreVectorFunctions.textOnPath("CIRCULAR BADGE LOGO", circle, "Helvetica", 18.0, "CENTER", 0.0, 5.0, 2.0, false);
        assertNotNull(onPath);
        assertFalse("Text on path should contain glyph contours", onPath.getContours().isEmpty());

        // Flipped
        Path flipped = CoreVectorFunctions.textOnPath("INSIDE TEXT", circle, "Helvetica", 18.0, "LEFT", 10.0, 0.0, 0.0, true);
        assertNotNull(flipped);
        assertFalse(flipped.getContours().isEmpty());
    }

    @Test
    public void testHalftone() throws Exception {
        // Create small synthetic gradient image for test
        BufferedImage img = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 40; x++) {
                int gray = (x * 255) / 40;
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        File tmpFile = File.createTempFile("test_halftone", ".png");
        tmpFile.deleteOnExit();
        ImageIO.write(img, "png", tmpFile);

        Geometry htDots = CoreVectorFunctions.halftone(tmpFile.getAbsolutePath(), new Point(0, 0), 100, 100, 10, 45, "dot", 1.0, 0.1, false);
        assertNotNull(htDots);
        assertFalse("Halftone dot screen should generate paths", htDots.getPaths().isEmpty());

        Geometry htLines = CoreVectorFunctions.halftone(tmpFile.getAbsolutePath(), new Point(0, 0), 100, 100, 10, 15, "line", 1.0, 0.0, true);
        assertNotNull(htLines);
        assertFalse("Halftone line screen should generate paths", htLines.getPaths().isEmpty());
    }

    @Test
    public void testTruchetTiles() {
        Geometry arcs = CoreVectorFunctions.truchetTiles(new Point(0, 0), 200, 200, 40, "arcs", 2.0, 42);
        assertNotNull(arcs);
        assertEquals("5x5 grid with 2 arcs per tile = 50 paths", 50, arcs.getPaths().size());

        Geometry diags = CoreVectorFunctions.truchetTiles(new Point(0, 0), 120, 120, 40, "diagonals", 1.5, 123);
        assertNotNull(diags);
        assertEquals("3x3 grid = 9 paths", 9, diags.getPaths().size());
    }

    @Test
    public void testGuilloche() {
        Path g = CoreVectorFunctions.guilloche(new Point(0, 0), 100.0, 30.0, 40.0, 500, 5.0, 5.0, 6.0);
        assertNotNull(g);
        assertFalse(g.getPoints().isEmpty());
        assertTrue("Guilloche should have hundreds of sampled vertices", g.getPointCount() >= 400);
    }

    @Test
    public void testMetaballs() {
        Path c1 = new Path();
        c1.ellipse(0, 0, 60, 60);
        Path c2 = new Path();
        c2.ellipse(50, 0, 50, 50);

        IGeometry mb = CoreVectorFunctions.metaballs(ImmutableList.of(c1, c2), 40.0, 0.5, true);
        assertNotNull(mb);
        Rect b = mb.getBounds();
        assertTrue("Metaballs bridge should span both circles", b.getWidth() > 80);
    }

    @Test
    public void testModularGrid() {
        Geometry gridCells = CoreVectorFunctions.modularGrid(new Point(0, 0), 400, 300, 4, 3, 10, 10, 20, 20, 20, 20, "cells");
        assertNotNull(gridCells);
        assertEquals("4 cols x 3 rows = 12 cell rects", 12, gridCells.getPaths().size());

        Geometry gridAll = CoreVectorFunctions.modularGrid(new Point(0, 0), 400, 300, 4, 3, 10, 10, 20, 20, 20, 20, "all");
        assertNotNull(gridAll);
        assertTrue("Cells + guides should be > 12 paths", gridAll.getPaths().size() > 12);
    }

    @Test
    public void testAlignDistribute() {
        Path p1 = new Path(); p1.rect(0, 0, 40, 40);
        Path p2 = new Path(); p2.rect(100, 20, 20, 20);
        Path p3 = new Path(); p3.rect(50, -30, 30, 30);

        // Align left
        List<IGeometry> aligned = CoreVectorFunctions.alignDistribute(ImmutableList.of(p1, p2, p3), "left", "none", 10.0);
        assertEquals(3, aligned.size());
        double leftX = aligned.get(0).getBounds().getX();
        assertEquals("Left bounds must match", leftX, aligned.get(1).getBounds().getX(), 0.001);
        assertEquals("Left bounds must match", leftX, aligned.get(2).getBounds().getX(), 0.001);

        // Distribute horizontal with 15px gap
        List<IGeometry> distributed = CoreVectorFunctions.alignDistribute(ImmutableList.of(p1, p2, p3), "none", "horizontal", 15.0);
        assertEquals(3, distributed.size());
        double gap1 = distributed.get(1).getBounds().getX() - (distributed.get(0).getBounds().getX() + distributed.get(0).getBounds().getWidth());
        assertEquals("Gap between shapes must be 15px", 15.0, gap1, 0.001);
    }

    @Test
    public void testPackCircles() {
        List<Double> radii = ImmutableList.of(40.0, 30.0, 25.0, 20.0, 15.0, 10.0);
        List<Path> packed = CoreVectorFunctions.packCircles(radii, new Point(0, 0), 150.0, 50, 42);
        assertEquals(6, packed.size());
        for (Path p : packed) {
            assertNotNull(p);
            Rect b = p.getBounds();
            assertFalse(Double.isNaN(b.getX()));
            assertFalse(Double.isNaN(b.getY()));
        }
    }

    @Test
    public void testRoughen() {
        Path rect = new Path();
        rect.rect(0, 0, 100, 100);
        int origPoints = rect.getPointCount();

        IGeometry roughened = CoreVectorFunctions.roughen(rect, 8.0, 2.0, "smooth", 42);
        assertNotNull(roughened);
        assertTrue("Roughening subdivides path into more points", roughened.getPoints().size() > origPoints);
    }

    @Test
    public void testLongShadow() {
        Path star = new Path();
        star.rect(0, 0, 50, 50);

        Geometry shadowGeo = CoreVectorFunctions.longShadow(star, 45.0, 60.0, 0.2);
        assertNotNull(shadowGeo);
        assertEquals("Contains shadow path + original shape", 2, shadowGeo.getPaths().size());
        Path shadow = shadowGeo.getPaths().get(0);
        Rect b = shadow.getBounds();
        assertTrue("Shadow must project in direction of 45 deg", b.getWidth() > 50 && b.getHeight() > 50);
    }

    @Test
    public void testExtrude3d() {
        Path poly = new Path();
        poly.rect(0, 0, 80, 80);

        Geometry extruded = CoreVectorFunctions.extrude3d(poly, 30.0, 45.0, new Color(0.9, 0.9, 0.9), new Color(0.4, 0.4, 0.5), 80.0);
        assertNotNull(extruded);
        // 4 side quads + 1 front face = 5 paths
        assertEquals(5, extruded.getPaths().size());
    }

    @Test
    public void testColorHarmoniesAndBlend() {
        Color brand = new Color(0.2, 0.6, 0.9, 1.0);

        // Harmony palette
        List<Color> comp = ColorHarmony.getHarmony(brand, "complementary");
        assertFalse(comp.isEmpty());

        List<Color> triad = ColorHarmony.getHarmony(brand, "triadic");
        assertEquals(3, triad.size());

        List<Color> mono = ColorHarmony.getHarmony(brand, "monochromatic");
        assertEquals(5, mono.size());

        // Blend modes
        Color white = new Color(1.0, 1.0, 1.0);
        Color red = new Color(1.0, 0.0, 0.0);
        Color blue = new Color(0.0, 0.0, 1.0);

        Color mult = ColorHarmony.blend(red, blue, "multiply", 1.0);
        assertEquals("Red x Blue in Multiply = Black", 0.0, mult.getRed() + mult.getGreen() + mult.getBlue(), 0.001);

        Color scr = ColorHarmony.blend(red, blue, "screen", 1.0);
        assertEquals("Screen combines lights", 1.0, scr.getRed(), 0.001);
        assertEquals("Screen combines lights", 1.0, scr.getBlue(), 0.001);

        Color diff = ColorHarmony.blend(white, red, "difference", 1.0);
        assertEquals("White - Red = Cyan (0, 1, 1)", 0.0, diff.getRed(), 0.001);
        assertEquals(1.0, diff.getGreen(), 0.001);
        assertEquals(1.0, diff.getBlue(), 0.001);
    }

    @Test
    public void testExtractPalette() throws Exception {
        BufferedImage img = new BufferedImage(60, 60, BufferedImage.TYPE_INT_RGB);
        // Fill half red, half green
        for (int y = 0; y < 60; y++) {
            for (int x = 0; x < 30; x++) img.setRGB(x, y, 0xff0000);
            for (int x = 30; x < 60; x++) img.setRGB(x, y, 0x00ff00);
        }
        File tmpFile = File.createTempFile("palette_test", ".png");
        tmpFile.deleteOnExit();
        ImageIO.write(img, "png", tmpFile);

        List<Color> pal = ColorFunctions.extractPalette(tmpFile.getAbsolutePath(), 2, 2);
        assertEquals(2, pal.size());
    }

    @Test
    public void testArtboard() {
        Path circle = new Path();
        circle.ellipse(0, 0, 500, 500);

        Artboard story = CoreVectorFunctions.artboard(circle, "Story Campaign", "instagram_story", 1080.0, 1920.0, new Point(0, 0), true, new Color(0.1, 0.1, 0.1), true);
        assertNotNull(story);
        assertEquals("Story Campaign", story.getName());
        assertEquals(1080.0, story.getWidth(), 0.001);
        assertEquals(1920.0, story.getHeight(), 0.001);
        assertNotNull(story.getExportBounds());
        assertEquals(1080.0, story.getExportBounds().getWidth(), 0.001);
        assertEquals(1920.0, story.getExportBounds().getHeight(), 0.001);
        assertFalse(story.getContentForExport().getPaths().isEmpty());

        // Business Card preset
        Artboard card = CoreVectorFunctions.artboard(circle, "Card", "business_card", 0.0, 0.0, new Point(1200, 0), false, Color.WHITE, false);
        assertEquals(1050.0, card.getWidth(), 0.001);
        assertEquals(600.0, card.getHeight(), 0.001);
    }

    @Test
    public void testNewShowcaseExamplesLoad() {
        NodeRepository repo = NodeRepository.of(
                NodeLibrary.loadSystemLibrary("math"),
                NodeLibrary.loadSystemLibrary("string"),
                NodeLibrary.loadSystemLibrary("color"),
                NodeLibrary.loadSystemLibrary("list"),
                NodeLibrary.loadSystemLibrary("data"),
                NodeLibrary.loadSystemLibrary("corevector"),
                NodeLibrary.loadSystemLibrary("device"),
                NodeLibrary.loadSystemLibrary("network")
        );
        File[] exampleFiles = new File[]{
                new File("examples/02 Topics/Graphic Design/04 Multi-Artboard Campaign/04 Multi-Artboard Campaign.ndbx"),
                new File("examples/02 Topics/Graphic Design/05 Generative Halftone Poster/05 Generative Halftone Poster.ndbx"),
                new File("examples/02 Topics/Graphic Design/06 3D Typography and Shadow/06 3D Typography and Shadow.ndbx"),
                new File("examples/02 Topics/Graphic Design/07 Geometric Round Corners/07 Geometric Round Corners.ndbx"),
                new File("examples/02 Topics/Graphic Design/08 Perceptual Color Gradient/08 Perceptual Color Gradient.ndbx"),
                new File("examples/02 Topics/Generative Design/Organic Flow Field/Organic Flow Field.ndbx"),
                new File("examples/02 Topics/Generative Design/Voronoi Crystal Mesh/Voronoi Crystal Mesh.ndbx"),
                new File("examples/02 Topics/Generative Design/L-System Fractal Tree/L-System Fractal Tree.ndbx"),
                new File("examples/02 Topics/Generative Design/07 Procedural Duplicator/07 Procedural Duplicator.ndbx"),
                new File("examples/02 Topics/Generative Design/08 Particle Attractor/08 Particle Attractor.ndbx"),
                new File("examples/02 Topics/Interaction/01 Mouse Follower/01 Mouse Follower.ndbx"),
                new File("examples/02 Topics/Interaction/02 Audio Beat Pulse/02 Audio Beat Pulse.ndbx")
        };
        for (File f : exampleFiles) {
            assertTrue("Example file should exist: " + f.getPath(), f.exists());
            NodeLibrary lib = NodeLibrary.load(f, repo);
            assertNotNull("Loaded library should not be null for " + f.getName(), lib);
            assertNotNull("Root node should exist for " + f.getName(), lib.getRoot());
        }
    }

    @Test
    public void testManualRecipesLoad() {
        NodeRepository repo = NodeRepository.of(
                NodeLibrary.loadSystemLibrary("math"),
                NodeLibrary.loadSystemLibrary("string"),
                NodeLibrary.loadSystemLibrary("color"),
                NodeLibrary.loadSystemLibrary("list"),
                NodeLibrary.loadSystemLibrary("data"),
                NodeLibrary.loadSystemLibrary("corevector"),
                NodeLibrary.loadSystemLibrary("device"),
                NodeLibrary.loadSystemLibrary("network")
        );
        File[] recipeFiles = new File[]{
                new File("examples/02 Topics/Manual Recipes/01 Moroccan Rosette Wallpaper/01 Moroccan Rosette Wallpaper.ndbx"),
                new File("examples/02 Topics/Manual Recipes/02 Golden Ratio Duplicator/02 Golden Ratio Duplicator.ndbx"),
                new File("examples/02 Topics/Manual Recipes/03 Audio Reactive Visualizer/03 Audio Reactive Visualizer.ndbx"),
                new File("examples/02 Topics/Manual Recipes/04 Flow Field Typography Poster/04 Flow Field Typography Poster.ndbx"),
                new File("examples/02 Topics/Manual Recipes/05 Geometric Round Corners Branding/05 Geometric Round Corners Branding.ndbx"),
                new File("examples/02 Topics/Manual Recipes/06 Halftone Stippled Engraving/06 Halftone Stippled Engraving.ndbx"),
                new File("examples/02 Topics/Manual Recipes/07 Mouse Gravity Vortex/07 Mouse Gravity Vortex.ndbx"),
                new File("examples/02 Topics/Manual Recipes/08 Multi-Artboard Campaign/08 Multi-Artboard Campaign.ndbx")
        };
        for (File f : recipeFiles) {
            assertTrue("Recipe file should exist: " + f.getPath(), f.exists());
            NodeLibrary lib = NodeLibrary.load(f, repo);
            assertNotNull("Loaded recipe library should not be null for " + f.getName(), lib);
            assertNotNull("Root node should exist for " + f.getName(), lib.getRoot());
        }
    }
}

