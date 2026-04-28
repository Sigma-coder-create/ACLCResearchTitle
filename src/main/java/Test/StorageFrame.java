
package Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import ACLCapp.DBConnection;
import java.awt.*;

public class StorageFrame extends JFrame {


    private static StorageFrame instance; // THE INSTANCE
    private JTable table;
    private DefaultTableModel model;
    private JButton btnAddOffline, btnEditOffline, btnDeletePermanently;
    
        public static StorageFrame getInstance() {
        if (instance == null) {
            instance = new StorageFrame();
        }
        return instance;
    }
    public StorageFrame() {
        setTitle("Storage (Offline & Deleted Data)");
        setSize(950, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        model = new DefaultTableModel(
            new Object[][]{},
            new String[]{"ID", "Research Title", "SY-YR", "Status", "Approved By",
                         "Applied", "Strand", "Software", "Webpage", "State"}
        );

        table = new JTable(model);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnAddOffline = new JButton("Add Offline Record");
        btnEditOffline = new JButton("Edit Selected");
        btnDeletePermanently = new JButton("Delete Permanently");

        controlPanel.add(btnAddOffline);
        controlPanel.add(btnEditOffline);
        controlPanel.add(btnDeletePermanently);
        add(controlPanel, BorderLayout.SOUTH);

        btnAddOffline.addActionListener(e -> addOfflineRecord());
        btnEditOffline.addActionListener(e -> editOfflineRecord());
        btnDeletePermanently.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                int id = (int) model.getValueAt(row, 0);
                deletePermanently(id);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a row.");
            }
        });

        loadStorage();
    }

    public void loadStorage() {
        model.setRowCount(0);
        String sql = "SELECT * FROM ACLC_research_titles WHERE record_state != 'ACTIVE'";
        
        try (Connection con = DBConnection.getSQLiteConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("ID"),
                    rs.getString("Research Title"),
                    rs.getString("SY-YR"),
                    rs.getString("Status"),
                    rs.getString("Approved by"),
                    rs.getString("Applied"),
                    rs.getString("Strand"),
                    rs.getString("Software"),
                    rs.getString("Webpage"),
                    rs.getString("record_state")
                });
            }
            // Explicitly tell the UI to refresh
            table.revalidate();
            table.repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deletePermanently(int id) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "⚠️ This will delete the record from BOTH Local and Live Server. Continue?",
                "Confirm Global Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) return;

        // 1. Delete from SQLite (Local)
        try {
        // 1. Delete from SQLite
            try (Connection sqliteCon = DBConnection.getSQLiteConnection();
                 PreparedStatement psSqlite = sqliteCon.prepareStatement("DELETE FROM ACLC_research_titles WHERE ID = ?")) {
                psSqlite.setInt(1, id);
                psSqlite.executeUpdate();
            }

            // 2. Delete from MySQL if online
            if (DBConnection.isInternetAvailable()) {
                try (Connection mysqlCon = DBConnection.getMySQLConnection();
                     PreparedStatement psMysql = mysqlCon.prepareStatement("DELETE FROM ACLC_research_titles WHERE ID = ?")) {
                    if (mysqlCon != null) {
                        psMysql.setInt(1, id);
                        psMysql.executeUpdate();
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Local deleted, but MySQL update failed.");
                }
            }

            loadStorage();
            JOptionPane.showMessageDialog(this, "✅ Record permanently deleted.");
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // AddOffline and EditOffline logic follows...
  private void addOfflineRecord() {
        // Form Implementation...
        JTextField titleField = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 1));
        form.add(new JLabel("Research Title:"));
        form.add(titleField);
        
        int result = JOptionPane.showConfirmDialog(null, form, "Add Record", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try (Connection con = DBConnection.getSQLiteConnection();
                 PreparedStatement ps = con.prepareStatement("INSERT INTO ACLC_research_titles (`Research Title`, record_state) VALUES (?, 'PENDING_ADD')")) {
                ps.setString(1, titleField.getText());
                ps.executeUpdate();
                loadStorage();
            } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    private void editOfflineRecord() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        // Logic for editing...
        loadStorage();
    }
}