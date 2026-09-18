package nodebox.ui;

import nodebox.client.ExamplesBrowser;
import nodebox.client.NetworkView;
import nodebox.client.NodeBoxMenuBar;
import nodebox.node.Port;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.Assert.*;

public class ThemeTest {

    private String originalTheme;

    @Before
    public void setUp() {
        originalTheme = Theme.getTheme();
    }

    @After
    public void tearDown() {
        Theme.setTheme(originalTheme);
    }

    @Test
    public void testDefaultThemeIsLight() {
        Theme.setTheme(Theme.THEME_LIGHT);
        assertFalse(Theme.isDark());
        assertEquals(Theme.THEME_LIGHT, Theme.getTheme());
        assertEquals(new Color(60, 60, 60), Theme.TEXT_NORMAL_COLOR);
        assertEquals(new Color(232, 232, 232), Theme.VIEWER_BACKGROUND_COLOR);
        assertEquals(new Color(69, 69, 69), Theme.NETWORK_BACKGROUND_COLOR);
    }

    @Test
    public void testSwitchToDarkTheme() {
        Theme.setTheme(Theme.THEME_DARK);
        assertTrue(Theme.isDark());
        assertEquals(Theme.THEME_DARK, Theme.getTheme());
        assertEquals(new Color(228, 228, 231), Theme.TEXT_NORMAL_COLOR);
        assertEquals(new Color(30, 30, 34), Theme.VIEWER_BACKGROUND_COLOR);
        assertEquals(new Color(24, 24, 27), Theme.NETWORK_BACKGROUND_COLOR);
        assertEquals(new Color(39, 39, 42), Theme.NETWORK_GRID_COLOR);
    }

    @Test
    public void testThemeRoundTrip() {
        Theme.setTheme(Theme.THEME_LIGHT);
        assertFalse(Theme.isDark());

        Theme.setTheme(Theme.THEME_DARK);
        assertTrue(Theme.isDark());

        Theme.setTheme(Theme.THEME_LIGHT);
        assertFalse(Theme.isDark());
        assertEquals(new Color(60, 60, 60), Theme.TEXT_NORMAL_COLOR);
    }

    @Test
    public void testSetDarkBoolean() {
        Theme.setDark(true);
        assertTrue(Theme.isDark());
        assertEquals(Theme.THEME_DARK, Theme.getTheme());

        Theme.setDark(false);
        assertFalse(Theme.isDark());
        assertEquals(Theme.THEME_LIGHT, Theme.getTheme());
    }

    @Test
    public void testProceduralPaintMethods() {
        BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        // Dark theme rendering
        Theme.setTheme(Theme.THEME_DARK);
        Theme.paintHeaderBackground(g2, 0, 0, 200, 25);
        Theme.paintAnimationBackground(g2, 0, 0, 200, 27);
        Theme.paintAddressBackground(g2, 0, 0, 200, 25);
        Theme.paintDivider(g2, 0, 0, 20);
        Theme.paintPaneMenu(g2, 0, 0, 100, 21);
        Theme.paintDraggableNumber(g2, 80, 20, true);
        Theme.paintDraggableNumber(g2, 80, 20, false);

        // Borders paint test
        Theme.PARAMETER_ROW_BORDER.paintBorder(null, g2, 0, 0, 200, 30);
        Theme.PARAMETER_NOTES_BORDER.paintBorder(null, g2, 0, 0, 200, 30);
        Theme.INNER_SHADOW_BORDER.paintBorder(null, g2, 0, 0, 100, 20);
        Theme.TOP_BOTTOM_BORDER.paintBorder(null, g2, 0, 0, 100, 20);
        Theme.TOP_BORDER.paintBorder(null, g2, 0, 0, 100, 20);
        Theme.BOTTOM_BORDER.paintBorder(null, g2, 0, 0, 100, 20);

        // Light theme borders paint test
        Theme.setTheme(Theme.THEME_LIGHT);
        Theme.PARAMETER_ROW_BORDER.paintBorder(null, g2, 0, 0, 200, 30);
        Theme.PARAMETER_NOTES_BORDER.paintBorder(null, g2, 0, 0, 200, 30);
        Theme.INNER_SHADOW_BORDER.paintBorder(null, g2, 0, 0, 100, 20);
        Theme.TOP_BOTTOM_BORDER.paintBorder(null, g2, 0, 0, 100, 20);
        Theme.TOP_BORDER.paintBorder(null, g2, 0, 0, 100, 20);
        Theme.BOTTOM_BORDER.paintBorder(null, g2, 0, 0, 100, 20);

        g2.dispose();
    }

    @Test
    public void testUIManagerThemingAndPalette() {
        Theme.setTheme(Theme.THEME_DARK);
        assertEquals(new Color(34, 34, 38), Theme.PANEL_BACKGROUND);
        assertEquals(new Color(34, 34, 38), Theme.MENUBAR_BACKGROUND);
        assertEquals(new Color(40, 40, 44), Theme.DIALOG_BACKGROUND);
        assertEquals(new Color(40, 40, 44), UIManager.getColor("Panel.background"));
        assertEquals(new Color(228, 228, 231), UIManager.getColor("Label.foreground"));

        Theme.setTheme(Theme.THEME_LIGHT);
        assertEquals(new Color(240, 240, 240), Theme.PANEL_BACKGROUND);
        assertEquals(new Color(245, 245, 245), Theme.MENUBAR_BACKGROUND);
        assertEquals(new Color(240, 240, 240), Theme.DIALOG_BACKGROUND);
    }

    @Test
    public void testCustomSplitPanePainting() {
        Theme.setTheme(Theme.THEME_DARK);
        CustomSplitPane vSplit = new CustomSplitPane(JSplitPane.VERTICAL_SPLIT, new javax.swing.JPanel(), new javax.swing.JPanel());
        vSplit.setSize(400, 300);
        vSplit.doLayout();
        BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        vSplit.paint(g2);

        CustomSplitPane hSplit = new CustomSplitPane(JSplitPane.HORIZONTAL_SPLIT, new javax.swing.JPanel(), new javax.swing.JPanel());
        hSplit.setSize(400, 300);
        hSplit.doLayout();
        hSplit.paint(g2);
        g2.dispose();
    }

    @Test
    public void testMenuPainting() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        Theme.captureDefaults();
        Theme.setTheme(Theme.THEME_DARK);

        JPopupMenu popup = new JPopupMenu();
        JMenuItem item = new JMenuItem("Test Item");
        popup.add(item);
        Theme.applyPopupMenuTheme(popup);
        popup.setSize(150, 40);
        popup.doLayout();

        BufferedImage img = new BufferedImage(150, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        popup.paint(g2);
        g2.dispose();

        int rgb = img.getRGB(20, 20);
        Color c = new Color(rgb);
        assertEquals(new Color(40, 40, 44), c);
        assertEquals(new Color(40, 40, 44), item.getBackground());
        assertEquals(Theme.TEXT_NORMAL_COLOR, item.getForeground());

        // Light mode restoration
        Theme.setTheme(Theme.THEME_LIGHT);
        Theme.applyPopupMenuTheme(popup);
    }

    @Test
    public void testMenuBarTheming() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        Theme.captureDefaults();
        Theme.setTheme(Theme.THEME_DARK);

        NodeBoxMenuBar menuBar = new NodeBoxMenuBar();
        menuBar.setSize(800, 30);
        menuBar.doLayout();

        assertEquals(Theme.MENUBAR_BACKGROUND, menuBar.getBackground());
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu != null) {
                assertEquals(Theme.TEXT_NORMAL_COLOR, menu.getForeground());
                assertEquals(new Color(40, 40, 44), menu.getPopupMenu().getBackground());
            }
        }
    }

    @Test
    public void testPortTypeColorInversionInDarkTheme() {
        Theme.setTheme(Theme.THEME_LIGHT);
        Color lightGeometry = NetworkView.portTypeColor("geometry");
        assertEquals(new Color(20, 20, 20), lightGeometry);

        Color lightInt = NetworkView.portTypeColor(Port.TYPE_INT);
        assertEquals(new Color(116, 119, 121), lightInt);

        Theme.setTheme(Theme.THEME_DARK);
        Color darkGeometry = NetworkView.portTypeColor("geometry");
        assertEquals(new Color(235, 235, 235), darkGeometry);

        Color darkInt = NetworkView.portTypeColor(Port.TYPE_INT);
        assertEquals(new Color(139, 136, 134), darkInt);

        // Verify that dark geometry is bright enough to contrast with dark canvas (24, 24, 27)
        double darkGeoLum = 0.299 * darkGeometry.getRed() + 0.587 * darkGeometry.getGreen() + 0.114 * darkGeometry.getBlue();
        assertTrue("Dark theme geometry color must be bright", darkGeoLum > 200);

        // Switch back to light theme
        Theme.setTheme(Theme.THEME_LIGHT);
        assertEquals(new Color(20, 20, 20), NetworkView.portTypeColor("geometry"));
    }

    @Test
    public void testInvertColorHelper() {
        Color c = new Color(10, 20, 30, 200);
        Color inv = NetworkView.invertColor(c);
        assertEquals(245, inv.getRed());
        assertEquals(235, inv.getGreen());
        assertEquals(225, inv.getBlue());
        assertEquals(200, inv.getAlpha());
        assertNull(NetworkView.invertColor(null));
    }

    @Test
    public void testInvertedImage() {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        // Fully transparent pixel
        img.setRGB(0, 0, 0x00ffffff);
        // Opaque white pixel
        img.setRGB(1, 0, 0xffffffff);
        // Semi-transparent white pixel
        img.setRGB(0, 1, 0x80ffffff);
        // Opaque black pixel
        img.setRGB(1, 1, 0xff000000);

        BufferedImage inv = NetworkView.getInvertedImage(img);
        assertNotNull(inv);
        // Alpha preserved on transparent pixel
        assertEquals(0, (inv.getRGB(0, 0) >> 24) & 0xff);
        // White inverted to black with full opacity
        assertEquals(0xff000000, inv.getRGB(1, 0));
        // Semi-transparent white inverted to semi-transparent black
        assertEquals(0x80, (inv.getRGB(0, 1) >> 24) & 0xff);
        assertEquals(0, inv.getRGB(0, 1) & 0x00ffffff);
        // Black inverted to white with full opacity
        assertEquals(0xffffffff, inv.getRGB(1, 1));
    }

    @Test
    public void testExamplesBrowserThemeUpdate() {
        Theme.setTheme(Theme.THEME_LIGHT);
        ExamplesBrowser browser = new ExamplesBrowser();
        assertNotNull(browser);

        Theme.setTheme(Theme.THEME_DARK);
        browser.updateTheme();

        Theme.setTheme(Theme.THEME_LIGHT);
        browser.updateTheme();

        browser.dispose();
    }

    @Test
    public void testThemeButtonUIPainting() {
        JButton button = new JButton("Test Button");
        button.setSize(100, 30);
        BufferedImage img = new BufferedImage(100, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        // Dark theme tests across states
        Theme.setTheme(Theme.THEME_DARK);
        button.setUI(new ThemeButtonUI());
        button.paint(g2);

        // Rollover
        button.getModel().setRollover(true);
        button.paint(g2);

        // Pressed
        button.getModel().setPressed(true);
        button.getModel().setArmed(true);
        button.paint(g2);

        // Disabled
        button.setEnabled(false);
        button.paint(g2);
        button.setEnabled(true);

        // Light theme tests across states
        Theme.setTheme(Theme.THEME_LIGHT);
        button.paint(g2);
        button.getModel().setRollover(false);
        button.getModel().setPressed(false);
        button.getModel().setArmed(false);
        button.paint(g2);

        g2.dispose();
    }

    @Test
    public void testThemeComboBoxUIPainting() {
        JComboBox<String> box = new JComboBox<String>(new String[]{"Item 1", "Item 2"});
        box.setSize(120, 26);
        BufferedImage img = new BufferedImage(120, 26, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        Theme.setTheme(Theme.THEME_DARK);
        box.setUI(new ThemeComboBoxUI());
        box.paint(g2);

        Theme.setTheme(Theme.THEME_LIGHT);
        box.setUI(new ThemeComboBoxUI());
        box.paint(g2);

        g2.dispose();
    }

    @Test
    public void testThemeScrollBarUIPainting() {
        JScrollBar scrollBar = new JScrollBar(JScrollBar.VERTICAL, 0, 10, 0, 100);
        scrollBar.setSize(16, 200);
        BufferedImage img = new BufferedImage(16, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        Theme.setTheme(Theme.THEME_DARK);
        scrollBar.setUI(new ThemeScrollBarUI());
        scrollBar.paint(g2);

        Theme.setTheme(Theme.THEME_LIGHT);
        scrollBar.setUI(new ThemeScrollBarUI());
        scrollBar.paint(g2);

        g2.dispose();
    }

    @Test
    public void testClientCustomSplitPanePainting() {
        Theme.setTheme(Theme.THEME_DARK);
        nodebox.client.CustomSplitPane vSplit = new nodebox.client.CustomSplitPane(JSplitPane.VERTICAL_SPLIT, new JPanel(), new JPanel());
        vSplit.setSize(400, 300);
        vSplit.doLayout();
        BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        vSplit.paint(g2);

        nodebox.client.CustomSplitPane hSplit = new nodebox.client.CustomSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JPanel(), new JPanel());
        hSplit.setSize(400, 300);
        hSplit.doLayout();
        hSplit.paint(g2);

        // Test Light mode
        Theme.setTheme(Theme.THEME_LIGHT);
        vSplit.updateUI();
        vSplit.paint(g2);
        hSplit.updateUI();
        hSplit.paint(g2);

        g2.dispose();
    }

    @Test
    public void testThemeTableHeaderUIPainting() {
        JTable table = new JTable(2, 2);
        javax.swing.table.JTableHeader header = table.getTableHeader();
        header.setSize(200, 25);
        BufferedImage img = new BufferedImage(200, 25, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        Theme.setTheme(Theme.THEME_DARK);
        header.setUI(new ThemeTableHeaderUI());
        header.paint(g2);

        Theme.setTheme(Theme.THEME_LIGHT);
        header.setUI(new ThemeTableHeaderUI());
        header.paint(g2);

        g2.dispose();
    }

    @Test
    public void testMenuBarThemeSwitching() {
        NodeBoxMenuBar menuBar = new NodeBoxMenuBar();
        menuBar.setSize(600, 25);
        BufferedImage img = new BufferedImage(600, 25, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        // Dark theme: menubar and menus should have light foreground
        Theme.setTheme(Theme.THEME_DARK);
        menuBar.updateTheme();
        menuBar.paint(g2);
        assertTrue(Theme.isDark());
        assertEquals(new Color(228, 228, 231), menuBar.getForeground());
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu != null) {
                assertEquals("Menu foreground in dark theme should be light", new Color(228, 228, 231), menu.getForeground());
            }
        }

        // Light theme: menubar and menus must have dark foreground (NOT white or light gray!)
        Theme.setTheme(Theme.THEME_LIGHT);
        menuBar.updateTheme();
        menuBar.paint(g2);
        assertFalse(Theme.isDark());
        assertEquals(new Color(30, 30, 32), menuBar.getForeground());
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu != null) {
                assertEquals("Menu foreground in light theme should be dark", new Color(30, 30, 32), menu.getForeground());
            }
        }

        g2.dispose();
    }
}
