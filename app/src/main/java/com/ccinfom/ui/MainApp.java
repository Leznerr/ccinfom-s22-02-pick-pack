package com.ccinfom.ui;

import com.ccinfom.config.DbConnection;
import com.ccinfom.ui.common.StatusPanel;
import com.ccinfom.ui.core.BranchForm;
import com.ccinfom.ui.core.CustomerForm;
import com.ccinfom.ui.core.EmployeeForm;
import com.ccinfom.ui.core.ProductForm;
import com.ccinfom.ui.core.VehicleForm;
import com.ccinfom.ui.report.ReportR1Form;
import com.ccinfom.ui.report.ReportR2Form;
import com.ccinfom.ui.report.ReportR4Form;
import com.ccinfom.ui.t1.TicketForm;
import com.ccinfom.ui.t2.PickingForm;
import com.ccinfom.ui.t3.PackForm;
import com.ccinfom.ui.t4.DispatchForm;
import com.ccinfom.ui.t5.CloseForm;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
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

        JButton coreBtn = createMainButton("Core Records", MainApp::showCoreDialog);
        JButton txBtn = createMainButton("Transactions", MainApp::showTransactionsDialog);
        JButton reportsBtn = createMainButton("Reports", MainApp::showReportsDialog);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 12, 12));
        buttonPanel.add(coreBtn);
        buttonPanel.add(txBtn);
        buttonPanel.add(reportsBtn);

        JLabel helperText = new JLabel("Choose a category to launch its actions.", JLabel.CENTER);

        frame.add(statusPanel, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.CENTER);
        frame.add(helperText, BorderLayout.SOUTH);

        frame.setPreferredSize(new Dimension(440, 320));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JButton createMainButton(String label, Runnable action) {
        JButton btn = new JButton(label);
        btn.setMargin(new java.awt.Insets(12, 18, 12, 18));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private static void showCoreDialog() {
        JDialog dialog = buildDialog("Core Records", new JButton[]{
                createActionButton("Products", () -> new ProductForm().setVisible(true)),
                createActionButton("Customers", () -> new CustomerForm().setVisible(true)),
                createActionButton("Branches", () -> new BranchForm().setVisible(true)),
                createActionButton("Employees", () -> new EmployeeForm().setVisible(true)),
                createActionButton("Vehicles", () -> new VehicleForm().setVisible(true))
        });
        dialog.setVisible(true);
    }

    private static void showTransactionsDialog() {
        JDialog dialog = buildDialog("Transactions", new JButton[]{
                createActionButton("T1 - Create Pick Ticket", () -> new TicketForm().setVisible(true)),
                createActionButton("T2 - Allocate & Pick", () -> new PickingForm().setVisible(true)),
                createActionButton("T3 - Pack & Box", () -> new PackForm().setVisible(true)),
                createActionButton("T4 - Dispatch", () -> new DispatchForm().setVisible(true)),
                createActionButton("T5 - Close Ticket", () -> new CloseForm().setVisible(true))
        });
        dialog.setVisible(true);
    }

    private static void showReportsDialog() {
        JDialog dialog = buildDialog("Reports", new JButton[]{
                createActionButton("R1 - Daily Outcomes", ReportR1Form::showWindow),
                createActionButton("R2 - Weekly Picker Productivity", ReportR2Form::showWindow),
                createActionButton("R4 - On-Time Delivery & PoD", ReportR4Form::showWindow)
        });
        dialog.setVisible(true);
    }

    private static JDialog buildDialog(String title, JButton[] buttons) {
        JDialog dialog = new JDialog((JFrame) null, title, true);
        dialog.setLayout(new BorderLayout(8, 8));
        JPanel grid = new JPanel(new GridLayout(0, 1, 8, 8));
        for (JButton b : buttons) {
            grid.add(b);
        }
        dialog.add(grid, BorderLayout.CENTER);
        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);
        dialog.add(south, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        return dialog;
    }

    private static JButton createActionButton(String label, Runnable action) {
        JButton btn = new JButton(label);
        btn.setMargin(new java.awt.Insets(10, 14, 10, 14));
        btn.addActionListener(e -> action.run());
        return btn;
    }
}

