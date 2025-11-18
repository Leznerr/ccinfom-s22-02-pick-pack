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
import com.ccinfom.ui.report.ReportR3Form;
import com.ccinfom.ui.report.ReportR4Form;
import com.ccinfom.ui.t1.TicketForm;
import com.ccinfom.ui.t2.PickingForm;
import com.ccinfom.ui.t3.PackForm;
import com.ccinfom.ui.t4.DispatchForm;
import com.ccinfom.ui.t5.CloseForm;
import com.ccinfom.ui.common.UiStyle;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * Launcher entry point for all Swing forms.
 */
public final class MainApp {

    private MainApp() {
        // Utility class
    }

    public static void main(String[] args) {
        // Try to use Nimbus for a cleaner, modern look; fall back silently.
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        // Apply a consistent font across the app.
        UiStyle.applyGlobalFont(new Font("Segoe UI", Font.PLAIN, 14));
        SwingUtilities.invokeLater(MainApp::createAndShow);
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("CCINFOM Control Center");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

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

        JPanel north = UiStyle.paddedPanel(new BorderLayout());
        north.add(statusPanel, BorderLayout.CENTER);
        frame.add(north, BorderLayout.NORTH);

        JPanel center = UiStyle.paddedPanel(new BorderLayout());
        center.add(buttonPanel, BorderLayout.CENTER);
        frame.add(center, BorderLayout.CENTER);

        JPanel south = UiStyle.paddedPanel(new BorderLayout());
        south.add(helperText, BorderLayout.CENTER);
        frame.add(south, BorderLayout.SOUTH);

        frame.setPreferredSize(new Dimension(480, 320));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void showCoreDialog() {
        JDialog dialog = buildDialog("Core Records", new ActionItem[]{
                new ActionItem("Products", () -> new ProductForm().setVisible(true)),
                new ActionItem("Customers", () -> new CustomerForm().setVisible(true)),
                new ActionItem("Branches", () -> new BranchForm().setVisible(true)),
                new ActionItem("Employees", () -> new EmployeeForm().setVisible(true)),
                new ActionItem("Vehicles", () -> new VehicleForm().setVisible(true))
        });
        dialog.setVisible(true);
    }

    private static void showTransactionsDialog() {
        JDialog dialog = buildDialog("Transactions", new ActionItem[]{
                new ActionItem("T1 - Create Pick Ticket", () -> new TicketForm().setVisible(true)),
                new ActionItem("T2 - Allocate & Pick", () -> new PickingForm().setVisible(true)),
                new ActionItem("T3 - Pack & Box", () -> new PackForm().setVisible(true)),
                new ActionItem("T4 - Dispatch", () -> new DispatchForm().setVisible(true)),
                new ActionItem("T5 - Close Ticket", () -> new CloseForm().setVisible(true))
        });
        dialog.setVisible(true);
    }

    private static void showReportsDialog() {
        JDialog dialog = buildDialog("Reports", new ActionItem[]{
                new ActionItem("R1 - Daily Outcomes", ReportR1Form::showWindow),
                new ActionItem("R2 - Weekly Picker Productivity", ReportR2Form::showWindow),
                new ActionItem("R3 - Monthly Inventory Throughput", ReportR3Form::showWindow),
                new ActionItem("R4 - On-Time Delivery & PoD", ReportR4Form::showWindow)
        });
        dialog.setVisible(true);
    }

    private static JButton createMainButton(String label, Runnable action) {
        JButton btn = new JButton(label);
        UiStyle.styleButton(btn);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private static JDialog buildDialog(String title, ActionItem[] items) {
        JDialog dialog = new JDialog((JFrame) null, title, true);
        dialog.setLayout(new BorderLayout(8, 8));
        JPanel grid = new JPanel(new GridLayout(0, 1, 8, 8));
        for (ActionItem item : items) {
            JButton button = new JButton(item.label);
            UiStyle.styleButton(button);
            button.addActionListener(e -> {
                dialog.dispose(); // close the launcher before opening the next window
                SwingUtilities.invokeLater(item.action);
            });
            grid.add(button);
        }
        dialog.add(grid, BorderLayout.CENTER);
        JButton close = new JButton("Close");
        UiStyle.styleButton(close);
        close.addActionListener(e -> dialog.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);
        dialog.add(south, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        return dialog;
    }

    private static final class ActionItem {
        private final String label;
        private final Runnable action;

        private ActionItem(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }
}
