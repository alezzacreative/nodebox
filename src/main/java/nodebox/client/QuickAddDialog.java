package nodebox.client;

import nodebox.node.Node;
import nodebox.node.NodeLibrary;
import nodebox.node.NodeRepository;
import nodebox.ui.Theme;
import nodebox.util.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.Pattern;

public class QuickAddDialog extends JDialog {

    private final NodeRepository repository;
    private final JTextField searchField;
    private final JList<Node> nodeList;
    private final QuickNodeListModel listModel;
    private Node selectedNode;

    public QuickAddDialog(Frame owner, NodeLibrary library, NodeRepository repository) {
        super(owner, "Quick Add Node", true);
        this.repository = repository;
        setUndecorated(true);
        getRootPane().putClientProperty("Window.style", "small");

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        boolean dark = Theme.isDark();
        Color bg = dark ? new Color(30, 30, 34) : Color.WHITE;
        Color borderCol = dark ? new Color(60, 60, 68) : new Color(180, 180, 180);
        mainPanel.setBackground(bg);
        mainPanel.setBorder(BorderFactory.createLineBorder(borderCol, 1));

        // Header / Search Field
        searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, borderCol),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        if (dark) {
            searchField.setBackground(new Color(24, 24, 27));
            searchField.setForeground(new Color(240, 240, 242));
            searchField.setCaretColor(Color.WHITE);
        } else {
            searchField.setBackground(new Color(250, 250, 250));
            searchField.setForeground(Color.BLACK);
        }

        listModel = new QuickNodeListModel(repository.getNodes());
        nodeList = new JList<Node>(listModel) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                if (index > -1) {
                    Rectangle bounds = getCellBounds(index, index);
                    if (bounds != null && bounds.contains(e.getPoint())) {
                        Node node = listModel.getElementAt(index);
                        if (node != null) {
                            return NodeDocumentation.getHtmlTooltip(node);
                        }
                    }
                }
                return null;
            }
        };
        ToolTipManager.sharedInstance().registerComponent(nodeList);
        nodeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        nodeList.setCellRenderer(new QuickNodeRenderer());
        if (listModel.getSize() > 0) {
            nodeList.setSelectedIndex(0);
        }

        if (dark) {
            nodeList.setBackground(new Color(30, 30, 34));
        } else {
            nodeList.setBackground(Color.WHITE);
        }

        JScrollPane scrollPane = new JScrollPane(nodeList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(nodeList.getBackground());

        // Footer hint
        JLabel hintLabel = new JLabel(" \u2191\u2193 Navigate   \u21B5 Insert   Esc Cancel");
        hintLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        hintLabel.setForeground(dark ? new Color(140, 140, 148) : new Color(120, 120, 120));
        hintLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, borderCol),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        if (dark) {
            hintLabel.setOpaque(true);
            hintLabel.setBackground(new Color(24, 24, 27));
        }

        mainPanel.add(searchField, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(hintLabel, BorderLayout.SOUTH);
        setContentPane(mainPanel);
        setSize(340, 360);

        // Listeners
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateFilter(); }
            public void removeUpdate(DocumentEvent e) { updateFilter(); }
            public void changedUpdate(DocumentEvent e) { updateFilter(); }
        });

        KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_ESCAPE) {
                    selectedNode = null;
                    dispose();
                } else if (code == KeyEvent.VK_ENTER) {
                    confirmSelection();
                } else if (code == KeyEvent.VK_UP) {
                    moveSelection(-1);
                    e.consume();
                } else if (code == KeyEvent.VK_DOWN) {
                    moveSelection(1);
                    e.consume();
                }
            }
        };

        searchField.addKeyListener(keyAdapter);
        nodeList.addKeyListener(keyAdapter);

        nodeList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 1) {
                    int index = nodeList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        nodeList.setSelectedIndex(index);
                        confirmSelection();
                    }
                }
            }
        });

        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                dispose();
            }
        });
    }

    private void updateFilter() {
        String text = searchField.getText();
        listModel.filter(text);
        if (listModel.getSize() > 0) {
            nodeList.setSelectedIndex(0);
            nodeList.ensureIndexIsVisible(0);
        }
        nodeList.repaint();
    }

    private void moveSelection(int delta) {
        int size = listModel.getSize();
        if (size == 0) return;
        int next = (nodeList.getSelectedIndex() + delta + size) % size;
        nodeList.setSelectedIndex(next);
        nodeList.ensureIndexIsVisible(next);
    }

    private void confirmSelection() {
        selectedNode = nodeList.getSelectedValue();
        dispose();
    }

    public Node getSelectedNode() {
        return selectedNode;
    }

    public void setLocationRelativeToScreenPoint(Point screenPoint) {
        if (screenPoint == null) {
            setLocationRelativeTo(getOwner());
            return;
        }
        GraphicsConfiguration gc = getGraphicsConfiguration();
        Rectangle screenBounds = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        int minX = screenBounds.x + insets.left;
        int minY = screenBounds.y + insets.top;
        int maxX = screenBounds.x + screenBounds.width - insets.right - getWidth();
        int maxY = screenBounds.y + screenBounds.height - insets.bottom - getHeight();

        int x = Math.max(minX, Math.min(screenPoint.x - getWidth() / 2, maxX));
        int y = Math.max(minY, Math.min(screenPoint.y - 30, maxY));
        setLocation(x, y);
    }

    static class QuickNodeListModel extends AbstractListModel<Node> {
        private final java.util.List<Node> allNodes;
        private final java.util.List<Node> displayedNodes;

        public QuickNodeListModel(Collection<Node> nodes) {
            allNodes = new ArrayList<>(nodes);
            Collections.sort(allNodes, new Comparator<Node>() {
                public int compare(Node a, Node b) {
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });
            displayedNodes = new ArrayList<>(allNodes);
        }

        public void filter(String query) {
            displayedNodes.clear();
            if (query == null || query.trim().isEmpty()) {
                displayedNodes.addAll(allNodes);
                fireContentsChanged(this, 0, Math.max(0, displayedNodes.size() - 1));
                return;
            }
            String q = query.trim().toLowerCase(Locale.US);
            Pattern initials = Pattern.compile("^" + StringUtils.join(q, "\\w*_") + ".*");

            java.util.List<Node> exact = new ArrayList<Node>();
            java.util.List<Node> startsWith = new ArrayList<Node>();
            java.util.List<Node> initialsList = new ArrayList<Node>();
            java.util.List<Node> contains = new ArrayList<Node>();
            java.util.List<Node> desc = new ArrayList<Node>();

            for (Node n : allNodes) {
                String name = n.getName().toLowerCase(Locale.US);
                if (name.equals(q)) {
                    exact.add(n);
                } else if (name.startsWith(q)) {
                    startsWith.add(n);
                } else if (initials.matcher(name).matches()) {
                    initialsList.add(n);
                } else if (name.contains(q)) {
                    contains.add(n);
                } else if (n.getDescription() != null && n.getDescription().toLowerCase(Locale.US).contains(q)) {
                    desc.add(n);
                }
            }
            displayedNodes.addAll(exact);
            displayedNodes.addAll(startsWith);
            displayedNodes.addAll(initialsList);
            displayedNodes.addAll(contains);
            displayedNodes.addAll(desc);
            fireContentsChanged(this, 0, Math.max(0, displayedNodes.size() - 1));
        }

        public int getSize() {
            return displayedNodes.size();
        }

        public Node getElementAt(int index) {
            return displayedNodes.get(index);
        }
    }

    private class QuickNodeRenderer extends JPanel implements ListCellRenderer<Node> {
        private final JLabel nameLabel = new JLabel();
        private final JLabel categoryLabel = new JLabel();
        private final JPanel swatch = new JPanel();

        public QuickNodeRenderer() {
            super(new BorderLayout(8, 0));
            setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            swatch.setPreferredSize(new Dimension(14, 14));
            swatch.setOpaque(true);

            nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            categoryLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            left.setOpaque(false);
            left.add(swatch);
            left.add(nameLabel);

            add(left, BorderLayout.WEST);
            add(categoryLabel, BorderLayout.EAST);
        }

        public Component getListCellRendererComponent(JList<? extends Node> list, Node node, int index, boolean isSelected, boolean cellHasFocus) {
            boolean dark = Theme.isDark();
            if (node == null) return this;

            Color typeCol = NetworkView.portTypeColor(node.getOutputType());
            swatch.setBackground(typeCol);

            nameLabel.setText(StringUtils.humanizeName(node.getName()));
            String cat = node.getCategory();
            if (cat == null || cat.isEmpty()) cat = node.getOutputType();
            categoryLabel.setText(cat != null ? cat : "");

            if (isSelected) {
                setBackground(dark ? new Color(50, 75, 110) : new Color(210, 230, 255));
                nameLabel.setForeground(dark ? Color.WHITE : new Color(10, 40, 90));
                categoryLabel.setForeground(dark ? new Color(200, 215, 240) : new Color(80, 110, 150));
            } else {
                setBackground(dark ? new Color(30, 30, 34) : Color.WHITE);
                nameLabel.setForeground(dark ? new Color(228, 228, 231) : new Color(30, 30, 30));
                categoryLabel.setForeground(dark ? new Color(140, 140, 148) : new Color(140, 140, 140));
            }
            setOpaque(true);
            return this;
        }
    }
}
