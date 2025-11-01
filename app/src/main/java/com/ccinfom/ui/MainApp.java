package com.ccinfom.ui;

import com.ccinfom.config.DbConnection;
import com.ccinfom.ui.t1.TicketForm;
import com.ccinfom.ui.t2.PickingForm;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Simple launcher that exposes entry points for T1 and T2 forms.
 */
public final class MainApp {

    private MainApp() {
        // Utility class
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainApp::createAndShow);
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("CCINFOM Pick & Pack - Phase D");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        boolean canConnect = DbConnection.ping();
        JLabel statusLabel = new JLabel(canConnect ? "DB Status: Connected" : "DB Status: Connection Failed");

        JButton ticketButton = new JButton("Create Pick Ticket (T1)");
        ticketButton.addActionListener(e -> SwingUtilities.invokeLater(() -> new TicketForm().setVisible(true)));

        JButton pickingButton = new JButton("Start Picking (T2)");
        pickingButton.addActionListener(e -> SwingUtilities.invokeLater(() -> new PickingForm().setVisible(true)));

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.add(ticketButton);
        buttonPanel.add(pickingButton);

        frame.add(statusLabel, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.CENTER);

        frame.setPreferredSize(new Dimension(420, 160));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
