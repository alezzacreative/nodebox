package nodebox.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.node.UpgradeResult;
import nodebox.ui.Theme;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class UpgradeWarningsDialog extends JDialog {

    public UpgradeWarningsDialog(UpgradeResult upgradeResult) {
        UpgradeResult upgradeResult1 = upgradeResult;
        List<Map<String, String>> warnings = new ArrayList<Map<String, String>>();
        for (String warning : upgradeResult.getWarnings()) {
            warnings.add(ImmutableMap.of("Description", warning));
        }

        boolean dark = Theme.isDark();
        JTable table = new JTable();
        table.setModel(new ListOfMapsModel(warnings));
        if (dark) {
            table.setBackground(new Color(26, 26, 30));
            table.setForeground(new Color(228, 228, 231));
            table.setGridColor(new Color(45, 45, 52));
            table.setSelectionBackground(new Color(63, 63, 70));
            table.setSelectionForeground(Color.WHITE);
        } else {
            table.setBackground(Color.WHITE);
            table.setForeground(new Color(30, 30, 32));
            table.setGridColor(new Color(220, 220, 225));
            table.setSelectionBackground(new Color(210, 225, 245));
            table.setSelectionForeground(Color.BLACK);
        }
        if (table.getTableHeader() != null) {
            table.getTableHeader().setDefaultRenderer(new nodebox.ui.ThemeTableHeaderUI.ThemeTableHeaderRenderer());
            table.getTableHeader().setBackground(dark ? new Color(36, 36, 42) : new Color(236, 236, 240));
            table.getTableHeader().setForeground(dark ? new Color(228, 228, 231) : new Color(30, 30, 32));
        }
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(null);
        tableScroll.getViewport().setBackground(dark ? new Color(26, 26, 30) : Color.WHITE);

        JLabel warningLabel = new JLabel("Some warnings occurred while upgrading your document:");
        warningLabel.setFont(Theme.SMALL_BOLD_FONT);
        warningLabel.setForeground(Theme.TEXT_NORMAL_COLOR);
        JPanel warningPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        warningPanel.add(warningLabel);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
            }
        });
        closeButton.setUI(new nodebox.ui.ThemeButtonUI());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttonPanel.add(closeButton);

        JPanel dialogPanel = new JPanel(new BorderLayout());
        Color bg = dark ? Theme.PANEL_BACKGROUND : Theme.DIALOG_BACKGROUND;
        dialogPanel.setBackground(bg);
        warningPanel.setBackground(bg);
        buttonPanel.setBackground(bg);
        getContentPane().setBackground(bg);

        dialogPanel.add(warningPanel, BorderLayout.NORTH);
        dialogPanel.add(tableScroll, BorderLayout.CENTER);
        dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(dialogPanel);
        getRootPane().setDefaultButton(closeButton);
        setSize(450, 280);
    }

    private class ListOfMapsModel extends AbstractTableModel {

        private List<Map<String, String>> data;
        private List<String> keys = ImmutableList.of();


        private ListOfMapsModel(List<Map<String, String>> data) {
            setData(data);
        }

        public void setData(List<Map<String, String>> data) {
            checkNotNull(data);
            this.data = data;
            if (data.isEmpty()) {
                keys = ImmutableList.of();
            } else {
                // The ordering of the key is random.
                keys = ImmutableList.copyOf(data.get(0).keySet());
            }
            fireTableDataChanged();
        }

        public int getRowCount() {
            return this.data.size();
        }

        public int getColumnCount() {
            return keys.size();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return keys.get(columnIndex);
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            checkArgument(rowIndex < data.size(), "The row index %s is larger than the number of values.", rowIndex);
            checkArgument(columnIndex < keys.size() + 1, "The column index %s is larger than the number of columns.", columnIndex);
            Map<String, String> row = data.get(rowIndex);
            String key = keys.get(columnIndex);
            return row.get(key);
        }
    }

}
