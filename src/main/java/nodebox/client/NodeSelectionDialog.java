package nodebox.client;

import nodebox.node.Node;
import nodebox.node.NodeLibrary;
import nodebox.node.NodeRepository;
import nodebox.ui.Theme;
import nodebox.util.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataListener;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.regex.Pattern;

public class NodeSelectionDialog extends JDialog {

    private class FilteredNodeListModel implements ListModel<Node> {

        private NodeLibrary library;
        private NodeRepository repository;
        private java.util.List<Node> filteredNodes;
        private String searchString;
        private String category;

        private FilteredNodeListModel(NodeLibrary library, NodeRepository repository) {
            this.library = library;
            this.repository = repository;
            searchString = "";
            category = null;
            filteredNodes = new ArrayList<Node>();
            filteredNodes.addAll(repository.getNodes());
            Collections.sort(filteredNodes, new NodeNameComparator());
        }

        public String getSearchString() {
            return searchString;
        }

        public void setSearchString(String searchString) {
            this.searchString = searchString = searchString.trim().toLowerCase(Locale.US);
            java.util.List<Node> nodes = new ArrayList<Node>();
            filteredNodes.clear();
            // Add all the nodes from the repository.
            filteredNodes.addAll(filterNodes(repository.getNodesByCategory(category), this.searchString));
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
            setSearchString(searchString);
        }

        private java.util.List<Node> filterNodes(java.util.List<Node> nodes, String searchString) {
            Pattern findFirstLettersPattern = Pattern.compile("^" + StringUtils.join(searchString, "\\w*_") + ".*");
            Pattern findConsecutiveLettersPattern = Pattern.compile(".*" + searchString + ".*");
            Pattern findNonConsecutiveLettersPattern = Pattern.compile(".*" + StringUtils.join(searchString, "\\w*") + ".*");

            java.util.List<Node> sortedNodes = new ArrayList<Node>();
            java.util.List<Node> startsWithNodes = new ArrayList<Node>();
            java.util.List<Node> firstLettersNodes = new ArrayList<Node>();
            java.util.List<Node> consecutiveLettersNodes = new ArrayList<Node>();
            java.util.List<Node> nonConsecutiveLettersNodes = new ArrayList<Node>();
            java.util.List<Node> descriptionNodes = new ArrayList<Node>();

            for (Node node : nodes) {
                if (node.getName().equals(searchString))
                    sortedNodes.add(0, node);
                else if (node.getName().startsWith(searchString))
                    startsWithNodes.add(node);
                else if (findFirstLettersPattern.matcher(node.getName()).matches())
                    firstLettersNodes.add(node);
                else if (findConsecutiveLettersPattern.matcher(node.getName()).matches())
                    consecutiveLettersNodes.add(node);
                else if (findNonConsecutiveLettersPattern.matcher(node.getName()).matches())
                    nonConsecutiveLettersNodes.add(node);
                else if (node.getDescription().contains(searchString))
                    descriptionNodes.add(node);
            }
            Collections.sort(startsWithNodes, new NodeNameComparator());
            sortedNodes.addAll(startsWithNodes);
            Collections.sort(firstLettersNodes, new NodeNameComparator());
            sortedNodes.addAll(firstLettersNodes);
            Collections.sort(consecutiveLettersNodes, new NodeNameComparator());
            sortedNodes.addAll(consecutiveLettersNodes);
            Collections.sort(nonConsecutiveLettersNodes, new NodeNameComparator());
            sortedNodes.addAll(nonConsecutiveLettersNodes);
            Collections.sort(descriptionNodes, new NodeNameComparator());
            sortedNodes.addAll(descriptionNodes);
            return sortedNodes;
        }

        public int getSize() {
            return filteredNodes.size();
        }

        public Node getElementAt(int index) {
            return filteredNodes.get(index);
        }

        public void addListDataListener(ListDataListener l) {
            // The list is immutable; don't listen.
        }

        public void removeListDataListener(ListDataListener l) {
            // The list is immutable; don't listen.
        }
    }

    private class NodeRenderer extends JLabel implements ListCellRenderer<Node> {

        public Component getListCellRendererComponent(JList<? extends Node> list, Node node, int index, boolean isSelected, boolean cellHasFocus) {
            String titleColor = Theme.isDark() ? "#f4f4f5" : "#000000";
            String descColor = Theme.isDark() ? "#a1a1aa" : "#555555";
            String html = "<html><b style='color:" + titleColor + ";'>" + StringUtils.humanizeName(node.getName()) + "</b> <span style='color:" + descColor + ";'>- " + node.getDescription() + "</span></html>";
            setText(html);
            if (isSelected) {
                setBackground(Theme.NODE_SELECTION_ACTIVE_BACKGROUND_COLOR);
            } else {
                setBackground(Theme.NODE_SELECTION_BACKGROUND_COLOR);
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            int iconSize = NetworkView.NODE_ICON_SIZE;
            int nodePadding = NetworkView.NODE_PADDING;
            BufferedImage bi = new BufferedImage(iconSize + nodePadding * 2, iconSize + nodePadding * 2, BufferedImage.TYPE_INT_ARGB);
            Graphics g = bi.createGraphics();
            Color portColor = NetworkView.portTypeColor(node.getOutputType());
            g.setColor(portColor);
            g.fillRect(0, 0, iconSize + nodePadding * 2, iconSize + nodePadding * 2);
            BufferedImage nodeImg = NetworkView.getImageForNode(node, repository);
            double luminance = 0.299 * portColor.getRed() + 0.587 * portColor.getGreen() + 0.114 * portColor.getBlue();
            if (luminance > 130) {
                nodeImg = NetworkView.getInvertedImage(nodeImg);
            }
            g.drawImage(nodeImg, nodePadding, nodePadding, iconSize, iconSize, null, null);
            setIcon(new ImageIcon(bi));
            setBorder(Theme.BOTTOM_BORDER);
            setOpaque(true);
            return this;
        }
    }

    private NodeRepository repository;
    private JTextField searchField;
    private CategoryList categoryList;
    private JList<Node> nodeList;
    private Node selectedNode;
    private FilteredNodeListModel filteredNodeListModel;

    private final String ALL_CATEGORIES = "All";
    private final String OTHER_CATEGORIES = "Other";

    public NodeSelectionDialog(NodeLibrary library, NodeRepository repository) {
        this(null, library, repository);
    }

    public NodeSelectionDialog(Frame owner, NodeLibrary library, NodeRepository repository) {
        super(owner, "New Node", true);
        getRootPane().putClientProperty("Window.style", "small");
        JPanel panel = new JPanel(new BorderLayout());
        this.repository = repository;
        filteredNodeListModel = new FilteredNodeListModel(library, repository);
        searchField = new JTextField();
        searchField.putClientProperty("JTextField.variant", "search");
        EscapeListener escapeListener = new EscapeListener();
        searchField.addKeyListener(escapeListener);
        ArrowKeysListener arrowKeysListener = new ArrowKeysListener();
        searchField.addKeyListener(arrowKeysListener);
        SearchFieldChangeListener searchFieldChangeListener = new SearchFieldChangeListener();
        searchField.getDocument().addDocumentListener(searchFieldChangeListener);
        nodeList = new JList<Node>(filteredNodeListModel) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                if (index > -1) {
                    Rectangle bounds = getCellBounds(index, index);
                    if (bounds != null && bounds.contains(e.getPoint())) {
                        Node node = getModel().getElementAt(index);
                        if (node != null) {
                            return NodeDocumentation.getHtmlTooltip(node);
                        }
                    }
                }
                return null;
            }
        };
        ToolTipManager.sharedInstance().registerComponent(nodeList);
        DoubleClickListener doubleClickListener = new DoubleClickListener();
        nodeList.addMouseListener(doubleClickListener);
        nodeList.addKeyListener(escapeListener);
        nodeList.addKeyListener(arrowKeysListener);
        nodeList.setSelectedIndex(0);
        nodeList.setCellRenderer(new NodeRenderer());
        JScrollPane nodeScroll = new JScrollPane(nodeList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        nodeScroll.setBorder(null);
        JPanel centerPanel = new JPanel(new BorderLayout());
        categoryList = new CategoryList();
        reloadCategories();
        centerPanel.add(categoryList, BorderLayout.WEST);
        centerPanel.add(nodeScroll, BorderLayout.CENTER);
        centerPanel.validate();

        panel.add(searchField, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);

        if (Theme.isDark()) {
            panel.setBackground(Theme.PANEL_BACKGROUND);
            centerPanel.setBackground(Theme.PANEL_BACKGROUND);
            nodeList.setBackground(Theme.NODE_SELECTION_BACKGROUND_COLOR);
            nodeScroll.getViewport().setBackground(Theme.NODE_SELECTION_BACKGROUND_COLOR);
            searchField.setBackground(new Color(26, 26, 30));
            searchField.setForeground(Theme.TEXT_NORMAL_COLOR);
            searchField.setCaretColor(Theme.TEXT_NORMAL_COLOR);
            searchField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.SPLIT_PANE_BORDER),
                    BorderFactory.createEmptyBorder(5, 7, 5, 7)));
        }

        setContentPane(panel);
        setSize(500, 400);
        setLocationRelativeTo(owner);
    }

    public void reloadCategories() {
        categoryList.removeAll();
        categoryList.addCategory(ALL_CATEGORIES, null);
        for (String category : repository.getCategories()) {
            if (category != null && ! category.isEmpty())
                categoryList.addCategory(StringUtils.humanizeName(category), category);
        }
        categoryList.addCategory(OTHER_CATEGORIES, "");
        categoryList.setSelectedCategory(ALL_CATEGORIES);
    }

    public Node getSelectedNode() {
        return selectedNode;
    }

    public NodeRepository getRepository() {
        return repository;
    }

    private void closeDialog() {
        setVisible(false);
    }

    private void moveUp() {
        int index = nodeList.getSelectedIndex();
        index--;
        if (index < 0) {
            index = nodeList.getModel().getSize() - 1;
        }
        nodeList.setSelectedIndex(index);
        nodeList.ensureIndexIsVisible(index);
    }

    private void moveDown() {
        int index = nodeList.getSelectedIndex();
        index++;
        if (index >= nodeList.getModel().getSize()) {
            index = 0;
        }
        nodeList.setSelectedIndex(index);
        nodeList.ensureIndexIsVisible(index);
    }

    private void selectAndClose() {
        if (nodeList.getModel().getSize() == 0) return;
        selectedNode = (Node) nodeList.getSelectedValue();
        closeDialog();
    }

    private class DoubleClickListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                selectAndClose();
            }
        }
    }

    private class EscapeListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                closeDialog();
        }
    }


    private class ArrowKeysListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_UP) {
                if (e.getSource() == searchField)
                    moveUp();
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                if (e.getSource() == searchField)
                    moveDown();
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                selectAndClose();
            }
        }
    }

    private class SearchFieldChangeListener implements DocumentListener {
        public void insertUpdate(DocumentEvent e) {
            changedEvent();
        }

        public void removeUpdate(DocumentEvent e) {
            changedEvent();
        }

        public void changedUpdate(DocumentEvent e) {
            changedEvent();
        }

        private void changedEvent() {
            if (filteredNodeListModel.getSearchString().equals(searchField.getText())) return;
            filteredNodeListModel.setSearchString(searchField.getText());
            // Trigger a model reload.
            nodeList.setSelectedIndex(0);
            nodeList.ensureIndexIsVisible(0);
            nodeList.revalidate();
            repaint();
        }
    }

    private class NodeNameComparator implements Comparator<Node> {
        public int compare(Node node1, Node node2) {
            return node1.getName().compareTo(node2.getName());
        }
    }

    private class CategoryLabel extends JComponent {

        private String text;
        private Object source;

        private boolean selected;

        private CategoryLabel(String text, Object source) {
            this.text = text;
            this.source = source;
            setMinimumSize(new Dimension(120, 25));
            setMaximumSize(new Dimension(500, 25));
            setPreferredSize(new Dimension(120, 25));
            setSize(new Dimension(120, 25));
            setAlignmentX(JComponent.LEFT_ALIGNMENT);
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            if (selected) {
                Rectangle clip = g2.getClipBounds();
                g2.setColor(Theme.isDark() ? Theme.NODE_SELECTION_ACTIVE_BACKGROUND_COLOR : new java.awt.Color(224, 224, 224));
                g2.fillRect(clip.x, clip.y, clip.width, clip.height);
            }
            g2.setFont(Theme.SMALL_FONT);
            g2.setColor(Theme.isDark() ? Theme.TEXT_NORMAL_COLOR : Color.BLACK);
            g2.drawString(text, 15, 18);
        }
    }


    private class CategoryList extends JPanel {
        private CategoryLabel selectedCategory;
        private Map<String, CategoryLabel> labelMap = new HashMap<String, CategoryLabel>();

        private CategoryList() {
            super(null);
            setBackground(Theme.isDark() ? new Color(34, 34, 38) : new java.awt.Color(244, 244, 244));
            setBorder(Theme.isDark() ? BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.SPLIT_PANE_BORDER) : null);
            setOpaque(true);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        public void addCategory(final String categoryLabel, final Object source) {
            final CategoryLabel label = new CategoryLabel(categoryLabel, source);
            label.addMouseListener(new MouseInputAdapter() {
                public void mouseClicked(MouseEvent e) {
                    setSelectedCategory(label);
                }
            });
            labelMap.put(categoryLabel, label);
            add(label);
        }

        public void setSelectedCategory(CategoryLabel label) {
            if (selectedCategory != null)
                selectedCategory.setSelected(false);
            selectedCategory = label;
            if (selectedCategory != null) {
                selectedCategory.setSelected(true);
                filteredNodeListModel.setCategory((String) selectedCategory.source);
                // Trigger a model reload.
                nodeList.setSelectedIndex(0);
                nodeList.ensureIndexIsVisible(0);
                nodeList.revalidate();
                nodeList.repaint();
            }
        }

        public void setSelectedCategory(String value) {
            CategoryLabel label = labelMap.get(value);
            assert label != null;
            setSelectedCategory(label);
        }
    }
}