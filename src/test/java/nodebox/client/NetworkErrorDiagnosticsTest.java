package nodebox.client;

import nodebox.node.Node;
import nodebox.node.NodeLibrary;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NetworkErrorDiagnosticsTest {

    @Test
    public void testSuggestedFixes() {
        String fixArgs = NetworkPane.getSuggestedFix("TypeError: flow() takes exactly 5 arguments (4 given)");
        assertTrue("Should suggest argument fix", fixArgs.contains("unexpected number of parameters"));

        String fixNone = NetworkPane.getSuggestedFix("AttributeError: 'NoneType' object has no attribute 'getPoints'");
        assertTrue("Should suggest upstream data fix", fixNone.contains("upstream nodes"));

        String fixDiv = NetworkPane.getSuggestedFix("ZeroDivisionError: float division by zero");
        assertTrue("Should suggest zero denominator fix", fixDiv.contains("Division by zero"));

        String fixMem = NetworkPane.getSuggestedFix("java.lang.OutOfMemoryError: Java heap space");
        assertTrue("Should suggest memory reduction fix", fixMem.contains("Memory limit exceeded"));

        String fixIdx = NetworkPane.getSuggestedFix("IndexError: list index out of range");
        assertTrue("Should suggest index bounds fix", fixIdx.contains("Index out of range"));

        String fixType = NetworkPane.getSuggestedFix("TypeError: cannot convert float to Geometry");
        assertTrue("Should suggest type mismatch fix", fixType.contains("Data type mismatch"));

        String fixDefault = NetworkPane.getSuggestedFix("UnknownCustomError: something went wrong");
        assertNotNull("Should provide default fix advice", fixDefault);
        assertTrue(fixDefault.length() > 0);
    }

    @Test
    public void testGenerateErrorTooltip() {
        Node testNode = Node.ROOT.withName("flow");
        String tooltip = NetworkPane.generateErrorTooltip(testNode, new RuntimeException("wrong number of arguments"), "wrong number of arguments");
        assertNotNull(tooltip);
        assertTrue("Tooltip should contain html tag", tooltip.startsWith("<html>"));
        assertTrue("Tooltip should mention node name", tooltip.contains("flow"));
        assertTrue("Tooltip should mention error message", tooltip.contains("wrong number of arguments"));
        assertTrue("Tooltip should include Suggested Fix section", tooltip.contains("Suggested Fix"));
    }

    @Test
    public void testNetworkViewErrorTracking() {
        Node nodeA = Node.ROOT.withName("nodeA");
        Node nodeB = Node.ROOT.withName("nodeB");
        Node net = Node.NETWORK.withChildAdded(nodeA).withChildAdded(nodeB);
        NodeLibrary library = NodeLibrary.create("test", net);
        NodeBoxDocument doc = new NodeBoxDocument(library);
        NetworkView view = doc.getNetworkView();

        assertFalse("Initially no error node", view.isErrorNode(nodeA));

        view.setErrorNode(nodeA, "TypeError: invalid input");
        assertTrue("nodeA should be recognized as error node", view.isErrorNode(nodeA));
        assertFalse("nodeB should not be error node", view.isErrorNode(nodeB));
        assertEquals(nodeA, view.getErrorNode());
        assertEquals("TypeError: invalid input", view.getErrorNodeMessage());

        // Test focus node
        view.focusNode(nodeA);
        assertEquals("Active node should be updated to nodeA", "nodeA", doc.getActiveNodeName());

        // Test clear error
        view.clearErrorNode();
        assertFalse("Error node should be cleared", view.isErrorNode(nodeA));
        assertEquals(null, view.getErrorNode());
    }

}
