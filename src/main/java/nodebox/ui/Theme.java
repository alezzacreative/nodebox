package nodebox.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Theme {

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private static String currentTheme = THEME_LIGHT;

    // Default colors
    public static Color DEFAULT_ARROW_COLOR;
    public static Color DEFAULT_SHADOW_COLOR;
    public static Color DEFAULT_SPLIT_COLOR;

    // Panels, Menus & Dialogs
    public static Color PANEL_BACKGROUND;
    public static Color MENUBAR_BACKGROUND;
    public static Color DIALOG_BACKGROUND;

    // Viewer
    public static Color VIEWER_BACKGROUND_COLOR;
    public static Color SELECTED_TAB_BACKGROUND_COLOR;
    public static Color TAB_BACKGROUND_COLOR;

    // Network view
    public static Color NETWORK_BACKGROUND_COLOR;
    public static Color NETWORK_GRID_COLOR;
    public static Color NETWORK_SELECTION_COLOR;
    public static Color NETWORK_SELECTION_BORDER_COLOR;
    public static Color NETWORK_NODE_NAME_COLOR;
    public static Color NETWORK_NODE_NAME_SHADOW_COLOR;
    public static Color CONNECTION_DEFAULT_COLOR;
    public static Color CONNECTION_CONNECTING_COLOR;
    public static Color CONNECTION_ACTION_COLOR;

    // Port view
    public static Color PORT_EXPRESSION_BACKGROUND_COLOR;
    public static Color PORT_LABEL_BACKGROUND;
    public static Color PORT_VALUE_BACKGROUND;
    public static Color PORT_EMPTY_BACKGROUND;
    public static Color PORT_DIVIDER_1;
    public static Color PORT_DIVIDER_2;
    public static Color PORT_DIVIDER_3;
    public static Color DRAGGABLE_NUMBER_HIGHLIGHT_COLOR;

    // Source editor
    public static Color MESSAGES_BACKGROUND_COLOR;
    public static Color EDITOR_SPLITTER_DIVIDER_COLOR;
    public static Color EDITOR_DISABLED_BACKGROUND_COLOR;

    // Node attributes editor
    public static Color NODE_ATTRIBUTES_PARAMETER_LIST_BACKGROUND_COLOR;
    public static Color NODE_ATTRIBUTES_PARAMETER_COLOR;

    // Node selection dialog
    public static Color NODE_SELECTION_BACKGROUND_COLOR;
    public static Color NODE_SELECTION_ACTIVE_BACKGROUND_COLOR;

    // Text
    public static Color TEXT_NORMAL_COLOR;
    public static Color TEXT_ARMED_COLOR;
    public static Color TEXT_SHADOW_COLOR;
    public static Color TEXT_DISABLED_COLOR;
    public static Color TEXT_HEADER_COLOR;
    public static Color TEXT_WARNING_COLOR;

    // Split pane
    public static Color SPLIT_PANE_BACKGROUND;
    public static Color SPLIT_PANE_BORDER;

    // Borders
    public static Color BORDER_COLOR;
    public static Border LINE_BORDER;
    public static Border TOP_BOTTOM_BORDER;
    public static Border TOP_BORDER;
    public static Border BOTTOM_BORDER;
    public static Border PARAMETER_ROW_BORDER;
    public static Border PARAMETER_NOTES_BORDER;
    public static Border INNER_SHADOW_BORDER;
    public static Border EMPTY_BORDER;

    // Fonts
    public static final Font EDITOR_FONT;
    public static final Font MESSAGE_FONT;
    public static final Font NETWORK_FONT;
    public static final Font INFO_FONT;
    public static final Font SMALL_FONT;
    public static final Font SMALL_BOLD_FONT;
    public static final Font SMALL_MONO_FONT;

    public static final int LABEL_WIDTH = 114;

    private static final Map<String, Object> defaultUIMap = new HashMap<String, Object>();
    private static final String[] UI_KEYS = new String[] {
        "Panel.background", "Panel.foreground",
        "Dialog.background", "Dialog.foreground",
        "OptionPane.background", "OptionPane.foreground", "OptionPane.messageForeground",
        "Label.foreground",
        "CheckBox.background", "CheckBox.foreground",
        "RadioButton.background", "RadioButton.foreground",
        "ComboBox.background", "ComboBox.foreground", "ComboBox.selectionBackground", "ComboBox.selectionForeground",
        "TextField.background", "TextField.foreground", "TextField.caretForeground",
        "TextArea.background", "TextArea.foreground", "TextArea.caretForeground",
        "List.background", "List.foreground", "List.selectionBackground", "List.selectionForeground",
        "ScrollPane.background", "Viewport.background",
        "MenuBar.background", "MenuBar.foreground",
        "Menu.background", "Menu.foreground", "Menu.selectionBackground", "Menu.selectionForeground",
        "MenuItem.background", "MenuItem.foreground", "MenuItem.selectionBackground", "MenuItem.selectionForeground",
        "CheckBoxMenuItem.background", "CheckBoxMenuItem.foreground",
        "PopupMenu.background", "PopupMenu.foreground",
        "Separator.background", "Separator.foreground",
        "Button.background", "Button.foreground",
        "Table.background", "Table.foreground", "Table.gridColor", "Table.selectionBackground", "Table.selectionForeground",
        "TableHeader.background", "TableHeader.foreground",
        "ToolTip.background", "ToolTip.foreground", "ToolTip.border",
        "ScrollBar.background", "ScrollBar.foreground", "ScrollBar.track", "ScrollBar.thumb",
        "SplitPane.background"
    };

    private static final String[] UI_CLASS_KEYS = new String[] {
        "MenuBarUI",
        "MenuUI",
        "MenuItemUI",
        "CheckBoxMenuItemUI",
        "RadioButtonMenuItemUI",
        "PopupMenuUI",
        "PopupMenuSeparatorUI",
        "ButtonUI",
        "ComboBoxUI",
        "ScrollBarUI",
        "TableHeaderUI"
    };

    public static void captureDefaults() {
        for (String key : UI_KEYS) {
            defaultUIMap.put(key, UIManager.get(key));
        }
        for (String key : UI_CLASS_KEYS) {
            defaultUIMap.put(key, UIManager.get(key));
        }
    }

    static {
        // Initialize fonts.
        if (Platform.onMac()) {
            EDITOR_FONT = new Font("Monaco", Font.PLAIN, 11);
            MESSAGE_FONT = new Font("Lucida Grande", Font.BOLD, 13);
            NETWORK_FONT = new Font("Lucida Grande", Font.PLAIN, 12);
            INFO_FONT = new Font("Lucida Grande", Font.PLAIN, 11);
            SMALL_FONT = new Font("Lucida Grande", Font.PLAIN, 11);
            SMALL_BOLD_FONT = new Font("Lucida Grande", Font.BOLD, 11);
            SMALL_MONO_FONT = new Font("Monaco", Font.PLAIN, 10);
        } else {
            EDITOR_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 11);
            MESSAGE_FONT = new Font("Verdana", Font.BOLD, 11);
            NETWORK_FONT = new Font("Verdana", Font.PLAIN, 11);
            INFO_FONT = new Font("Verdana", Font.PLAIN, 10);
            SMALL_FONT = new Font("Verdana", Font.PLAIN, 10);
            SMALL_BOLD_FONT = new Font("Verdana", Font.BOLD, 10);
            SMALL_MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 10);
        }

        captureDefaults();
        applyLightPalette();
    }

    public static boolean isDark() {
        return THEME_DARK.equalsIgnoreCase(currentTheme);
    }

    public static String getTheme() {
        return currentTheme;
    }

    public static void setDark(boolean dark) {
        setTheme(dark ? THEME_DARK : THEME_LIGHT);
    }

    public static void setTheme(String themeName) {
        if (THEME_DARK.equalsIgnoreCase(themeName)) {
            currentTheme = THEME_DARK;
            applyDarkPalette();
        } else {
            currentTheme = THEME_LIGHT;
            applyLightPalette();
        }
    }

    private static void applyLightPalette() {
        PANEL_BACKGROUND = new Color(240, 240, 240);
        MENUBAR_BACKGROUND = new Color(245, 245, 245);
        DIALOG_BACKGROUND = new Color(240, 240, 240);

        DEFAULT_ARROW_COLOR = new Color(136, 136, 136);
        DEFAULT_SHADOW_COLOR = new Color(176, 176, 176);
        DEFAULT_SPLIT_COLOR = new Color(210, 210, 215);

        VIEWER_BACKGROUND_COLOR = new Color(232, 232, 232);
        SELECTED_TAB_BACKGROUND_COLOR = new Color(198, 198, 198);
        TAB_BACKGROUND_COLOR = new Color(210, 210, 210);

        NETWORK_BACKGROUND_COLOR = new Color(69, 69, 69);
        NETWORK_GRID_COLOR = new Color(85, 85, 85);
        NETWORK_SELECTION_COLOR = new Color(200, 200, 200, 100);
        NETWORK_SELECTION_BORDER_COLOR = new Color(100, 100, 100, 100);
        NETWORK_NODE_NAME_COLOR = new Color(194, 194, 194);
        NETWORK_NODE_NAME_SHADOW_COLOR = new Color(23, 23, 23);
        CONNECTION_DEFAULT_COLOR = new Color(200, 200, 200);
        CONNECTION_CONNECTING_COLOR = new Color(170, 167, 18);
        CONNECTION_ACTION_COLOR = new Color(0, 116, 168);

        PORT_EXPRESSION_BACKGROUND_COLOR = new Color(255, 255, 240);
        PORT_LABEL_BACKGROUND = new Color(153, 153, 153);
        PORT_VALUE_BACKGROUND = new Color(196, 196, 196);
        PORT_EMPTY_BACKGROUND = new Color(196, 196, 196);
        PORT_DIVIDER_1 = new Color(146, 146, 146);
        PORT_DIVIDER_2 = new Color(133, 133, 133);
        PORT_DIVIDER_3 = new Color(112, 112, 112);
        DRAGGABLE_NUMBER_HIGHLIGHT_COLOR = new Color(223, 223, 223);

        MESSAGES_BACKGROUND_COLOR = new Color(240, 240, 240);
        EDITOR_SPLITTER_DIVIDER_COLOR = new Color(210, 210, 210);
        EDITOR_DISABLED_BACKGROUND_COLOR = new Color(240, 240, 240);

        NODE_ATTRIBUTES_PARAMETER_LIST_BACKGROUND_COLOR = new Color(240, 240, 250);
        NODE_ATTRIBUTES_PARAMETER_COLOR = new Color(60, 60, 60);

        NODE_SELECTION_BACKGROUND_COLOR = new Color(244, 244, 244);
        NODE_SELECTION_ACTIVE_BACKGROUND_COLOR = new Color(224, 224, 224);

        TEXT_NORMAL_COLOR = new Color(60, 60, 60);
        TEXT_ARMED_COLOR = new Color(0, 0, 0);
        TEXT_SHADOW_COLOR = new Color(255, 255, 255);
        TEXT_DISABLED_COLOR = new Color(98, 112, 130);
        TEXT_HEADER_COLOR = new Color(93, 93, 93);
        TEXT_WARNING_COLOR = new Color(200, 0, 0);

        SPLIT_PANE_BACKGROUND = new Color(235, 235, 238);
        SPLIT_PANE_BORDER = new Color(215, 215, 220);
        BORDER_COLOR = new Color(215, 215, 220);
        LINE_BORDER = BorderFactory.createLineBorder(BORDER_COLOR);
        Color topColor = new Color(224, 224, 224);
        Color bottomColor = new Color(245, 245, 245);
        TOP_BOTTOM_BORDER = new TopBottomBorder(topColor, bottomColor);
        TOP_BORDER = new TopBorder(new Color(168, 168, 168));
        Color whiteColor = new Color(255, 255, 255);
        BOTTOM_BORDER = new BottomBorder(whiteColor);
        PARAMETER_ROW_BORDER = new RowBorder();
        PARAMETER_NOTES_BORDER = new NotesBorder();
        INNER_SHADOW_BORDER = new InnerShadowBorder();
        EMPTY_BORDER = BorderFactory.createEmptyBorder(0, 0, 0, 0);

        Color lightPanelBg = new Color(240, 240, 240);
        Color lightText = new Color(30, 30, 32);
        Color lightSelectionBg = new Color(210, 225, 245);
        Color lightBorder = new Color(215, 215, 220);

        UIManager.put("Panel.background", lightPanelBg);
        UIManager.put("Panel.foreground", lightText);
        UIManager.put("Dialog.background", DIALOG_BACKGROUND);
        UIManager.put("Dialog.foreground", lightText);
        UIManager.put("OptionPane.background", lightPanelBg);
        UIManager.put("OptionPane.foreground", lightText);
        UIManager.put("OptionPane.messageForeground", lightText);
        UIManager.put("Label.foreground", lightText);
        UIManager.put("CheckBox.background", lightPanelBg);
        UIManager.put("CheckBox.foreground", lightText);
        UIManager.put("RadioButton.background", lightPanelBg);
        UIManager.put("RadioButton.foreground", lightText);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.foreground", lightText);
        UIManager.put("ComboBox.selectionBackground", lightSelectionBg);
        UIManager.put("ComboBox.selectionForeground", Color.BLACK);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", lightText);
        UIManager.put("TextField.caretForeground", Color.BLACK);
        UIManager.put("TextArea.background", Color.WHITE);
        UIManager.put("TextArea.foreground", lightText);
        UIManager.put("TextArea.caretForeground", Color.BLACK);
        UIManager.put("List.background", Color.WHITE);
        UIManager.put("List.foreground", lightText);
        UIManager.put("List.selectionBackground", lightSelectionBg);
        UIManager.put("List.selectionForeground", Color.BLACK);
        UIManager.put("ScrollPane.background", lightPanelBg);
        UIManager.put("Viewport.background", lightPanelBg);

        if (!Platform.onMac()) {
            UIManager.put("MenuBarUI", "javax.swing.plaf.basic.BasicMenuBarUI");
            UIManager.put("MenuUI", "javax.swing.plaf.basic.BasicMenuUI");
            UIManager.put("MenuItemUI", "javax.swing.plaf.basic.BasicMenuItemUI");
            UIManager.put("CheckBoxMenuItemUI", "javax.swing.plaf.basic.BasicCheckBoxMenuItemUI");
            UIManager.put("RadioButtonMenuItemUI", "javax.swing.plaf.basic.BasicRadioButtonMenuItemUI");
            UIManager.put("PopupMenuUI", "javax.swing.plaf.basic.BasicPopupMenuUI");
            UIManager.put("PopupMenuSeparatorUI", "javax.swing.plaf.basic.BasicPopupMenuSeparatorUI");
        }

        UIManager.put("MenuBar.background", MENUBAR_BACKGROUND);
        UIManager.put("MenuBar.foreground", lightText);

        UIManager.put("Menu.background", MENUBAR_BACKGROUND);
        UIManager.put("Menu.foreground", lightText);
        UIManager.put("Menu.selectionBackground", lightSelectionBg);
        UIManager.put("Menu.selectionForeground", Color.BLACK);
        UIManager.put("Menu.acceleratorForeground", new Color(110, 110, 115));
        UIManager.put("Menu.acceleratorSelectionForeground", Color.BLACK);
        UIManager.put("Menu.border", BorderFactory.createEmptyBorder(4, 10, 4, 10));

        UIManager.put("MenuItem.background", Color.WHITE);
        UIManager.put("MenuItem.foreground", lightText);
        UIManager.put("MenuItem.selectionBackground", new Color(37, 99, 235));
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.acceleratorForeground", new Color(110, 110, 115));
        UIManager.put("MenuItem.acceleratorSelectionForeground", Color.WHITE);
        UIManager.put("MenuItem.border", BorderFactory.createEmptyBorder(4, 10, 4, 10));

        UIManager.put("CheckBoxMenuItem.background", Color.WHITE);
        UIManager.put("CheckBoxMenuItem.foreground", lightText);
        UIManager.put("CheckBoxMenuItem.selectionBackground", new Color(37, 99, 235));
        UIManager.put("CheckBoxMenuItem.selectionForeground", Color.WHITE);
        UIManager.put("CheckBoxMenuItem.acceleratorForeground", new Color(110, 110, 115));
        UIManager.put("CheckBoxMenuItem.acceleratorSelectionForeground", Color.WHITE);
        UIManager.put("CheckBoxMenuItem.border", BorderFactory.createEmptyBorder(4, 10, 4, 10));

        UIManager.put("PopupMenu.background", Color.WHITE);
        UIManager.put("PopupMenu.foreground", lightText);
        UIManager.put("PopupMenu.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(lightBorder),
                BorderFactory.createEmptyBorder(4, 0, 4, 0)));

        UIManager.put("Separator.background", lightBorder);
        UIManager.put("Separator.foreground", lightPanelBg);
        UIManager.put("Button.background", lightPanelBg);
        UIManager.put("Button.foreground", lightText);

        UIManager.put("ButtonUI", "nodebox.ui.ThemeButtonUI");
        UIManager.put("ComboBoxUI", "nodebox.ui.ThemeComboBoxUI");
        UIManager.put("ScrollBarUI", "nodebox.ui.ThemeScrollBarUI");
        UIManager.put("TableHeaderUI", "nodebox.ui.ThemeTableHeaderUI");

        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.foreground", lightText);
        UIManager.put("Table.gridColor", new Color(220, 220, 225));
        UIManager.put("Table.selectionBackground", lightSelectionBg);
        UIManager.put("Table.selectionForeground", Color.BLACK);
        UIManager.put("TableHeader.background", new Color(236, 236, 240));
        UIManager.put("TableHeader.foreground", lightText);
        UIManager.put("ToolTip.background", Color.WHITE);
        UIManager.put("ToolTip.foreground", Color.BLACK);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(new Color(180, 180, 186)));
        UIManager.put("ScrollBar.background", new Color(240, 240, 242));
        UIManager.put("ScrollBar.track", new Color(240, 240, 242));
        UIManager.put("ScrollBar.thumb", new Color(195, 195, 202));
        UIManager.put("SplitPane.background", SPLIT_PANE_BACKGROUND);
    }

    private static void applyDarkPalette() {
        PANEL_BACKGROUND = new Color(34, 34, 38);
        MENUBAR_BACKGROUND = new Color(34, 34, 38);
        DIALOG_BACKGROUND = new Color(40, 40, 44);

        DEFAULT_ARROW_COLOR = new Color(160, 160, 160);
        DEFAULT_SHADOW_COLOR = new Color(0, 0, 0, 160);
        DEFAULT_SPLIT_COLOR = new Color(48, 48, 52);

        VIEWER_BACKGROUND_COLOR = new Color(30, 30, 34);
        SELECTED_TAB_BACKGROUND_COLOR = new Color(63, 63, 70); // Zinc 700
        TAB_BACKGROUND_COLOR = new Color(39, 39, 42); // Zinc 800

        NETWORK_BACKGROUND_COLOR = new Color(24, 24, 27); // Zinc 900
        NETWORK_GRID_COLOR = new Color(39, 39, 42); // Zinc 800
        NETWORK_SELECTION_COLOR = new Color(161, 161, 170, 60);
        NETWORK_SELECTION_BORDER_COLOR = new Color(212, 212, 216, 120);
        NETWORK_NODE_NAME_COLOR = new Color(228, 228, 231); // Zinc 200
        NETWORK_NODE_NAME_SHADOW_COLOR = new Color(0, 0, 0);
        CONNECTION_DEFAULT_COLOR = new Color(161, 161, 170); // Zinc 400
        CONNECTION_CONNECTING_COLOR = new Color(234, 179, 8); // Yellow 500
        CONNECTION_ACTION_COLOR = new Color(56, 189, 248); // Sky 400

        PORT_EXPRESSION_BACKGROUND_COLOR = new Color(40, 40, 48);
        PORT_LABEL_BACKGROUND = new Color(35, 35, 39);
        PORT_VALUE_BACKGROUND = new Color(26, 26, 29);
        PORT_EMPTY_BACKGROUND = new Color(26, 26, 29);
        PORT_DIVIDER_1 = new Color(30, 30, 34);
        PORT_DIVIDER_2 = new Color(20, 20, 23);
        PORT_DIVIDER_3 = new Color(45, 45, 50);
        DRAGGABLE_NUMBER_HIGHLIGHT_COLOR = new Color(0, 0, 0, 120);

        MESSAGES_BACKGROUND_COLOR = new Color(24, 24, 27);
        EDITOR_SPLITTER_DIVIDER_COLOR = new Color(48, 48, 52);
        EDITOR_DISABLED_BACKGROUND_COLOR = new Color(32, 32, 36);

        NODE_ATTRIBUTES_PARAMETER_LIST_BACKGROUND_COLOR = new Color(30, 30, 34);
        NODE_ATTRIBUTES_PARAMETER_COLOR = new Color(63, 63, 70);

        NODE_SELECTION_BACKGROUND_COLOR = new Color(32, 32, 36);
        NODE_SELECTION_ACTIVE_BACKGROUND_COLOR = new Color(55, 55, 62);

        TEXT_NORMAL_COLOR = new Color(228, 228, 231); // Zinc 200
        TEXT_ARMED_COLOR = new Color(255, 255, 255);
        TEXT_SHADOW_COLOR = new Color(0, 0, 0, 160);
        TEXT_DISABLED_COLOR = new Color(113, 113, 122); // Zinc 500
        TEXT_HEADER_COLOR = new Color(161, 161, 170); // Zinc 400
        TEXT_WARNING_COLOR = new Color(248, 113, 113); // Red 400

        SPLIT_PANE_BACKGROUND = new Color(34, 34, 38);
        SPLIT_PANE_BORDER = new Color(24, 24, 27);

        BORDER_COLOR = new Color(55, 55, 60);
        LINE_BORDER = BorderFactory.createLineBorder(BORDER_COLOR);
        Color topColor = new Color(50, 50, 55);
        Color bottomColor = new Color(24, 24, 27);
        TOP_BOTTOM_BORDER = new TopBottomBorder(topColor, bottomColor);
        TOP_BORDER = new TopBorder(new Color(45, 45, 50));
        Color darkBottom = new Color(28, 28, 32);
        BOTTOM_BORDER = new BottomBorder(darkBottom);
        PARAMETER_ROW_BORDER = new RowBorder();
        PARAMETER_NOTES_BORDER = new NotesBorder();
        INNER_SHADOW_BORDER = new InnerShadowBorder();
        EMPTY_BORDER = BorderFactory.createEmptyBorder(0, 0, 0, 0);

        Color darkPanelBg = new Color(40, 40, 44);
        Color darkControlBg = new Color(48, 48, 54);
        Color darkText = new Color(228, 228, 231);
        Color darkInputBg = new Color(26, 26, 30);
        Color darkBorder = new Color(60, 60, 66);
        Color darkSelectionBg = new Color(63, 63, 70);

        UIManager.put("Panel.background", darkPanelBg);
        UIManager.put("Panel.foreground", darkText);
        UIManager.put("Dialog.background", darkPanelBg);
        UIManager.put("Dialog.foreground", darkText);
        UIManager.put("OptionPane.background", darkPanelBg);
        UIManager.put("OptionPane.foreground", darkText);
        UIManager.put("OptionPane.messageForeground", darkText);
        UIManager.put("Label.foreground", darkText);
        UIManager.put("CheckBox.background", darkPanelBg);
        UIManager.put("CheckBox.foreground", darkText);
        UIManager.put("RadioButton.background", darkPanelBg);
        UIManager.put("RadioButton.foreground", darkText);
        UIManager.put("ComboBox.background", darkInputBg);
        UIManager.put("ComboBox.foreground", darkText);
        UIManager.put("ComboBox.selectionBackground", darkSelectionBg);
        UIManager.put("ComboBox.selectionForeground", darkText);
        UIManager.put("TextField.background", darkInputBg);
        UIManager.put("TextField.foreground", darkText);
        UIManager.put("TextField.caretForeground", darkText);
        UIManager.put("TextArea.background", darkInputBg);
        UIManager.put("TextArea.foreground", darkText);
        UIManager.put("TextArea.caretForeground", darkText);
        UIManager.put("List.background", darkPanelBg);
        UIManager.put("List.foreground", darkText);
        UIManager.put("List.selectionBackground", darkSelectionBg);
        UIManager.put("List.selectionForeground", darkText);
        UIManager.put("ScrollPane.background", darkPanelBg);
        UIManager.put("Viewport.background", darkPanelBg);

        if (!Platform.onMac()) {
            UIManager.put("MenuBarUI", "javax.swing.plaf.basic.BasicMenuBarUI");
            UIManager.put("MenuUI", "javax.swing.plaf.basic.BasicMenuUI");
            UIManager.put("MenuItemUI", "javax.swing.plaf.basic.BasicMenuItemUI");
            UIManager.put("CheckBoxMenuItemUI", "javax.swing.plaf.basic.BasicCheckBoxMenuItemUI");
            UIManager.put("RadioButtonMenuItemUI", "javax.swing.plaf.basic.BasicRadioButtonMenuItemUI");
            UIManager.put("PopupMenuUI", "javax.swing.plaf.basic.BasicPopupMenuUI");
            UIManager.put("PopupMenuSeparatorUI", "javax.swing.plaf.basic.BasicPopupMenuSeparatorUI");
        }

        UIManager.put("MenuBar.background", new Color(34, 34, 38));
        UIManager.put("MenuBar.foreground", darkText);

        UIManager.put("Menu.background", new Color(34, 34, 38));
        UIManager.put("Menu.foreground", darkText);
        UIManager.put("Menu.selectionBackground", darkSelectionBg);
        UIManager.put("Menu.selectionForeground", Color.WHITE);
        UIManager.put("Menu.acceleratorForeground", new Color(161, 161, 170));
        UIManager.put("Menu.acceleratorSelectionForeground", Color.WHITE);
        UIManager.put("Menu.border", BorderFactory.createEmptyBorder(4, 10, 4, 10));

        UIManager.put("MenuItem.background", new Color(40, 40, 44));
        UIManager.put("MenuItem.foreground", darkText);
        UIManager.put("MenuItem.selectionBackground", darkSelectionBg);
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.acceleratorForeground", new Color(161, 161, 170));
        UIManager.put("MenuItem.acceleratorSelectionForeground", Color.WHITE);
        UIManager.put("MenuItem.border", BorderFactory.createEmptyBorder(4, 10, 4, 10));

        UIManager.put("CheckBoxMenuItem.background", new Color(40, 40, 44));
        UIManager.put("CheckBoxMenuItem.foreground", darkText);
        UIManager.put("CheckBoxMenuItem.selectionBackground", darkSelectionBg);
        UIManager.put("CheckBoxMenuItem.selectionForeground", Color.WHITE);
        UIManager.put("CheckBoxMenuItem.acceleratorForeground", new Color(161, 161, 170));
        UIManager.put("CheckBoxMenuItem.acceleratorSelectionForeground", Color.WHITE);
        UIManager.put("CheckBoxMenuItem.border", BorderFactory.createEmptyBorder(4, 10, 4, 10));

        UIManager.put("PopupMenu.background", new Color(40, 40, 44));
        UIManager.put("PopupMenu.foreground", darkText);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(darkBorder));

        UIManager.put("Separator.background", darkBorder);
        UIManager.put("Separator.foreground", darkPanelBg);
        UIManager.put("Button.background", darkControlBg);
        UIManager.put("Button.foreground", darkText);

        UIManager.put("ButtonUI", "nodebox.ui.ThemeButtonUI");
        UIManager.put("ComboBoxUI", "nodebox.ui.ThemeComboBoxUI");
        UIManager.put("ScrollBarUI", "nodebox.ui.ThemeScrollBarUI");
        UIManager.put("TableHeaderUI", "nodebox.ui.ThemeTableHeaderUI");

        UIManager.put("Table.background", new Color(26, 26, 30));
        UIManager.put("Table.foreground", darkText);
        UIManager.put("Table.gridColor", new Color(45, 45, 52));
        UIManager.put("Table.selectionBackground", darkSelectionBg);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("TableHeader.background", new Color(36, 36, 42));
        UIManager.put("TableHeader.foreground", darkText);
        UIManager.put("ToolTip.background", new Color(34, 34, 38));
        UIManager.put("ToolTip.foreground", darkText);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(darkBorder));
        UIManager.put("ScrollBar.background", new Color(34, 34, 38));
        UIManager.put("ScrollBar.track", new Color(34, 34, 38));
        UIManager.put("ScrollBar.thumb", new Color(70, 70, 78));
        UIManager.put("SplitPane.background", SPLIT_PANE_BACKGROUND);
    }

    public static void applyPopupMenuTheme(JPopupMenu popup) {
        if (popup == null) return;
        boolean dark = isDark();
        if (!Platform.onMac()) {
            popup.setUI(new javax.swing.plaf.basic.BasicPopupMenuUI());
        }
        if (dark) {
            popup.setBackground(new Color(40, 40, 44));
            popup.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(60, 60, 66)),
                    BorderFactory.createEmptyBorder(4, 0, 4, 0)));
        } else {
            popup.setBackground(Color.WHITE);
            popup.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(210, 210, 215)),
                    BorderFactory.createEmptyBorder(4, 0, 4, 0)));
        }
        popup.setOpaque(true);
        for (Component c : popup.getComponents()) {
            if (c instanceof JMenuItem) {
                applyMenuItemTheme((JMenuItem) c, dark);
            } else if (c instanceof JSeparator) {
                if (dark) {
                    c.setBackground(new Color(60, 60, 66));
                    c.setForeground(new Color(40, 40, 44));
                } else {
                    c.setBackground(new Color(225, 225, 230));
                    c.setForeground(new Color(245, 245, 245));
                }
            }
        }
    }

    public static void applyMenuItemTheme(JMenuItem item, boolean dark) {
        if (item == null) return;
        if (!Platform.onMac()) {
            if (item instanceof JCheckBoxMenuItem) {
                item.setUI(new javax.swing.plaf.basic.BasicCheckBoxMenuItemUI());
            } else if (item instanceof JRadioButtonMenuItem) {
                item.setUI(new javax.swing.plaf.basic.BasicRadioButtonMenuItemUI());
            } else if (item instanceof JMenu) {
                item.setUI(new javax.swing.plaf.basic.BasicMenuUI());
            } else {
                item.setUI(new javax.swing.plaf.basic.BasicMenuItemUI());
            }
        }
        if (dark) {
            item.setBackground(new Color(40, 40, 44));
            item.setForeground(new Color(228, 228, 231));
            item.setOpaque(true);
            item.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        } else {
            item.setBackground(Color.WHITE);
            item.setForeground(new Color(30, 30, 32));
            item.setOpaque(true);
            item.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        }
        if (item instanceof JMenu) {
            JMenu menu = (JMenu) item;
            menu.setOpaque(false);
            menu.setForeground(dark ? new Color(228, 228, 231) : new Color(30, 30, 32));
            applyPopupMenuTheme(menu.getPopupMenu());
        }
    }

    public static void paintHeaderBackground(Graphics2D g2, int x, int y, int width, int height) {
        if (isDark()) {
            GradientPaint gp = new GradientPaint(x, y, new Color(45, 45, 49), x, y + height, new Color(34, 34, 38));
            g2.setPaint(gp);
            g2.fillRect(x, y, width, height);
            g2.setColor(new Color(60, 60, 66));
            g2.drawLine(x, y, x + width, y);
            g2.setColor(new Color(24, 24, 27));
            g2.drawLine(x, y + height - 1, x + width, y + height - 1);
        } else {
            GradientPaint gp = new GradientPaint(x, y, new Color(245, 245, 248), x, y + height, new Color(228, 228, 232));
            g2.setPaint(gp);
            g2.fillRect(x, y, width, height);
            g2.setColor(new Color(255, 255, 255));
            g2.drawLine(x, y, x + width, y);
            g2.setColor(new Color(210, 210, 215));
            g2.drawLine(x, y + height - 1, x + width, y + height - 1);
        }
    }

    public static void paintAnimationBackground(Graphics2D g2, int x, int y, int width, int height) {
        if (isDark()) {
            GradientPaint gp = new GradientPaint(x, y, new Color(36, 36, 40), x, y + height, new Color(28, 28, 31));
            g2.setPaint(gp);
            g2.fillRect(x, y, width, height);
            g2.setColor(new Color(50, 50, 55));
            g2.drawLine(x, y, x + width, y);
        } else {
            GradientPaint gp = new GradientPaint(x, y, new Color(245, 245, 248), x, y + height, new Color(230, 230, 235));
            g2.setPaint(gp);
            g2.fillRect(x, y, width, height);
            g2.setColor(new Color(215, 215, 220));
            g2.drawLine(x, y, x + width, y);
        }
    }

    public static void paintAddressBackground(Graphics2D g2, int x, int y, int width, int height) {
        if (isDark()) {
            GradientPaint gp = new GradientPaint(x, y, new Color(42, 42, 46), x, y + height, new Color(32, 32, 35));
            g2.setPaint(gp);
            g2.fillRect(x, y, width, height);
            g2.setColor(new Color(55, 55, 60));
            g2.drawLine(x, y + height - 1, x + width, y + height - 1);
        } else {
            GradientPaint gp = new GradientPaint(x, y, new Color(248, 248, 250), x, y + height, new Color(236, 236, 240));
            g2.setPaint(gp);
            g2.fillRect(x, y, width, height);
            g2.setColor(new Color(215, 215, 220));
            g2.drawLine(x, y + height - 1, x + width, y + height - 1);
        }
    }

    public static void paintDivider(Graphics2D g2, int x, int y, int height) {
        if (isDark()) {
            g2.setColor(new Color(24, 24, 27));
            g2.drawLine(x, y + 2, x, y + height - 3);
            g2.setColor(new Color(55, 55, 60));
            g2.drawLine(x + 1, y + 2, x + 1, y + height - 3);
        } else {
            g2.setColor(new Color(195, 195, 200));
            g2.drawLine(x, y + 2, x, y + height - 3);
            g2.setColor(new Color(255, 255, 255));
            g2.drawLine(x + 1, y + 2, x + 1, y + height - 3);
        }
    }

    public static void paintPaneMenu(Graphics2D g2, int x, int y, int width, int height) {
        if (isDark()) {
            g2.setColor(new Color(48, 48, 54));
            g2.fillRoundRect(x, y + 1, width, height - 2, 4, 4);
            g2.setColor(new Color(65, 65, 72));
            g2.drawRoundRect(x, y + 1, width - 1, height - 3, 4, 4);
        } else {
            g2.setColor(new Color(230, 230, 235));
            g2.fillRoundRect(x, y + 1, width, height - 2, 4, 4);
            g2.setColor(new Color(190, 190, 195));
            g2.drawRoundRect(x, y + 1, width - 1, height - 3, 4, 4);
        }
    }

    public static void paintDraggableNumber(Graphics2D g2, int width, int height, boolean enabled) {
        int w = width - 1;
        int h = height - 1;
        boolean dark = isDark();
        g2.setColor(dark ? (enabled ? new Color(45, 45, 50) : new Color(35, 35, 39))
                         : (enabled ? new Color(245, 245, 248) : new Color(232, 232, 236)));
        g2.fillRoundRect(0, 0, w, h, 6, 6);
        g2.setColor(dark ? new Color(65, 65, 72) : new Color(190, 190, 195));
        g2.drawRoundRect(0, 0, w, h, 6, 6);
        // Subtle arrow indicators on sides
        g2.setColor(dark ? (enabled ? new Color(160, 160, 165) : new Color(80, 80, 85))
                         : (enabled ? new Color(100, 100, 105) : new Color(180, 180, 185)));
        g2.drawLine(7, h / 2, 9, h / 2 - 3);
        g2.drawLine(7, h / 2, 9, h / 2 + 3);
        g2.drawLine(w - 7, h / 2, w - 9, h / 2 - 3);
        g2.drawLine(w - 7, h / 2, w - 9, h / 2 + 3);
    }

    public static class ArrowIcon implements Icon {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Theme.DEFAULT_ARROW_COLOR);
            g.drawLine(x + 1, y, x + 1, y);
            g.drawLine(x + 1, y + 1, x + 2, y + 1);
            g.drawLine(x + 1, y + 2, x + 3, y + 2);
            g.drawLine(x + 1, y + 3, x + 4, y + 3);
            g.drawLine(x + 1, y + 4, x + 3, y + 4);
            g.drawLine(x + 1, y + 5, x + 2, y + 5);
            g.drawLine(x + 1, y + 6, x + 1, y + 6);
        }

        public int getIconWidth() {
            return 6;
        }

        public int getIconHeight() {
            return 8;
        }
    }

    public static class TopBottomBorder implements Border {
        private Color topColor;
        private Color bottomColor;

        public TopBottomBorder(Color topColor, Color bottomColor) {
            this.topColor = topColor;
            this.bottomColor = bottomColor;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (isDark()) {
                g.setColor(new Color(50, 50, 55));
                g.drawLine(x, y, x + width, y);
                g.setColor(new Color(24, 24, 27));
                g.drawLine(x, y + height - 1, x + width, y + height - 1);
            } else {
                g.setColor(topColor);
                g.drawLine(x, y, x + width, y);
                g.setColor(bottomColor);
                g.drawLine(x, y + height - 1, x + width, y + height - 1);
            }
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(1, 0, 1, 0);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    public static class TopBorder implements Border {
        private Color topColor;

        public TopBorder(Color topColor) {
            this.topColor = topColor;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (isDark()) {
                g.setColor(new Color(45, 45, 50));
            } else {
                g.setColor(topColor);
            }
            g.drawLine(x, y, x + width, y);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(1, 0, 0, 0);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    public static class BottomBorder implements Border {
        private Color bottomColor;

        public BottomBorder(Color bottomColor) {
            this.bottomColor = bottomColor;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (isDark()) {
                g.setColor(new Color(28, 28, 32));
            } else {
                g.setColor(bottomColor);
            }
            g.drawLine(x, y + height - 1, x + width, y + height - 1);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(0, 0, 1, 0);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    public static class RowBorder implements Border {

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Color labelUp = isDark() ? new Color(28, 28, 31) : new Color(140, 140, 140);
            Color labelDown = isDark() ? new Color(42, 42, 46) : new Color(166, 166, 166);
            Color paramUp = isDark() ? new Color(22, 22, 25) : new Color(179, 179, 179);
            Color paramDown = isDark() ? new Color(38, 38, 42) : new Color(213, 213, 213);

            // Draw border on the side of the label
            g.setColor(labelUp);
            g.fillRect(x, y + height - 2, LABEL_WIDTH - 2, 1);
            g.setColor(labelDown);
            g.fillRect(x, y + height - 1, LABEL_WIDTH - 2, 1);
            // Draw border on port side
            g.setColor(paramUp);
            g.fillRect(x + LABEL_WIDTH + 1, y + height - 2, width - LABEL_WIDTH - 1, 1);
            g.setColor(paramDown);
            g.fillRect(x + LABEL_WIDTH + 1, y + height - 1, width - LABEL_WIDTH - 1, 1);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(4, 0, 4, 0);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    public static class NotesBorder implements Border {

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Color labelUp = isDark() ? new Color(28, 28, 31) : new Color(140, 140, 140);
            Color labelDown = isDark() ? new Color(42, 42, 46) : new Color(166, 166, 166);
            Color paramUp = isDark() ? new Color(120, 124, 35) : new Color(150, 154, 43);
            Color paramDown = isDark() ? new Color(38, 38, 42) : new Color(213, 213, 213);

            // Draw border on the side of the label
            g.setColor(labelUp);
            g.fillRect(x, y + height - 2, LABEL_WIDTH - 2, 1);
            g.setColor(labelDown);
            g.fillRect(x, y + height - 1, LABEL_WIDTH - 2, 1);
            // Draw border on port side
            g.setColor(paramUp);
            g.fillRect(x + LABEL_WIDTH, y + height - 2, width - LABEL_WIDTH, 1);
            g.setColor(paramDown);
            g.fillRect(x + LABEL_WIDTH + 1, y + height - 1, width - LABEL_WIDTH - 1, 1);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(4, 0, 4, 0);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    public static class InsetsBorder implements Border {
        private Insets insets;

        public InsetsBorder(Insets insets) {
            this.insets = insets;
        }

        public InsetsBorder(int x, int y, int width, int height) {
            this.insets = new Insets(x, y, width, height);
        }

        public void paintBorder(Component component, Graphics graphics, int i, int i1, int i2, int i3) {
        }

        public Insets getBorderInsets(Component component) {
            return insets;
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    public static class InnerShadowBorder implements Border {

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Color edge = isDark() ? new Color(35, 35, 39) : new Color(166, 166, 166);
            Color highlight = isDark() ? new Color(55, 55, 62) : new Color(237, 237, 237);
            Color shadow = isDark() ? new Color(20, 20, 23) : new Color(119, 119, 119);

            g.setColor(edge);
            g.drawRect(x + 1, y + 1, width - 3, height - 3);
            g.setColor(shadow);
            g.drawLine(x, y, x + width - 1, y);
            g.drawLine(x, y, x, y + height - 1);
            g.setColor(highlight);
            g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
            g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(2, 2, 2, 2);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

}
