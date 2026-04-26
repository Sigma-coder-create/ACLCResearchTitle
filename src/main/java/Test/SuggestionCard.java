package Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SuggestionCard extends JPanel {
    public SuggestionCard(String title, double percent, JTextField targetField) {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        setBackground(new Color(245, 245, 245));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel titleLabel = new JLabel("<html><b>" + title + "</b></html>");
        JLabel percentLabel = new JLabel((int)(percent * 100) + "% match");
        percentLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        percentLabel.setForeground(percent > 0.75 ? Color.RED : new Color(100, 100, 255));

        add(titleLabel, BorderLayout.CENTER);
        add(percentLabel, BorderLayout.EAST);

        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { setBackground(new Color(230, 230, 255)); }
            public void mouseExited(MouseEvent e) { setBackground(new Color(245, 245, 245)); }
            public void mouseClicked(MouseEvent e) { targetField.setText(title); }
        });
    }
}