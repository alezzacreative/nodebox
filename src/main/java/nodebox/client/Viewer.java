package nodebox.client;

import com.google.common.collect.ImmutableList;
import nodebox.client.visualizer.*;
import nodebox.graphics.CanvasContext;
import nodebox.graphics.IGeometry;
import nodebox.handle.Handle;
import nodebox.node.Node;
import nodebox.ui.Theme;
import nodebox.ui.Zoom;
import nodebox.util.PerfMonitor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.VolatileImage;
import nodebox.util.GPUUtils;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static nodebox.util.ListUtils.listClass;

public class Viewer extends ZoomableView implements OutputView, Zoom, MouseListener, MouseMotionListener, KeyListener {

    public static final double MIN_ZOOM = 0.01;
    public static final double MAX_ZOOM = 64.0;

    public enum ModalTransformMode {
        NONE, GRAB, ROTATE, SCALE
    }
    public enum AxisConstraint {
        NONE, X, Y
    }

    private NodeBoxDocument document;
    private ModalTransformMode modalMode = ModalTransformMode.NONE;
    private AxisConstraint axisConstraint = AxisConstraint.NONE;
    private nodebox.graphics.Point modalStartMouse = nodebox.graphics.Point.ZERO;
    private nodebox.graphics.Point modalObjectCenter = nodebox.graphics.Point.ZERO;
    private String modalNodePath = null;
    private final Map<String, Object> modalInitialValues = new HashMap<String, Object>();
    private double modalHudDeltaX = 0, modalHudDeltaY = 0, modalHudAngle = 0, modalHudScale = 1.0;

    private boolean gpuAcceleration = Application.isGpuAccelerationEnabled();

    private boolean infiniteTileMode = false;
    private ViewerPane viewerPane;

    private boolean isAltScrubbing = false;
    private Point altScrubStartScreenPoint;
    private Object altScrubStartValue;
    private String altScrubPortName;
    private String altScrubNodePath;
    private String altScrubPortType;
    private double altScrubCurrentValue = 0;

    public static final String FRAME_GUIDE_OFF = "Off";
    public static final String FRAME_GUIDE_1_1 = "1:1";
    public static final String FRAME_GUIDE_16_9 = "16:9";
    public static final String FRAME_GUIDE_9_16 = "9:16";
    public static final String FRAME_GUIDE_4_3 = "4:3";
    public static final String FRAME_GUIDE_3_4 = "3:4";

    private final JPopupMenu viewerMenu;

    private nodebox.graphics.Point lastMousePosition = nodebox.graphics.Point.ZERO;

    private Handle handle;
    private boolean showHandle = true;
    private boolean showPoints = false;
    private boolean showPointNumbers = false;
    private boolean showOrigin = false;
    private boolean showBounds = false;
    private boolean showSelectionGizmo = true;
    private String frameGuide = FRAME_GUIDE_OFF;
    private boolean viewPositioned = false;

    private java.util.List<?> outputValues;
    private Rectangle2D canvasBounds = new Rectangle2D.Double(-500, -500, 1000, 1000);
    private Class valuesClass;
    private Visualizer currentVisualizer = VisualizerFactory.getDefaultVisualizer();

    public void setViewerPane(ViewerPane viewerPane) {
        this.viewerPane = viewerPane;
    }

    public void setInfiniteTileMode(boolean infiniteTileMode) {
        this.infiniteTileMode = infiniteTileMode;
        repaint();
    }

    public boolean isInfiniteTileMode() {
        return infiniteTileMode;
    }

    public Viewer() {
        super(MIN_ZOOM, MAX_ZOOM);
        addMouseListener(this);
        addMouseMotionListener(this);
        setFocusable(true);
        addKeyListener(this);
        setBackground(Theme.VIEWER_BACKGROUND_COLOR);

        viewerMenu = new JPopupMenu();
        PopupHandler popupHandler = new PopupHandler();
        addMouseListener(popupHandler);
    }

    public void zoom(double scaleDelta) {
        super.zoom(scaleDelta, getWidth() / 2.0, getHeight() / 2.0);
    }

    public void setDocument(NodeBoxDocument document) {
        this.document = document;
    }

    public NodeBoxDocument getDocument() {
        return document;
    }

    public boolean containsPoint(Point point) {
        return isVisible() && getBounds().contains(point);
    }

    public void updateTheme() {
        setBackground(Theme.VIEWER_BACKGROUND_COLOR);
        if (viewerMenu != null) {
            Theme.applyPopupMenuTheme(viewerMenu);
        }
        repaint();
    }

    public void setShowHandle(boolean showHandle) {
        this.showHandle = showHandle;
        repaint();
    }

    public void setShowPoints(boolean showPoints) {
        this.showPoints = showPoints;
        repaint();
    }

    public void setShowPointNumbers(boolean showPointNumbers) {
        this.showPointNumbers = showPointNumbers;
        repaint();
    }

    public void setShowOrigin(boolean showOrigin) {
        this.showOrigin = showOrigin;
        repaint();
    }

    public void setShowBounds(boolean showBounds) {
        this.showBounds = showBounds;
        repaint();
    }

    public String getFrameGuide() {
        return frameGuide;
    }

    public void setFrameGuide(String frameGuide) {
        this.frameGuide = frameGuide;
        repaint();
    }

    public JMenu createFrameGuideMenu() {
        JMenu menu = new JMenu("Frame Guides");
        String[] options = new String[]{FRAME_GUIDE_OFF, FRAME_GUIDE_1_1, FRAME_GUIDE_16_9, FRAME_GUIDE_9_16, FRAME_GUIDE_4_3, FRAME_GUIDE_3_4};
        for (final String opt : options) {
            JMenuItem item = new JCheckBoxMenuItem(new AbstractAction(opt) {
                public void actionPerformed(ActionEvent e) {
                    setFrameGuide(opt);
                }
            });
            if (opt.equals(frameGuide)) {
                item.setSelected(true);
            }
            menu.add(item);
        }
        return menu;
    }

    //// Handle support ////

    public Handle getHandle() {
        return handle;
    }

    public void setHandle(Handle handle) {
        this.handle = handle;
        repaint();
    }

    @Override
    protected void onViewTransformChanged(double viewX, double viewY, double viewScale) {
        super.onViewTransformChanged(viewX, viewY, viewScale);
        // Keep the handle's transform current so hit-testing is correct immediately after a
        // zoom or pan, before the next repaint. Drawing also refreshes it in paintHandle.
        if (handle != null)
            handle.setViewTransform(viewX, viewY, viewScale);
    }

    public void updateHandle() {
        if (handle == null) return;
        handle.update();
    }

    public boolean hasVisibleHandle() {
        if (handle == null) return false;
        if (!showHandle) return false;

        // Don't show handles for LastResortVisualizer and ColorVisualizer.
        if (currentVisualizer instanceof LastResortVisualizer) return false;
        if (currentVisualizer instanceof ColorVisualizer) return false;

        return handle.isVisible();
    }

    //// Network data events ////

    public void setOutputValues(java.util.List<?> outputValues) {
        this.outputValues = outputValues;
        valuesClass = listClass(outputValues);
        Visualizer visualizer = VisualizerFactory.getVisualizer(outputValues, valuesClass);
        if (visualizer instanceof LastResortVisualizer && outputValues.size() == 0) {
            // This scenario means likely that we're in a node that normally outputs
            // some visual type but currently outputs null (or None)
            // If we'd reset the visualizer the screen offset would change, and this would
            // lead to strange (and wrong) interactions with handles (big leaps in
            // current mouse locations).
            repaint();
            return;
        }
        if (currentVisualizer != visualizer) {
            currentVisualizer = visualizer;
            resetViewTransform();
        }
        checkNotNull(currentVisualizer);
        repaint();
    }

    public void setCanvasBounds(Rectangle2D bounds) {
        this.canvasBounds = bounds;
        repaint();
    }

    public void setGpuAcceleration(boolean enabled) {
        this.gpuAcceleration = enabled;
        repaint();
    }

    public boolean isGpuAcceleration() {
        return gpuAcceleration;
    }

    @Override
    public void setViewPosition(double x, double y) {
        super.setViewPosition(x, y);
    }

    @Override
    public void resetViewTransform() {
        Point2D position = currentVisualizer.getOffset(outputValues, getSize());
        setViewTransform(position.getX(), position.getY(), 1);
    }

    //// Mouse events ////

    private nodebox.graphics.Point pointForEvent(MouseEvent e) {
        Point2D pt = inverseViewTransformPoint(e.getPoint());
        return new nodebox.graphics.Point(pt);
    }

    public nodebox.graphics.Point getLastMousePosition() {
        return lastMousePosition;
    }

    public void mouseClicked(MouseEvent e) {
        // We register the mouse click as an edit since it can trigger a change to the node.
        if (e.isPopupTrigger()) return;
        if (handle != null)
            handle.mouseClicked(pointForEvent(e));
    }

    public void mousePressed(MouseEvent e) {
        if (modalMode != ModalTransformMode.NONE) {
            if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
                cancelModalTransform();
                return;
            } else if (e.getButton() == MouseEvent.BUTTON1) {
                confirmModalTransform();
                return;
            }
        }
        // Alt + Left-Drag: Quick numeric parameter scrubbing on active node
        if (e.getButton() == MouseEvent.BUTTON1 && e.isAltDown() && modalMode == ModalTransformMode.NONE) {
            Node activeNode = (document != null) ? document.getActiveNode() : null;
            if (activeNode != null) {
                String[] candidateNames = new String[]{
                        "count", "amount", "radius", "spacing", "wavelength", "amplitude", "strength",
                        "copies", "width", "height", "size", "distance", "angle", "offset", "start", "end", "value"
                };
                nodebox.node.Port targetPort = null;
                for (String name : candidateNames) {
                    if (activeNode.hasInput(name)) {
                        nodebox.node.Port p = activeNode.getInput(name);
                        if (nodebox.node.Port.TYPE_INT.equals(p.getType()) || nodebox.node.Port.TYPE_FLOAT.equals(p.getType())) {
                            targetPort = p;
                            break;
                        }
                    }
                }
                if (targetPort == null) {
                    for (nodebox.node.Port p : activeNode.getInputs()) {
                        if (nodebox.node.Port.TYPE_INT.equals(p.getType()) || nodebox.node.Port.TYPE_FLOAT.equals(p.getType())) {
                            targetPort = p;
                            break;
                        }
                    }
                }
                if (targetPort != null) {
                    isAltScrubbing = true;
                    altScrubStartScreenPoint = e.getPoint();
                    altScrubPortName = targetPort.getName();
                    altScrubPortType = targetPort.getType();
                    altScrubStartValue = targetPort.getValue();
                    altScrubNodePath = Node.path(document.getActiveNetworkPath(), activeNode);
                    if (altScrubStartValue instanceof Number) {
                        altScrubCurrentValue = ((Number) altScrubStartValue).doubleValue();
                    } else {
                        altScrubCurrentValue = 0;
                    }
                    repaint();
                    return;
                }
            }
        }

        // We register the mouse press as an edit since it can trigger a change to the node.
        if (e.isPopupTrigger()) return;

        boolean handleHit = false;
        if (handle != null) {
            handleHit = handle.mousePressed(pointForEvent(e));
        }

        // Check if user clicked on empty canvas space to deselect
        if (e.getButton() == MouseEvent.BUTTON1 && !isPanning() && !handleHit) {
            nodebox.graphics.Point docPt = pointForEvent(e);
            nodebox.graphics.Rect bounds = getOutputGeometryBounds();
            boolean hitGeometry = false;
            if (bounds != null) {
                double margin = 6.0 / Math.max(0.001, getViewScale());
                nodebox.graphics.Rect hitRect = new nodebox.graphics.Rect(
                        bounds.getX() - margin,
                        bounds.getY() - margin,
                        bounds.getWidth() + 2 * margin,
                        bounds.getHeight() + 2 * margin);
                hitGeometry = hitRect.contains(docPt);
            }

            if (!hitGeometry) {
                // Clicked on empty canvas space: deselect active node and hide bounding box
                if (document != null) {
                    document.setActiveNode((Node) null);
                    if (document.getNetworkView() != null) {
                        document.getNetworkView().deselectAll();
                    }
                }
                showSelectionGizmo = false;
                repaint();
            }
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (isAltScrubbing) {
            isAltScrubbing = false;
            repaint();
            return;
        }
        // We register the mouse release as an edit since it can trigger a change to the node.
        if (e.isPopupTrigger()) return;
        if (handle != null)
            handle.mouseReleased(pointForEvent(e));
    }

    public void mouseEntered(MouseEvent e) {
        requestFocusInWindow();
        // Entering the viewer with your mouse should not change the node, so we do not register an edit.
        if (e.isPopupTrigger()) return;
        if (handle != null)
            handle.mouseEntered(pointForEvent(e));
    }

    public void mouseExited(MouseEvent e) {
        // Exiting the viewer with your mouse should not change the node, so we do not register an edit.
        if (e.isPopupTrigger()) return;
        if (handle != null)
            handle.mouseExited(pointForEvent(e));
    }

    public void mouseDragged(MouseEvent e) {
        lastMousePosition = pointForEvent(e);
        if (modalMode != ModalTransformMode.NONE) {
            updateModalTransform(lastMousePosition, e.isShiftDown());
            return;
        }
        if (isAltScrubbing) {
            double dx = e.getPoint().x - altScrubStartScreenPoint.x;
            if (nodebox.node.Port.TYPE_INT.equals(altScrubPortType)) {
                double step = e.isShiftDown() ? 0.1 : 1.0;
                long startVal = (altScrubStartValue instanceof Number) ? ((Number) altScrubStartValue).longValue() : 0L;
                long newVal = Math.round(startVal + dx * step);
                altScrubCurrentValue = newVal;
                document.setValue(altScrubNodePath, altScrubPortName, newVal);
            } else {
                double step = e.isShiftDown() ? 0.05 : 0.5;
                double startVal = (altScrubStartValue instanceof Number) ? ((Number) altScrubStartValue).doubleValue() : 0.0;
                double newVal = startVal + dx * step;
                newVal = Math.round(newVal * 100.0) / 100.0;
                altScrubCurrentValue = newVal;
                document.setValue(altScrubNodePath, altScrubPortName, (float) newVal);
            }
            repaint();
            return;
        }
        // We register the mouse drag as an edit since it can trigger a change to the node.
        if (e.isPopupTrigger()) return;
        if (isPanning()) return;
        if (handle != null)
            handle.mouseDragged(pointForEvent(e));
    }

    public void mouseMoved(MouseEvent e) {
        lastMousePosition = pointForEvent(e);
        if (modalMode != ModalTransformMode.NONE) {
            updateModalTransform(lastMousePosition, e.isShiftDown());
            return;
        }
        // Moving the mouse in the viewer area should not change the node, so we do not register an edit.
        if (e.isPopupTrigger()) return;
        if (handle != null)
            handle.mouseMoved(pointForEvent(e));
    }

    public void keyTyped(KeyEvent e) {
        if (handle != null)
            handle.keyTyped(e.getKeyCode(), e.getModifiersEx());
    }

    public void keyPressed(KeyEvent e) {
        if (modalMode != ModalTransformMode.NONE) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                cancelModalTransform();
                e.consume();
                return;
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                confirmModalTransform();
                e.consume();
                return;
            } else if (e.getKeyCode() == KeyEvent.VK_X) {
                axisConstraint = (axisConstraint == AxisConstraint.X) ? AxisConstraint.NONE : AxisConstraint.X;
                updateModalTransform(lastMousePosition, e.isShiftDown());
                e.consume();
                return;
            } else if (e.getKeyCode() == KeyEvent.VK_Y) {
                axisConstraint = (axisConstraint == AxisConstraint.Y) ? AxisConstraint.NONE : AxisConstraint.Y;
                updateModalTransform(lastMousePosition, e.isShiftDown());
                e.consume();
                return;
            }
        } else {
            if (!e.isControlDown() && !e.isMetaDown() && !e.isAltDown()) {
                if (e.getKeyCode() == KeyEvent.VK_G) {
                    startModalTransform(ModalTransformMode.GRAB, lastMousePosition);
                    e.consume();
                    return;
                } else if (e.getKeyCode() == KeyEvent.VK_R) {
                    startModalTransform(ModalTransformMode.ROTATE, lastMousePosition);
                    e.consume();
                    return;
                } else if (e.getKeyCode() == KeyEvent.VK_S) {
                    startModalTransform(ModalTransformMode.SCALE, lastMousePosition);
                    e.consume();
                    return;
                } else if (e.getKeyCode() == KeyEvent.VK_T) {
                    setInfiniteTileMode(!infiniteTileMode);
                    if (viewerPane != null) {
                        viewerPane.updateRepeatCheck(infiniteTileMode);
                    }
                    e.consume();
                    return;
                }
            }
        }

        if (handle != null)
            handle.keyPressed(e.getKeyCode(), e.getModifiersEx());
    }

    //// Blender-Style Modal Transformations (G, R, S) ////

    private Node getTransformTargetNode() {
        if (document == null) return null;
        Node node = document.getActiveNode();
        if (node == null && document.getActiveNetwork() != null) {
            node = document.getActiveNetwork().getRenderedChild();
        }
        return node;
    }

    private nodebox.graphics.Point computeObjectCenter(Node node) {
        if (node != null) {
            if (node.hasInput("position")) {
                Object v = node.getInput("position").getValue();
                if (v instanceof nodebox.graphics.Point) return (nodebox.graphics.Point) v;
            }
            if (node.hasInput("translate")) {
                Object v = node.getInput("translate").getValue();
                if (v instanceof nodebox.graphics.Point) return (nodebox.graphics.Point) v;
            }
            if (node.hasInput("x") && node.hasInput("y")) {
                Object vx = node.getInput("x").getValue();
                Object vy = node.getInput("y").getValue();
                if (vx instanceof Number && vy instanceof Number) {
                    return new nodebox.graphics.Point(((Number) vx).doubleValue(), ((Number) vy).doubleValue());
                }
            }
        }
        if (outputValues != null) {
            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            boolean hasGeom = false;
            for (Object obj : outputValues) {
                if (obj instanceof IGeometry) {
                    nodebox.graphics.Rect r = ((IGeometry) obj).getBounds();
                    if (r != null && !r.isEmpty()) {
                        minX = Math.min(minX, r.getX());
                        minY = Math.min(minY, r.getY());
                        maxX = Math.max(maxX, r.getX() + r.getWidth());
                        maxY = Math.max(maxY, r.getY() + r.getHeight());
                        hasGeom = true;
                    }
                }
            }
            if (hasGeom && minX <= maxX && minY <= maxY) {
                return new nodebox.graphics.Point((minX + maxX) / 2.0, (minY + maxY) / 2.0);
            }
        }
        return nodebox.graphics.Point.ZERO;
    }

    private void startModalTransform(ModalTransformMode mode, nodebox.graphics.Point startPt) {
        Node target = getTransformTargetNode();
        if (target == null || document == null) return;

        modalInitialValues.clear();
        modalNodePath = Node.path(document.getActiveNetworkPath(), target);
        modalObjectCenter = computeObjectCenter(target);
        modalStartMouse = startPt != null ? startPt : lastMousePosition;
        axisConstraint = AxisConstraint.NONE;
        modalHudDeltaX = 0;
        modalHudDeltaY = 0;
        modalHudAngle = 0;
        modalHudScale = 1.0;

        if (mode == ModalTransformMode.GRAB) {
            if (target.hasInput("position")) modalInitialValues.put("position", target.getInput("position").getValue());
            if (target.hasInput("translate")) modalInitialValues.put("translate", target.getInput("translate").getValue());
            if (target.hasInput("x")) modalInitialValues.put("x", target.getInput("x").getValue());
            if (target.hasInput("y")) modalInitialValues.put("y", target.getInput("y").getValue());
            if (modalInitialValues.isEmpty()) return;
            modalMode = ModalTransformMode.GRAB;
            document.startEdits("Grab " + target.getName());
        } else if (mode == ModalTransformMode.ROTATE) {
            if (target.hasInput("angle")) modalInitialValues.put("angle", target.getInput("angle").getValue());
            else if (target.hasInput("rotation")) modalInitialValues.put("rotation", target.getInput("rotation").getValue());
            else if (target.hasInput("rotate")) modalInitialValues.put("rotate", target.getInput("rotate").getValue());
            if (modalInitialValues.isEmpty()) return;
            modalMode = ModalTransformMode.ROTATE;
            document.startEdits("Rotate " + target.getName());
        } else if (mode == ModalTransformMode.SCALE) {
            if (target.hasInput("width")) modalInitialValues.put("width", target.getInput("width").getValue());
            if (target.hasInput("height")) modalInitialValues.put("height", target.getInput("height").getValue());
            if (target.hasInput("scale")) modalInitialValues.put("scale", target.getInput("scale").getValue());
            if (target.hasInput("radius")) modalInitialValues.put("radius", target.getInput("radius").getValue());
            if (modalInitialValues.isEmpty()) return;
            modalMode = ModalTransformMode.SCALE;
            document.startEdits("Scale " + target.getName());
        }
        repaint();
    }

    private void confirmModalTransform() {
        if (modalMode == ModalTransformMode.NONE) return;
        if (document != null) {
            document.stopEditing();
        }
        modalMode = ModalTransformMode.NONE;
        modalInitialValues.clear();
        repaint();
    }

    private void cancelModalTransform() {
        if (modalMode == ModalTransformMode.NONE) return;
        if (document != null && modalNodePath != null) {
            for (Map.Entry<String, Object> entry : modalInitialValues.entrySet()) {
                document.setValue(modalNodePath, entry.getKey(), entry.getValue());
            }
            document.stopEditing();
        }
        modalMode = ModalTransformMode.NONE;
        modalInitialValues.clear();
        repaint();
    }

    private void updateModalTransform(nodebox.graphics.Point currentMouse, boolean isShiftDown) {
        if (modalMode == ModalTransformMode.NONE || document == null || modalNodePath == null) return;

        if (modalMode == ModalTransformMode.GRAB) {
            double dx = currentMouse.x - modalStartMouse.x;
            double dy = currentMouse.y - modalStartMouse.y;
            if (isShiftDown) {
                dx *= 0.2;
                dy *= 0.2;
            }
            if (axisConstraint == AxisConstraint.X) dy = 0;
            if (axisConstraint == AxisConstraint.Y) dx = 0;
            modalHudDeltaX = Math.round(dx * 10.0) / 10.0;
            modalHudDeltaY = Math.round(dy * 10.0) / 10.0;

            if (modalInitialValues.containsKey("position")) {
                nodebox.graphics.Point orig = (nodebox.graphics.Point) modalInitialValues.get("position");
                document.setValue(modalNodePath, "position", new nodebox.graphics.Point(orig.x + dx, orig.y + dy));
            } else if (modalInitialValues.containsKey("translate")) {
                nodebox.graphics.Point orig = (nodebox.graphics.Point) modalInitialValues.get("translate");
                document.setValue(modalNodePath, "translate", new nodebox.graphics.Point(orig.x + dx, orig.y + dy));
            } else if (modalInitialValues.containsKey("x") && modalInitialValues.containsKey("y")) {
                double ox = ((Number) modalInitialValues.get("x")).doubleValue();
                double oy = ((Number) modalInitialValues.get("y")).doubleValue();
                document.setValue(modalNodePath, "x", (float) (ox + dx));
                document.setValue(modalNodePath, "y", (float) (oy + dy));
            }
        } else if (modalMode == ModalTransformMode.ROTATE) {
            double a0 = Math.atan2(modalStartMouse.y - modalObjectCenter.y, modalStartMouse.x - modalObjectCenter.x);
            double a1 = Math.atan2(currentMouse.y - modalObjectCenter.y, currentMouse.x - modalObjectCenter.x);
            double deltaDeg = Math.toDegrees(a1 - a0);
            if (isShiftDown) {
                deltaDeg = Math.round(deltaDeg / 15.0) * 15.0;
            }
            modalHudAngle = Math.round(deltaDeg * 10.0) / 10.0;

            String rotPort = modalInitialValues.containsKey("angle") ? "angle" :
                    (modalInitialValues.containsKey("rotation") ? "rotation" : "rotate");
            if (modalInitialValues.containsKey(rotPort)) {
                double origRot = ((Number) modalInitialValues.get(rotPort)).doubleValue();
                document.setValue(modalNodePath, rotPort, (float) (origRot + deltaDeg));
            }
        } else if (modalMode == ModalTransformMode.SCALE) {
            double d0 = Math.max(5.0, Point2D.distance(modalStartMouse.x, modalStartMouse.y, modalObjectCenter.x, modalObjectCenter.y));
            double d1 = Point2D.distance(currentMouse.x, currentMouse.y, modalObjectCenter.x, modalObjectCenter.y);
            double factor = d1 / d0;
            if (isShiftDown) {
                factor = 1.0 + (factor - 1.0) * 0.2;
            }
            factor = Math.max(0.01, Math.round(factor * 100.0) / 100.0);
            modalHudScale = factor;

            if (modalInitialValues.containsKey("width") && modalInitialValues.containsKey("height")) {
                double origW = ((Number) modalInitialValues.get("width")).doubleValue();
                double origH = ((Number) modalInitialValues.get("height")).doubleValue();
                double nw = (axisConstraint == AxisConstraint.Y) ? origW : origW * factor;
                double nh = (axisConstraint == AxisConstraint.X) ? origH : origH * factor;
                document.setValue(modalNodePath, "width", (float) (Math.round(nw * 10.0) / 10.0f));
                document.setValue(modalNodePath, "height", (float) (Math.round(nh * 10.0) / 10.0f));
            } else if (modalInitialValues.containsKey("scale")) {
                Object orig = modalInitialValues.get("scale");
                if (orig instanceof nodebox.graphics.Point) {
                    nodebox.graphics.Point pt = (nodebox.graphics.Point) orig;
                    double sx = (axisConstraint == AxisConstraint.Y) ? pt.x : pt.x * factor;
                    double sy = (axisConstraint == AxisConstraint.X) ? pt.y : pt.y * factor;
                    document.setValue(modalNodePath, "scale", new nodebox.graphics.Point(sx, sy));
                } else if (orig instanceof Number) {
                    double s = ((Number) orig).doubleValue() * factor;
                    document.setValue(modalNodePath, "scale", (float) s);
                }
            } else if (modalInitialValues.containsKey("radius")) {
                double origR = ((Number) modalInitialValues.get("radius")).doubleValue();
                document.setValue(modalNodePath, "radius", (float) (origR * factor));
            }
        }
        repaint();
    }

    public void keyReleased(KeyEvent e) {
        Component c = SwingUtilities.getWindowAncestor(Viewer.this);
        if (c instanceof FullScreenFrame) {
            FullScreenFrame frame = (FullScreenFrame) c;
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                frame.close();
            } else if (e.getKeyCode() == KeyEvent.VK_P) {
                int metaMask = InputEvent.META_DOWN_MASK;
                int metaShiftMask = InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;
                if (e.getModifiersEx() == metaMask)
                    frame.toggleAnimation();
                else if (e.getModifiersEx() == metaShiftMask)
                    frame.rewindAnimation();
            }
        }

        if (handle != null)
            handle.keyReleased(e.getKeyCode(), e.getModifiersEx());
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public void paintComponent(Graphics g) {
        long paintStart = PerfMonitor.isEnabled() ? System.nanoTime() : 0;
        try {
            paintViewer(g);
        } finally {
            if (paintStart != 0) PerfMonitor.recordPaint(System.nanoTime() - paintStart);
        }
    }

    private void paintViewer(Graphics g) {
        if (!viewPositioned) {
            setViewPosition(getWidth() / 2.0, getHeight() / 2.0);
            viewPositioned = true;
        }
        Graphics2D g2 = (Graphics2D) g;

        if (gpuAcceleration) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
        }

        // Always fill the entire viewport with the active theme background color
        Color bgColor = Theme.VIEWER_BACKGROUND_COLOR != null ? Theme.VIEWER_BACKGROUND_COLOR : getBackground();
        setBackground(bgColor);
        g2.setColor(bgColor);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Set the view transform
        AffineTransform originalTransform = g2.getTransform();
        g2.transform(getViewTransform());

        paintBounds(g2);
        paintFrameGuides(g2);
        paintObjects(g2);
        paintPoints(g2);
        paintPointNumbers(g2);

        // Restore original transform
        g2.setClip(null);
        g2.setTransform(originalTransform);
        g2.setStroke(new BasicStroke(1));

        // Handles are drawn in screen space (outside the view transform). They project the
        // document coordinates they operate on, so their decorations stay a constant pixel size.
        paintHandle(g2);
        paintOrigin(g2);
        if (showSelectionGizmo && document != null && document.getActiveNode() != null) {
            paintSelectionGizmo(g2);
        }
        paintModalHud(g2);
        paintAltScrubHud(g2);
    }


    public void paintObjects(Graphics2D g) {
        if (currentVisualizer != null) {
            if (infiniteTileMode) {
                double tileW = canvasBounds != null ? canvasBounds.getWidth() : 1000;
                double tileH = canvasBounds != null ? canvasBounds.getHeight() : 1000;
                if (tileW <= 0) tileW = 1000;
                if (tileH <= 0) tileH = 1000;

                AffineTransform orig = g.getTransform();
                Composite origComp = g.getComposite();

                // Draw 8 adjacent tiles with slight transparency (0.75f)
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        AffineTransform tileTrans = new AffineTransform(orig);
                        tileTrans.translate(dx * tileW, dy * tileH);
                        g.setTransform(tileTrans);
                        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
                        currentVisualizer.draw(g, outputValues);
                    }
                }
                // Draw central tile
                g.setTransform(orig);
                g.setComposite(origComp);
                currentVisualizer.draw(g, outputValues);

                // Draw subtle dashed boundary grid separating the 3x3 tiles
                Stroke origStroke = g.getStroke();
                Color origColor = g.getColor();
                g.setColor(new Color(245, 158, 11, 150)); // Amber dashed grid
                g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 6f}, 0f));
                double left = (canvasBounds != null ? canvasBounds.getX() : -tileW / 2.0);
                double top = (canvasBounds != null ? canvasBounds.getY() : -tileH / 2.0);
                for (int i = -1; i <= 2; i++) {
                    int vx = (int) Math.round(left + i * tileW);
                    g.drawLine(vx, (int) Math.round(top - tileH), vx, (int) Math.round(top + 2 * tileH));
                    int hy = (int) Math.round(top + i * tileH);
                    g.drawLine((int) Math.round(left - tileW), hy, (int) Math.round(left + 2 * tileW), hy);
                }
                g.setStroke(origStroke);
                g.setColor(origColor);
            } else {
                currentVisualizer.draw(g, outputValues);
            }
        }
    }

    private void paintPoints(Graphics2D g) {
        if (showPoints && IGeometry.class.isAssignableFrom(valuesClass)) {
            // TODO Create a dynamic iterator that combines all output values into one flat sequence.
            LinkedList<nodebox.graphics.Point> points = new LinkedList<nodebox.graphics.Point>();
            for (Object o : outputValues) {
                IGeometry geo = (IGeometry) o;
                points.addAll(geo.getPoints());
            }
            PointVisualizer.drawPoints(g, points);
        }
    }


    private void paintPointNumbers(Graphics2D g) {
        if (!showPointNumbers) return;
        g.setFont(Theme.SMALL_MONO_FONT);
        g.setColor(Color.BLUE);
        int index = 0;

        if (IGeometry.class.isAssignableFrom(valuesClass)) {
            for (Object o : outputValues) {
                IGeometry geo = (IGeometry) o;
                for (nodebox.graphics.Point pt : geo.getPoints())
                    paintPointNumber(g, pt, index++);
            }
        } else if (nodebox.graphics.Point.class.isAssignableFrom(valuesClass)) {
            for (Object o : outputValues)
                paintPointNumber(g, (nodebox.graphics.Point) o, index++);
        }
    }

    private void paintPointNumber(Graphics2D g, nodebox.graphics.Point pt, int number) {
        if (pt.isOnCurve()) {
            g.setColor(Color.BLUE);
        } else {
            g.setColor(Color.RED);
        }
        g.drawString(number + "", (int) (pt.x + 3), (int) (pt.y - 2));
    }

    public void paintOrigin(Graphics2D g) {
        if (showOrigin) {
            int x = (int) Math.round(getViewX());
            int y = (int) Math.round(getViewY());
            g.setColor(Color.DARK_GRAY);
            g.drawLine(x, 0, x, getHeight());
            g.drawLine(0, y, getWidth(), y);
        }
    }

    public void setShowSelectionGizmo(boolean show) {
        this.showSelectionGizmo = show;
        repaint();
    }

    public boolean isShowSelectionGizmo() {
        return showSelectionGizmo;
    }

    public nodebox.graphics.Rect getOutputGeometryBounds() {
        if (outputValues == null || outputValues.isEmpty()) return null;

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean hasGeometry = false;

        for (Object o : outputValues) {
            if (o instanceof IGeometry) {
                nodebox.graphics.Rect r = ((IGeometry) o).getBounds();
                if (r != null && r.getWidth() > 0 && r.getHeight() > 0) {
                    hasGeometry = true;
                    if (r.getX() < minX) minX = r.getX();
                    if (r.getX() + r.getWidth() > maxX) maxX = r.getX() + r.getWidth();
                    if (r.getY() < minY) minY = r.getY();
                    if (r.getY() + r.getHeight() > maxY) maxY = r.getY() + r.getHeight();
                }
            } else if (o instanceof nodebox.graphics.Point) {
                nodebox.graphics.Point p = (nodebox.graphics.Point) o;
                hasGeometry = true;
                if (p.x < minX) minX = p.x;
                if (p.x > maxX) maxX = p.x;
                if (p.y < minY) minY = p.y;
                if (p.y > maxY) maxY = p.y;
            }
        }

        if (!hasGeometry || minX >= maxX || minY >= maxY) return null;
        return new nodebox.graphics.Rect(minX, minY, maxX - minX, maxY - minY);
    }

    public void paintSelectionGizmo(Graphics2D g) {
        nodebox.graphics.Rect bounds = getOutputGeometryBounds();
        if (bounds == null) return;

        double minX = bounds.getX();
        double maxX = bounds.getX() + bounds.getWidth();
        double minY = bounds.getY();
        double maxY = bounds.getY() + bounds.getHeight();

        // Project document bounding box to screen space
        Point2D pTopLeft = getViewTransform().transform(new Point2D.Double(minX, minY), null);
        Point2D pBottomRight = getViewTransform().transform(new Point2D.Double(maxX, maxY), null);

        int sx = (int) Math.round(pTopLeft.getX());
        int sy = (int) Math.round(pTopLeft.getY());
        int sw = (int) Math.round(pBottomRight.getX() - pTopLeft.getX());
        int sh = (int) Math.round(pBottomRight.getY() - pTopLeft.getY());

        if (sw <= 0 || sh <= 0) return;

        Color gizmoColor = new Color(56, 189, 248, 220); // Sky blue
        Color handleFill = Color.WHITE;

        Stroke origStroke = g.getStroke();
        Color origColor = g.getColor();

        // Draw selection bounding rectangle (dashed)
        g.setColor(new Color(56, 189, 248, 140));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
        g.drawRect(sx, sy, sw, sh);

        // Draw 8 handles
        int handleSize = 6;
        int half = handleSize / 2;
        int[][] handles = new int[][]{
                {sx, sy},
                {sx + sw / 2, sy},
                {sx + sw, sy},
                {sx, sy + sh / 2},
                {sx + sw, sy + sh / 2},
                {sx, sy + sh},
                {sx + sw / 2, sy + sh},
                {sx + sw, sy + sh}
        };

        g.setStroke(new BasicStroke(1.0f));
        for (int[] h : handles) {
            g.setColor(handleFill);
            g.fillRect(h[0] - half, h[1] - half, handleSize, handleSize);
            g.setColor(gizmoColor);
            g.drawRect(h[0] - half, h[1] - half, handleSize, handleSize);
        }

        // Draw dimension badge at bottom
        String dimText = String.format(java.util.Locale.US, "%.1f \u00D7 %.1f", (maxX - minX), (maxY - minY));
        g.setFont(Theme.SMALL_MONO_FONT);
        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(dimText);
        int textH = fm.getAscent();
        int badgeX = sx + sw / 2 - textW / 2 - 4;
        int badgeY = sy + sh + 8;

        g.setColor(new Color(15, 23, 42, 210));
        g.fillRoundRect(badgeX, badgeY, textW + 8, textH + 4, 4, 4);
        g.setColor(new Color(56, 189, 248, 180));
        g.drawRoundRect(badgeX, badgeY, textW + 8, textH + 4, 4, 4);
        g.setColor(new Color(226, 232, 240));
        g.drawString(dimText, badgeX + 4, badgeY + textH);

        g.setStroke(origStroke);
        g.setColor(origColor);
    }

    private void paintModalHud(Graphics2D g) {
        if (modalMode == ModalTransformMode.NONE) return;

        Point2D screenCenter = getViewTransform().transform(new Point2D.Double(modalObjectCenter.x, modalObjectCenter.y), null);
        Point2D screenMouse = getViewTransform().transform(new Point2D.Double(lastMousePosition.x, lastMousePosition.y), null);

        Stroke origStroke = g.getStroke();
        Color origColor = g.getColor();

        // 1. Draw dashed line from center to mouse
        g.setColor(new Color(251, 191, 36, 170)); // Warm amber
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
        g.drawLine((int) Math.round(screenCenter.getX()), (int) Math.round(screenCenter.getY()),
                   (int) Math.round(screenMouse.getX()), (int) Math.round(screenMouse.getY()));

        // 2. Draw axis constraint lines if active
        if (axisConstraint == AxisConstraint.X) {
            g.setColor(new Color(239, 68, 68, 200)); // Red for X axis
            g.setStroke(new BasicStroke(1.5f));
            int cy = (int) Math.round(screenCenter.getY());
            g.drawLine(0, cy, getWidth(), cy);
        } else if (axisConstraint == AxisConstraint.Y) {
            g.setColor(new Color(34, 197, 94, 200)); // Green for Y axis
            g.setStroke(new BasicStroke(1.5f));
            int cx = (int) Math.round(screenCenter.getX());
            g.drawLine(cx, 0, cx, getHeight());
        }

        // 3. Floating HUD Badge (top left)
        String title = "";
        String details = "";
        if (modalMode == ModalTransformMode.GRAB) {
            title = "[G] GRAB / MOVE";
            details = String.format(java.util.Locale.US, "\u0394X: %+.1f,  \u0394Y: %+.1f", modalHudDeltaX, modalHudDeltaY);
        } else if (modalMode == ModalTransformMode.ROTATE) {
            title = "[R] ROTATE";
            details = String.format(java.util.Locale.US, "Angle: %+.1f\u00B0", modalHudAngle);
        } else if (modalMode == ModalTransformMode.SCALE) {
            title = "[S] SCALE / RESIZE";
            details = String.format(java.util.Locale.US, "Factor: %.2f\u00D7", modalHudScale);
        }

        String axisText = (axisConstraint == AxisConstraint.X) ? "Axis: X" :
                ((axisConstraint == AxisConstraint.Y) ? "Axis: Y" : "Axis: Free");
        String footer = "X/Y: Axis  |  Shift: Fine/Snap  |  LMB/Enter: Confirm  |  RMB/Esc: Cancel";

        g.setFont(Theme.SMALL_BOLD_FONT);
        FontMetrics fm = g.getFontMetrics();
        int lineH = fm.getHeight();
        int pad = 12;
        int badgeW = 340;
        int badgeH = lineH * 3 + pad * 2 - 2;
        int badgeX = 16;
        int badgeY = 16;

        // Background card
        g.setColor(new Color(15, 23, 42, 235));
        g.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);
        g.setColor(new Color(251, 191, 36, 220)); // Amber accent border
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);

        // Header
        g.setColor(new Color(251, 191, 36));
        g.drawString(title + "  (" + axisText + ")", badgeX + pad, badgeY + pad + fm.getAscent());

        // Value line
        g.setColor(Color.WHITE);
        g.setFont(Theme.SMALL_MONO_FONT);
        g.drawString(details, badgeX + pad, badgeY + pad + lineH + fm.getAscent());

        // Footer instructions
        g.setFont(Theme.SMALL_FONT);
        g.setColor(new Color(148, 163, 184));
        g.drawString(footer, badgeX + pad, badgeY + pad + lineH * 2 + fm.getAscent());

        g.setStroke(origStroke);
        g.setColor(origColor);
    }

    private void paintAltScrubHud(Graphics2D g) {
        if (!isAltScrubbing || altScrubPortName == null) return;

        String title = "ALT-SCRUB: " + altScrubPortName;
        String valStr = nodebox.node.Port.TYPE_INT.equals(altScrubPortType) ?
                String.valueOf(Math.round(altScrubCurrentValue)) :
                String.format(java.util.Locale.US, "%.2f", altScrubCurrentValue);
        String tip = "Drag \u2194 left/right  |  Shift: Fine precision";

        g.setFont(Theme.SMALL_BOLD_FONT);
        FontMetrics fm = g.getFontMetrics();
        int lineH = fm.getHeight();
        int pad = 10;
        int badgeW = 280;
        int badgeH = lineH * 2 + pad * 2 + 2;
        int badgeX = getWidth() / 2 - badgeW / 2;
        int badgeY = 20;

        Stroke origStroke = g.getStroke();
        Color origColor = g.getColor();

        g.setColor(new Color(15, 23, 42, 235));
        g.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);
        g.setColor(new Color(56, 189, 248, 220)); // Sky blue border
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);

        g.setColor(new Color(56, 189, 248));
        g.drawString(title, badgeX + pad, badgeY + pad + fm.getAscent());

        g.setFont(Theme.SMALL_MONO_FONT);
        g.setColor(Color.WHITE);
        g.drawString("=  " + valStr, badgeX + pad + fm.stringWidth(title) + 12, badgeY + pad + fm.getAscent());

        g.setFont(Theme.SMALL_FONT);
        g.setColor(new Color(148, 163, 184));
        g.drawString(tip, badgeX + pad, badgeY + pad + lineH + 2 + fm.getAscent());

        g.setStroke(origStroke);
        g.setColor(origColor);
    }

    public void paintBounds(Graphics2D g) {
        if (showBounds) {
            g.setColor(Color.DARK_GRAY);
            int x = (int) Math.round(canvasBounds.getX());
            int y = (int) Math.round(canvasBounds.getY());
            int width = (int) Math.round(canvasBounds.getWidth());
            int height = (int) Math.round(canvasBounds.getHeight());
            g.drawRect(x, y, width, height);
            g.drawLine(x + width + 1, y + 1, x + width + 1, y + height + 1);
            g.drawLine(x + 1, y + height + 1, x + width + 1, y + height + 1);
        }
    }

    public void paintFrameGuides(Graphics2D g) {
        if (FRAME_GUIDE_OFF.equalsIgnoreCase(frameGuide)) return;

        double targetRatio = 1.0;
        if (FRAME_GUIDE_16_9.equals(frameGuide)) targetRatio = 16.0 / 9.0;
        else if (FRAME_GUIDE_9_16.equals(frameGuide)) targetRatio = 9.0 / 16.0;
        else if (FRAME_GUIDE_4_3.equals(frameGuide)) targetRatio = 4.0 / 3.0;
        else if (FRAME_GUIDE_3_4.equals(frameGuide)) targetRatio = 3.0 / 4.0;

        double cbW = canvasBounds != null ? canvasBounds.getWidth() : 1000;
        double cbH = canvasBounds != null ? canvasBounds.getHeight() : 1000;
        double cx = canvasBounds != null ? canvasBounds.getCenterX() : 0;
        double cy = canvasBounds != null ? canvasBounds.getCenterY() : 0;

        double guideW, guideH;
        if (cbW / cbH > targetRatio) {
            guideH = cbH;
            guideW = cbH * targetRatio;
        } else {
            guideW = cbW;
            guideH = cbW / targetRatio;
        }
        double guideX = cx - guideW / 2.0;
        double guideY = cy - guideH / 2.0;

        Stroke originalStroke = g.getStroke();
        Color originalColor = g.getColor();

        // Draw rule of thirds (faint dotted lines)
        g.setColor(new Color(56, 189, 248, 70));
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 6f}, 0f));
        // Vertical thirds
        g.drawLine((int) Math.round(guideX + guideW / 3.0), (int) Math.round(guideY),
                   (int) Math.round(guideX + guideW / 3.0), (int) Math.round(guideY + guideH));
        g.drawLine((int) Math.round(guideX + 2.0 * guideW / 3.0), (int) Math.round(guideY),
                   (int) Math.round(guideX + 2.0 * guideW / 3.0), (int) Math.round(guideY + guideH));
        // Horizontal thirds
        g.drawLine((int) Math.round(guideX), (int) Math.round(guideY + guideH / 3.0),
                   (int) Math.round(guideX + guideW), (int) Math.round(guideY + guideH / 3.0));
        g.drawLine((int) Math.round(guideX), (int) Math.round(guideY + 2.0 * guideH / 3.0),
                   (int) Math.round(guideX + guideW), (int) Math.round(guideY + 2.0 * guideH / 3.0));

        // Draw frame border (dashed)
        g.setColor(new Color(56, 189, 248, 180));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 6f}, 0f));
        g.drawRect((int) Math.round(guideX), (int) Math.round(guideY), (int) Math.round(guideW), (int) Math.round(guideH));

        // Draw label
        g.setFont(Theme.SMALL_MONO_FONT);
        g.setColor(new Color(56, 189, 248, 220));
        String label = frameGuide + " (" + (int) Math.round(guideW) + " \u00D7 " + (int) Math.round(guideH) + ")";
        g.drawString(label, (int) Math.round(guideX + 4), (int) Math.round(guideY - 4));

        g.setStroke(originalStroke);
        g.setColor(originalColor);
    }

    public void paintHandle(Graphics2D g) {
        if (hasVisibleHandle()) {
            // Refresh the handle's view transform before drawing. This also covers view changes
            // that don't fire onViewTransformChanged (e.g. the first-paint setViewPosition).
            handle.setViewTransform(getViewX(), getViewY(), getViewScale());
            // Create a canvas with a transparent background. Handles draw in screen space, so the
            // canvas must cover the whole viewport (Canvas.draw clips to its bounds). Size it to
            // the component and offset it so its bounds are (0, 0, width, height) in screen space.
            nodebox.graphics.Canvas canvas = new nodebox.graphics.Canvas(getWidth(), getHeight());
            canvas.setOffsetX(getWidth() / 2.0);
            canvas.setOffsetY(getHeight() / 2.0);
            canvas.setBackground(new nodebox.graphics.Color(0, 0, 0, 0));
            CanvasContext ctx = new CanvasContext(canvas);
            try {
                handle.draw(ctx);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ctx.getCanvas().draw(g);
        }
    }


    private class PopupHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        public void showPopup(MouseEvent e) {
            if (!e.isPopupTrigger()) return;
            viewerMenu.removeAll();
            viewerMenu.add(new ResetViewAction());
            viewerMenu.addSeparator();
            viewerMenu.add(createFrameGuideMenu());
            JCheckBoxMenuItem gizmoItem = new JCheckBoxMenuItem(new AbstractAction("Show Selection Bounds") {
                public void actionPerformed(ActionEvent e) {
                    setShowSelectionGizmo(!showSelectionGizmo);
                }
            });
            gizmoItem.setSelected(showSelectionGizmo);
            viewerMenu.add(gizmoItem);

            JCheckBoxMenuItem repeatItem = new JCheckBoxMenuItem(new AbstractAction("3\u00D73 Infinite Repeat (T)") {
                public void actionPerformed(ActionEvent e) {
                    setInfiniteTileMode(!infiniteTileMode);
                    if (viewerPane != null) {
                        viewerPane.updateRepeatCheck(infiniteTileMode);
                    }
                }
            });
            repeatItem.setSelected(infiniteTileMode);
            viewerMenu.add(repeatItem);

            Theme.applyPopupMenuTheme(viewerMenu);
            viewerMenu.show(Viewer.this, e.getX(), e.getY());
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

}
