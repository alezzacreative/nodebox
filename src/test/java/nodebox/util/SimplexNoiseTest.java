package nodebox.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimplexNoiseTest {

    @Test
    public void testSimplex2DBounds() {
        for (double x = -10.0; x <= 10.0; x += 1.5) {
            for (double y = -10.0; y <= 10.0; y += 1.5) {
                double val = SimplexNoise.simplex2D(x, y);
                assertTrue("2D noise must be >= -1.0: " + val, val >= -1.0);
                assertTrue("2D noise must be <= 1.0: " + val, val <= 1.0);
            }
        }
    }

    @Test
    public void testSimplex3DBounds() {
        for (double x = -5.0; x <= 5.0; x += 2.0) {
            for (double y = -5.0; y <= 5.0; y += 2.0) {
                for (double z = -5.0; z <= 5.0; z += 2.0) {
                    double val = SimplexNoise.simplex3D(x, y, z);
                    assertTrue("3D noise must be >= -1.0: " + val, val >= -1.0);
                    assertTrue("3D noise must be <= 1.0: " + val, val <= 1.0);
                }
            }
        }
    }

    @Test
    public void testWorleyNoise() {
        for (double x = 0.0; x < 5.0; x += 0.5) {
            for (double y = 0.0; y < 5.0; y += 0.5) {
                double val = SimplexNoise.worley2D(x, y);
                assertTrue("Worley noise must be non-negative: " + val, val >= 0.0);
                assertTrue("Worley noise must be <= 1.0: " + val, val <= 1.0);
            }
        }
    }

    @Test
    public void testFbm() {
        double fbmVal = SimplexNoise.fbm(1.23, 4.56, 0.0, "simplex", 4, 0.5);
        assertTrue("fBm must be bounded: " + fbmVal, fbmVal >= -1.5 && fbmVal <= 1.5);

        double worleyFbm = SimplexNoise.fbm(1.23, 4.56, 0.0, "worley", 3, 0.5);
        assertTrue("Worley fBm must be non-negative: " + worleyFbm, worleyFbm >= 0.0);
    }

    @Test
    public void testDeterminism() {
        double v1 = SimplexNoise.simplex2D(3.1415, 2.7182);
        double v2 = SimplexNoise.simplex2D(3.1415, 2.7182);
        assertEquals(v1, v2, 0.000001);
    }
}
