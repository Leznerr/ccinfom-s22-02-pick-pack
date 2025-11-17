package com.ccinfom.ui.common;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Lightweight grouped bar chart for embedding in Swing reports without extra dependencies.
 * Supports multiple series and PNG export. Designed for small datasets (<= 50 categories).
 */
public class SimpleBarChartPanel extends JPanel {

    public static class Series {
        private final String name;
        private final List<Double> values;
        private final Color color;

        public Series(String name, List<Double> values, Color color) {
            this.name = name;
            this.values = values;
            this.color = color;
        }
    }

    private List<String> categories = Collections.emptyList();
    private List<Series> seriesList = Collections.emptyList();
    private String title = "";
    private String yLabel = "";

    public SimpleBarChartPanel() {
        setBackground(Color.WHITE);
    }

    public void setData(List<String> categories, List<Series> seriesList, String title, String yLabel) {
        this.categories = categories != null ? categories : Collections.emptyList();
        this.seriesList = seriesList != null ? seriesList : Collections.emptyList();
        this.title = title != null ? title : "";
        this.yLabel = yLabel != null ? yLabel : "";
        repaint();
    }

    public boolean hasData() {
        return !categories.isEmpty() && !seriesList.isEmpty();
    }

    public void saveAsPng(File file, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        paintComponent(g2, width, height);
        g2.dispose();
        ImageIO.write(img, "png", file);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintComponent((Graphics2D) g, getWidth(), getHeight());
    }

    private void paintComponent(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRect(0, 0, width, height);

        if (!hasData()) {
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            String msg = "No data to chart.";
            Rectangle2D str = g2.getFontMetrics().getStringBounds(msg, g2);
            g2.drawString(msg, (int) ((width - str.getWidth()) / 2), (int) ((height - str.getHeight()) / 2));
            return;
        }

        int topMargin = 40;
        int leftMargin = 60;
        int bottomMargin = 70;
        int rightMargin = 30;

        // Title
        if (!title.isBlank()) {
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            Rectangle2D str = g2.getFontMetrics().getStringBounds(title, g2);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(title, (int) ((width - str.getWidth()) / 2), 24);
            topMargin += 10;
        }

        // Compute max value
        double max = seriesList.stream()
                .flatMap(s -> s.values.stream())
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0d);
        if (max == 0) {
            max = 1d;
        }

        int plotW = Math.max(100, width - leftMargin - rightMargin);
        int plotH = Math.max(100, height - topMargin - bottomMargin);
        int plotX = leftMargin;
        int plotY = topMargin;

        // Axes
        g2.setColor(Color.GRAY);
        g2.drawLine(plotX, plotY + plotH, plotX + plotW, plotY + plotH); // x axis
        g2.drawLine(plotX, plotY, plotX, plotY + plotH); // y axis

        // Y label
        if (!yLabel.isBlank()) {
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(g2.getFont().deriveFont(11f));
            Rectangle2D lbl = g2.getFontMetrics().getStringBounds(yLabel, g2);
            g2.rotate(-Math.PI / 2, plotX - 35, plotY + plotH / 2d);
            g2.drawString(yLabel, (int) (plotX - 35 - lbl.getWidth() / 2), (int) (plotY + plotH / 2));
            g2.rotate(Math.PI / 2, plotX - 35, plotY + plotH / 2d);
        }

        int seriesCount = seriesList.size();
        int catCount = categories.size();
        if (catCount == 0 || seriesCount == 0) {
            return;
        }

        int groupWidth = plotW / Math.max(catCount, 1);
        int barWidth = Math.max(6, (int) ((groupWidth * 0.7) / seriesCount));

        FontMetrics fm = g2.getFontMetrics(g2.getFont().deriveFont(10f));

        for (int c = 0; c < catCount; c++) {
            int groupStart = plotX + c * groupWidth;
            String label = categories.get(c);
            int labelWidth = fm.stringWidth(label);
            // Optionally rotate if long
            if (labelWidth > groupWidth) {
                g2.setColor(Color.DARK_GRAY);
                g2.setFont(g2.getFont().deriveFont(9f));
                g2.rotate(-Math.PI / 4, groupStart + groupWidth / 2d, plotY + plotH + 15);
                g2.drawString(label, groupStart + groupWidth / 2 - labelWidth / 2, plotY + plotH + 15);
                g2.rotate(Math.PI / 4, groupStart + groupWidth / 2d, plotY + plotH + 15);
            } else {
                g2.setColor(Color.DARK_GRAY);
                g2.setFont(g2.getFont().deriveFont(10f));
                g2.drawString(label, groupStart + (groupWidth - labelWidth) / 2, plotY + plotH + 15);
            }

            for (int s = 0; s < seriesCount; s++) {
                Series series = seriesList.get(s);
                Double val = c < series.values.size() ? series.values.get(c) : 0d;
                if (val == null) val = 0d;
                int barHeight = (int) (plotH * (val / max));
                int x = groupStart + (int) (groupWidth * 0.15) + s * barWidth;
                int y = plotY + plotH - barHeight;
                g2.setColor(series.color);
                g2.fillRect(x, y, barWidth, barHeight);
            }
        }

        // Legend
        int legendX = plotX + 10;
        int legendY = plotY + 10;
        int legendH = 16;
        g2.setFont(g2.getFont().deriveFont(11f));
        for (Series s : seriesList) {
            g2.setColor(s.color);
            g2.fillRect(legendX, legendY - 10, 12, 12);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(legendX, legendY - 10, 12, 12);
            g2.drawString(s.name, legendX + 18, legendY);
            legendY += legendH;
        }

        // Horizontal grid lines (4 steps)
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(220, 220, 220));
        int ticks = 4;
        for (int i = 1; i <= ticks; i++) {
            int y = plotY + plotH - (plotH * i / ticks);
            g2.drawLine(plotX, y, plotX + plotW, y);
        }
    }

    public static Series series(String name, List<Double> values, Color color) {
        return new Series(name, new ArrayList<>(values), color);
    }
}
