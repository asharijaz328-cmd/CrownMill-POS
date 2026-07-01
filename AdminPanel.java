import java.awt.*;
import java.nio.file.Paths;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class AdminPanel extends JFrame {
    private Connection dbConnection;
    private DefaultTableModel productTableModel;
    private JTable productTable;

    // Theme Colors (same as POS for consistency)
    private final Color COLOR_BG = new Color(0, 0, 0);
    private final Color COLOR_ACCENT = new Color(255, 215, 0);
    private final Color COLOR_TEXT = new Color(255, 255, 255);
    private final Color COLOR_CARD = new Color(26, 26, 26);

    public AdminPanel() {
        dbConnection = getDBConnection();
        setTitle("Crown Mill - Admin Panel");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_BG);

        initUI();
        loadProducts();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private Connection getDBConnection() {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            java.nio.file.Path dbPath = Paths.get("crownmill_db.db").toAbsolutePath();
            String url = "jdbc:sqlite:" + dbPath.toString();
            conn = DriverManager.getConnection(url);
        } catch (Exception e) {
            System.out.println("Admin Connection Error: " + e.getMessage());
        }
        return conn;
    }

    private void initUI() {
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("ADMIN PANEL - PRODUCT MANAGEMENT");
        titleLabel.setForeground(COLOR_ACCENT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        add(headerPanel, BorderLayout.NORTH);

        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(COLOR_BG);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Table
        String[] columns = {"ID", "Product Name", "Price", "Category", "Image Path", "Stock"};
        productTableModel = new DefaultTableModel(columns, 0);
        productTable = new JTable(productTableModel);
        productTable.setBackground(COLOR_CARD);
        productTable.setForeground(COLOR_TEXT);
        productTable.setFont(new Font("Arial", Font.PLAIN, 12));
        productTable.setSelectionBackground(COLOR_ACCENT);

        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setBackground(COLOR_BG);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setBackground(COLOR_BG);

        JButton addBtn = new JButton("Add Product");
        JButton editBtn = new JButton("Edit Product");
        JButton deleteBtn = new JButton("Delete Product");
        JButton refreshBtn = new JButton("Refresh");

        addBtn.setBackground(COLOR_ACCENT);
        addBtn.setForeground(COLOR_BG);
        addBtn.setFont(new Font("Arial", Font.BOLD, 12));

        editBtn.setBackground(COLOR_ACCENT);
        editBtn.setForeground(COLOR_BG);
        editBtn.setFont(new Font("Arial", Font.BOLD, 12));

        deleteBtn.setBackground(new Color(200, 0, 0));
        deleteBtn.setForeground(COLOR_TEXT);
        deleteBtn.setFont(new Font("Arial", Font.BOLD, 12));

        refreshBtn.setBackground(COLOR_CARD);
        refreshBtn.setForeground(COLOR_TEXT);
        refreshBtn.setFont(new Font("Arial", Font.BOLD, 12));

        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(refreshBtn);

        // Button Actions
        addBtn.addActionListener(e -> showAddProductDialog());
        editBtn.addActionListener(e -> showEditProductDialog());
        deleteBtn.addActionListener(e -> deleteProduct());
        refreshBtn.addActionListener(e -> loadProducts());

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.CENTER);
    }

    private void loadProducts() {
        productTableModel.setRowCount(0);
        try {
            String query = "SELECT id, name, price, category, image_path, COALESCE(stock, 0) as stock FROM products";
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getString("category"),
                    rs.getString("image_path"),
                    rs.getInt("stock")
                };
                productTableModel.addRow(row);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading products: " + e.getMessage());
        }
    }

    private void showAddProductDialog() {
        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(COLOR_BG);

        JLabel nameLabel = new JLabel("Product Name:");
        nameLabel.setForeground(COLOR_TEXT);
        JTextField nameField = new JTextField();

        JLabel priceLabel = new JLabel("Price:");
        priceLabel.setForeground(COLOR_TEXT);
        JTextField priceField = new JTextField();

        JLabel categoryLabel = new JLabel("Category:");
        categoryLabel.setForeground(COLOR_TEXT);
        JComboBox<String> categoryBox = new JComboBox<>(new String[]{
            "Tools", "Electronics", "Accessories"
        });

        JLabel imageLabel = new JLabel("Image Path:");
        imageLabel.setForeground(COLOR_TEXT);
        JTextField imageField = new JTextField();
        
        JLabel stockLabel = new JLabel("Stock Quantity:");
        stockLabel.setForeground(COLOR_TEXT);
        JTextField stockField = new JTextField("0");

        panel.add(nameLabel);
        panel.add(nameField);
        panel.add(priceLabel);
        panel.add(priceField);
        panel.add(categoryLabel);
        panel.add(categoryBox);
        panel.add(imageLabel);
        panel.add(imageField);
        panel.add(stockLabel);
        panel.add(stockField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Product", 
                                                   JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText();
                double price = Double.parseDouble(priceField.getText());
                String category = (String) categoryBox.getSelectedItem();
                String image = imageField.getText();
                int stock = Integer.parseInt(stockField.getText());

                String insertSQL = "INSERT INTO products (name, price, category, image_path, stock) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement pstmt = dbConnection.prepareStatement(insertSQL);
                pstmt.setString(1, name);
                pstmt.setDouble(2, price);
                pstmt.setString(3, category);
                pstmt.setString(4, image);
                pstmt.setInt(5, stock);
                pstmt.executeUpdate();
                pstmt.close();

                JOptionPane.showMessageDialog(this, "Product added successfully!");
                loadProducts();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void showEditProductDialog() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to edit");
            return;
        }

        int id = (Integer) productTableModel.getValueAt(selectedRow, 0);
        String name = (String) productTableModel.getValueAt(selectedRow, 1);
        double price = (Double) productTableModel.getValueAt(selectedRow, 2);
        String category = (String) productTableModel.getValueAt(selectedRow, 3);
        String image = (String) productTableModel.getValueAt(selectedRow, 4);
        int stock = (Integer) productTableModel.getValueAt(selectedRow, 5);

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel nameLabel = new JLabel("Product Name:");
        JTextField nameField = new JTextField(name);

        JLabel priceLabel = new JLabel("Price:");
        JTextField priceField = new JTextField(String.valueOf(price));

        JLabel categoryLabel = new JLabel("Category:");
        JComboBox<String> categoryBox = new JComboBox<>(new String[]{
            "Tools", "Electronics", "Accessories"
        });
        categoryBox.setSelectedItem(category);

        JLabel imageLabel = new JLabel("Image Path:");
        JTextField imageField = new JTextField(image);
        
        JLabel stockLabel = new JLabel("Stock Quantity:");
        JTextField stockField = new JTextField(String.valueOf(stock));

        panel.add(nameLabel);
        panel.add(nameField);
        panel.add(priceLabel);
        panel.add(priceField);
        panel.add(categoryLabel);
        panel.add(categoryBox);
        panel.add(imageLabel);
        panel.add(imageField);
        panel.add(stockLabel);
        panel.add(stockField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Product", 
                                                   JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String updateSQL = "UPDATE products SET name=?, price=?, category=?, image_path=?, stock=? WHERE id=?";
                PreparedStatement pstmt = dbConnection.prepareStatement(updateSQL);
                pstmt.setString(1, nameField.getText());
                pstmt.setDouble(2, Double.parseDouble(priceField.getText()));
                pstmt.setString(3, (String) categoryBox.getSelectedItem());
                pstmt.setString(4, imageField.getText());
                pstmt.setInt(5, Integer.parseInt(stockField.getText()));
                pstmt.setInt(6, id);
                pstmt.executeUpdate();
                pstmt.close();

                JOptionPane.showMessageDialog(this, "Product updated successfully!");
                loadProducts();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void deleteProduct() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to delete");
            return;
        }

        int id = (Integer) productTableModel.getValueAt(selectedRow, 0);
        String name = (String) productTableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this, 
                                                   "Delete '" + name + "'?", 
                                                   "Confirm Delete",
                                                   JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String deleteSQL = "DELETE FROM products WHERE id=?";
                PreparedStatement pstmt = dbConnection.prepareStatement(deleteSQL);
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
                pstmt.close();

                JOptionPane.showMessageDialog(this, "Product deleted successfully!");
                loadProducts();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminPanel());
    }
}
