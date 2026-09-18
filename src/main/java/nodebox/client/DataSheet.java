package nodebox.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.ui.Theme;
import nodebox.util.IOrderedFields;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The Data Sheet presents data in a spreadsheet view.
 */
public class DataSheet extends JPanel implements OutputView {

    private final DataTableModel tableModel;
    private final JTable table;

    public DataSheet() {
        super(new BorderLayout());
        table = new DataTable();
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.addColumn(new TableColumn(0));
        updateTheme();
        tableModel = new DataTableModel();
        table.setModel(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        add(tableScroll, BorderLayout.CENTER);
    }

    public void updateTheme() {
        boolean dark = Theme.isDark();
        if (dark) {
            table.setBackground(new Color(24, 24, 27));
            table.setForeground(new Color(228, 228, 231));
            table.setGridColor(new Color(45, 45, 50));
            if (table.getTableHeader() != null) {
                table.getTableHeader().setDefaultRenderer(new nodebox.ui.ThemeTableHeaderUI.ThemeTableHeaderRenderer());
                table.getTableHeader().setBackground(new Color(36, 36, 42));
                table.getTableHeader().setForeground(new Color(228, 228, 231));
            }
        } else {
            table.setBackground(Color.WHITE);
            table.setForeground(new Color(30, 30, 32));
            table.setGridColor(new Color(220, 220, 225));
            if (table.getTableHeader() != null) {
                table.getTableHeader().setDefaultRenderer(new nodebox.ui.ThemeTableHeaderUI.ThemeTableHeaderRenderer());
                table.getTableHeader().setBackground(new Color(236, 236, 240));
                table.getTableHeader().setForeground(new Color(30, 30, 32));
            }
        }
        table.repaint();
        repaint();
    }

    public void setOutputValues(List<?> objects) {
        tableModel.setOutputValues(objects);
        table.setModel(tableModel);
    }

    // Optimization techniques based on "Christmas Tree" article:
    // http://java.sun.com/products/jfc/tsc/articles/ChristmasTree/
    private final class DataTable extends JTable {

        private final DataCellRenderer cellRenderer = new DataCellRenderer();

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            // Always return the same object for the whole table.
            return cellRenderer;
        }

    }

    private final class DataCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
            setValue(value);
            Color zebra = Theme.isDark() ? new Color(34, 34, 38) : UIManager.getColor("Table.alternateRowColor");
            if (isSelected) {
                setBackground(Theme.isDark() ? new Color(63, 63, 70) : table.getSelectionBackground());
                setForeground(Theme.isDark() ? Color.WHITE : table.getSelectionForeground());
            } else if (rowIndex % 2 == 0) {
                setBackground(Theme.isDark() ? new Color(24, 24, 27) : null);
                setForeground(Theme.isDark() ? new Color(228, 228, 231) : Color.BLACK);
            } else {
                setBackground(zebra);
                setForeground(Theme.isDark() ? new Color(228, 228, 231) : Color.BLACK);
            }
            return this;
        }

        @Override
        public void paint(Graphics g) {
            ui.update(g, this);
        }

        @Override
        public boolean isOpaque() {
            return getBackground() != null;
        }

        @Override
        public void invalidate() {
            // This is generally ok for non-Composite components (like Labels)
        }

        @Override
        public void repaint() {
            // Can be ignored, we don't exist in the containment hierarchy.
        }
    }

    private final class DataTableModel extends AbstractTableModel {

        public static final int MAX_VALUE_LENGTH = 100;
        private List<?> outputValues = ImmutableList.of();
        private List<String> keys = ImmutableList.of();

        public void setOutputValues(List<?> outputValues) {
            if (outputValues == null) {
                this.outputValues = ImmutableList.of();
            } else {
                this.outputValues = outputValues;
            }
            if (this.outputValues.size() == 0) {
                keys = ImmutableList.of();
            } else {
                keys = getColumnNames(this.outputValues.get(0));
            }
            fireTableChanged(new TableModelEvent(this, TableModelEvent.ALL_COLUMNS));
        }

        /**
         * Inspect the object and return a list of column names.
         * <p/>
         * If the object is already a map, return the keys.
         * <p/>
         * If the object is something else, return it as a {"Data": o.toString()}
         *
         * @param o The object to inspect
         * @return a Map.
         */
        private List<String> getColumnNames(Object o) {
            if (o instanceof IOrderedFields) {
                return ImmutableList.copyOf(((IOrderedFields)o).getOrderedFields());
            } else if (o instanceof Map) {
                ImmutableList.Builder<String> b = ImmutableList.builder();
                for (Object k : ((Map) o).keySet()) {
                    b.add(k.toString());
                }
                return b.build();
            } else {
                return ImmutableList.of("Data");
            }
        }

        private Map inspect(Object o) {
            if (o instanceof Map) {
                return (Map) o;
            } else {
                return ImmutableMap.of("Data", o);
            }
        }

        public int getRowCount() {
            return outputValues.size();
        }

        public int getColumnCount() {
            return keys.size() + 1;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            checkArgument(rowIndex < outputValues.size(), "The row index %s is larger than the number of values.", rowIndex);
            checkArgument(columnIndex < keys.size() + 1, "The column index %s is larger than the number of columns.", columnIndex);

            Map o = inspect(outputValues.get(rowIndex));
            if (columnIndex > o.size()) {
                return "<not found>";
            } else if (columnIndex == 0) {
                return rowIndex;
            } else {
                String key = keys.get(columnIndex - 1);
                return objectToString(o.get(key));
            }
        }

        public String objectToString(Object o) {
            String s = o == null ? "<null>" : o.toString();
            if (s.length() <= MAX_VALUE_LENGTH) {
                return s;
            } else {
                return s.substring(0, MAX_VALUE_LENGTH) + "...";
            }

        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return "Index";
            } else {
                return keys.get(columnIndex - 1);
            }
        }
    }

}
