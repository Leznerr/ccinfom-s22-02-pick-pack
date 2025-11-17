package com.ccinfom.ui.core;

import com.ccinfom.dao.impl.ProductDaoImpl;
import com.ccinfom.model.Product;
import com.ccinfom.service.ProductService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.impl.ProductServiceImpl;
import com.ccinfom.ui.common.StatusPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Swing form for managing Products (core CRUD).
 */
public class ProductForm extends JFrame {

    private final ProductService productService = new ProductServiceImpl(new ProductDaoImpl());
    private final ProductTableModel tableModel = new ProductTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(20);
    private final JCheckBox showInactive = new JCheckBox("Show inactive");
    private final StatusPanel statusPanel = new StatusPanel("Status: Ready");

    public ProductForm() {
        super("Products – Core Record Management");
        initLayout();
        loadProducts();
    }

    private void initLayout() {
        setSize(960, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(new EmptyBorder(8, 8, 0, 8));
        topPanel.add(new JLabel("Search:"));
        topPanel.add(searchField);
        JButton searchBtn = new JButton("Apply");
        searchBtn.addActionListener(e -> loadProducts());
        topPanel.add(searchBtn);
        showInactive.addActionListener(e -> loadProducts());
        topPanel.add(showInactive);
        add(topPanel, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Product");
        addBtn.addActionListener(e -> onAdd());
        JButton editBtn = new JButton("Edit");
        editBtn.addActionListener(e -> onEdit());
        JButton toggleBtn = new JButton("Toggle Active");
        toggleBtn.addActionListener(e -> onToggleActive());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadProducts());
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());

        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(toggleBtn);
        actions.add(refreshBtn);
        actions.add(closeBtn);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(new EmptyBorder(0, 8, 8, 8));
        bottom.add(statusPanel, BorderLayout.CENTER);
        bottom.add(actions, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    private void onAdd() {
        ProductDialog dialog = new ProductDialog(this, "Add Product", null);
        dialog.setVisible(true);
        dialog.getResult().ifPresent(product -> {
            try {
                productService.createProduct(product, "ui");
                statusPanel.setSuccess("Product created.");
                loadProducts();
            } catch (SQLException | ValidationException ex) {
                statusPanel.setError("Create failed: " + ex.getMessage());
            }
        });
    }

    private void onEdit() {
        Product selected = getSelectedProduct();
        if (selected == null) {
            statusPanel.setInfo("Select a product to edit.");
            return;
        }
        ProductDialog dialog = new ProductDialog(this, "Edit Product", selected);
        dialog.setVisible(true);
        dialog.getResult().ifPresent(product -> {
            product.setProductId(selected.getProductId());
            product.setActiveFlag(selected.isActiveFlag());
            try {
                productService.updateProduct(product, "ui");
                statusPanel.setSuccess("Product updated.");
                loadProducts();
            } catch (SQLException | ValidationException ex) {
                statusPanel.setError("Update failed: " + ex.getMessage());
            }
        });
    }

    private void onToggleActive() {
        Product selected = getSelectedProduct();
        if (selected == null) {
            statusPanel.setInfo("Select a product to activate/deactivate.");
            return;
        }
        boolean newState = !selected.isActiveFlag();
        int confirm = JOptionPane.showConfirmDialog(
                this,
                String.format("Are you sure you want to %s product %s?",
                        newState ? "activate" : "deactivate",
                        selected.getSku()),
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            productService.setProductActive(selected.getProductId(), newState, "ui");
            statusPanel.setSuccess("Product status updated.");
            loadProducts();
        } catch (SQLException | ValidationException ex) {
            statusPanel.setError("Status change failed: " + ex.getMessage());
        }
    }

    private void loadProducts() {
        try {
            List<Product> products = productService.listProducts(searchField.getText(), showInactive.isSelected());
            tableModel.setRows(products);
            statusPanel.setSuccess(String.format("Loaded %d product(s).", products.size()));
        } catch (SQLException ex) {
            statusPanel.setError("Failed to load products: " + ex.getMessage());
        }
    }

    private Product getSelectedProduct() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return tableModel.getProductAt(modelRow);
    }

    private static class ProductTableModel extends AbstractTableModel {
        private final List<Product> rows = new ArrayList<>();
        private final String[] columns = {
                "SKU", "Name", "Category", "Unit Price",
                "On Hand", "Reserved", "Status", "Updated", "Updated By"
        };
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        public void setRows(List<Product> products) {
            rows.clear();
            rows.addAll(products);
            fireTableDataChanged();
        }

        public Product getProductAt(int row) {
            return rows.get(row);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Product p = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> p.getSku();
                case 1 -> p.getProductName();
                case 2 -> p.getCategory();
                case 3 -> p.getUnitPrice();
                case 4 -> p.getOnHandQty();
                case 5 -> p.getReservedQty();
                case 6 -> p.isActiveFlag() ? "Active" : "Inactive";
                case 7 -> p.getUpdatedAt() != null ? formatter.format(p.getUpdatedAt()) : "";
                case 8 -> p.getUpdatedBy();
                default -> "";
            };
        }
    }

    private static class ProductDialog extends JDialog {
        private final JTextField skuField = new JTextField(20);
        private final JTextField nameField = new JTextField(25);
        private final JTextField categoryField = new JTextField(20);
        private final JTextField priceField = new JTextField(10);
        private final JTextField uomField = new JTextField(10);
        private final JTextField onHandField = new JTextField(8);
        private final JTextField reservedField = new JTextField(8);
        private Optional<Product> result = Optional.empty();

        public ProductDialog(Frame owner, String title, Product existing) {
            super(owner, title, true);
            initLayout();
            if (existing != null) {
                skuField.setText(existing.getSku());
                nameField.setText(existing.getProductName());
                categoryField.setText(existing.getCategory());
                priceField.setText(existing.getUnitPrice() != null ? existing.getUnitPrice().toPlainString() : "");
                uomField.setText(existing.getUnitOfMeasure());
                onHandField.setText(existing.getOnHandQty() != null ? existing.getOnHandQty().toPlainString() : "");
                reservedField.setText(existing.getReservedQty() != null ? existing.getReservedQty().toPlainString() : "");
            }
            pack();
            setLocationRelativeTo(owner);
        }

        private void initLayout() {
            setLayout(new BorderLayout());
            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.setBorder(new EmptyBorder(12, 12, 12, 12));
            form.add(new JLabel("SKU"));
            form.add(skuField);
            form.add(new JLabel("Name"));
            form.add(nameField);
            form.add(new JLabel("Category"));
            form.add(categoryField);
            form.add(new JLabel("Unit Price"));
            form.add(priceField);
            form.add(new JLabel("Unit of Measure"));
            form.add(uomField);
            form.add(new JLabel("On Hand Qty"));
            form.add(onHandField);
            form.add(new JLabel("Reserved Qty"));
            form.add(reservedField);
            add(form, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton save = new JButton("Save");
            save.addActionListener(e -> onSave());
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(e -> dispose());
            buttons.add(save);
            buttons.add(cancel);
            add(buttons, BorderLayout.SOUTH);
        }

        private void onSave() {
            Product product = new Product();
            product.setSku(skuField.getText());
            product.setProductName(nameField.getText());
            product.setCategory(categoryField.getText());
            product.setUnitPrice(parseDecimal(priceField.getText()));
            product.setUnitOfMeasure(uomField.getText());
            product.setOnHandQty(parseDecimal(onHandField.getText()));
            product.setReservedQty(parseDecimal(reservedField.getText()));
            product.setActiveFlag(true);
            result = Optional.of(product);
            dispose();
        }

        private BigDecimal parseDecimal(String value) {
            if (value == null || value.isBlank()) {
                return BigDecimal.ZERO;
            }
            try {
                return new BigDecimal(value.trim());
            } catch (NumberFormatException ex) {
                return BigDecimal.ZERO;
            }
        }

        public Optional<Product> getResult() {
            return result;
        }
    }
}

