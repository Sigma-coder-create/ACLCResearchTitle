    package Test;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;    
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.UIManager;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import ACLCapp.DBSync;
import ACLCapp.DBConnection;
    
public class SettingsFrame extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SettingsFrame.class.getName());

    /**
     * Creates new form SettingsFrame
     */
    public SettingsFrame() {
        initComponents();
        setLocationRelativeTo(null);

        jToggleButton1.setSelected(FlatLaf.isLafDark());
        Suggestions.setSelected(ACLCapp.AppSettings.SUGGESTIONS_ENABLED);
        Web_Search.setSelected(ACLCapp.AppSettings.WEB_SEARCH_ENABLED);
        
        javax.swing.Timer networkTimer = new javax.swing.Timer(3000, e -> {
        // We use the method from your DBConnection class
            boolean online = ACLCapp.DBConnection.isInternetAvailable();

            Web_Search.setEnabled(online);

            if (!online) {
                // Force it off if internet is lost
                Web_Search.setSelected(false);
                ACLCapp.AppSettings.WEB_SEARCH_ENABLED = false;
                Web_Search.setToolTipText("Web Search unavailable: Offline");
            } else {
                Web_Search.setToolTipText("Web Search available");
            }
        });
        networkTimer.start();

        jToggleButton1.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:999;"
                + "borderWidth:0;"
                + "focusWidth:0;"
                + "innerFocusWidth:0");

        jToggleButton1.addActionListener(new ActionListener() {

            private final ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(1);
            private ScheduledFuture<?> scheduledFuture;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (scheduledFuture != null) {
                    scheduledFuture.cancel(true);
                }
                scheduledFuture = scheduled.schedule(() -> {
                    changeThemes(jToggleButton1.isSelected());
                }, 500, TimeUnit.MILLISECONDS);
            }
        });
    }
    private void showUpdatedPanel() {
        // The public link to your JSON data
        String webLink = DBConnection.getProperty("jsonblob.public.url");

        // Build a nice little dialog
        JPanel panel = new JPanel(new java.awt.BorderLayout(10, 10));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top message
        JLabel message = new JLabel("<html><b>✅ Website has been updated!</b></html>");
        message.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        panel.add(message, java.awt.BorderLayout.NORTH);

        // Link field (non-editable but you can select and copy)
        JTextField linkField = new JTextField(webLink);
        linkField.setEditable(false);
        linkField.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        linkField.setBackground(javax.swing.UIManager.getColor("TextField.background"));
        panel.add(linkField, java.awt.BorderLayout.CENTER);

        // Copy button at the bottom
        JButton copyBtn = new JButton("📋 Copy link");
        copyBtn.addActionListener(e -> {
            linkField.selectAll();
            linkField.copy();
            copyBtn.setText("Copied!");
            // Reset the text after 2 seconds
            new javax.swing.Timer(2000, evt -> copyBtn.setText("📋 Copy link")).start();
        });

        JPanel bottomPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        bottomPanel.add(copyBtn);
        panel.add(bottomPanel, java.awt.BorderLayout.SOUTH);

        // Show it as a dialog
        javax.swing.JOptionPane.showMessageDialog(
            this,
            panel,
            "Website Update",
            javax.swing.JOptionPane.PLAIN_MESSAGE
        );
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        themeToggleIcon1 = new Test.ThemeToggleIcon();
        jLayeredPane2 = new javax.swing.JLayeredPane();
        jLabel1 = new javax.swing.JLabel();
        jToggleButton1 = new javax.swing.JToggleButton();
        jLayeredPane3 = new javax.swing.JLayeredPane();
        jLabel3 = new javax.swing.JLabel();
        Suggestions = new Test.CustomToggleButton();
        jLayeredPane4 = new javax.swing.JLayeredPane();
        jLabel4 = new javax.swing.JLabel();
        Web_Search = new Test.CustomToggleButton();
        Press_Update = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLayeredPane2.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setText("Light/Dark");

        jToggleButton1.setIcon(themeToggleIcon1);
        jToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton1ActionPerformed(evt);
            }
        });

        jLayeredPane2.setLayer(jLabel1, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane2.setLayer(jToggleButton1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout jLayeredPane2Layout = new javax.swing.GroupLayout(jLayeredPane2);
        jLayeredPane2.setLayout(jLayeredPane2Layout);
        jLayeredPane2Layout.setHorizontalGroup(
            jLayeredPane2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jLayeredPane2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jToggleButton1)
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addContainerGap())
        );
        jLayeredPane2Layout.setVerticalGroup(
            jLayeredPane2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jLayeredPane2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jLayeredPane2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jToggleButton1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLayeredPane3.setBackground(new java.awt.Color(255, 255, 255));

        jLabel3.setText("Suggestions");

        Suggestions.setText("customToggleButton1");
        Suggestions.setMaximumSize(new java.awt.Dimension(55, 29));
        Suggestions.setMinimumSize(new java.awt.Dimension(55, 29));
        Suggestions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SuggestionsActionPerformed(evt);
            }
        });

        jLayeredPane3.setLayer(jLabel3, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane3.setLayer(Suggestions, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout jLayeredPane3Layout = new javax.swing.GroupLayout(jLayeredPane3);
        jLayeredPane3.setLayout(jLayeredPane3Layout);
        jLayeredPane3Layout.setHorizontalGroup(
            jLayeredPane3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jLayeredPane3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Suggestions, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addContainerGap(18, Short.MAX_VALUE))
        );
        jLayeredPane3Layout.setVerticalGroup(
            jLayeredPane3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jLayeredPane3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jLayeredPane3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Suggestions, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(18, Short.MAX_VALUE))
        );

        jLayeredPane4.setBackground(new java.awt.Color(255, 255, 255));

        jLabel4.setText("Web Search");

        Web_Search.setText("customToggleButton1");
        Web_Search.setMaximumSize(new java.awt.Dimension(55, 29));
        Web_Search.setMinimumSize(new java.awt.Dimension(55, 29));
        Web_Search.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Web_SearchActionPerformed(evt);
            }
        });

        jLayeredPane4.setLayer(jLabel4, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane4.setLayer(Web_Search, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout jLayeredPane4Layout = new javax.swing.GroupLayout(jLayeredPane4);
        jLayeredPane4.setLayout(jLayeredPane4Layout);
        jLayeredPane4Layout.setHorizontalGroup(
            jLayeredPane4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jLayeredPane4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Web_Search, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                .addComponent(jLabel4)
                .addContainerGap())
        );
        jLayeredPane4Layout.setVerticalGroup(
            jLayeredPane4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jLayeredPane4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jLayeredPane4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Web_Search, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        Press_Update.setText("Update In Website");
        Press_Update.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Press_UpdateActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(67, 67, 67)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLayeredPane4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLayeredPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLayeredPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Press_Update))
                .addContainerGap(175, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(jLayeredPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLayeredPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLayeredPane4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(Press_Update, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(80, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jToggleButton1ActionPerformed

    private void SuggestionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SuggestionsActionPerformed
        boolean enabled = Suggestions.isSelected();
    
        ACLCapp.AppSettings.SUGGESTIONS_ENABLED = enabled;

        System.out.println("[Settings] Suggestions: " + (enabled ? "ON" : "OFF"));
    }//GEN-LAST:event_SuggestionsActionPerformed

    private void Web_SearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Web_SearchActionPerformed
        ACLCapp.AppSettings.WEB_SEARCH_ENABLED = Web_Search.isSelected();
        System.out.println("Web Search Toggled: " + ACLCapp.AppSettings.WEB_SEARCH_ENABLED);
    }//GEN-LAST:event_Web_SearchActionPerformed

    private void Press_UpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Press_UpdateActionPerformed
            Press_Update.setEnabled(false);
    
        // Run the update in a background thread so the GUI doesn't freeze
        new Thread(() -> {
            DBSync.exportForWeb();
            Web.updateOnlineBin();
            // Now show the success panel on the Event Dispatch Thread
            javax.swing.SwingUtilities.invokeLater(() -> {
                showUpdatedPanel();
                Press_Update.setEnabled(true);
            });
        }).start();
    }//GEN-LAST:event_Press_UpdateActionPerformed
    private void changeThemes(boolean dark) {
        if (FlatLaf.isLafDark() != dark) {
            if (!dark) {
                EventQueue.invokeLater(() -> {
                    FlatAnimatedLafChange.showSnapshot();
                    FlatIntelliJLaf.setup();
                    FlatLaf.updateUI();
                    FlatAnimatedLafChange.hideSnapshotWithAnimation();
                });
            } else {
                EventQueue.invokeLater(() -> {
                    FlatAnimatedLafChange.showSnapshot();
                    FlatDarculaLaf.setup();
                    FlatLaf.updateUI();
                    FlatAnimatedLafChange.hideSnapshotWithAnimation();
                });
            }
        }
    }

    public static void main(String args[]) {
        FlatRobotoFont.install();
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 30));
        FlatIntelliJLaf.setup();
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Press_Update;
    private Test.CustomToggleButton Suggestions;
    private Test.CustomToggleButton Web_Search;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLayeredPane jLayeredPane2;
    private javax.swing.JLayeredPane jLayeredPane3;
    private javax.swing.JLayeredPane jLayeredPane4;
    private javax.swing.JToggleButton jToggleButton1;
    private Test.ThemeToggleIcon themeToggleIcon1;
    // End of variables declaration//GEN-END:variables
}
