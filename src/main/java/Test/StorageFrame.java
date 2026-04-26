package Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import ACLCapp.DBConnection;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StorageFrame extends JFrame {

    private JTable table;
    private JButton btnAddOffline, btnEditOffline, btnDeletePermanently;

    public StorageFrame() {
        setTitle("Storage (Offline & Deleted Data)");
        setSize(950, 450);  // wider to fit all columns
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        table = new JTable(new DefaultTableModel(
            new Object[][]{},
            new String[]{"ID", "Research Title", "SY-YR", "Status", "Approved By",
                         "Applied", "Strand", "Software", "Webpage", "State"}
        ));
        // Hide ID column?
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
                int id = (int) table.getValueAt(row, 0);
                deletePermanently(id);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a row.");
            }
        });

        loadStorage();
    }

    // public so it can be called from StudentWindowGUI
    public void loadStorage() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addOfflineRecord() {
        JTextField titleField = new JTextField();
        JTextField syyrField = new JTextField();
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"Denied", "Pending", "Approved"});
        JTextField approvedByField = new JTextField();
        JComboBox<String> appliedBox = new JComboBox<>(new String[]{"Yes", "Not yet", "No"});
        JComboBox<String> strandBox = new JComboBox<>(new String[]{"ICT", "GAS"});
        JComboBox<String> softwareBox = new JComboBox<>(new String[]{"✔", "✘"});
        JComboBox<String> webpageBox = new JComboBox<>(new String[]{"✔", "✘"});

        JPanel form = new JPanel(new GridLayout(8, 2, 5, 5));
        form.add(new JLabel("Research Title:")); form.add(titleField);
        form.add(new JLabel("SY-YR:")); form.add(syyrField);
        form.add(new JLabel("Status:")); form.add(statusBox);
        form.add(new JLabel("Approved by:")); form.add(approvedByField);
        form.add(new JLabel("Applied:")); form.add(appliedBox);
        form.add(new JLabel("Strand:")); form.add(strandBox);
        form.add(new JLabel("Software:")); form.add(softwareBox);
        form.add(new JLabel("Webpage:")); form.add(webpageBox);

        int option = JOptionPane.showConfirmDialog(this, form, "Add Offline Record", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            String sql = "INSERT INTO ACLC_research_titles (`Research Title`, `SY-YR`, Status, `Approved by`, Applied, Strand, Software, Webpage, record_state) "
                       + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING_ADD')";
            try (Connection con = DBConnection.getSQLiteConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, titleField.getText().trim());
                ps.setString(2, syyrField.getText().trim());
                ps.setString(3, (String) statusBox.getSelectedItem());
                ps.setString(4, approvedByField.getText().trim());
                ps.setString(5, (String) appliedBox.getSelectedItem());
                ps.setString(6, (String) strandBox.getSelectedItem());
                ps.setString(7, (String) softwareBox.getSelectedItem());
                ps.setString(8, (String) webpageBox.getSelectedItem());
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Offline record added!");
                loadStorage();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void editOfflineRecord() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a row to edit.");
            return;
        }
        int id = (int) table.getValueAt(row, 0);
        // Fetch current data
        String curTitle = (String) table.getValueAt(row, 1);
        String curSyyr = (String) table.getValueAt(row, 2);
        String curStatus = (String) table.getValueAt(row, 3);
        String curApproved = (String) table.getValueAt(row, 4);
        String curApplied = (String) table.getValueAt(row, 5);
        String curStrand = (String) table.getValueAt(row, 6);
        String curSoftware = (String) table.getValueAt(row, 7);
        String curWebpage = (String) table.getValueAt(row, 8);

        JTextField titleField = new JTextField(curTitle);
        JTextField syyrField = new JTextField(curSyyr);
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"Denied", "Pending", "Approved"});
        statusBox.setSelectedItem(curStatus);
        JTextField approvedByField = new JTextField(curApproved);
        JComboBox<String> appliedBox = new JComboBox<>(new String[]{"Yes", "Not yet", "No"});
        appliedBox.setSelectedItem(curApplied);
        JComboBox<String> strandBox = new JComboBox<>(new String[]{"ICT", "GAS"});
        strandBox.setSelectedItem(curStrand);
        JComboBox<String> softwareBox = new JComboBox<>(new String[]{"✔", "✘"});
        softwareBox.setSelectedItem(curSoftware);
        JComboBox<String> webpageBox = new JComboBox<>(new String[]{"✔", "✘"});
        webpageBox.setSelectedItem(curWebpage);

        JPanel form = new JPanel(new GridLayout(8, 2, 5, 5));
        form.add(new JLabel("Research Title:")); form.add(titleField);
        form.add(new JLabel("SY-YR:")); form.add(syyrField);
        form.add(new JLabel("Status:")); form.add(statusBox);
        form.add(new JLabel("Approved by:")); form.add(approvedByField);
        form.add(new JLabel("Applied:")); form.add(appliedBox);
        form.add(new JLabel("Strand:")); form.add(strandBox);
        form.add(new JLabel("Software:")); form.add(softwareBox);
        form.add(new JLabel("Webpage:")); form.add(webpageBox);

        int option = JOptionPane.showConfirmDialog(this, form, "Edit Offline Record",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            String sql = "UPDATE ACLC_research_titles SET `Research Title`=?, `SY-YR`=?, Status=?, "
                       + "`Approved by`=?, Applied=?, Strand=?, Software=?, Webpage=? WHERE ID=?";
            try (Connection con = DBConnection.getSQLiteConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, titleField.getText().trim());
                ps.setString(2, syyrField.getText().trim());
                ps.setString(3, (String) statusBox.getSelectedItem());
                ps.setString(4, approvedByField.getText().trim());
                ps.setString(5, (String) appliedBox.getSelectedItem());
                ps.setString(6, (String) strandBox.getSelectedItem());
                ps.setString(7, (String) softwareBox.getSelectedItem());
                ps.setString(8, (String) webpageBox.getSelectedItem());
                ps.setInt(9, id);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Offline record updated!");
                loadStorage();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void deletePermanently(int id) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "⚠️ Permanently delete this record? This cannot be undone.",
                "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection con = DBConnection.getSQLiteConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM ACLC_research_titles WHERE ID = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Permanently deleted.");
            loadStorage();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}