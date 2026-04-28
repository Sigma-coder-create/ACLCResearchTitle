
package Test;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;
import textfield.SearchOptinEvent;
import textfield.SearchOption;
import ACLCapp.DBConnection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import ACLCapp.SimilarityUtil;
import Test.ButtonEditor;
import Test.ButtonRenderer;
import Test.SettingsFrame;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import java.sql.Statement;


    public class StudentWindowGUI extends javax.swing.JFrame {
        private boolean showEditButton = true;
        private boolean deleteMode = false;
        private int selectedId = -1;
        private String column = "Research Title"; // default search column
        private JPanel suggestionPanel;
    
    // popup for suggestions (you already declared jPopupMenu1 in the form; we use it)

    public boolean isShowEditButton() {
        return showEditButton;
    }

    public void setShowEditButton(boolean showEditButton) {
        this.showEditButton = showEditButton;
    }
    
    public boolean isDeleteMode() {
        return deleteMode;
    }

    public void setDeleteMode(boolean deleteMode) {
        this.deleteMode = deleteMode;
    }

    
    public StudentWindowGUI() {
        try {
            FlatLightLaf.setup(); // default light theme
            // FlatDarkLaf.setup(); // uncomment for dark theme
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex.getMessage());
        }

        initComponents();
            boolean online = isOnline();
            
            Add.setEnabled(online);
            Edit.setEnabled(online);
            Delete.setEnabled(online);
            

        if (jPopupMenu1 == null) {
            jPopupMenu1 = new JPopupMenu();
        }
        try {
            Connection conn = DBConnection.getMySQLConnection();
            if (conn == null) {
                System.err.println("Failed to connect to MySQL database.");
            } else {
                System.out.println("Connected to MySQL database.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        txt.addEventOptionSelected(new SearchOptinEvent() {
            @Override
            public void optionSelected(SearchOption option, int index) {
                txt.setHint("Search by " + option.getName() + "...");
            }
        });

        txt.addOption(new SearchOption("Research Title", new ImageIcon(getClass().getClassLoader().getResource("user.png"))));
        txt.addOption(new SearchOption("SY-YR", new ImageIcon(getClass().getClassLoader().getResource("email.png"))));
        txt.addOption(new SearchOption("Strand", new ImageIcon(getClass().getClassLoader().getResource("Strand.png"))));
        txt.setSelectedIndex(0);

        loadResearchTitles("");

        txt.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String text = txt.getText() != null ? txt.getText().trim() : "";
                if (text.length() > 0) {
                    showSuggestions(text);
                } else {
                    jPopupMenu1.setVisible(false);
                }
            }
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.getSelectedRow();
                if (row != -1) {
                    selectedId = (int) table.getValueAt(row, 0); // Column 0 is ID
                    System.out.println("Selected ID: " + selectedId);
                }
            }
        });
    }
    private boolean isOnline() {
        try (Connection con = DBConnection.getMySQLConnection()) {
            return con != null;
        } catch (Exception e) {
            return false;
        }
    }
    private void performHybridSimilarityCheck(String inputTitle) {
        if (inputTitle.trim().isEmpty()) return;

        // 1. LOCAL CHECK: Use your SimilarityUtil to check the ACLC Database
        // We assume 'localDatabaseTitles' is a list of titles you've fetched
        double highestLocalScore = 0.0;

        /* for (String savedTitle : localDatabaseTitles) {
            double score = ACLCapp.SimilarityUtil.calculateSimilarity(inputTitle, savedTitle);
            if (score > highestLocalScore) highestLocalScore = score;
        }
        */

        // 2. WEB CHECK: If local similarity is low, but Web Search is ON, check globally
        if (ACLCapp.AppSettings.WEB_SEARCH_ENABLED && highestLocalScore < 0.90) {
            try {
                // Encode for a Google Scholar "Exact Phrase" search
                String encoded = java.net.URLEncoder.encode("\"" + inputTitle + "\"", "UTF-8");
                String url = "https://scholar.google.com/scholar?q=" + encoded;

                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                    System.out.println("No local match found. Launching Web Similarity Check...");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (highestLocalScore >= 0.90) {
            System.out.println("Local duplicate detected. Skipping Web Search.");
            // Show a popup or warning to the user here
        }
    }
    private boolean isValidResearchTitle(String title) {
        if (title == null) return false;

        title = title.trim();

        if (title.length() < 10 || title.length() > 255) {
            JOptionPane.showMessageDialog(this,
                "Research Title must be between 10 and 255 characters.");
            return false;
        }

        if (!title.matches("[A-Za-z0-9 ,:!@#$%^&*_+{};'/?().\\\\-]+")) {
            JOptionPane.showMessageDialog(this,
            "Research Title contains invalid characters.");
            return false;
        }

        return true;
    }

    private boolean isValidSchoolYear(String syyr) {
        if (syyr == null) return false;

        syyr = syyr.trim();

        if (!syyr.matches("\\d{4}-\\d{4}")) {
            JOptionPane.showMessageDialog(this,
                "SY-YR must be in format YYYY-YYYY.");
            return false;
        }

        String[] parts = syyr.split("-");
        int start = Integer.parseInt(parts[0]);
        int end = Integer.parseInt(parts[1]);

        if (end != start + 1) {
            JOptionPane.showMessageDialog(this,
                "End year must be start year + 1.");
            return false;
        }

        return true;
    }
    
    private boolean looksLikeAResearchTitle(String title) {
        title = title.trim().toLowerCase();


        String[] words = title.split("\\s+");
        if (words.length < 5) {
            JOptionPane.showMessageDialog(this,
                "Research Title is too short to be valid.");
            return false;
        }

        if (!title.matches(".*[a-zA-Z].*")) {
            JOptionPane.showMessageDialog(this,
                "Research Title must contain words, not just numbers.");
            return false;
        }

        return true;
    }
    
    private void loadResearchTitles(String filter) {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        int providedColumn = -1;
        try {
            providedColumn = table.getColumnModel().getColumnIndex("Provided");
        } catch (IllegalArgumentException e) {
            return; // column not found, skip
        }
        if (showEditButton) {
            // Show the button column
            table.getColumnModel().getColumn(providedColumn).setCellRenderer(new ButtonRenderer(this));
            table.getColumnModel().getColumn(providedColumn).setCellEditor(new ButtonEditor(table, this));
        } else {
            // Hide the button column by removing renderer/editor
            table.getColumnModel().getColumn(providedColumn).setCellRenderer(null);
            table.getColumnModel().getColumn(providedColumn).setCellEditor(null);
        }
    
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        String sql = "SELECT `ID`, `Research Title`, `SY-YR`, `Status`, `Approved by`, `Applied`, `Strand`, `Software`, `Webpage` " +
             "FROM ACLC_research_titles WHERE record_state = 'ACTIVE'";
        boolean hasFilter = filter != null && !filter.isEmpty();
        if (hasFilter && column != null) {
        String col = getSafeColumn();
            sql += " AND `" + col + "` LIKE ?";
        }

        // Try sqlite first
        Connection con = null;
        try {
            con = DBConnection.getMySQLConnection();
            if (con == null) {
                // MySQL failed, try SQLite
                System.out.println("MySQL not available, falling back to SQLite for data load.");
                con = DBConnection.getSQLiteConnection();
        }
        } catch (Exception e) {
            System.out.println("Error getting MySQL connection: " + e.getMessage());
            con = DBConnection.getSQLiteConnection();
        }

        if (con == null) {
            System.err.println("No database connection available!");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            System.out.println("========== DEBUG ==========");
            System.out.println("SQL Query: " + sql);
            System.out.println("Filter Value: " + filter);
            System.out.println("Column Used: " + column);
            System.out.println("Has Filter: " + hasFilter);
            System.out.println("===========================");

            if (hasFilter) {
                ps.setString(1, "%" + filter + "%");
            }

            ResultSet rs = ps.executeQuery();
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
                    ""
                });
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage());
        }
    }

    private void loadData() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        String sql = "SELECT ID, `Research Title`, `SY-YR`, Status, `Approved by`, Applied, Strand, Software, Webpage " +
                     "FROM ACLC_research_titles WHERE record_state = 'ACTIVE'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("ID"),
                    rs.getString("Research Title"),
                    rs.getString("SY_YR"),
                    rs.getString("Status"),
                    rs.getString("Approved by"),
                    rs.getString("Applied"),
                    rs.getString("Strand"),
                    rs.getString("Software"),
                    rs.getString("Webpage")
                });
            }

            System.out.println("[GUI] Loaded from: " + con.getMetaData().getURL());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private Connection getBestConnection() throws SQLException {
        Connection con = DBConnection.getMySQLConnection();
        if (con != null) {
            return con; // Online mode
        }
        con = DBConnection.getSQLiteConnection();
        if (con != null) {
            return con; // Offline mode
        }
        throw new SQLException("Both MySQL and SQLite are offline!");
    }
    
    private boolean confirmIfSimilarTitle(String newTitle, int currentId) {
        try (Connection con = getBestConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT ID, `Research Title` FROM ACLC_research_titles"
             )) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int dbId = rs.getInt("ID");
                if (dbId == currentId) continue;

                String existingTitle = rs.getString("Research Title");

                double similarity = SimilarityUtil.calculateSimilarity(
                    newTitle.toLowerCase(),
                    existingTitle.toLowerCase()
                );

            if (similarity >= 0.80) {
                int choice = JOptionPane.showConfirmDialog(
                    this,
                    "This title is very similar to an existing research title:\n\n"
                    + existingTitle + "\n\n"
                    + "Similarity: " + (int)(similarity * 100) + "%\n\n"
                    + "Do you want to continue editing?",
                    "Similar Research Title Detected",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );

                    return choice == JOptionPane.YES_OPTION;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Show up to 5 suggestions for the currently selected column.
     * This method is required so the call showSuggestions(text) compiles.
     */
    private String getSafeColumn() {
        switch (column) {
            case "Research Title":
                return "Research Title";
            case "SY-YR":
                return "SY-YR";
            case "Strand":
                return "Strand";
            default:
                return "Research Title";
        }
    }
    private void showSuggestions(String text) {
        jPopupMenu1.setFocusable(false);

       jPopupMenu1.removeAll();

        Set<String> seen = new HashSet<>();

        // wrap column in backticks for safety
        String col = getSafeColumn();
        String sql = "SELECT `" + col + "` FROM ACLC_research_titles WHERE `" + col + "` LIKE ? LIMIT 5";

        try (Connection con = getBestConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + text + "%");
            ResultSet rs = ps.executeQuery();

            boolean hasResults = false;
            while (rs.next()) {
                String suggestion = rs.getString(1);
                if (suggestion == null) continue;
                if (seen.contains(suggestion)) continue;
                seen.add(suggestion);

                hasResults = true;
                JMenuItem item = new JMenuItem(suggestion);
                // popup should NOT steal focus
                item.setFocusable(false);

                item.addActionListener(e -> {
                    txt.setText(suggestion);
                    jPopupMenu1.setVisible(false);
                    loadResearchTitles(suggestion);
                });

                jPopupMenu1.add(item);
            }
            rs.close();

            if (hasResults) {
                // let text field keep focus for typing
                txt.requestFocusInWindow();
                jPopupMenu1.show(txt, 0, txt.getHeight());
            } else {
                jPopupMenu1.setVisible(false);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            jPopupMenu1.setVisible(false);
        }
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFrame1 = new javax.swing.JFrame();
        jPopupMenu1 = new javax.swing.JPopupMenu();
        txt = new textfield.TextFieldSearchOption();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        Edit = new javax.swing.JButton();
        Storage = new javax.swing.JButton();
        Add = new javax.swing.JButton();
        Delete = new javax.swing.JButton();
        Settings = new javax.swing.JButton();

        javax.swing.GroupLayout jFrame1Layout = new javax.swing.GroupLayout(jFrame1.getContentPane());
        jFrame1.getContentPane().setLayout(jFrame1Layout);
        jFrame1Layout.setHorizontalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jFrame1Layout.setVerticalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        txt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtActionPerformed(evt);
            }
        });

        jScrollPane1.setOpaque(false);

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Research Title", "SY-YR", "Status", "Approved by", "Applied", "Strand", "Software", "Webpage", "Provided"
            }
        ));
        jScrollPane1.setViewportView(table);
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMinWidth(0);
            table.getColumnModel().getColumn(0).setPreferredWidth(0);
            table.getColumnModel().getColumn(0).setMaxWidth(0);
        }

        Edit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Edit-button.png"))); // NOI18N
        Edit.setInheritsPopupMenu(true);
        Edit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EditActionPerformed(evt);
            }
        });

        Storage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Storage.png"))); // NOI18N
        Storage.setInheritsPopupMenu(true);
        Storage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StorageActionPerformed(evt);
            }
        });

        Add.setIcon(new javax.swing.ImageIcon(getClass().getResource("/add-button.png"))); // NOI18N
        Add.setInheritsPopupMenu(true);
        Add.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddActionPerformed(evt);
            }
        });

        Delete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Delete-button.png"))); // NOI18N
        Delete.setInheritsPopupMenu(true);
        Delete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteActionPerformed(evt);
            }
        });

        Settings.setIcon(new javax.swing.ImageIcon(getClass().getResource("/settings.png"))); // NOI18N
        Settings.setInheritsPopupMenu(true);
        Settings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SettingsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Settings, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(Storage, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(Add, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(Edit, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(Delete, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24)
                .addComponent(txt, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 632, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Edit, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Storage, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Add, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Delete, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Settings, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtActionPerformed
        if (txt.isSelected()) {
            String selected = txt.getSelectedOption().getName();
            if (selected != null && !selected.isEmpty()) {
                if (selected.equals("Research Title")) {
                    column = "Research Title";
                } else if (selected.equals("SY-YR")) {
                    column = "SY-YR";
                } else if (selected.equals("Strand")) {
                    column = "Strand";
                } else {
                    column = "Research Title"; // fallback
                }
                loadResearchTitles(txt.getText());
            }
        }
    }//GEN-LAST:event_txtActionPerformed

    private void EditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EditActionPerformed
        setDeleteMode(false); 
        table.repaint();
        javax.swing.JOptionPane.showMessageDialog(this, "Switched to EDIT mode. Click row buttons to edit.");
    }//GEN-LAST:event_EditActionPerformed
 
    private void StorageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StorageActionPerformed

        new StorageFrame().setVisible(true);
    }//GEN-LAST:event_StorageActionPerformed

    private void AddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddActionPerformed
        JTextField titleField = new JTextField();
        JTextField syyrField = new JTextField();
        
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"Denied", "Pending", "Approved"});
        
        JTextField approvedByField = new JTextField();
        
        JComboBox<String> appliedBox = new JComboBox<>(new String[]{"Yes", "Not yet", "No"});
        
        JComboBox<String> strandBox = new JComboBox<>(new String[]{"ICT", "GAS"});
        
        JComboBox<String> softwareBox = new JComboBox<>(new String[]{"✔", "✘"});
        
        JComboBox<String> webpageBox = new JComboBox<>(new String[]{"✔", "✘"});
       // Create the form panel
        JPanel formPanel = new JPanel(new GridLayout(8, 2, 5, 5));
        formPanel.add(new JLabel("Research Title:")); formPanel.add(titleField);
        formPanel.add(new JLabel("SY-YR:")); formPanel.add(syyrField);
        formPanel.add(new JLabel("Status:")); formPanel.add(statusBox);
        formPanel.add(new JLabel("Approved by:")); formPanel.add(approvedByField);
        formPanel.add(new JLabel("Applied:")); formPanel.add(appliedBox);
        formPanel.add(new JLabel("Strand:")); formPanel.add(strandBox);
        formPanel.add(new JLabel("Software:")); formPanel.add(softwareBox);
        formPanel.add(new JLabel("Webpage:")); formPanel.add(webpageBox);

        // Live suggestion panel
        JPanel suggestionPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(suggestionPanel, BorderLayout.SOUTH);

        // Live similarity listener
        titleField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                if (!ACLCapp.AppSettings.SUGGESTIONS_ENABLED) {
                    suggestionPanel.removeAll();
                    suggestionPanel.repaint();
                    return;
                }
                suggestionPanel.removeAll();
                java.util.List<TitleMatch> matches = getTopSimilarTitles(titleField.getText());
                
                for (TitleMatch m : matches) {
                    SuggestionCard card = new SuggestionCard(m.title, m.score, "Database", titleField);
                    suggestionPanel.add(card);
                }
                suggestionPanel.revalidate();
                suggestionPanel.repaint();
                // Resize dialog to fit new cards
                java.awt.Window w = SwingUtilities.getWindowAncestor(mainPanel);
                if (w != null) w.pack();
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        ImageIcon amaIcon = new ImageIcon(getClass().getResource("/AMA.png"));

        int option = JOptionPane.showConfirmDialog(
            this, mainPanel, "Add Research Title",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            amaIcon
        );

        String title = titleField.getText().trim();
        String syyr = syyrField.getText().trim();
        String approvedby = approvedByField.getText().trim();

        if (!isValidResearchTitle(title)
                || !looksLikeAResearchTitle(title)
                || !isValidSchoolYear(syyr)) {
            return;
        }

        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Research Title cannot be empty!");
            return;
        }

        String[] bannedWords = {"warren sarap sarap"};
        for (String bad : bannedWords) {
            if (title.toLowerCase().contains(bad)) {
                JOptionPane.showMessageDialog(this,
                    "Your title contains inappropriate or invalid words.");
                return;
            }
        }

        String status = (String) statusBox.getSelectedItem();
        String applied = (String) appliedBox.getSelectedItem();
        String strand = (String) strandBox.getSelectedItem();
        String software = (String) softwareBox.getSelectedItem();
        String webpage = (String) webpageBox.getSelectedItem();

        Connection con = null;

        try {
            con = DBConnection.getMySQLConnection();
        } catch (Exception e) {
            con = null;
        }

        // =========================
        // 🔴 OFFLINE MODE
        // =========================
        if (con == null) {
            try (Connection sqlite = DBConnection.getSQLiteConnection()) {

                String sql = "INSERT INTO ACLC_research_titles " +
                             "(`Research Title`, `SY-YR`, `Status`, `Approved by`, `Applied`, `Strand`, `Software`, `Webpage`, record_state) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING_ADD')";

                PreparedStatement ps = sqlite.prepareStatement(sql);
                ps.setString(1, title);
                ps.setString(2, syyr);
                ps.setString(3, status);
                ps.setString(4, approvedby);
                ps.setString(5, applied);
                ps.setString(6, strand);
                ps.setString(7, software);
                ps.setString(8, webpage);

                ps.executeUpdate();
                ps.close();

                JOptionPane.showMessageDialog(this, "📦 Saved OFFLINE (Storage)");
                loadResearchTitles("");
                return;

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Offline save failed: " + e.getMessage());
                return;
            }
        }

        // =========================
        // 🟢 ONLINE MODE
        // =========================
        try {

            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT `Research Title` FROM ACLC_research_titles");

            boolean tooSimilar = false;
            String similarTo = "";
            double highestSim = 0.0;

            while (rs.next()) {
                String existing = rs.getString("Research Title");
                double sim = SimilarityUtil.similarityScore(title, existing);

                if (sim > highestSim) {
                    highestSim = sim;
                    similarTo = existing;
                }

                if (sim >= 0.7) {
                    tooSimilar = true;
                }
            }

            rs.close();
            st.close();

            double percent = highestSim * 100.0;
            String msg = String.format("Highest similarity: %.2f%%\nMost similar:\n\"%s\"", percent, similarTo);

            if (tooSimilar) {
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    msg + "\n\n⚠️ Still add?",
                    "Possible Duplicate",
                    JOptionPane.YES_NO_OPTION
                );

                if (confirm != JOptionPane.YES_OPTION) {
                    JOptionPane.showMessageDialog(this, "Cancelled.");
                    return;
                }
            } else {
                JOptionPane.showMessageDialog(this, msg + "\n\n✅ Unique enough.");
            }

            String sql = "INSERT INTO ACLC_research_titles " +
                         "(`Research Title`, `SY-YR`, `Status`, `Approved by`, `Applied`, `Strand`, `Software`, `Webpage`, record_state) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, title);
            ps.setString(2, syyr);
            ps.setString(3, status);
            ps.setString(4, approvedby);
            ps.setString(5, applied);
            ps.setString(6, strand);
            ps.setString(7, software);
            ps.setString(8, webpage);

            ps.executeUpdate();
            ps.close();

            JOptionPane.showMessageDialog(this, "✅ Added successfully!");
            loadResearchTitles("");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }//GEN-LAST:event_AddActionPerformed

    private void DeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteActionPerformed
        setDeleteMode(true); // Skibidi Moralidad.
        table.repaint();
        JOptionPane.showMessageDialog(this, "Switched to DELETE mode. Click row buttons to delete."); 
    }//GEN-LAST:event_DeleteActionPerformed

    private void SettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SettingsActionPerformed
        new SettingsFrame().setVisible(true);
    }//GEN-LAST:event_SettingsActionPerformed
    public void editRow(int id) {
        if (id <= 0) {
            JOptionPane.showMessageDialog(this, "Invalid record ID.");
            return;
        }

        // Fetch the record from the database
        String sql = "SELECT `Research Title`, `SY-YR`, `Status`, `Approved by`, `Applied`, `Strand`, `Software`, `Webpage` FROM ACLC_research_titles WHERE ID = ?";

        try (Connection con = getBestConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Record not found.");
                return;
            }

            String currentTitle = rs.getString("Research Title");
            String currentSY = rs.getString("SY-YR");
            String currentStatus = rs.getString("Status");
            String currentApprovedBy = rs.getString("Approved by");
            String currentApplied = rs.getString("Applied");
            String currentStrand = rs.getString("Strand");
            String currentSoftware = rs.getString("Software");
            String currentWebpage = rs.getString("Webpage");
            rs.close();

            // Build the edit dialog exactly as before
            JTextField titleField = new JTextField(currentTitle);
            JTextField syyrField = new JTextField(currentSY);

            JComboBox<String> statusBox = new JComboBox<>(new String[]{"Denied", "Pending", "Approved"});
            statusBox.setSelectedItem(currentStatus);

            JTextField approvedbyField = new JTextField(currentApprovedBy);

            JComboBox<String> appliedBox = new JComboBox<>(new String[]{"Yes", "Not yet", "No"});
            appliedBox.setSelectedItem(currentApplied);

            JComboBox<String> strandBox = new JComboBox<>(new String[]{"ICT", "GAS"});
            strandBox.setSelectedItem(currentStrand);

            JComboBox<String> softwareBox = new JComboBox<>(new String[]{"✔", "✘"});
            softwareBox.setSelectedItem(currentSoftware);

            JComboBox<String> webpageBox = new JComboBox<>(new String[]{"✔", "✘"});
            webpageBox.setSelectedItem(currentWebpage);

            Object[] message = {
                "Research Title:", titleField,
                "SY-YR:", syyrField,
                "Status:", statusBox,
                "Approved by:", approvedbyField,
                "Applied:", appliedBox,
                "Strand:", strandBox,
                "Software:", softwareBox,
                "Webpage:", webpageBox
            };

            ImageIcon amaIcon = new ImageIcon(getClass().getResource("/AMA.png"));
            int option = JOptionPane.showConfirmDialog(
                this, message, "Edit Research Title",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, amaIcon
            );

            if (option == JOptionPane.OK_OPTION) {
                String inputTitle = titleField.getText().trim();
                java.util.List<TitleMatch> topMatches = getTopSimilarTitles(inputTitle);
                if (!topMatches.isEmpty() && topMatches.get(0).score > 0.80) {
                    int answer = JOptionPane.showConfirmDialog(this,
                        "⚠️ This title is " + (int)(topMatches.get(0).score * 100) + "% similar to:\n\""
                        + topMatches.get(0).title + "\"\n\nDo you still want to save?",
                        "High Similarity Detected",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (answer != JOptionPane.YES_OPTION) return;
                }
                String title = titleField.getText().trim();
                String syyr = syyrField.getText().trim();
                String approvedby = approvedbyField.getText().trim();

                if (title.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Research Title cannot be empty!");
                    return;
                }
                if (!looksLikeAResearchTitle(title)) {
                    return;
                }
                if (!syyr.matches("\\d{4}-\\d{4}")) {
                    JOptionPane.showMessageDialog(this, "SY-YR must be in format YYYY-YYYY");
                    return;
                }
                if (!confirmIfSimilarTitle(title, id)) {
                    return;
                }
                String[] years = syyr.split("-");
                int start = Integer.parseInt(years[0]);
                int end = Integer.parseInt(years[1]);
                if (end != start + 1) {
                    JOptionPane.showMessageDialog(this, "SY-YR must be consecutive (e.g., 2024-2025)");
                    return;
                }

            // Update the database
                String updateSql = "UPDATE ACLC_research_titles SET `Research Title`=?, `SY-YR`=?, Status=?, `Approved by`=?, Applied=?, Strand=?, Software=?, Webpage=? WHERE ID=?";
                try (PreparedStatement updatePs = con.prepareStatement(updateSql)) {
                    updatePs.setString(1, title);
                    updatePs.setString(2, syyr);
                    updatePs.setString(3, (String) statusBox.getSelectedItem());
                    updatePs.setString(4, approvedby);
                    updatePs.setString(5, (String) appliedBox.getSelectedItem());
                    updatePs.setString(6, (String) strandBox.getSelectedItem());
                    updatePs.setString(7, (String) softwareBox.getSelectedItem());
                    updatePs.setString(8, (String) webpageBox.getSelectedItem());
                    updatePs.setInt(9, id);
                    updatePs.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Research Title updated successfully!");
                    loadResearchTitles("");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error editing record: " + e.getMessage());
        }
    }
    public void deleteRow(int id) {
        if (id <= 0) {
            JOptionPane.showMessageDialog(this, "Invalid record ID.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Move this research title to Storage?",
            "Confirm", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "UPDATE ACLC_research_titles SET record_state = 'DELETED' WHERE ID = ?";

            try (Connection con = getBestConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, id);
                ps.executeUpdate();
                //StorageFrame.getInstance().setVisible(true);//
                StorageFrame.getInstance().loadStorage();
                
                JOptionPane.showMessageDialog(this, "Moved to Storage!");
                loadResearchTitles("");

                // ✅ Refresh StorageFrame if it's open
                for (Window w : Window.getWindows()) {
                    if (w instanceof StorageFrame) {
                        ((StorageFrame) w).loadStorage();
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    class TitleMatch {
        String title;
        double score;
        TitleMatch(String t, double s) { title = t; score = s; }
    }

    private java.util.List<TitleMatch> getTopSimilarTitles(String input) {
        java.util.List<TitleMatch> matches = new java.util.ArrayList<>();
        if (input == null || input.trim().length() < 5) return matches;

        String sql = "SELECT `Research Title` FROM ACLC_research_titles WHERE record_state = 'ACTIVE'";
        try (Connection con = getBestConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String dbTitle = rs.getString("Research Title");
                double sim = SimilarityUtil.similarityScore(input, dbTitle);
                if (sim >= 0.10) {
                    matches.add(new TitleMatch(dbTitle, sim));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        matches.sort((a, b) -> Double.compare(b.score, a.score));
        return matches.stream().limit(3).collect(java.util.stream.Collectors.toList());
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(StudentWindowGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(StudentWindowGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(StudentWindowGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(StudentWindowGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        

        
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new StudentWindowGUI().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Add;
    private javax.swing.JButton Delete;
    private javax.swing.JButton Edit;
    private javax.swing.JButton Settings;
    private javax.swing.JButton Storage;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable table;
    private textfield.TextFieldSearchOption txt;
    // End of variables declaration//GEN-END:variables
}
