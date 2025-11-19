package com.ccinfom.ui.common;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Reusable banner for displaying status information across Swing forms.
 * Forms can switch between informational, success, and error states while
 * keeping a consistent look and feel.
 */
public final class StatusPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Available status levels. */
    public enum Level {
        INFO(new Color(0xE6, 0xF2, 0xFF), new Color(0x00, 0x62, 0xA6)),
        SUCCESS(new Color(0xE3, 0xF6, 0xEC), new Color(0x1E, 0x81, 0x4B)),
        ERROR(new Color(0xFF, 0xEB, 0xEA), new Color(0xC6, 0x19, 0x19));

        private final Color background;
        private final Color foreground;

        Level(Color background, Color foreground) {
            this.background = background;
            this.foreground = foreground;
        }

        public Color background() {
            return background;
        }

        public Color foreground() {
            return foreground;
        }
    }

    private final JLabel messageLabel;

    public StatusPanel() {
        this("Status: Ready");
    }

    public StatusPanel(String initialMessage) {
        super(new FlowLayout(FlowLayout.LEFT, 10, 6));
        setOpaque(true);
        setPreferredSize(new Dimension(10, 34));

        messageLabel = new JLabel(initialMessage, SwingConstants.LEFT);
        messageLabel.setOpaque(false);
        add(messageLabel);

        setStatus(Level.INFO, initialMessage);
    }

    public void setInfo(String message) {
        setStatus(Level.INFO, message);
    }

    public void setSuccess(String message) {
        setStatus(Level.SUCCESS, message);
    }

    public void setError(String message) {
        setStatus(Level.ERROR, message);
    }

    public void setStatus(Level level, String message) {
        messageLabel.setText(message);
        setBackground(level.background());
        messageLabel.setForeground(level.foreground());
        repaint();
    }
}
