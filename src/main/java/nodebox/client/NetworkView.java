package nodebox.client;

import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.*;
import nodebox.node.*;
import nodebox.ui.PaneView;
import nodebox.ui.Platform;
import nodebox.ui.Theme;
import nodebox.ui.Zoom;
import org.python.google.common.base.Joiner;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

public class NetworkView extends ZoomableView implements PaneView, Zoom {

    public static final int GRID_CELL_SIZE = 48;
    public static final int NODE_MARGIN = 6;
    public static final int NODE_PADDING = 5;
    public static final int NODE_WIDTH = GRID_CELL_SIZE * 3 - NODE_MARGIN * 2;
    public static final int NODE_HEIGHT = GRID_CELL_SIZE - NODE_MARGIN * 2;
    public static final int NODE_ICON_SIZE = 26;
    public static final int GRID_OFFSET = 6;
    public static final int PORT_WIDTH = 10;
    public static final int PORT_HEIGHT = 3;
    public static final int PORT_MARGIN = 6;
    public static final int PORT_SPACING = 10;
    public static final Dimension NODE_DIMENSION = new Dimension(NODE_WIDTH, NODE_HEIGHT);

    public static final String SELECT_PROPERTY = "NetworkView.select";
    public static final int COMMENT_BOX_MARGIN_HORIZONTAL = 5;

    private static Map<String, BufferedImage> fileImageCache = new HashMap<String, BufferedImage>();
    private static BufferedImage nodeGeneric, commentIcon, commentBox;

    public static final float MIN_ZOOM = 0.05f;
    public static final float MAX_ZOOM = 1.0f;

    public static final Map<String, Color> PORT_COLORS = Maps.newHashMap();
    public static final Color DEFAULT_PORT_COLOR = new Color(52, 85, 52);
    public static final Color OUTPUT_PORT_COLOR = new Color(0x22, 0xB1, 0x4C);
    public static final Color PORT_HOVER_COLOR = Color.YELLOW;
    public static final Color TOOLTIP_BACKGROUND_COLOR = new Color(254, 255, 215);
    public static final Color TOOLTIP_STROKE_COLOR = Color.DARK_GRAY;
    public static final Color TOOLTIP_TEXT_COLOR = Color.DARK_GRAY;
    public static final Color DRAG_SELECTION_COLOR = new Color(255, 255, 255, 100);
    public static final BasicStroke DRAG_SELECTION_STROKE = new BasicStroke(1f);
    public static final BasicStroke CONNECTION_STROKE = new BasicStroke(2);

    private final NodeBoxDocument document;

    private JPopupMenu networkMenu;
    private Point networkMenuLocation;

    private Point nodeMenuLocation;

    private LoadingCache<Node, BufferedImage> nodeImageCache;

    private Set<String> selectedNodes = new HashSet<String>();

    // Interaction state
    private boolean isDraggingNodes = false;
    private boolean isShiftPressed = false;
    private boolean isAltPressed = false;
    private boolean isDragSelecting = false;
    private ImmutableMap<String, nodebox.graphics.Point> dragPositions = ImmutableMap.of();
    private NodePort overInput;
    private Node overOutput;
    private Node overComment;
    private Node connectionOutput;
    private NodePort connectionInput;
    private Point2D connectionPoint;
    private boolean startDragging;
    private Point2D dragStartPoint;
    private Point2D dragCurrentPoint;
    private Point lastMousePoint = null;
    private boolean showProfiler = false;
    private Map<String, Long> executionTimes = new HashMap<String, Long>();
    private Rectangle miniMapBounds = new Rectangle();
    private boolean showMiniMap = true;
    private Node errorNode = null;
    private String errorNodeMessage = null;

    static {
        try {
            nodeGeneric = ImageIO.read(NetworkView.class.getResourceAsStream("/node-generic.png"));
            commentIcon = ImageIO.read(NetworkView.class.getResourceAsStream("/comment-icon.png"));
            commentBox = ImageIO.read(NetworkView.class.getResourceAsStream("/notes-background.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        PORT_COLORS.put(Port.TYPE_INT, new Color(116, 119, 121));
        PORT_COLORS.put(Port.TYPE_FLOAT, new Color(116, 119, 121));
        PORT_COLORS.put(Port.TYPE_STRING, new Color(92, 90, 91));
        PORT_COLORS.put(Port.TYPE_BOOLEAN, new Color(92, 90, 91));
        PORT_COLORS.put(Port.TYPE_POINT, new Color(119, 154, 173));
        PORT_COLORS.put(Port.TYPE_COLOR, new Color(94, 85, 112));
        PORT_COLORS.put("geometry", new Color(20, 20, 20));
        PORT_COLORS.put("list", new Color(76, 137, 174));
        PORT_COLORS.put("data", new Color(52, 85, 129));
    }

    /**
     * Tries to find an image representation for the node.
     * The image should be located near the library, and have the same name as the library.
     * <p/>
     * If this node has no image, the prototype is searched to find its image. If no image could be found,
     * a generic image is returned.
     *
     * @param node           the node
     * @param nodeRepository the list of nodes to look for the icon
     * @return an Image object.
     */
    public static BufferedImage getImageForNode(Node node, NodeRepository nodeRepository) {
        for (NodeLibrary library : nodeRepository.getLibraries()) {
            BufferedImage img = findNodeImage(library, node);
            if (img != null) {
                return img;
            }
        }
        if (node.getPrototype() != null) {
            return getImageForNode(node.getPrototype(), nodeRepository);
        } else {
            return nodeGeneric;
        }
    }

    public static BufferedImage findNodeImage(NodeLibrary library, Node node) {
        if (node == null || node.getImage() == null || node.getImage().isEmpty()) return null;
        if (!library.getRoot().hasChild(node)) return null;

        File libraryDirectory = null;
        if (library.getFile() != null)
            libraryDirectory = library.getFile().getParentFile();
        else if (library.equals(NodeLibrary.coreLibrary))
            libraryDirectory = new File("libraries/core");

        if (libraryDirectory != null) {
            File nodeImageFile = new File(libraryDirectory, node.getImage());
            if (nodeImageFile.exists()) {
                return readNodeImage(nodeImageFile);
            }
        }
        return null;
    }

    public static BufferedImage readNodeImage(File nodeImageFile) {
        String imagePath = nodeImageFile.getAbsolutePath();
        if (fileImageCache.containsKey(imagePath)) {
            return fileImageCache.get(imagePath);
        } else {
            try {
                BufferedImage image = ImageIO.read(nodeImageFile);
                fileImageCache.put(imagePath, image);
                return image;
            } catch (IOException e) {
                return null;
            }
        }
    }

    public NetworkView(NodeBoxDocument document) {
        super(MIN_ZOOM, MAX_ZOOM);
        this.document = document;
        setBackground(Theme.NETWORK_BACKGROUND_COLOR);
        initEventHandlers();
        initMenus();
        nodeImageCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .build(new NodeImageCacheLoader(document.getNodeRepository()));
    }

    private void initEventHandlers() {
        setFocusable(true);
        // This is disabled so we can detect the tab key.
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new KeyHandler());
        MouseHandler mh = new MouseHandler();
        addMouseListener(mh);
        addMouseMotionListener(mh);
        addFocusListener(new FocusHandler());
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Point2D pt = inverseViewTransformPoint(e.getPoint());
        Node node = getNodeAt(pt);
        if (node != null) {
            if (isErrorNode(node)) {
                return getErrorHtmlTooltip(node);
            }
            return NodeDocumentation.getHtmlTooltip(node);
        }
        return null;
    }

    private String getErrorHtmlTooltip(Node node) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='width: 340px; font-family: sans-serif; font-size: 11px; padding: 6px;'>");
        sb.append("<div style='margin-bottom: 6px;'>");
        sb.append("<b style='color: #EF4444; font-size: 12px;'>&#9888; Node Execution Error</b>");
        sb.append("<br/><span style='color: #94A3B8;'>Node: </span><b style='color: #38BDF8;'>").append(node.getName()).append("</b>");
        sb.append("</div>");

        if (errorNodeMessage != null && !errorNodeMessage.trim().isEmpty()) {
            sb.append("<div style='background-color: #1E293B; color: #F8FAFC; padding: 6px 8px; border-radius: 4px; font-family: monospace; font-size: 11px; margin-bottom: 6px;'>");
            sb.append(escapeHtml(errorNodeMessage));
            sb.append("</div>");
        }

        String fix = NetworkPane.getSuggestedFix(errorNodeMessage);
        if (fix != null) {
            sb.append("<div style='margin-bottom: 6px;'>");
            sb.append("<b style='color: #F59E0B;'>&#128161; Suggested Fix:</b><br/>");
            sb.append("<span style='color: #E2E8F0;'>").append(fix).append("</span>");
            sb.append("</div>");
        }

        String doc = NodeDocumentation.getHtmlTooltip(node);
        if (doc != null && !doc.isEmpty()) {
            sb.append("<div style='border-top: 1px solid #475569; padding-top: 6px; margin-top: 6px;'>");
            String innerDoc = doc.replaceAll("(?i)</?html>|</?body[^>]*>", "");
            sb.append(innerDoc);
            sb.append("</div>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private void initMenus() {
        networkMenu = new JPopupMenu();
        networkMenu.add(new NewNodeAction());
        networkMenu.add(new AbstractAction("Quick Add Node (Tab)") {
            public void actionPerformed(ActionEvent e) {
                Point pt = lastMousePoint != null ? lastMousePoint : new Point(getWidth() / 2, getHeight() / 2);
                Point gridPoint = pointToGridPoint(pt);
                Point screenPoint = null;
                try {
                    Point pOnScreen = getLocationOnScreen();
                    screenPoint = new Point(pOnScreen.x + pt.x, pOnScreen.y + pt.y);
                } catch (Exception ex) {
                    screenPoint = pt;
                }
                getDocument().showQuickAddDialog(gridPoint, screenPoint);
            }
        });
        networkMenu.add(new ResetViewAction());
        networkMenu.add(new AbstractAction("Zoom to Fit (F)") {
            public void actionPerformed(ActionEvent e) {
                zoomToFit();
            }
        });
        networkMenu.add(new GoUpAction());
        networkMenu.addSeparator();
        networkMenu.add(new AbstractAction(showProfiler ? "Hide Node Profiler (P)" : "Show Node Profiler (P)") {
            public void actionPerformed(ActionEvent e) {
                showProfiler = !showProfiler;
                repaint();
            }
        });
        Theme.applyPopupMenuTheme(networkMenu);
    }

    private JPopupMenu createNodeMenu(Node node) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(new SetRenderedAction());
        menu.add(new RenameAction());
        menu.add(new DeleteAction());
        menu.add(new GroupIntoNetworkAction(null));

        if (node.isNetwork()) {
            menu.add(new GoInAction());
        }
        if (!node.hasComment()) {
            menu.add(new AddCommentAction());
        } else {
            menu.add(new EditCommentAction());
            menu.add(new RemoveCommentAction());
        }

        if (selectedNodes.size() > 1) {
            menu.addSeparator();
            JMenu alignMenu = new JMenu("Align");
            alignMenu.add(new JMenuItem(new AbstractAction("Align Left") {
                public void actionPerformed(ActionEvent e) { alignSelectedNodes("left"); }
            }));
            alignMenu.add(new JMenuItem(new AbstractAction("Align Right") {
                public void actionPerformed(ActionEvent e) { alignSelectedNodes("right"); }
            }));
            alignMenu.add(new JMenuItem(new AbstractAction("Align Center Horizontally") {
                public void actionPerformed(ActionEvent e) { alignSelectedNodes("center-x"); }
            }));
            alignMenu.add(new JMenuItem(new AbstractAction("Align Top") {
                public void actionPerformed(ActionEvent e) { alignSelectedNodes("top"); }
            }));
            alignMenu.add(new JMenuItem(new AbstractAction("Align Bottom") {
                public void actionPerformed(ActionEvent e) { alignSelectedNodes("bottom"); }
            }));
            alignMenu.add(new JMenuItem(new AbstractAction("Align Center Vertically") {
                public void actionPerformed(ActionEvent e) { alignSelectedNodes("center-y"); }
            }));
            menu.add(alignMenu);

            if (selectedNodes.size() > 2) {
                JMenu distributeMenu = new JMenu("Distribute");
                distributeMenu.add(new JMenuItem(new AbstractAction("Distribute Horizontally") {
                    public void actionPerformed(ActionEvent e) { distributeSelectedNodes("horizontal"); }
                }));
                distributeMenu.add(new JMenuItem(new AbstractAction("Distribute Vertically") {
                    public void actionPerformed(ActionEvent e) { distributeSelectedNodes("vertical"); }
                }));
                menu.add(distributeMenu);
            }
        }

        menu.addSeparator();
        menu.add(new AbstractAction(showProfiler ? "Hide Node Profiler (P)" : "Show Node Profiler (P)") {
            public void actionPerformed(ActionEvent e) {
                showProfiler = !showProfiler;
                repaint();
            }
        });

        menu.add(new HelpAction());
        Theme.applyPopupMenuTheme(menu);
        return menu;
    }

    public NodeBoxDocument getDocument() {
        return document;
    }

    public Node getActiveNetwork() {
        return document.getActiveNetwork();
    }

    //// Events ////

    /**
     * Refresh the nodes and connections cache.
     */
    public void updateAll() {
        updateNodes();
        updateConnections();
    }

    public void updateNodes() {
        repaint();
    }

    public void updateConnections() {
        repaint();
    }

    public void updatePosition(Node node) {
        updateConnections();
    }

    public void setErrorNode(Node node, String message) {
        this.errorNode = node;
        this.errorNodeMessage = message;
        repaint();
    }

    public void clearErrorNode() {
        this.errorNode = null;
        this.errorNodeMessage = null;
        repaint();
    }

    public Node getErrorNode() {
        return errorNode;
    }

    public String getErrorNodeMessage() {
        return errorNodeMessage;
    }

    public boolean isErrorNode(Node node) {
        if (node == null || errorNode == null) return false;
        return node == errorNode || (node.getName() != null && node.getName().equals(errorNode.getName()));
    }

    public void focusNode(Node node) {
        if (node == null) return;
        Rectangle r = nodeRect(node);
        double scale = getViewScale();
        double viewW = getWidth();
        double viewH = getHeight();
        if (viewW > 0 && viewH > 0) {
            double vx = (viewW / 2.0) - (r.getCenterX() * scale);
            double vy = (viewH / 2.0) - (r.getCenterY() * scale);
            setViewTransform(vx, vy, scale);
        }
        try {
            if (document != null && document.getActiveNetwork() != null && document.getActiveNetwork().hasChild(node.getName())) {
                document.setActiveNode(node);
            }
        } catch (Exception ignored) {
        }
        repaint();
    }

    public void checkErrorAndRepaint() {
        repaint();
    }

    public void codeChanged(Node node, boolean changed) {
        repaint();
    }

    //// Model queries ////

    private ImmutableList<Node> getNodes() {
        return getDocument().getActiveNetwork().getChildren();
    }

    private ImmutableList<Node> getNodesReversed() {
        return getNodes().reverse();
    }

    private Iterable<Connection> getConnections() {
        return getDocument().getActiveNetwork().getConnections();
    }

    public static boolean isPublished(Node network, Node childNode, Port childPort) {
        return network.hasPublishedInput(childNode.getName(), childPort.getName());
    }

    //// Painting the nodes ////

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Draw background
        g2.setColor(Theme.NETWORK_BACKGROUND_COLOR);
        g2.fill(g.getClipBounds());

        // Paint the grid
        // (The grid is not really affected by the view transform)
        paintGrid(g2);

        // Set the view transform
        AffineTransform originalTransform = g2.getTransform();
        g2.transform(getViewTransform());

        paintNodes(g2);
        paintConnections(g2);
        paintCurrentConnection(g2);
        paintPortTooltip(g2);
        paintDragSelection(g2);
        paintCommentBox(g2);

        // Restore original transform
        g2.setTransform(originalTransform);

        if (showMiniMap) {
            paintMiniMap(g2);
        }
    }

    private void paintMiniMap(Graphics2D g) {
        java.util.Collection<Node> nodes = getNodes();
        if (nodes.isEmpty()) return;

        int mapW = 160;
        int mapH = 110;
        int mapMargin = 15;
        int mapX = getWidth() - mapW - mapMargin;
        int mapY = getHeight() - mapH - mapMargin;
        miniMapBounds.setBounds(mapX, mapY, mapW, mapH);

        // Compute network bounding box
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Node n : nodes) {
            nodebox.graphics.Point pt = n.getPosition();
            double nx = pt.x * GRID_CELL_SIZE;
            double ny = pt.y * GRID_CELL_SIZE;
            if (nx < minX) minX = nx;
            if (nx + NODE_WIDTH > maxX) maxX = nx + NODE_WIDTH;
            if (ny < minY) minY = ny;
            if (ny + NODE_HEIGHT > maxY) maxY = ny + NODE_HEIGHT;
        }

        Point2D viewTopLeft = inverseViewTransformPoint(new Point(0, 0));
        Point2D viewBottomRight = inverseViewTransformPoint(new Point(getWidth(), getHeight()));
        if (viewTopLeft.getX() < minX) minX = viewTopLeft.getX();
        if (viewBottomRight.getX() > maxX) maxX = viewBottomRight.getX();
        if (viewTopLeft.getY() < minY) minY = viewTopLeft.getY();
        if (viewBottomRight.getY() > maxY) maxY = viewBottomRight.getY();

        double pad = 60.0;
        minX -= pad; minY -= pad;
        maxX += pad; maxY += pad;
        double spanX = Math.max(1.0, maxX - minX);
        double spanY = Math.max(1.0, maxY - minY);

        double scale = Math.min((mapW - 16) / spanX, (mapH - 16) / spanY);
        double offsetX = mapX + 8 + ((mapW - 16) - spanX * scale) / 2.0;
        double offsetY = mapY + 8 + ((mapH - 16) - spanY * scale) / 2.0;

        // Draw translucent dark card
        g.setColor(new Color(24, 26, 32, 215));
        g.fillRoundRect(mapX, mapY, mapW, mapH, 10, 10);
        g.setColor(new Color(65, 70, 85, 180));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(mapX, mapY, mapW, mapH, 10, 10);

        // Draw nodes as dots
        Node renderedNode = getActiveNetwork().getRenderedChild();
        for (Node n : nodes) {
            nodebox.graphics.Point pt = n.getPosition();
            double nx = pt.x * GRID_CELL_SIZE;
            double ny = pt.y * GRID_CELL_SIZE;
            int dotX = (int) Math.round(offsetX + (nx - minX) * scale);
            int dotY = (int) Math.round(offsetY + (ny - minY) * scale);
            int dotW = Math.max(3, (int) Math.round(NODE_WIDTH * scale));
            int dotH = Math.max(2, (int) Math.round(NODE_HEIGHT * scale));

            if (isErrorNode(n)) {
                g.setColor(new Color(239, 68, 68)); // Red highlight for error node
            } else if (isSelected(n)) {
                g.setColor(new Color(249, 115, 22)); // Orange highlight
            } else if (n == renderedNode) {
                g.setColor(new Color(34, 197, 94)); // Green highlight
            } else {
                g.setColor(new Color(148, 163, 184)); // Slate light gray
            }
            g.fillRoundRect(dotX, dotY, dotW, dotH, 2, 2);
        }

        // Draw current viewport rectangle
        int vpX = (int) Math.round(offsetX + (viewTopLeft.getX() - minX) * scale);
        int vpY = (int) Math.round(offsetY + (viewTopLeft.getY() - minY) * scale);
        int vpW = (int) Math.round((viewBottomRight.getX() - viewTopLeft.getX()) * scale);
        int vpH = (int) Math.round((viewBottomRight.getY() - viewTopLeft.getY()) * scale);

        g.setColor(new Color(56, 189, 248, 40));
        g.fillRect(vpX, vpY, vpW, vpH);
        g.setColor(new Color(56, 189, 248, 220));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRect(vpX, vpY, vpW, vpH);

        // Mini-map badge
        g.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g.setColor(new Color(148, 163, 184, 180));
        g.drawString("RADAR", mapX + 8, mapY + 12);
    }

    private void handleMiniMapClick(Point clickPt) {
        java.util.Collection<Node> nodes = getNodes();
        if (nodes.isEmpty() || miniMapBounds.isEmpty()) return;

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Node n : nodes) {
            nodebox.graphics.Point pt = n.getPosition();
            double nx = pt.x * GRID_CELL_SIZE;
            double ny = pt.y * GRID_CELL_SIZE;
            if (nx < minX) minX = nx;
            if (nx + NODE_WIDTH > maxX) maxX = nx + NODE_WIDTH;
            if (ny < minY) minY = ny;
            if (ny + NODE_HEIGHT > maxY) maxY = ny + NODE_HEIGHT;
        }
        Point2D viewTopLeft = inverseViewTransformPoint(new Point(0, 0));
        Point2D viewBottomRight = inverseViewTransformPoint(new Point(getWidth(), getHeight()));
        if (viewTopLeft.getX() < minX) minX = viewTopLeft.getX();
        if (viewBottomRight.getX() > maxX) maxX = viewBottomRight.getX();
        if (viewTopLeft.getY() < minY) minY = viewTopLeft.getY();
        if (viewBottomRight.getY() > maxY) maxY = viewBottomRight.getY();

        double pad = 60.0;
        minX -= pad; minY -= pad;
        maxX += pad; maxY += pad;
        double spanX = Math.max(1.0, maxX - minX);
        double spanY = Math.max(1.0, maxY - minY);

        double scale = Math.min((miniMapBounds.width - 16) / spanX, (miniMapBounds.height - 16) / spanY);
        double offsetX = miniMapBounds.x + 8 + ((miniMapBounds.width - 16) - spanX * scale) / 2.0;
        double offsetY = miniMapBounds.y + 8 + ((miniMapBounds.height - 16) - spanY * scale) / 2.0;

        double targetNetX = minX + (clickPt.x - offsetX) / scale;
        double targetNetY = minY + (clickPt.y - offsetY) / scale;

        double newViewX = getWidth() / 2.0 - targetNetX * getViewScale();
        double newViewY = getHeight() / 2.0 - targetNetY * getViewScale();
        setViewTransform(newViewX, newViewY, getViewScale());
    }

    private void paintGrid(Graphics2D g) {
        g.setColor(Theme.NETWORK_GRID_COLOR);

        int gridCellSize = (int) Math.round(GRID_CELL_SIZE * getViewScale());
        int gridOffset = (int) Math.round(GRID_OFFSET * getViewScale());
        if (gridCellSize < 10) return;

        int transformOffsetX = (int) (getViewX() % gridCellSize);
        int transformOffsetY = (int) (getViewY() % gridCellSize);

        for (int y = -gridCellSize; y < getHeight() + gridCellSize; y += gridCellSize) {
            g.drawLine(0, y - gridOffset + transformOffsetY, getWidth(), y - gridOffset + transformOffsetY);
        }
        for (int x = -gridCellSize; x < getWidth() + gridCellSize; x += gridCellSize) {
            g.drawLine(x - gridOffset + transformOffsetX, 0, x - gridOffset + transformOffsetX, getHeight());
        }
    }

    private void paintConnections(Graphics2D g) {
        g.setColor(Theme.CONNECTION_DEFAULT_COLOR);
        g.setStroke(CONNECTION_STROKE);
        for (Connection connection : getConnections()) {
            paintConnection(g, connection);
        }
    }

    private void paintConnection(Graphics2D g, Connection connection) {
        Node outputNode = findNodeWithName(connection.getOutputNode());
        Node inputNode = findNodeWithName(connection.getInputNode());
        Port inputPort = inputNode.getInput(connection.getInputPort());
        g.setColor(portTypeColor(outputNode.getOutputType()));
        Rectangle outputRect = nodeRect(outputNode);
        Rectangle inputRect = nodeRect(inputNode);
        paintConnectionLine(g, outputRect.x + 4, outputRect.y + outputRect.height + 1, inputRect.x + portOffset(inputNode, inputPort) + getPortWidth(inputNode) / 2, inputRect.y - 4);

    }

    private void paintCurrentConnection(Graphics2D g) {
        g.setColor(Theme.CONNECTION_DEFAULT_COLOR);
        if (connectionOutput != null) {
            Rectangle outputRect = nodeRect(connectionOutput);
            g.setColor(portTypeColor(connectionOutput.getOutputType()));
            paintConnectionLine(g, outputRect.x + 4, outputRect.y + outputRect.height + 1, (int) connectionPoint.getX(), (int) connectionPoint.getY());
        }
    }

    private static void paintConnectionLine(Graphics2D g, int x0, int y0, int x1, int y1) {
        String cableStyle = Application.getInstance() != null ? Application.getInstance().getCableStyle() : Application.CABLE_STYLE_CURVED;
        if (Application.CABLE_STYLE_STRAIGHT.equals(cableStyle)) {
            g.drawLine(x0, y0, x1, y1);
        } else if (Application.CABLE_STYLE_ORTHOGONAL.equals(cableStyle)) {
            int yMid = (y0 + y1) / 2;
            GeneralPath p = new GeneralPath();
            p.moveTo(x0, y0);
            p.lineTo(x0, yMid);
            p.lineTo(x1, yMid);
            p.lineTo(x1, y1);
            g.draw(p);
        } else {
            double dy = Math.abs(y1 - y0);
            if (dy < GRID_CELL_SIZE) {
                g.drawLine(x0, y0, x1, y1);
            } else {
                double halfDx = Math.abs(x1 - x0) / 2.0;
                GeneralPath p = new GeneralPath();
                p.moveTo(x0, y0);
                p.curveTo(x0, y0 + halfDx, x1, y1 - halfDx, x1, y1);
                g.draw(p);
            }
        }
    }

    private void paintNodes(Graphics2D g) {
        g.setColor(Theme.NETWORK_NODE_NAME_COLOR);
        Node renderedNode = getActiveNetwork().getRenderedChild();
        for (Node node : getNodes()) {
            Port hoverInputPort = overInput != null && overInput.node.equals(node.getName()) ? findNodeWithName(overInput.node).getInput(overInput.port) : null;
            BufferedImage icon = getCachedImageForNode(node);
            paintNode(g, getActiveNetwork(), node, icon, commentIcon, isSelected(node), renderedNode == node, connectionOutput, hoverInputPort, overOutput == node);
        }
    }

    private BufferedImage getCachedImageForNode(Node node) {
        try {
            return nodeImageCache.get(node);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Map<BufferedImage, BufferedImage> invertedImageCache = new java.util.WeakHashMap<BufferedImage, BufferedImage>();

    public static BufferedImage getInvertedImage(BufferedImage src) {
        if (src == null) return null;
        synchronized (invertedImageCache) {
            BufferedImage inverted = invertedImageCache.get(src);
            if (inverted != null) return inverted;
            int width = src.getWidth();
            int height = src.getHeight();
            inverted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgba = src.getRGB(x, y);
                    int a = (rgba >> 24) & 0xff;
                    int r = 255 - ((rgba >> 16) & 0xff);
                    int g = 255 - ((rgba >> 8) & 0xff);
                    int b = 255 - (rgba & 0xff);
                    inverted.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }
            invertedImageCache.put(src, inverted);
            return inverted;
        }
    }

    public static Color invertColor(Color c) {
        if (c == null) return null;
        return new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue(), c.getAlpha());
    }

    public static Color portTypeColor(String type) {
        Color portColor = PORT_COLORS.get(type);
        if (portColor == null) portColor = DEFAULT_PORT_COLOR;
        if (Theme.isDark()) {
            return invertColor(portColor);
        }
        return portColor;
    }

    private static String getShortenedName(String name, int startChars) {
        nodebox.graphics.Text text = new nodebox.graphics.Text(name, nodebox.graphics.Point.ZERO);
        text.setFontName(Theme.NETWORK_FONT.getFontName());
        text.setFontSize(Theme.NETWORK_FONT.getSize());
        int cells = Math.min(Math.max(3, 1 + (int) Math.ceil(text.getMetrics().getWidth() / (GRID_CELL_SIZE - 6))), 6);
        if (cells > 3)
            return getShortenedName(name.substring(0, startChars) + "\u2026" + name.substring(name.length() - 3, name.length()), startChars - 1);
        return name;
    }

    private void paintNode(Graphics2D g, Node network, Node node, BufferedImage icon, BufferedImage commentIcon, boolean selected, boolean rendered, Node connectionOutput, Port hoverInputPort, boolean hoverOutput) {
        Rectangle r = nodeRect(node);
        String outputType = node.getOutputType();
        Color nodeColor = portTypeColor(outputType);
        double luminance = (0.299 * nodeColor.getRed() + 0.587 * nodeColor.getGreen() + 0.114 * nodeColor.getBlue());
        boolean lightNode = luminance > 130;

        boolean isError = isErrorNode(node);

        // Draw error outline / glow or selection ring
        if (isError) {
            g.setColor(new Color(239, 68, 68, 80));
            g.fillRoundRect(r.x - 4, r.y - 4, NODE_WIDTH + 8, NODE_HEIGHT + 8, 8, 8);
            g.setColor(new Color(239, 68, 68));
            g.setStroke(new BasicStroke(selected ? 3.5f : 2.5f));
            g.drawRoundRect(r.x - 2, r.y - 2, NODE_WIDTH + 3, NODE_HEIGHT + 3, 6, 6);
        } else if (selected) {
            // Draw selection ring
            g.setColor(Theme.isDark() ? new Color(56, 189, 248) : Color.WHITE);
            g.fillRect(r.x, r.y, NODE_WIDTH, NODE_HEIGHT);
        }

        // Draw node
        g.setColor(nodeColor);
        if (isError || selected) {
            g.fillRect(r.x + 2, r.y + 2, NODE_WIDTH - 4, NODE_HEIGHT - 4);
        } else {
            g.fillRect(r.x, r.y, NODE_WIDTH, NODE_HEIGHT);
        }

        // Draw render flag
        if (rendered) {
            g.setColor(lightNode ? new Color(24, 24, 27) : Color.WHITE);
            GeneralPath gp = new GeneralPath();
            gp.moveTo(r.x + NODE_WIDTH - 2, r.y + NODE_HEIGHT - 20);
            gp.lineTo(r.x + NODE_WIDTH - 2, r.y + NODE_HEIGHT - 2);
            gp.lineTo(r.x + NODE_WIDTH - 20, r.y + NODE_HEIGHT - 2);
            g.fill(gp);
        }

        // Draw input ports
        g.setColor(Color.WHITE);
        int pw = getPortWidth(node);
        for (Port input : node.getInputs()) {
            if (isHiddenPort(input)) {
                continue;
            }
            int portX = portOffset(node, input);
            if (hoverInputPort == input) {
                g.setColor(PORT_HOVER_COLOR);
            } else {
                g.setColor(portTypeColor(input.getType()));
            }
            // Highlight ports that match the dragged connection type
            int portHeight = PORT_HEIGHT;
            if (connectionOutput != null) {
                String connectionOutputType = connectionOutput.getOutputType();
                String inputType = input.getType();
                if (connectionOutputType.equals(inputType) || inputType.equals(Port.TYPE_LIST)) {
                    portHeight = PORT_HEIGHT * 2;
                } else if (TypeConversions.canBeConverted(connectionOutputType, inputType)) {
                    portHeight = PORT_HEIGHT - 1;
                } else {
                    portHeight = 1;
                }
            }

            if (isPublished(network, node, input)) {
                Point2D topLeft = inverseViewTransformPoint(new Point(4, 0));
                g.setColor(portTypeColor(input.getType()));
                g.setStroke(CONNECTION_STROKE);
                paintConnectionLine(g, (int) topLeft.getX(), (int) topLeft.getY(), r.x + portX + pw / 2, r.y - 2);
            }

            g.fillRect(r.x + portX, r.y - portHeight, pw, portHeight);
        }

        // Draw output port in green #22B14C
        if (hoverOutput && connectionOutput == null) {
            g.setColor(PORT_HOVER_COLOR);
        } else {
            g.setColor(OUTPUT_PORT_COLOR);
        }
        g.fillRect(r.x, r.y + NODE_HEIGHT, PORT_WIDTH, PORT_HEIGHT);

        // Draw icon
        BufferedImage drawIcon = lightNode ? getInvertedImage(icon) : icon;
        g.drawImage(drawIcon, r.x + NODE_PADDING, r.y + NODE_PADDING, NODE_ICON_SIZE, NODE_ICON_SIZE, null);
        g.setColor(lightNode ? new Color(24, 24, 27) : Color.WHITE);
        g.setFont(Theme.NETWORK_FONT);
        g.drawString(getShortenedName(node.getName(), 7), r.x + NODE_ICON_SIZE + NODE_PADDING * 2 + 2, r.y + 22);

        // Draw comment icon
        if (node.hasComment()) {
            BufferedImage drawComment = lightNode ? getInvertedImage(commentIcon) : commentIcon;
            g.drawImage(drawComment, r.x + NODE_WIDTH - 13, r.y + 5, null);
        }

        // Draw error alert badge (!)
        if (isError) {
            int badgeSize = 16;
            int badgeX = r.x + NODE_WIDTH - badgeSize + 3;
            int badgeY = r.y - 6;
            g.setColor(new Color(239, 68, 68));
            g.fillOval(badgeX, badgeY, badgeSize, badgeSize);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(badgeX, badgeY, badgeSize, badgeSize);
            g.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
            FontMetrics bfm = g.getFontMetrics();
            int exW = bfm.stringWidth("!");
            g.drawString("!", badgeX + (badgeSize - exW) / 2, badgeY + badgeSize - 4);
        }

        // Draw profiler badge if enabled
        if (showProfiler) {
            String nodePath = Node.path(document.getActiveNetworkPath(), node);
            Long timeNanos = executionTimes != null ? executionTimes.get(nodePath) : null;
            if (timeNanos != null) {
                double ms = timeNanos / 1000000.0;
                String badge = ms < 0.1 ? "<0.1ms" : String.format(java.util.Locale.US, "%.1fms", ms);
                Color badgeBg;
                if (ms < 1.0) {
                    badgeBg = new Color(16, 185, 129, 210); // Emerald
                } else if (ms < 10.0) {
                    badgeBg = new Color(245, 158, 11, 220); // Amber
                } else {
                    badgeBg = new Color(239, 68, 68, 230); // Red
                }
                g.setFont(new Font("SansSerif", Font.BOLD, 9));
                FontMetrics fm = g.getFontMetrics();
                int bw = fm.stringWidth(badge) + 6;
                int bh = fm.getHeight();
                int bx = r.x + NODE_WIDTH - bw - 2;
                int by = r.y + NODE_HEIGHT - bh - 2;
                g.setColor(badgeBg);
                g.fillRoundRect(bx, by, bw, bh, 4, 4);
                g.setColor(Color.WHITE);
                g.drawString(badge, bx + 3, by + fm.getAscent() - 1);
            }
        }
    }

    private void paintPortTooltip(Graphics2D g) {
        if (overInput != null) {
            Node overInputNode = findNodeWithName(overInput.node);
            Port overInputPort = overInputNode.getInput(overInput.port);
            Rectangle r = inputPortRect(overInputNode, overInputPort, false);
            Point2D pt = new Point2D.Double(r.getX(), r.getY() + 11);
            String text = String.format("%s (%s)", overInput.port, overInputPort.getType());
            paintTooltip(g, pt, text);
        } else if (overOutput != null && connectionOutput == null) {
            Rectangle r = outputPortRect(overOutput);
            Point2D pt = new Point2D.Double(r.getX(), r.getY() + 11);
            String text = String.format("output (%s)", overOutput.getOutputType());
            paintTooltip(g, pt, text);
        }
    }

    private static void paintTooltip(Graphics2D g, Point2D point, String text) {
        FontMetrics fontMetrics = g.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(text);

        int verticalOffset = 10;
        Rectangle r = new Rectangle((int) point.getX(), (int) point.getY() + verticalOffset, textWidth, fontMetrics.getHeight());
        r.grow(4, 3);
        Color bgColor = Theme.isDark() ? new Color(40, 40, 44) : TOOLTIP_BACKGROUND_COLOR;
        Color strokeColor = Theme.isDark() ? new Color(80, 80, 86) : TOOLTIP_STROKE_COLOR;
        Color textColor = Theme.isDark() ? new Color(228, 228, 231) : TOOLTIP_TEXT_COLOR;
        g.setColor(strokeColor);
        g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setColor(bgColor);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);

        g.setColor(textColor);
        g.drawString(text, (float) point.getX(), (float) point.getY() + fontMetrics.getAscent() + verticalOffset);
    }

    private void paintCommentBox(Graphics2D g) {
        if (overComment != null) {
            Rectangle r = nodeRect(overComment);
            FontMetrics fontMetrics = g.getFontMetrics();
            int commentWidth = fontMetrics.stringWidth(overComment.getComment());
            int x = r.x + 16;
            int y = r.y + GRID_CELL_SIZE - 5;
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x + 1, y + 1, commentWidth + COMMENT_BOX_MARGIN_HORIZONTAL * 2, commentBox.getHeight());
            g.drawImage(commentBox, x, y, commentWidth + COMMENT_BOX_MARGIN_HORIZONTAL * 2, commentBox.getHeight(), null);
            g.setColor(Color.DARK_GRAY);
            g.drawString(overComment.getComment(), x + COMMENT_BOX_MARGIN_HORIZONTAL, y + 14);
        }
    }

    private void paintDragSelection(Graphics2D g) {
        if (isDragSelecting) {
            Rectangle r = dragSelectRect();
            g.setColor(DRAG_SELECTION_COLOR);
            g.setStroke(DRAG_SELECTION_STROKE);
            g.fill(r);
            // To get a smooth line we need to subtract one from the width and height.
            g.drawRect((int) r.getX(), (int) r.getY(), (int) r.getWidth() - 1, (int) r.getHeight() - 1);
        }
    }

    private Rectangle dragSelectRect() {
        if (dragStartPoint == null || dragCurrentPoint == null) return new Rectangle();
        int x0 = (int) dragStartPoint.getX();
        int y0 = (int) dragStartPoint.getY();
        int x1 = (int) dragCurrentPoint.getX();
        int y1 = (int) dragCurrentPoint.getY();
        int x = Math.min(x0, x1);
        int y = Math.min(y0, y1);
        int w = (int) Math.abs(dragCurrentPoint.getX() - dragStartPoint.getX());
        int h = (int) Math.abs(dragCurrentPoint.getY() - dragStartPoint.getY());
        return new Rectangle(x, y, w, h);
    }

    private static Rectangle nodeRect(Node node) {
        return new Rectangle(nodePoint(node), NODE_DIMENSION);
    }

    private static Rectangle inputPortRect(Node node, Port port, boolean isConnecting) {
        if (isHiddenPort(port)) return new Rectangle();
        Point pt = nodePoint(node);
        int pw = getPortWidth(node);
        int portWidth = !isConnecting ? pw : pw + PORT_MARGIN;
        int portHeight = !isConnecting ? PORT_HEIGHT : PORT_HEIGHT + NODE_HEIGHT;
        Rectangle portRect = new Rectangle(pt.x + portOffset(node, port), pt.y - PORT_HEIGHT, portWidth, portHeight);
        growHitRectangle(portRect);
        return portRect;
    }

    private static Rectangle outputPortRect(Node node) {
        Point pt = nodePoint(node);
        Rectangle portRect = new Rectangle(pt.x, pt.y + NODE_HEIGHT - 10, PORT_WIDTH + 10, PORT_HEIGHT + 10);
        growHitRectangle(portRect);
        return portRect;
    }

    private static void growHitRectangle(Rectangle r) {
        r.grow(2, 2);
    }

    private static Point nodePoint(Node node) {
        int nodeX = ((int) node.getPosition().getX()) * GRID_CELL_SIZE;
        int nodeY = ((int) node.getPosition().getY()) * GRID_CELL_SIZE;
        return new Point(nodeX, nodeY);
    }

    private Point pointToGridPoint(Point e) {
        Point2D pt = getInverseViewTransform().transform(e, null);
        return new Point(
                (int) Math.floor(pt.getX() / GRID_CELL_SIZE),
                (int) Math.floor(pt.getY() / GRID_CELL_SIZE));
    }

    public Point centerGridPoint() {
        Point pt = pointToGridPoint(new Point((int) (getBounds().getWidth() / 2), (int) (getBounds().getHeight() / 2)));
        return new Point((int) pt.getX() - 1, (int) pt.getY());
    }

    public static int visibleInputCount(Node node) {
        if (node == null) return 0;
        int count = 0;
        for (Port input : node.getInputs()) {
            if (!isHiddenPort(input)) {
                count++;
            }
        }
        return count;
    }

    public static int getPortWidth(Node node) {
        int count = visibleInputCount(node);
        if (count <= 1) return PORT_WIDTH;
        int available = NODE_WIDTH - 4;
        if (count * (PORT_WIDTH + 2) <= available) {
            return PORT_WIDTH;
        }
        int w = (available / count) - 2;
        return Math.max(4, Math.min(PORT_WIDTH, w));
    }

    public static int getPortSpacing(Node node) {
        int count = visibleInputCount(node);
        if (count <= 1) return PORT_SPACING;
        int pw = getPortWidth(node);
        int totalPw = count * pw;
        int remaining = (NODE_WIDTH - 4) - totalPw;
        int spacing = remaining / (count - 1);
        return Math.max(1, Math.min(PORT_SPACING, spacing));
    }

    public static int portOffset(Node node, Port port) {
        if (node == null || port == null) return 0;
        int visibleIndex = 0;
        for (Port p : node.getInputs()) {
            if (p == port) break;
            if (!isHiddenPort(p)) {
                visibleIndex++;
            }
        }
        int pw = getPortWidth(node);
        int spacing = getPortSpacing(node);
        return 2 + visibleIndex * (pw + spacing);
    }

    //// View queries ////

    private Node findNodeWithName(String name) {
        return getActiveNetwork().getChild(name);
    }

    public Node getNodeAt(Point2D point) {
        for (Node node : getNodesReversed()) {
            Rectangle r = nodeRect(node);
            if (r.contains(point)) {
                return node;
            }
        }
        return null;
    }

    public Node getNodeWithOutputPortAt(Point2D point) {
        for (Node node : getNodesReversed()) {
            Rectangle r = outputPortRect(node);
            if (r.contains(point)) {
                return node;
            }
        }
        return null;
    }

    public NodePort getInputPortAt(Point2D point, boolean isConnecting) {
        for (Node node : getNodesReversed()) {
            for (Port port : node.getInputs()) {
                Rectangle r = inputPortRect(node, port, isConnecting);
                if (r.contains(point)) {
                    return NodePort.of(node.getName(), port.getName());
                }
            }
        }
        return null;
    }

    /**
     * Check if there is a commented node at a given point
     *
     * @param point The point that the mouse produces a MouseEvent
     * @return the Node if it exist at the given point
     */
    public Node getNodeWithCommentAt(Point2D point) {
        for (Node node : getNodesReversed()) {
            if (node.hasComment()) {
                Rectangle r = nodeRect(node);
                if (r.contains(point)) {
                    return node;
                }
            }
        }
        return null;
    }

    private static boolean isHiddenPort(Port port) {
        return port.getType().equals(Port.TYPE_STATE) || port.getType().equals(Port.TYPE_CONTEXT);
    }

    @Override
    protected void onViewTransformChanged(double viewX, double viewY, double viewScale) {
        document.setActiveNetworkPanZoom(viewX, viewY, viewScale);
    }

    //// Selections ////

    public boolean isSelected(Node node) {
        return (selectedNodes.contains(node.getName()));
    }

    public void select(Node node) {
        selectedNodes.add(node.getName());
    }

    /**
     * Select this node, and only this node.
     * <p/>
     * All other selected nodes will be deselected.
     *
     * @param node The node to select. If node is null, everything is deselected.
     */
    public void singleSelect(Node node) {
        if (selectedNodes.size() == 1 && selectedNodes.contains(node.getName())) return;
        selectedNodes.clear();
        if (node != null && getActiveNetwork().hasChild(node)) {
            selectedNodes.add(node.getName());
            firePropertyChange(SELECT_PROPERTY, null, selectedNodes);
            document.setActiveNode(node);
        }
        repaint();
    }

    public void select(Iterable<Node> nodes) {
        selectedNodes.clear();
        for (Node node : nodes) {
            selectedNodes.add(node.getName());
            document.setActiveNodeKeepSelection(node.getName());
        }
    }

    public void toggleSelection(Node node) {
        checkNotNull(node);
        if (selectedNodes.isEmpty()) {
            singleSelect(node);

        } else {
            if (selectedNodes.contains(node.getName())) {
                selectedNodes.remove(node.getName());
            } else {
                selectedNodes.add(node.getName());
            }
            firePropertyChange(SELECT_PROPERTY, null, selectedNodes);
            repaint();
        }
    }

    public void deselectAll() {
        if (selectedNodes.isEmpty()) return;
        selectedNodes.clear();
        firePropertyChange(SELECT_PROPERTY, null, selectedNodes);
        document.setActiveNode((Node) null);
        repaint();
    }

    public Iterable<String> getSelectedNodeNames() {
        return selectedNodes;
    }

    public Iterable<Node> getSelectedNodes() {
        if (selectedNodes.isEmpty()) return ImmutableList.of();
        ImmutableList.Builder<Node> b = new ImmutableList.Builder<nodebox.node.Node>();
        for (String name : getSelectedNodeNames()) {
            b.add(findNodeWithName(name));
        }
        return b.build();
    }

    public void deleteSelection() {
        // Delete the nodes from the document
        document.removeNodes(getSelectedNodes());

        // Remove the deleted nodes from the current selection
        selectedNodes.clear();
    }

    private void moveSelectedNodes(int dx, int dy) {
        for (Node node : getSelectedNodes()) {
            getDocument().setNodePosition(node, node.getPosition().moved(dx, dy));
        }
    }

    private void renameNode(Node node) {
        String s = JOptionPane.showInputDialog(this, "New name (no spaces, don't start with a digit):", node.getName());
        if (s == null || s.length() == 0)
            return;
        try {
            getDocument().setNodeName(node, s);
        } catch (InvalidNameException ex) {
            JOptionPane.showMessageDialog(this, "The given name is not valid.\n" + ex.getMessage(), Application.NAME, JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "An error occurred:\n" + ex.getMessage(), Application.NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Show an input dialog to insert a new comment.
     */
    private void addComment(Node node) {
        String comment = JOptionPane.showInputDialog(this, "New comment:");
        if (comment != null && !comment.trim().isEmpty()) {
            getDocument().setNodeComment(node, comment);
        }
    }

    private void editComment(Node node) {
        String comment = JOptionPane.showInputDialog(this, "Edit comment:", node.getComment());
        getDocument().setNodeComment(node, comment);
    }

    //// Network navigation ////

    private void goUp() {
        if (getDocument().getActiveNetworkPath().equals("/")) return;
        Iterable<String> it = Splitter.on("/").split(getDocument().getActiveNetworkPath());
        int parts = Iterables.size(it);
        String path = parts - 1 > 1 ? Joiner.on("/").join(Iterables.limit(it, parts - 1)) : "/";
        getDocument().setActiveNetwork(path);
    }

    public void zoomToFit() {
        Set<Node> targetNodes = Sets.newHashSet(getSelectedNodes());
        if (targetNodes.isEmpty()) {
            targetNodes = Sets.newHashSet(getNodes());
        }
        if (targetNodes.isEmpty()) {
            resetViewTransform();
            return;
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (Node n : targetNodes) {
            Rectangle r = nodeRect(n);
            if (r.x < minX) minX = r.x;
            if (r.y < minY) minY = r.y;
            if (r.x + r.width > maxX) maxX = r.x + r.width;
            if (r.y + r.height > maxY) maxY = r.y + r.height;
        }

        double boundingWidth = maxX - minX;
        double boundingHeight = maxY - minY;
        if (boundingWidth <= 0 || boundingHeight <= 0) {
            boundingWidth = NODE_WIDTH;
            boundingHeight = NODE_HEIGHT;
        }

        double centerX = minX + boundingWidth / 2.0;
        double centerY = minY + boundingHeight / 2.0;

        int viewW = getWidth();
        int viewH = getHeight();
        if (viewW <= 0) viewW = 800;
        if (viewH <= 0) viewH = 600;

        double padding = 80.0;
        double availW = Math.max(100.0, viewW - padding * 2);
        double availH = Math.max(100.0, viewH - padding * 2);

        double scaleX = availW / boundingWidth;
        double scaleY = availH / boundingHeight;
        double newScale = Math.min(scaleX, scaleY);
        newScale = Math.max(MIN_ZOOM, Math.min(1.0, newScale));

        double vx = (viewW / 2.0) - (centerX * newScale);
        double vy = (viewH / 2.0) - (centerY * newScale);
        setViewTransform(vx, vy, newScale);
    }

    public void alignSelectedNodes(String alignment) {
        Set<Node> selected = Sets.newHashSet(getSelectedNodes());
        if (selected.size() < 2) return;

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (Node node : selected) {
            double nx = node.getPosition().getX();
            double ny = node.getPosition().getY();
            if (nx < minX) minX = nx;
            if (nx > maxX) maxX = nx;
            if (ny < minY) minY = ny;
            if (ny > maxY) maxY = ny;
        }

        getDocument().startEdits("Align Nodes");
        for (Node node : selected) {
            double currentX = node.getPosition().getX();
            double currentY = node.getPosition().getY();
            double targetX = currentX;
            double targetY = currentY;

            if ("left".equals(alignment)) {
                targetX = minX;
            } else if ("right".equals(alignment)) {
                targetX = maxX;
            } else if ("center-x".equals(alignment)) {
                targetX = Math.round((minX + maxX) / 2.0);
            } else if ("top".equals(alignment)) {
                targetY = minY;
            } else if ("bottom".equals(alignment)) {
                targetY = maxY;
            } else if ("center-y".equals(alignment)) {
                targetY = Math.round((minY + maxY) / 2.0);
            }
            getDocument().setNodePosition(node, new nodebox.graphics.Point(targetX, targetY));
        }
        getDocument().stopEditing();
        repaint();
    }

    public void distributeSelectedNodes(String axis) {
        Set<Node> selected = Sets.newHashSet(getSelectedNodes());
        if (selected.size() < 3) return;

        java.util.List<Node> sorted = new java.util.ArrayList<Node>(selected);
        if ("horizontal".equals(axis)) {
            java.util.Collections.sort(sorted, new java.util.Comparator<Node>() {
                public int compare(Node a, Node b) {
                    return Double.compare(a.getPosition().getX(), b.getPosition().getX());
                }
            });
            double x0 = sorted.get(0).getPosition().getX();
            double xN = sorted.get(sorted.size() - 1).getPosition().getX();
            double step = (xN - x0) / (sorted.size() - 1);
            getDocument().startEdits("Distribute Horizontally");
            for (int i = 1; i < sorted.size() - 1; i++) {
                Node node = sorted.get(i);
                double targetX = Math.round(x0 + i * step);
                getDocument().setNodePosition(node, new nodebox.graphics.Point(targetX, node.getPosition().getY()));
            }
            getDocument().stopEditing();
        } else if ("vertical".equals(axis)) {
            java.util.Collections.sort(sorted, new java.util.Comparator<Node>() {
                public int compare(Node a, Node b) {
                    return Double.compare(a.getPosition().getY(), b.getPosition().getY());
                }
            });
            double y0 = sorted.get(0).getPosition().getY();
            double yN = sorted.get(sorted.size() - 1).getPosition().getY();
            double step = (yN - y0) / (sorted.size() - 1);
            getDocument().startEdits("Distribute Vertically");
            for (int i = 1; i < sorted.size() - 1; i++) {
                Node node = sorted.get(i);
                double targetY = Math.round(y0 + i * step);
                getDocument().setNodePosition(node, new nodebox.graphics.Point(node.getPosition().getX(), targetY));
            }
            getDocument().stopEditing();
        }
        repaint();
    }

    public void setExecutionTimes(Map<String, Long> times) {
        this.executionTimes = times != null ? times : new HashMap<String, Long>();
        if (showProfiler) {
            repaint();
        }
    }

    //// Input Events ////

    private class KeyHandler extends KeyAdapter {

        public void keyTyped(KeyEvent e) {
            switch (e.getKeyChar()) {
                case KeyEvent.VK_BACK_SPACE:
                    getDocument().deleteSelection();
                    break;
            }
        }

        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_SHIFT) {
                isShiftPressed = true;
            } else if (keyCode == KeyEvent.VK_ALT) {
                isAltPressed = true;
            } else if (keyCode == KeyEvent.VK_TAB) {
                Point pt = lastMousePoint != null ? lastMousePoint : new Point(getWidth() / 2, getHeight() / 2);
                Point gridPoint = pointToGridPoint(pt);
                Point screenPoint = null;
                try {
                    Point pOnScreen = getLocationOnScreen();
                    screenPoint = new Point(pOnScreen.x + pt.x, pOnScreen.y + pt.y);
                } catch (Exception ex) {
                    screenPoint = pt;
                }
                getDocument().showQuickAddDialog(gridPoint, screenPoint);
                e.consume();
            } else if (keyCode == KeyEvent.VK_F) {
                zoomToFit();
                e.consume();
            } else if (keyCode == KeyEvent.VK_P) {
                showProfiler = !showProfiler;
                repaint();
                e.consume();
            } else if (keyCode == KeyEvent.VK_UP) {
                moveSelectedNodes(0, -1);
            } else if (keyCode == KeyEvent.VK_RIGHT) {
                moveSelectedNodes(1, 0);
            } else if (keyCode == KeyEvent.VK_DOWN) {
                moveSelectedNodes(0, 1);
            } else if (keyCode == KeyEvent.VK_LEFT) {
                moveSelectedNodes(-1, 0);
            }
        }

        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                isShiftPressed = false;
            } else if (e.getKeyCode() == KeyEvent.VK_ALT) {
                isAltPressed = false;
            }
        }

    }

    private class MouseHandler implements MouseListener, MouseMotionListener {

        public void mouseClicked(MouseEvent e) {
            lastMousePoint = e.getPoint();
            Point2D pt = inverseViewTransformPoint(e.getPoint());
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (e.getClickCount() == 1) {
                    Node clickedNode = getNodeAt(pt);
                    if (clickedNode == null) {
                        deselectAll();
                    } else {
                        if (isShiftPressed) {
                            toggleSelection(clickedNode);
                        } else {
                            singleSelect(clickedNode);
                        }
                    }
                } else if (e.getClickCount() == 2) {
                    Node clickedNode = getNodeAt(pt);
                    if (clickedNode == null) {
                        Point gridPoint = pointToGridPoint(e.getPoint());
                        getDocument().showNodeSelectionDialog(gridPoint);
                    } else {
                        document.setRenderedNode(clickedNode);
                    }
                }
            }
        }

        public void mousePressed(MouseEvent e) {
            lastMousePoint = e.getPoint();
            if (showMiniMap && miniMapBounds.contains(e.getPoint())) {
                handleMiniMapClick(e.getPoint());
                return;
            }
            if (e.isPopupTrigger()) {
                showPopup(e);
            } else if (isDragTrigger(e)) {
            } else {
                Point2D pt = inverseViewTransformPoint(e.getPoint());

                // Check if we're over an output port.
                connectionOutput = getNodeWithOutputPortAt(pt);
                if (connectionOutput != null) return;

                // Check if we're over a connected input port.
                connectionInput = getInputPortAt(pt, false);
                if (connectionInput != null) {
                    // We're over a port, but is it connected?
                    Connection c = getActiveNetwork().getConnection(connectionInput.node, connectionInput.port);
                    // Disconnect it, but start a new connection on the same node immediately.
                    if (c != null) {
                        getDocument().disconnect(c);
                        connectionOutput = getActiveNetwork().getChild(c.getOutputNode());
                        connectionPoint = pt;
                    }
                    return;
                }

                // Check if we're pressing a node.
                Node pressedNode = getNodeAt(pt);
                if (pressedNode != null) {
                    // Don't immediately set "isDragging."
                    // We wait until we actually drag the first time to do the work.
                    startDragging = true;
                    return;
                }

                // We're creating a drag selection.
                isDragSelecting = true;
                dragStartPoint = pt;
            }
        }

        public void mouseReleased(MouseEvent e) {
            lastMousePoint = e.getPoint();
            if (e.isPopupTrigger()) {
                showPopup(e);
            } else {
                isDraggingNodes = false;
                isDragSelecting = false;
                if (isAltPressed)
                    getDocument().stopEditing();
                if (connectionOutput != null && connectionInput != null) {
                    getDocument().connect(connectionOutput.getName(), connectionInput.node, connectionInput.port);
                }
                connectionOutput = null;
                repaint();
            }
        }

        public void mouseEntered(MouseEvent e) {
            grabFocus();
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
            lastMousePoint = e.getPoint();
            if (showMiniMap && miniMapBounds.contains(e.getPoint())) {
                handleMiniMapClick(e.getPoint());
                return;
            }
            Point2D pt = inverseViewTransformPoint(e.getPoint());
            // Panning the view has the first priority.
            if (isPanning()) return;

            if (connectionOutput != null) {
                repaint();
                connectionInput = getInputPortAt(pt, true);
                connectionPoint = pt;
                overOutput = getNodeWithOutputPortAt(pt);
                overInput = getInputPortAt(pt, true);
                if (overInput != null && connectionOutput.getName().equals(overInput.node)) {
                    overInput = null;
                }
            }

            if (startDragging) {
                startDragging = false;
                Node pressedNode = getNodeAt(pt);
                if (pressedNode != null) {
                    if (selectedNodes == null || selectedNodes.isEmpty() || !selectedNodes.contains(pressedNode.getName())) {
                        singleSelect(pressedNode);
                    }
                    if (isAltPressed) {
                        getDocument().startEdits("Copy Node");
                        getDocument().dragCopy();
                    }
                    isDraggingNodes = true;
                    dragPositions = selectedNodePositions();
                    dragStartPoint = pt;
                    // Change selection here.
                } else {
                    isDraggingNodes = false;
                }
            }

            if (isDraggingNodes) {
                Point2D offset = minPoint(pt, dragStartPoint);
                int gridX = (int) Math.round(offset.getX() / GRID_CELL_SIZE);
                int gridY = (int) Math.round(offset.getY() / (float) GRID_CELL_SIZE);
                for (Map.Entry<String, nodebox.graphics.Point> entry : dragPositions.entrySet()) {
                    nodebox.graphics.Point originalPosition = entry.getValue();
                    if (originalPosition == null) {
                        // Just in case...
                        originalPosition = nodebox.graphics.Point.ZERO;
                    }
                    nodebox.graphics.Point newPosition = originalPosition.moved(gridX, gridY);
                    Node node = findNodeWithName(entry.getKey());
                    if (node != null) {
                        // This avoids an issue where you delete a node while dragging.
                        getDocument().setNodePosition(node, newPosition);
                    }
                }
            }

            if (isDragSelecting) {
                dragCurrentPoint = pt;
                Rectangle r = dragSelectRect();
                selectedNodes.clear();
                for (Node node : getNodes()) {
                    if (r.intersects(nodeRect(node))) {
                        selectedNodes.add(node.getName());
                    }
                }
                repaint();
            }
        }

        public void mouseMoved(MouseEvent e) {
            lastMousePoint = e.getPoint();
            Point2D pt = inverseViewTransformPoint(e.getPoint());
            overOutput = getNodeWithOutputPortAt(pt);
            overInput = getInputPortAt(pt, false);
            overComment = getNodeWithCommentAt(pt);
            // It is probably very inefficient to repaint the view every time the mouse moves.
            repaint();
        }
    }


    public void zoom(double scaleDelta) {
        // todo: implement
    }

    public boolean containsPoint(Point point) {
        return isVisible() && getBounds().contains(point);
    }

    private void showPopup(MouseEvent e) {
        Point pt = e.getPoint();
        NodePort nodePort = getInputPortAt(inverseViewTransformPoint(pt), false);
        if (nodePort != null) {
            JPopupMenu pMenu = new JPopupMenu();
            pMenu.add(new PublishAction(nodePort));

            if (findNodeWithName(nodePort.getNode()).hasPublishedInput(nodePort.getPort()))
                pMenu.add(new GoToPortAction(nodePort));

            Theme.applyPopupMenuTheme(pMenu);
            pMenu.show(this, e.getX(), e.getY());
        } else {
            Node pressedNode = getNodeAt(inverseViewTransformPoint(pt));
            if (pressedNode != null) {
                JPopupMenu nodeMenu = createNodeMenu(pressedNode);
                nodeMenuLocation = pt;
                Theme.applyPopupMenuTheme(nodeMenu);
                nodeMenu.show(this, e.getX(), e.getY());
            } else {
                networkMenuLocation = pt;
                Theme.applyPopupMenuTheme(networkMenu);
                networkMenu.show(this, e.getX(), e.getY());
            }
        }
    }

    public void updateTheme() {
        if (networkMenu != null) {
            Theme.applyPopupMenuTheme(networkMenu);
        }
        repaint();
    }

    private ImmutableMap<String, nodebox.graphics.Point> selectedNodePositions() {
        ImmutableMap.Builder<String, nodebox.graphics.Point> b = ImmutableMap.builder();
        for (String nodeName : selectedNodes) {
            b.put(nodeName, findNodeWithName(nodeName).getPosition());
        }
        return b.build();
    }

    private Point2D minPoint(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() - b.getX(), a.getY() - b.getY());
    }

    private class FocusHandler extends FocusAdapter {

        @Override
        public void focusLost(FocusEvent focusEvent) {
            isShiftPressed = false;
            isAltPressed = false;
        }

    }

    private static class NodeImageCacheLoader extends CacheLoader<Node, BufferedImage> {
        private NodeRepository nodeRepository;

        private NodeImageCacheLoader(NodeRepository nodeRepository) {
            this.nodeRepository = nodeRepository;
        }

        @Override
        public BufferedImage load(Node node) throws Exception {
            for (NodeLibrary library : nodeRepository.getLibraries()) {
                BufferedImage img = findNodeImage(library, node);
                if (img != null) {
                    return img;
                }
            }
            if (node.getPrototype() != null) {
                return load(node.getPrototype());
            } else {
                return nodeGeneric;
            }
        }
    }

    private class NewNodeAction extends AbstractAction {
        private NewNodeAction() {
            super("New Node");
        }

        public void actionPerformed(ActionEvent e) {
            Point gridPoint = pointToGridPoint(networkMenuLocation);
            getDocument().showNodeSelectionDialog(gridPoint);
        }
    }

    private class ResetViewAction extends AbstractAction {
        private ResetViewAction() {
            super("Reset View");
        }

        public void actionPerformed(ActionEvent e) {
            resetViewTransform();
        }
    }

    private class GoUpAction extends AbstractAction {
        private GoUpAction() {
            super("Go Up");
        }

        public void actionPerformed(ActionEvent e) {
            goUp();
        }
    }

    private class PublishAction extends AbstractAction {
        private NodePort nodePort;

        private PublishAction(NodePort nodePort) {
            super(getActiveNetwork().hasPublishedInput(nodePort.getNode(), nodePort.getPort()) ? "Unpublish" : "Publish");
            this.nodePort = nodePort;
        }

        public void actionPerformed(ActionEvent e) {
            if (getActiveNetwork().hasPublishedInput(nodePort.getNode(), nodePort.getPort())) {
                unpublish();
            } else {
                publish();
            }
        }

        private void unpublish() {
            Port port = getActiveNetwork().getPortByChildReference(nodePort.getNode(), nodePort.getPort());
            getDocument().unpublish(port.getName());
        }

        private void publish() {
            String s = JOptionPane.showInputDialog(NetworkView.this, "Publish as:", nodePort.getPort());
            if (s == null || s.length() == 0)
                return;
            getDocument().publish(nodePort.getNode(), nodePort.getPort(), s);
        }
    }

    private class GoToPortAction extends AbstractAction {
        private NodePort nodePort;

        private GoToPortAction(NodePort nodePort) {
            super("Go to Port");
            this.nodePort = nodePort;
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().setActiveNetwork(Node.path(getDocument().getActiveNetworkPath(), nodePort.getNode()));

            // todo: visually indicate the origin port.
            // Node node = findNodeWithName(nodePort.getNode());
            // Port publishedPort = node.getInput(nodePort.getPort());
            // publishedPort.getChildNodeName()
            // publishedPort.getChildPortName()
        }
    }

    private class SetRenderedAction extends AbstractAction {
        private SetRenderedAction() {
            super("Set Rendered");
        }

        public void actionPerformed(ActionEvent e) {
            Node node = getNodeAt(inverseViewTransformPoint(nodeMenuLocation));
            document.setRenderedNode(node);
        }
    }

    private class RenameAction extends AbstractAction {
        private RenameAction() {
            super("Rename");
        }

        public void actionPerformed(ActionEvent e) {
            Node node = getNodeAt(inverseViewTransformPoint(nodeMenuLocation));
            if (node != null) {
                renameNode(node);
            }
        }
    }

    private class DeleteAction extends AbstractAction {
        private DeleteAction() {
            super("Delete");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
        }

        public void actionPerformed(ActionEvent e) {
            deleteSelection();
        }
    }

    private class GoInAction extends AbstractAction {
        private GoInAction() {
            super("Edit Children");
        }

        public void actionPerformed(ActionEvent e) {
            Node node = getNodeAt(inverseViewTransformPoint(nodeMenuLocation));
            String childPath = Node.path(getDocument().getActiveNetworkPath(), node.getName());
            getDocument().setActiveNetwork(childPath);
        }
    }

    private class GroupIntoNetworkAction extends AbstractAction {
        private Point gridPoint;

        private GroupIntoNetworkAction(Point gridPoint) {
            super("Group into Network");
            this.gridPoint = gridPoint;
        }

        public void actionPerformed(ActionEvent e) {
            nodebox.graphics.Point position;
            if (gridPoint == null)
                position = getNodeAt(inverseViewTransformPoint(nodeMenuLocation)).getPosition();
            else
                position = new nodebox.graphics.Point(gridPoint);
            getDocument().groupIntoNetwork(position);
        }
    }

    private class RemoveCommentAction extends AbstractAction {
        private RemoveCommentAction() {
            super("Remove Comment");
        }

        public void actionPerformed(ActionEvent e) {
            Node node = getNodeAt(inverseViewTransformPoint(nodeMenuLocation));
            if (node != null) {
                getDocument().setNodeComment(node, "");
                // Since this node no longer has a comment, we're no longer over a comment node.
                overComment = null;
                repaint();
            }
        }
    }

    private class EditCommentAction extends AbstractAction {
        private EditCommentAction() {
            super("Edit Comment");
        }

        public void actionPerformed(ActionEvent e) {
            Node node = getNodeAt(inverseViewTransformPoint(nodeMenuLocation));
            if (node != null) {
                editComment(node);
            }
        }
    }

    private class AddCommentAction extends AbstractAction {
        private AddCommentAction() {
            super("Add Comment");
        }

        public void actionPerformed(ActionEvent e) {
            Node node = getNodeAt(inverseViewTransformPoint(nodeMenuLocation));
            if (node != null) {
                addComment(node);
                repaint();
            }
        }
    }

    private class HelpAction extends AbstractAction {
        private HelpAction() {
            super("Help");
        }

        public void actionPerformed(ActionEvent e) {
            Node node = getNodeAt(inverseViewTransformPoint(nodeMenuLocation));
            Node prototype = node.getPrototype();
            for (NodeLibrary library : document.getNodeRepository().getLibraries()) {
                if (library.getRoot().hasChild(prototype)) {
                    String libraryName = library.getName();
                    String nodeName = prototype.getName();
                    String nodeRef = String.format("http://nodebox.net/node/reference/%s/%s", libraryName, nodeName);
                    Platform.openURL(nodeRef);
                    return;
                }
            }
            JOptionPane.showMessageDialog(NetworkView.this, "There is no reference documentation for node " + prototype, Application.NAME, JOptionPane.WARNING_MESSAGE);
        }
    }
}
