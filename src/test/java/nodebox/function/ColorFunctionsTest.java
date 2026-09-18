package nodebox.function;

import nodebox.graphics.Color;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ColorFunctionsTest {

    @Test
    public void testPaletteAnalogous() {
        Color base = Color.fromHSB(0.5, 0.8, 0.9, 1.0);
        List<Color> colors = ColorFunctions.palette(base, "analogous", 5, 0.08);
        assertNotNull(colors);
        assertEquals(5, colors.size());
        assertEquals(0.5, colors.get(2).getHue(), 0.001);
    }

    @Test
    public void testPaletteComplementary() {
        Color base = Color.fromHSB(0.2, 0.8, 0.9, 1.0);
        List<Color> colors = ColorFunctions.palette(base, "complementary", 2, 0.08);
        assertNotNull(colors);
        assertEquals(2, colors.size());
        assertEquals(0.2, colors.get(0).getHue(), 0.001);
        assertEquals(0.7, colors.get(1).getHue(), 0.001);
    }

    @Test
    public void testPaletteMonochromatic() {
        Color base = Color.fromHSB(0.4, 0.8, 0.9, 1.0);
        List<Color> colors = ColorFunctions.palette(base, "monochromatic", 3, 0.08);
        assertNotNull(colors);
        assertEquals(3, colors.size());
        for (Color c : colors) {
            assertEquals(0.4, c.getHue(), 0.001);
        }
    }

}
