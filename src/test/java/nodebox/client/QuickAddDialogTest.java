package nodebox.client;

import com.google.common.collect.ImmutableList;
import nodebox.node.Node;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QuickAddDialogTest {

    @Test
    public void testFiltering() {
        Node rectNode = Node.ROOT.withName("rect").withDescription("Create a rectangle");
        Node resampleNode = Node.ROOT.withName("resample").withDescription("Resample a path");
        Node waveNode = Node.ROOT.withName("wave").withDescription("Wave generator");

        QuickAddDialog.QuickNodeListModel model = new QuickAddDialog.QuickNodeListModel(
                ImmutableList.of(rectNode, resampleNode, waveNode));

        assertEquals(3, model.getSize());

        // Exact / prefix match
        model.filter("rec");
        assertTrue(model.getSize() >= 1);
        assertEquals("rect", model.getElementAt(0).getName());

        // Substring / description match
        model.filter("rectangle");
        assertTrue(model.getSize() >= 1);
        assertEquals("rect", model.getElementAt(0).getName());

        // Empty filter returns all
        model.filter("");
        assertEquals(3, model.getSize());
    }

}
