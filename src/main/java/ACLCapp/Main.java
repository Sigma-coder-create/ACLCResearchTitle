package ACLCapp;

import Test.StudentWindowGUI;
import javax.swing.*;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {

        // 🔑 Prompt user for the hidden app password via a popup
        JPasswordField pf = new JPasswordField();
        int okCxl = JOptionPane.showConfirmDialog(null, pf, "Enter app password:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (okCxl != JOptionPane.OK_OPTION) {
            System.err.println("❌ App closed - password not entered.");
            System.exit(0);
        }

        String inputPassword = new String(pf.getPassword());
        if (!DBConnection.unlock(inputPassword)) {
            JOptionPane.showMessageDialog(null, "❌ Wrong password. Exiting application.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        // DBSync handles connections internally (SQLite first, MySQL optional)
        DBSync.insertTestDataIfEmpty();
        DBSync.syncResearchTitles();

        // 🚀 Initialize the self-learning similarity system after syncing
        SimilarityUtil.initializeFromDatabases();

        // Launch GUI
        SwingUtilities.invokeLater(() -> {
            try {
                StudentWindowGUI window = new StudentWindowGUI();
                window.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
                window.pack();
                window.setLocationRelativeTo(null);
                window.setVisible(true);
            } catch (Exception e) {
                System.err.println("❌ GUI initialization error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}