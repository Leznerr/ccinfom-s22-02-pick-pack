package com.ccinfom.ui.common;

import javax.swing.*;
import java.awt.*;

/**
 * Small helper to keep the UI consistent (fonts, margins, spacing).
 */
public final class UiStyle {

    private UiStyle() {}

    /**
    * Apply a consistent font across common UI defaults.
    */
    public static void applyGlobalFont(Font font) {
        UIManager.put("Button.font", font);
        UIManager.put("Label.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("ComboBox.font", font);
        UIManager.put("Table.font", font.deriveFont(Font.PLAIN, font.getSize() - 2));
        UIManager.put("TableHeader.font", font);
        UIManager.put("Menu.font", font);
        UIManager.put("MenuItem.font", font);
        UIManager.put("TabbedPane.font", font);
        UIManager.put("ToolTip.font", font);
    }

    /**
    * Give buttons a uniform margin and minimum size.
    */
    public static void styleButton(AbstractButton btn) {
        btn.setMargin(new Insets(10, 16, 10, 16));
        btn.setFocusPainted(false);
        btn.setMinimumSize(new Dimension(140, 36));
    }

    /**
    * Simple padded panel wrapper to enforce consistent spacing.
    */
    public static JPanel paddedPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }
}
