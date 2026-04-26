/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Test;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.Timer;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class CustomToggleButton extends JToggleButton {
    // Current position of the toggle button (0 = off, 1 = on)
    private float position = 0;
    // Position used for the glow animation effect
    private float glowPosition = 0;
    // Default background color when the toggle is off
    private Color toggleColor = new Color(200, 200, 220);
    // Default color when the toggle is active/on
    private Color activeColor = new Color(130, 90, 255);
    // Timer for handling smooth animations
    private Timer animationTimer;
    // Duration of the toggle animation in milliseconds
    private static final int ANIMATION_DURATION = 200;
    
    
    public CustomToggleButton(){
        
        setPreferredSize(new Dimension(70*2, 35*2));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Initialize animation timer that updates every 16ms
        animationTimer = new Timer(16, e ->{
            // Determine target position based on selected state
            float targetPosition = isSelected() ? 1 : 0;
            // Calculate step size for smooth animation
            float step = 1.0f / (ANIMATION_DURATION / 16.0f);
            
            // Animate toggle position
            if(position < targetPosition){
                // Move position towards 1 (on state)
                position = Math.min(position + step, 1);
                // Apply easing function to smooth the movement
                glowPosition = easeInOut(position);
                repaint();
            }
            else if(position > targetPosition){
                // Move position towards 0 (off state)
                position = Math.max(position - step, 0);
                // Apply easing function to smooth the movement
                glowPosition = easeInOut(position);
                repaint();
            }
        });
        
        // Start animation when the toggle state changes
        addItemListener(e -> {
            
            if(!animationTimer.isRunning()){
                animationTimer.start();
            }
        
        });
        
        
    }
    
    @Override
    protected void paintComponent(Graphics g){
        // Create a new Graphics2D object with anti-aliasing enabled
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Calculate dimensions for the toggle button
        int width = getWidth();
        int height = getHeight();
        int toggleWidth = width - 4;
        int toggleHeight = height - 4;
        int diameter = toggleHeight - 4;
        
        // Create rounded rectangle for button background
        RoundRectangle2D background = new RoundRectangle2D.Float(
                2, 2, toggleWidth, toggleHeight, toggleHeight, toggleHeight
        );
        
        // Create gradient for glass effect
        GradientPaint gradientBackground = new GradientPaint(
                0, 0, new Color(255, 255, 255, 50),
                0, height, new Color(255, 255, 255, 10)
        );
        
        // Draw main background
        g2d.setColor(isSelected() ? activeColor : toggleColor);
        g2d.fill(background);
         // Apply glass effect gradient
        g2d.setPaint(gradientBackground);
        g2d.fill(background);
        
        // Add subtle inner shadow to background
        g2d.setColor(new Color(0,0,0,10));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(background);
        
        // Calculate position of the toggle circle
        float circleX = 4 + (toggleWidth - diameter - 4) * glowPosition;
        float circleY = 4;
        
        // Draw the main toggle circle 
        g2d.setColor(Color.WHITE);
        Ellipse2D circle = new Ellipse2D.Float(circleX, circleY, diameter, diameter);
        g2d.fill(circle);
        
        // Add gradient to the toggle circle
        GradientPaint gradientKnob = new GradientPaint(
                circleX, circleY, new Color(255, 255, 255),
                circleX, circleY + diameter, new Color(240, 240, 240)
        );
        
        g2d.setPaint(gradientKnob);
        g2d.fill(circle);
        
        // Add border to toggle circle
        g2d.setColor(new Color(0, 0 , 0, 30));
        g2d.setStroke(new BasicStroke(1f));
        g2d.draw(circle);
        
        // Draw status indicator dot when selected
        if(isSelected()){
            float dotSize = diameter * 0.5f;
            float dotX = circleX + (diameter - dotSize) / 2;
            float dotY = circleY + (diameter - dotSize) / 2;
            g2d.setColor(activeColor);
            g2d.fill(new Ellipse2D.Float(dotX, dotY, dotSize, dotSize));
        }
        
        // Clean up graphics resources
        g2d.dispose();
                
        
    }
    
    
    
    // Easing function to create smooth acceleration and deceleration
    private float easeInOut(float t){
        
        return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
        
    }
    
    // Method to change the active color of the toggle
    public void setActiveColor(Color color){
        this.activeColor = color;
        repaint();
    }
    
    
    public static void main(String[] args) {
        
        JFrame frame = new JFrame("Toggle Buttons");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setSize(600, 250);
        frame.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
        
        CustomToggleButton cbtn = new CustomToggleButton();
        CustomToggleButton toggleBlue = new CustomToggleButton();
        CustomToggleButton toggleGreen = new CustomToggleButton();
        CustomToggleButton toggleRed = new CustomToggleButton();
        CustomToggleButton toggleBlack = new CustomToggleButton();
        CustomToggleButton toggleYellow = new CustomToggleButton();
        
        toggleBlue.setActiveColor(new Color(64, 150, 255));
        toggleGreen.setActiveColor(new Color(75, 210, 140));
        toggleRed.setActiveColor(new Color(239, 68, 68));
        toggleBlack.setActiveColor(new Color(20, 5, 5));
        toggleYellow.setActiveColor(new Color(241, 196, 15));
        
        
        frame.add(cbtn);
        frame.add(toggleBlue);
        frame.add(toggleGreen);
        frame.add(toggleRed);
        frame.add(toggleBlack);
        frame.add(toggleYellow);
        
        
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
    }

}