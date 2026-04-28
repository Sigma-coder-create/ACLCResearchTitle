package Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SuggestionCard extends JPanel {
    private TimeLimit limit; // Store the limit logic

    // KEEP THIS EXACTLY AS IS - Prevents StudentWindowGUI from breaking
    public SuggestionCard(String title, double percent, String source, JTextField targetField) {
        // We wrap the old parameters into a TimeLimit object with a default date
        this(new TimeLimit(title, percent, source, "Unknown"), targetField);
    }

    // NEW CONSTRUCTOR - Handles the TimeLimit logic
    public SuggestionCard(TimeLimit limit, JTextField targetField) {
        this.limit = limit;
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        
        // Background logic using TimeLimit source
        if ("Web".equals(limit.source)) {
            setBackground(new Color(230, 255, 230)); 
        } else {
            setBackground(new Color(245, 245, 245));
        }
        
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Use HTML to gray out the title if it is expired (over 5 years)
        String displayTitle = limit.isRecent ? "<b>" + limit.title + "</b>" : "<font color='gray'>[EXPIRED] " + limit.title + "</font>";
        JLabel titleLabel = new JLabel("<html>" + displayTitle + "</html>");
        
        JLabel percentLabel = new JLabel((int)(limit.score * 100) + "% match [" + limit.source + "]");
        percentLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Use the color logic defined in TimeLimit.java
        percentLabel.setForeground(limit.getStatusColor());

        add(titleLabel, BorderLayout.CENTER);
        add(percentLabel, BorderLayout.EAST);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (limit.isRecent) {
                    targetField.setText(limit.title);
                } else {
                    // Feature: Block selection if over 5 years old
                    JOptionPane.showMessageDialog(null, 
                        "This title is from " + limit.dateStr + ".\n" +
                        "Titles older than 5 years cannot be used as a basis.", 
                        "Time Limit Restriction", JOptionPane.WARNING_MESSAGE);
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                if(limit.isRecent) setBackground(new Color(230, 230, 255));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if ("Web".equals(limit.source)) {
                    setBackground(new Color(230, 255, 230));
                } else {
                    setBackground(new Color(245, 245, 245));
                }
            }
        });
    }
}