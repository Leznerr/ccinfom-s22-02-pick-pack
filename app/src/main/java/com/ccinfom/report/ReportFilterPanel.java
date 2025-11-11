package com.ccinfom.report;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ListCellRenderer;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.EnumSet;
import java.util.Locale;

/**
 * Shared panel for report filters (Stage 1 helper).
 * Provides:
 *  - Year dropdown (always visible)
 *  - Month vs ISO-week toggle with corresponding selectors
 *  - Placeholder panel for optional drill-down filters (branch, customer, picker, etc.)
 *
 * Report-specific forms can embed this panel, call the getters, and add extra controls
 * via {@link #addFilterField(String, JComponent)} when needed.
 */
public class ReportFilterPanel extends JPanel {

    public enum PeriodMode {
        MONTH,
        ISO_WEEK
    }

    private final JComboBox<Integer> yearCombo;
    private final JComboBox<MonthOption> monthCombo;
    private final JComboBox<WeekOption> isoWeekCombo;
    private final JRadioButton monthlyRadio;
    private final JRadioButton isoWeekRadio;
    private final JPanel extraFiltersPanel;
    private int extraFilterRow = 0;
    private EnumSet<PeriodMode> supportedModes = EnumSet.of(PeriodMode.MONTH, PeriodMode.ISO_WEEK);

    public ReportFilterPanel() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Filters"));

        yearCombo = new JComboBox<>(buildYearModel());
        monthCombo = new JComboBox<>(buildMonthModel());
        isoWeekCombo = new JComboBox<>(buildIsoWeekModel());

        monthCombo.setRenderer(monthRenderer());
        isoWeekCombo.setRenderer(weekRenderer());

        monthlyRadio = new JRadioButton("Month");
        isoWeekRadio = new JRadioButton("ISO Week");
        ButtonGroup group = new ButtonGroup();
        group.add(monthlyRadio);
        group.add(isoWeekRadio);

        monthlyRadio.addActionListener(e -> setPeriodMode(PeriodMode.MONTH));
        isoWeekRadio.addActionListener(e -> setPeriodMode(PeriodMode.ISO_WEEK));

        LocalDate today = LocalDate.now();
        selectYear(today.getYear());
        setSelectedMonth(today.getMonthValue());
        setSelectedIsoWeek(today.get(WeekFields.ISO.weekOfWeekBasedYear()));

        monthlyRadio.setSelected(true);
        isoWeekRadio.setSelected(false);
        applyModeState();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        add(new JLabel("Year"), gbc);
        gbc.gridx = 1;
        add(yearCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(monthlyRadio, gbc);
        gbc.gridx = 1;
        add(monthCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        add(isoWeekRadio, gbc);
        gbc.gridx = 1;
        add(isoWeekCombo, gbc);

        extraFiltersPanel = new JPanel(new GridBagLayout());
        extraFiltersPanel.setBorder(BorderFactory.createTitledBorder("Additional Filters"));

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        add(extraFiltersPanel, gbc);

        // Default supported modes: both. Consumers can narrow via setSupportedModes().
        setPeriodMode(PeriodMode.MONTH);
    }

    /**
     * Restricts which period modes are available (e.g., R3 only needs MONTH).
     */
    public void setSupportedModes(EnumSet<PeriodMode> modes) {
        if (modes == null || modes.isEmpty()) {
            throw new IllegalArgumentException("At least one period mode must be enabled.");
        }
        supportedModes = EnumSet.copyOf(modes);
        monthlyRadio.setVisible(supportedModes.contains(PeriodMode.MONTH));
        monthCombo.setVisible(supportedModes.contains(PeriodMode.MONTH));
        isoWeekRadio.setVisible(supportedModes.contains(PeriodMode.ISO_WEEK));
        isoWeekCombo.setVisible(supportedModes.contains(PeriodMode.ISO_WEEK));

        if (!supportedModes.contains(getPeriodMode())) {
            setPeriodMode(supportedModes.iterator().next());
        } else {
            applyModeState();
        }
    }

    public EnumSet<PeriodMode> getSupportedModes() {
        return EnumSet.copyOf(supportedModes);
    }

    public PeriodMode getPeriodMode() {
        return isoWeekRadio.isSelected() ? PeriodMode.ISO_WEEK : PeriodMode.MONTH;
    }

    public void setPeriodMode(PeriodMode mode) {
        if (mode == PeriodMode.ISO_WEEK && !supportedModes.contains(PeriodMode.ISO_WEEK)) {
            throw new IllegalStateException("ISO week mode not enabled.");
        }
        if (mode == PeriodMode.MONTH && !supportedModes.contains(PeriodMode.MONTH)) {
            throw new IllegalStateException("Month mode not enabled.");
        }
        if (mode == PeriodMode.ISO_WEEK) {
            isoWeekRadio.setSelected(true);
        } else {
            monthlyRadio.setSelected(true);
        }
        applyModeState();
    }

    private void applyModeState() {
        boolean monthMode = getPeriodMode() == PeriodMode.MONTH;
        monthCombo.setEnabled(monthMode);
        isoWeekCombo.setEnabled(!monthMode);
        if (!monthMode) {
            // Align year with ISO week selection when possible.
            WeekFields iso = WeekFields.ISO;
            LocalDate today = LocalDate.now();
            int isoYear = today.get(iso.weekBasedYear());
            ensureYearOption(isoYear);
        }
    }

    public int getSelectedYear() {
        Integer value = (Integer) yearCombo.getSelectedItem();
        if (value == null) {
            value = LocalDate.now().getYear();
        }
        return value;
    }

    public void setSelectedYear(int year) {
        selectYear(year);
    }

    public int getSelectedMonth() {
        MonthOption option = (MonthOption) monthCombo.getSelectedItem();
        return option != null ? option.getValue() : LocalDate.now().getMonthValue();
    }

    public void setSelectedMonth(int month) {
        setSelectedMonthInternal(month);
    }

    public int getSelectedIsoWeek() {
        WeekOption option = (WeekOption) isoWeekCombo.getSelectedItem();
        return option != null ? option.getValue() : LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
    }

    public void setSelectedIsoWeek(int isoWeek) {
        setSelectedIsoWeekInternal(isoWeek);
    }

    /**
     * Allows report-specific forms to attach custom filters (branch, customer, etc.).
     */
    public void addFilterField(String label, JComponent component) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = extraFilterRow;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);
        extraFiltersPanel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        extraFiltersPanel.add(component, gbc);

        extraFilterRow++;
        revalidate();
        repaint();
    }

    public JPanel getExtraFiltersPanel() {
        return extraFiltersPanel;
    }

    /**
     * Resets the panel to today's period (month or ISO week depending on the current mode).
     */
    public void resetToCurrentPeriod() {
        LocalDate today = LocalDate.now();
        selectYear(today.getYear());
        setSelectedMonthInternal(today.getMonthValue());
        setSelectedIsoWeekInternal(today.get(WeekFields.ISO.weekOfWeekBasedYear()));
        if (getPeriodMode() == PeriodMode.ISO_WEEK) {
            selectYear(today.get(WeekFields.ISO.weekBasedYear()));
        }
    }

    private DefaultComboBoxModel<Integer> buildYearModel() {
        DefaultComboBoxModel<Integer> model = new DefaultComboBoxModel<>();
        int currentYear = LocalDate.now().getYear();
        int startYear = Math.min(2023, currentYear - 1);
        int endYear = currentYear + 5;
        for (int year = startYear; year <= endYear; year++) {
            model.addElement(year);
        }
        return model;
    }

    private DefaultComboBoxModel<MonthOption> buildMonthModel() {
        DefaultComboBoxModel<MonthOption> model = new DefaultComboBoxModel<>();
        for (Month month : Month.values()) {
            model.addElement(new MonthOption(month.getValue(), month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)));
        }
        return model;
    }

    private DefaultComboBoxModel<WeekOption> buildIsoWeekModel() {
        DefaultComboBoxModel<WeekOption> model = new DefaultComboBoxModel<>();
        for (int week = 1; week <= 53; week++) {
            model.addElement(new WeekOption(week));
        }
        return model;
    }

    private ListCellRenderer<MonthOption> monthRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof MonthOption option) {
                    setText(option.getDisplayLabel());
                }
                return this;
            }
        };
    }

    private ListCellRenderer<WeekOption> weekRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof WeekOption option) {
                    setText(option.getDisplayLabel());
                }
                return this;
            }
        };
    }

    private void selectYear(int year) {
        ensureYearOption(year);
        yearCombo.setSelectedItem(year);
    }

    private void ensureYearOption(int year) {
        DefaultComboBoxModel<Integer> model = (DefaultComboBoxModel<Integer>) yearCombo.getModel();
        boolean exists = false;
        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i) == year) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            model.addElement(year);
        }
    }

    private void setSelectedMonthInternal(int month) {
        for (int i = 0; i < monthCombo.getItemCount(); i++) {
            MonthOption option = monthCombo.getItemAt(i);
            if (option.getValue() == month) {
                monthCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void setSelectedIsoWeekInternal(int isoWeek) {
        isoWeek = Math.max(1, Math.min(53, isoWeek));
        for (int i = 0; i < isoWeekCombo.getItemCount(); i++) {
            WeekOption option = isoWeekCombo.getItemAt(i);
            if (option.getValue() == isoWeek) {
                isoWeekCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private static class MonthOption {
        private final int monthValue;
        private final String displayLabel;

        private MonthOption(int monthValue, String displayLabel) {
            this.monthValue = monthValue;
            this.displayLabel = displayLabel;
        }

        private int getValue() {
            return monthValue;
        }

        private String getDisplayLabel() {
            return displayLabel;
        }

        @Override
        public String toString() {
            return displayLabel;
        }
    }

    private static class WeekOption {
        private final int weekValue;
        private final String displayLabel;

        private WeekOption(int weekValue) {
            this.weekValue = weekValue;
            this.displayLabel = String.format("Week %02d", weekValue);
        }

        private int getValue() {
            return weekValue;
        }

        private String getDisplayLabel() {
            return displayLabel;
        }

        @Override
        public String toString() {
            return displayLabel;
        }
    }
}
