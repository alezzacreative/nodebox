package nodebox.client;
import nodebox.node.Node;
import nodebox.node.Port;
import org.junit.Test;

import static org.junit.Assert.*;

public class NodeDocumentationTest {

    @Test
    public void testBuiltinNodeTooltip() {
        Node colorizeNode = Node.ROOT.withName("colorize")
                .withOutputType("geometry")
                .withInputAdded(Port.colorPort("fill", nodebox.graphics.Color.BLACK))
                .withInputAdded(Port.colorPort("stroke", nodebox.graphics.Color.BLACK))
                .withInputAdded(Port.floatPort("strokeWidth", 1.0f));

        String html = NodeDocumentation.getHtmlTooltip(colorizeNode);
        System.out.println("COLORIZE HTML: " + html);
        assertNotNull(html);
        assertTrue(html.contains("Colorize"));
        assertTrue(html.contains("Change the color of a shape."));
        assertTrue(html.contains("fill"));
        assertTrue(html.contains("stroke"));
        assertTrue(html.contains("OUTPUT:"));
        assertTrue(html.contains("geometry"));
    }

    @Test
    public void testCustomNodeTooltip() {
        Node flowField = Node.ROOT.withName("flow_field")
                .withDescription("Generate organic vector streamlines guided by a Simplex noise vector field.")
                .withOutputType("geometry")
                .withInputAdded(Port.intPort("steps", 50))
                .withInputAdded(Port.floatPort("stepLength", 5.0f))
                .withInputAdded(Port.floatPort("noiseScale", 0.005f));

        String html = NodeDocumentation.getHtmlTooltip(flowField);
        System.out.println("FLOW FIELD HTML: " + html);
        assertNotNull(html);
        assertTrue(html.contains("Flow Field"));
        assertTrue(html.contains("Simplex noise"));
        assertTrue(html.toLowerCase().contains("steps"));
        assertTrue(html.contains("StepLength"));
        assertTrue(html.contains("NoiseScale"));
    }

    @Test
    public void testNullNode() {
        assertNull(NodeDocumentation.getHtmlTooltip(null));
    }
}
