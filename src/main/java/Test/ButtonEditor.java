package Test;

import Test.StudentWindowGUI;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
    private final JButton button;
    private final StudentWindowGUI parent;
    private JTable table;

    public ButtonEditor(JTable table, StudentWindowGUI parent) {
        this.table = table;
        this.parent = parent;
        this.button = new JButton();

        button.addActionListener(e -> {
            // Get the editing row directly from the table at click time
            int editingRow = table.getEditingRow();
            if (editingRow == -1) {
                fireEditingCanceled();
                return;
            }

            // Retrieve the ID from column 0
            Object idObj = table.getValueAt(editingRow, 0);
            int id = (idObj != null) ? Integer.parseInt(idObj.toString()) : -1;

            fireEditingStopped();

            if (parent.isDeleteMode()) {
                parent.deleteRow(id);
            } else {
                parent.editRow(id);
            }
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        button.setText(parent.isDeleteMode() ? "Delete" : "Edit");
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }
}