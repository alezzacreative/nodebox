package nodebox.client;

import nodebox.node.Node;
import nodebox.node.NodeRenderException;
import nodebox.ui.*;
import org.python.core.PyException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static nodebox.ui.ExceptionDialog.getRootCause;

public class NetworkPane extends Pane {

    private final NodeBoxDocument document;
    private final PaneHeader paneHeader;
    private final JLabel errorLabel;
    private final NetworkView networkView;
    private Throwable nodeRenderException;
    private Node currentErrorNode = null;
    private String currentErrorMessage = null;

    public NetworkPane(NodeBoxDocument document) {
        this.document = document;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        paneHeader = new PaneHeader("Network");
        paneHeader.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        NButton newNodeButton = new NButton("New Node", getClass().getResourceAsStream("/network-new-node.png"));
        newNodeButton.setToolTipText("New Node (TAB)");
        newNodeButton.setActionMethod(this, "showNodeSelectionDialog");
        paneHeader.add(newNodeButton);
        add(paneHeader);

        errorLabel = new MessageBar("Error");
        errorLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        errorLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2 || SwingUtilities.isRightMouseButton(e)) {
                    ExceptionDialog ed = new ExceptionDialog(null, nodeRenderException, "", false);
                    ed.setVisible(true);
                } else if (currentErrorNode != null) {
                    networkView.focusNode(currentErrorNode);
                } else {
                    ExceptionDialog ed = new ExceptionDialog(null, nodeRenderException, "", false);
                    ed.setVisible(true);
                }
            }
        });
        errorLabel.setVisible(false);
        add(errorLabel);

        networkView = new NetworkView(document);
        document.addZoomListener(networkView);
        networkView.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        add(networkView);
    }

    public NetworkView getNetworkView() {
        return networkView;
    }

    public Pane duplicate() {
        return new NetworkPane(document);
    }

    public PaneHeader getPaneHeader() {
        return paneHeader;
    }

    public PaneView getPaneView() {
        return networkView;
    }

    public void showNodeSelectionDialog() {
        document.showNodeSelectionDialog();
    }

    public void setError(Throwable e) {
        Node node = null;
        if (e instanceof NodeRenderException) {
            node = ((NodeRenderException) e).getNode();
        }
        currentErrorNode = node;

        nodeRenderException = getRootCause(e);
        String rawMessage = "";
        if (nodeRenderException instanceof PyException) {
            PyException ex = (PyException) nodeRenderException;
            rawMessage = (ex.value != null) ? ex.value.toString() : ex.toString();
        } else if (nodeRenderException instanceof OutOfMemoryError) {
            rawMessage = "Out of memory. Are you trying to process an infinite list?";
        } else if (nodeRenderException != null && nodeRenderException.getMessage() != null) {
            rawMessage = nodeRenderException.getMessage();
        } else if (nodeRenderException != null) {
            rawMessage = nodeRenderException.getClass().getSimpleName();
        }
        currentErrorMessage = rawMessage;

        // Synchronize error node state with NetworkView for visual outline and mini-map
        networkView.setErrorNode(node, rawMessage);

        StringBuilder sb = new StringBuilder("<html>&nbsp;&nbsp;&nbsp;");
        if (node != null) {
            sb.append("<b>");
            sb.append(node.getName());
            sb.append(":</b> ");
        }
        sb.append("<u>");
        sb.append(escapeHtml(rawMessage));
        sb.append("</u>&nbsp;&nbsp;<span style='color: #FEF08A; font-size: 10px;'>[Click to Focus &bull; Double-click Details]</span></html>");
        errorLabel.setText(sb.toString());
        errorLabel.setToolTipText(generateErrorTooltip(node, nodeRenderException, rawMessage));
        errorLabel.setVisible(true);
    }

    public void clearError() {
        currentErrorNode = null;
        currentErrorMessage = null;
        nodeRenderException = null;
        errorLabel.setText("");
        errorLabel.setToolTipText(null);
        errorLabel.setVisible(false);
        networkView.clearErrorNode();
    }

    public static String getSuggestedFix(String errorMessage) {
        if (errorMessage == null) {
            return "Inspect the node's parameters and input connections in the Port View.";
        }
        String lower = errorMessage.toLowerCase();
        if (lower.contains("wrong number of arguments") || lower.contains("takes exactly") ||
            lower.contains("takes at least") || lower.contains("takes at most") || lower.contains("positional arguments")) {
            return "The function received an unexpected number of parameters. Verify that connected input ports match expected arguments and no outdated connections exist.";
        }
        if (lower.contains("nonetype") || lower.contains("'none' object") || lower.contains("nullpointer")) {
            return "A null or None value was received. Check that upstream nodes connected to this node's inputs are successfully producing valid data.";
        }
        if (lower.contains("zerodivision") || lower.contains("by zero")) {
            return "Division by zero occurred in a calculation. Verify that denominator inputs or range start/step parameters are not zero.";
        }
        if (lower.contains("outofmemory") || lower.contains("heap space") || lower.contains("infinite list")) {
            return "Memory limit exceeded. Reduce count, grid dimensions, resample point density, or recursion depth.";
        }
        if (lower.contains("indexerror") || lower.contains("out of range")) {
            return "Index out of range. The requested index is larger than the input list length. Check list length or index parameter.";
        }
        if (lower.contains("keyerror")) {
            return "Key or attribute not found. Check port names or dictionary lookup keys.";
        }
        if (lower.contains("typeerror") || lower.contains("cannot convert") || lower.contains("incompatible type")) {
            return "Data type mismatch. Ensure the connected data matches the port's expected type (e.g. number vs geometry vs color).";
        }
        if (lower.contains("syntaxerror") || lower.contains("parsing error")) {
            return "Syntax error in expression or script. Check expression syntax, quotes, and brackets.";
        }
        return "Check the node's inputs in the Port View to verify parameters and upstream connections.";
    }

    public static String generateErrorTooltip(Node node, Throwable ex, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='width: 360px; font-family: sans-serif; font-size: 11px; padding: 6px;'>");

        sb.append("<div style='margin-bottom: 6px;'>");
        sb.append("<b style='color: #EF4444; font-size: 12px;'>&#9888; Node Execution Warning / Error</b>");
        if (node != null) {
            sb.append("<br/><span style='color: #94A3B8;'>Node: </span><b style='color: #38BDF8;'>").append(node.getName()).append("</b>");
            if (node.getPrototype() != null) {
                sb.append(" <span style='color: #64748B;'>(").append(node.getPrototype().getName()).append(")</span>");
            }
        }
        sb.append("</div>");

        sb.append("<div style='background-color: #1E293B; color: #F8FAFC; padding: 6px 8px; border-radius: 4px; font-family: monospace; font-size: 11px; margin-bottom: 8px;'>");
        if (ex != null) {
            sb.append("<span style='color: #F87171;'>").append(ex.getClass().getSimpleName()).append(": </span>");
        }
        sb.append(escapeHtml(errorMessage));
        sb.append("</div>");

        String fix = getSuggestedFix(errorMessage);
        sb.append("<div style='margin-bottom: 8px;'>");
        sb.append("<b style='color: #F59E0B;'>&#128161; Suggested Fix / Help:</b><br/>");
        sb.append("<span style='color: #E2E8F0; line-height: 1.3;'>").append(fix).append("</span>");
        sb.append("</div>");

        sb.append("<div style='color: #94A3B8; font-size: 10px; border-top: 1px solid #475569; padding-top: 4px; margin-top: 6px;'>");
        sb.append("<b>Shortcuts:</b> Click bar to <i>Jump & Focus Node</i> &bull; Double-click for <i>Stack Trace</i>");
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    public static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

}
