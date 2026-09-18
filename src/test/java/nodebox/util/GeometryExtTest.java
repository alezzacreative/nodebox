package nodebox.util;

import com.google.common.collect.ImmutableList;
import nodebox.graphics.Path;
import nodebox.graphics.Point;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class GeometryExtTest {

    @Test
    public void testDelaunayTriangulation() {
        List<Point> pts = ImmutableList.of(
                new Point(0, 0),
                new Point(100, 0),
                new Point(50, 80),
                new Point(50, 30)
        );
        List<DelaunayVoronoi.Triangle> triangles = DelaunayVoronoi.triangulate(pts);
        assertNotNull(triangles);
        assertFalse(triangles.isEmpty());
        List<Path> paths = DelaunayVoronoi.trianglesToPaths(triangles);
        assertEquals(triangles.size(), paths.size());
        for (Path p : paths) {
            assertNotNull(p);
            assertTrue(p.getPointCount() >= 3);
        }
    }

    @Test
    public void testVoronoiCells() {
        List<Point> pts = ImmutableList.of(
                new Point(-50, -50),
                new Point(50, -50),
                new Point(50, 50),
                new Point(-50, 50)
        );
        List<Path> cells = DelaunayVoronoi.voronoiCells(pts, 400, 400, 0.1);
        assertNotNull(cells);
        assertFalse(cells.isEmpty());
        for (Path cell : cells) {
            assertNotNull(cell);
            assertTrue(cell.getPointCount() >= 3);
        }
    }

    @Test
    public void testConvexHull() {
        List<Point> pts = ImmutableList.of(
                new Point(0, 0),
                new Point(10, 0),
                new Point(10, 10),
                new Point(0, 10),
                new Point(5, 5) // Interior point
        );
        List<Point> hull = ConvexHull.computeHullPoints(pts);
        assertNotNull(hull);
        // Interior point (5,5) should not be on the 4-corner hull
        assertEquals(4, hull.size());

        Path hullPath = ConvexHull.computeHullPath(pts);
        assertNotNull(hullPath);
        assertTrue(hullPath.getPointCount() >= 4);
    }

    @Test
    public void testChaikinSmoothing() {
        List<Point> pts = ImmutableList.of(
                new Point(0, 0),
                new Point(50, 100),
                new Point(100, 0)
        );
        List<Point> smoothed = ChaikinSmoothing.smoothPoints(pts, false, 2, 0.25);
        assertNotNull(smoothed);
        // Subdivision increases points: 3 -> 4 -> 6 points
        assertTrue(smoothed.size() > pts.size());
        // Open curve preserves endpoints
        assertEquals(pts.get(0).x, smoothed.get(0).x, 0.001);
        assertEquals(pts.get(0).y, smoothed.get(0).y, 0.001);
        assertEquals(pts.get(2).x, smoothed.get(smoothed.size() - 1).x, 0.001);
        assertEquals(pts.get(2).y, smoothed.get(smoothed.size() - 1).y, 0.001);

        Path p = new Path();
        p.moveto(0, 0);
        p.lineto(50, 100);
        p.lineto(100, 0);
        Path smoothedPath = ChaikinSmoothing.smoothPath(p, 1, 0.25);
        assertNotNull(smoothedPath);
        assertTrue(smoothedPath.getPointCount() > p.getPointCount());
    }
}
