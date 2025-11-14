package com.ccinfom.ui;

import com.ccinfom.config.DbConnection;
import com.ccinfom.ui.common.StatusPanel;
import com.ccinfom.ui.report.ReportR1Form;
import com.ccinfom.ui.t1.TicketForm;
import com.ccinfom.ui.t2.PickingForm;
import com.ccinfom.ui.t3.PackForm;
import com.ccinfom.ui.t4.DispatchForm;
import com.ccinfom.ui.t5.CloseForm;
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
 * Launcher entry point for all Swing forms.
 */
public final class MainApp {

    private MainApp() {
        // Utility class
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainApp::createAndShow);
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("CCINFOM Ewan ano magandang name Control Center");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        StatusPanel statusPanel = new StatusPanel();
        if (DbConnection.ping()) {
            statusPanel.setSuccess("DB Status: Connected");
        } else {
            statusPanel.setError("DB Status: Connection Failed");
        }

        JButton ticketButton = new JButton("Create Pick Ticket (T1)");
        ticketButton.addActionListener(e -> SwingUtilities.invokeLater(() -> new TicketForm().setVisible(true)));

        JButton pickingButton = new JButton("Start Picking (T2)");
        pickingButton.addActionListener(e -> SwingUtilities.invokeLater(() -> new PickingForm().setVisible(true)));

        JButton packButton = new JButton("Pack Boxes (T3)");
        packButton.addActionListener(e -> SwingUtilities.invokeLater(() -> new PackForm().setVisible(true)));

        JButton dispatchButton = new JButton("Dispatch Manifest (T4)");
        dispatchButton.addActionListener(e -> SwingUtilities.invokeLater(() -> new DispatchForm().setVisible(true)));

        JButton closeButton = new JButton("Close Ticket (T5)");
        closeButton.addActionListener(e -> SwingUtilities.invokeLater(() -> new CloseForm().setVisible(true)));

        JButton r1ReportButton = new JButton("R1 – Daily Outcomes");
        r1ReportButton.addActionListener(e -> ReportR1Form.showWindow());

        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 8, 8));
        buttonPanel.add(ticketButton);
        buttonPanel.add(pickingButton);
        buttonPanel.add(packButton);
        buttonPanel.add(dispatchButton);
        buttonPanel.add(closeButton);
        buttonPanel.add(r1ReportButton);

        JLabel helperText = new JLabel("Select a transaction or report to launch its Swing form.", JLabel.CENTER);

        frame.add(statusPanel, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.CENTER);
        frame.add(helperText, BorderLayout.SOUTH);

        frame.setPreferredSize(new Dimension(440, 320));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
