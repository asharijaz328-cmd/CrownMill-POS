import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class POSUI extends JFrame {

    // Theme Colors
    private final Color COLOR_BG = new Color(0, 0, 0);
    private final Color COLOR_ACCENT = new Color(255, 215, 0);
    private final Color COLOR_ACCENT_HOVER = new Color(204, 172, 0);
    private final Color COLOR_TEXT = new Color(255, 255, 255);
    private final Color COLOR_CARD = new Color(26, 26, 26);

    private List<Product> allProducts;
    private CartManager cartManager;
    private Connection dbConnection;
    private boolean dbConnectionFromFile = false;
    
    private JLabel BarcodeLabel; // Added for barcode display
    private JPanel productsPanel;
    private DefaultTableModel cartTableModel;
    private JTable cartTable;
    private JLabel totalLabel;
    private JLabel timeLabel;
    private JLabel subTotalLabel;
    private JLabel taxLabel;
    private JTextField discountField;
    private JTextField searchField;
    private JComboBox<String> categoryBox;

    public POSUI() {
        dbConnection = getDBConnection();
        initDatabase();
        setTitle("Crown Mill Store POS");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_BG);

        cartManager = new CartManager();
        loadProductsFromDatabase();

        initUI();
        startClock();
        setLocationRelativeTo(null); // Center screen
    }
private Connection getDBConnection() {
    Connection conn = null;
    try {
        Class.forName("org.sqlite.JDBC");
        
        // Use the app folder where this class/jar is running from
        java.nio.file.Path appFolder = java.nio.file.Paths.get(POSUI.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        java.nio.file.Path dbPath = appFolder.resolve("crownmill_db.db");
        String url = "jdbc:sqlite:" + dbPath.toString();
        
        System.out.println(">>> Attempting to connect to: " + url);
        System.out.println(">>> Database file exists: " + java.nio.file.Files.exists(dbPath));
        conn = DriverManager.getConnection(url);
        System.out.println(">>> STEP 1 SUCCESS: Database connected!");
        
        // Test if database is actually valid by executing a simple PRAGMA
        try {
            Statement testStmt = conn.createStatement();
            testStmt.execute("PRAGMA schema_version");
            testStmt.close();
            dbConnectionFromFile = true;
            System.out.println(">>> STEP 1 VERIFIED: Database file is valid!");
        System.out.println(">>> DB loaded from file: " + dbConnectionFromFile);
            return conn; // Return file-based connection
        } catch (SQLException e) {
            System.out.println(">>> WARNING: File-based database test failed: " + e.getMessage());
            conn.close();
            conn = null;
        }
        
        // If file-based failed, use in-memory as fallback
        System.out.println(">>> FALLBACK: Using in-memory database");
        url = "jdbc:sqlite::memory:";
        conn = DriverManager.getConnection(url);
        System.out.println(">>> STEP 1 SUCCESS: Using in-memory database!");
        
    } catch (ClassNotFoundException e) {
        System.out.println(">>> STEP 1 ERROR: SQLite driver missing - " + e.getMessage());
    } catch (SQLException e) {
        System.out.println(">>> STEP 1 SQL ERROR: " + e.getMessage());
    } catch (Exception e) {
        System.out.println(">>> STEP 1 ERROR: " + e.getMessage());
    }
    return conn;
}

private void initDatabase() {
    try {
        if (dbConnection == null) return;
        
        String createProductsTable = "CREATE TABLE IF NOT EXISTS products (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name TEXT NOT NULL," +
            "price REAL NOT NULL," +
            "image_path TEXT," +
            "category TEXT," +
            "stock INTEGER DEFAULT 0" +
            ")";
        
        String createSalesTable = "CREATE TABLE IF NOT EXISTS sales (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "item_name TEXT NOT NULL," +
            "quantity INTEGER NOT NULL," +
            "price REAL NOT NULL," +
            "total REAL NOT NULL," +
            "discount REAL," +
            "tax REAL," +
            "sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";
        
        Statement stmt = dbConnection.createStatement();
        stmt.execute(createProductsTable);
        stmt.execute(createSalesTable);
        stmt.close();
        
        System.out.println(">>> STEP 2 SUCCESS: Database tables initialized!");
        
        // Check if products table is empty, if yes insert dummy data
        String checkProducts = "SELECT COUNT(*) as count FROM products";
        Statement checkStmt = dbConnection.createStatement();
        ResultSet rs = checkStmt.executeQuery(checkProducts);
        rs.next();
        int productCount = rs.getInt("count");
        rs.close();
        checkStmt.close();
        
        if (productCount == 0) {
            insertDummyProducts();
        }
    } catch (SQLException e) {
        System.out.println(">>> STEP 2 ERROR: " + e.getMessage());
    }
}

private void insertDummyProducts() {
    try {
        // Use PreparedStatement to avoid issues with special characters
        String insertSQL = "INSERT INTO products ( name, price, image_path, category, stock) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement pstmt = dbConnection.prepareStatement(insertSQL);

        // Product data - categories must match UI dropdown
        Object[][] products = {
            {"Hyundai Electric Blower", 5600.0, "assets/blower.png", "Tools", 10},
            {"Bosch Heavy Duty Drill", 15000.0, "assets/drill.png", "Tools", 5},
            {"Forward Grinder", 8000.0, "assets/grinder.png", "Tools", 8},
            {"Smart Watch", 41000.0, "assets/watch.png", "Electronics", 3},
            {"Samsung S25 Ultra", 370000.0, "assets/s25.png", "Electronics", 2},
            {"MacBook Pro M3", 280000.0, "assets/mac.png", "Electronics", 1},
            {"Safety Gloves", 1500.0, "assets/gloves.png", "Accessories", 20},
            {"Safety Helmet", 1200.0, "assets/helmet.png", "Accessories", 15},
            {"Apple Airpods", 32000.0, "assets/pods.png", "Accessories", 6}
        };

        for (Object[] product : products) {
            pstmt.setString(1, (String) product[0]);
            pstmt.setDouble(2, (Double) product[1]);
            pstmt.setString(3, (String) product[2]);
            pstmt.setString(4, (String) product[3]);
            pstmt.setInt(5, (Integer) product[4]);
            pstmt.executeUpdate();
        }

        pstmt.close();
        System.out.println(">>> Dummy products inserted into database!");
    } catch (SQLException e) {
        System.out.println(">>> ERROR inserting dummy products: " + e.getMessage());
    }
}

private void loadProductsFromDatabase() {
    allProducts = new ArrayList<>();
    try {
        if (dbConnection == null) {
            System.out.println(">>> WARNING: Database connection is null, loading dummy products");
            loadDummyProducts();
            return;
        }
        
        String query = "SELECT id, name, price, image_path, category, COALESCE(stock, 0) as stock FROM products";
        Statement stmt = dbConnection.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        
        while (rs.next()) {
           
            String name = rs.getString("name");
            double price = rs.getDouble("price");
            String imagePath = rs.getString("image_path");
            String category = rs.getString("category");
            int stock = rs.getInt("stock");
            

            allProducts.add(new Product( name, price, imagePath, category, stock));
        }
        
        rs.close();
        stmt.close();
        
        System.out.println(">>> STEP 3 SUCCESS: Loaded " + allProducts.size() + " products from database!");
    } catch (SQLException e) {
        System.out.println(">>> STEP 3 ERROR loading products: " + e.getMessage());
        loadDummyProducts();
    }
}


private void loadDummyProducts() {
    allProducts = new ArrayList<>();
    allProducts.add(new Product("Hyundai Electric Blower", 5600, "assets/blower.png", "Tools \uD83D\uDEE0\uFE0F", 10));
    allProducts.add(new Product("Bosch Heavy Duty Drill", 15000, "assets/drill.png", "Tools \uD83D\uDEE0\uFE0F", 5));
    allProducts.add(new Product("Forward Grinder", 8000, "assets/grinder.png", "Tools ", 8));
    allProducts.add(new Product("Smart Watch", 41000, "assets/watch.png", "Electronics ", 3));
    allProducts.add(new Product("Samsung S25 Ultra", 370000, "assets/s25.png", "Electronics ", 2));
    allProducts.add(new Product("MacBook Pro M3", 280000, "assets/mac.png", "Electronics ", 1));
    allProducts.add(new Product("Safety Gloves", 1500, "assets/gloves.png", "Accessories \uD83C\uDFA7", 20));
    allProducts.add(new Product("Safety Helmet", 1200, "assets/helmet.png", "Accessories \uD83C\uDFA7", 15));
    allProducts.add(new Product("Apple Airpods", 32000, "assets/pods.png", "Accessories ", 6));
}

    private void initUI() {
        // --- TOP HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_BG);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("CROWN MILL STORE POS");
        titleLabel.setForeground(COLOR_ACCENT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));

// Barcode Label ki setting
 BarcodeLabel = new JLabel("Scan Item", SwingConstants.CENTER);
BarcodeLabel.setOpaque(true);
BarcodeLabel.setBackground(Color.WHITE); // Taake barcode saaf nazar aaye
BarcodeLabel.setPreferredSize(new Dimension(150, 60));
BarcodeLabel.setBorder(BorderFactory.createLineBorder(COLOR_ACCENT, 1));

        timeLabel = new JLabel();
        timeLabel.setForeground(COLOR_TEXT);
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        JButton adminBtn = new JButton("Admin Panel");
        adminBtn.setBackground(COLOR_ACCENT);
        adminBtn.setForeground(COLOR_BG);
        adminBtn.setFont(new Font("Arial", Font.BOLD, 12));
        adminBtn.setFocusPainted(false);
        adminBtn.setBorderPainted(false);
        adminBtn.setOpaque(true);
        adminBtn.setContentAreaFilled(true);
        adminBtn.addActionListener(e -> openAdminPanel());

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHeader.setOpaque(false);
        rightHeader.add(BarcodeLabel);
        rightHeader.add(adminBtn);
        rightHeader.add(timeLabel);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(rightHeader, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // --- MAIN SPLIT LAYOUT ---
        JPanel mainPanel = new JPanel(new BorderLayout(10, 0));
        mainPanel.setBackground(COLOR_BG);
        mainPanel.setBorder(new EmptyBorder(0, 10, 10, 10));

        // LEFT PANEL (Products & Search)
        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setBackground(COLOR_BG);

        // Filters (Search & Category)
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBackground(COLOR_BG);
        
        JTextField searchField = new JTextField(20);
        searchField.setBackground(COLOR_CARD);
        searchField.setForeground(COLOR_TEXT);
        searchField.setCaretColor(COLOR_TEXT);
        searchField.setBorder(BorderFactory.createLineBorder(COLOR_ACCENT));

        // Scanner/Search Field ka logic
searchField.addActionListener(e -> {
    String query = searchField.getText().trim();
    if (!query.isEmpty()) {
        boolean productFound = false;
        
        // Hamare paas jo allProducts list hai us mein dhoondo
        for (Product p : allProducts) {
            // Agar naam ya ID match ho jaye (Scanner aksar ID ya Name deta hai)
            if (p.getName().equalsIgnoreCase(query)) {
                cartManager.addProduct(p); // Cart mein item daal do
                updateCartTable();         // Right side table update karo
                productFound = true;
                break;
            }
        }

        if (productFound) {
            searchField.setText(""); // Mil gaya to field saaf kar do aglay scan ke liye
        } else {
            // Agar product nahi milta to user ko pata chalay
            javax.swing.JOptionPane.showMessageDialog(this, "Product nahi mila: " + query);
            searchField.selectAll(); 
        }
    }
});

        categoryBox = new JComboBox<>(new String[]{"All Categories", "Electronics", "Tools", "Accessories"});
        categoryBox.setBackground(COLOR_CARD);
        categoryBox.setForeground(COLOR_TEXT);

        filterPanel.add(new JLabel("<html><font color='white'>Search:</font></html>"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("<html><font color='white'>Category:</font></html>"));
        filterPanel.add(categoryBox);

        // Products Grid
        productsPanel = new JPanel(new GridLayout(0, 3, 15, 15));
        productsPanel.setBackground(COLOR_BG);
        JScrollPane scrollPane = new JScrollPane(productsPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(COLOR_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        leftPanel.add(filterPanel, BorderLayout.NORTH);
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        // RIGHT PANEL (Cart)
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setBackground(COLOR_BG);
        rightPanel.setPreferredSize(new Dimension(400, 0));
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_CARD, 2),
                new EmptyBorder(10, 10, 10, 10)
        ));

        // Cart Table
        String[] columnNames = {"Name", "Qty", "Price", "Total"};
        cartTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        cartTable = new JTable(cartTableModel);
        cartTable.setBackground(COLOR_CARD);
        cartTable.setForeground(COLOR_TEXT);
        cartTable.setGridColor(COLOR_BG);
        cartTable.setRowHeight(30);
        
        // Columns ki width set karna taake Name aur Qty mein gap aaye
        cartTable.getColumnModel().getColumn(0).setPreferredWidth(180); // Name column
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(50);  // Qty column
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Price column
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Total column

        // Qty aur Prices ko center mein align karne ke liye renderer
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        
        cartTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Qty Center
        cartTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Price Center
        cartTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Total Center
        JTableHeader tableHeader = cartTable.getTableHeader();
        tableHeader.setBackground(COLOR_BG);
        tableHeader.setForeground(COLOR_ACCENT);
        tableHeader.setFont(new Font("Arial", Font.BOLD, 14));
        
        JScrollPane tableScroll = new JScrollPane(cartTable);
        tableScroll.getViewport().setBackground(COLOR_CARD);
        tableScroll.setBorder(BorderFactory.createLineBorder(COLOR_CARD));

    // Updated Checkout Section with Tax & Discount
        JPanel checkoutPanel = new JPanel(new GridLayout(5, 1, 0, 5));
        checkoutPanel.setBackground(COLOR_BG);

        subTotalLabel = new JLabel("Subtotal: Rs 0.0", SwingConstants.RIGHT);
        subTotalLabel.setForeground(COLOR_TEXT);

        taxLabel = new JLabel("GST (16%): Rs 0.0", SwingConstants.RIGHT);
        taxLabel.setForeground(COLOR_TEXT);

        // Discount Input
        JPanel discPanel = new JPanel(new BorderLayout());
        discPanel.setBackground(COLOR_BG);
        JLabel dText = new JLabel("Discount %: ");
        dText.setForeground(COLOR_TEXT);
        discountField = new JTextField("0", 5);
        discountField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { updateCartUI(); }
        });
        discPanel.add(dText, BorderLayout.WEST);
        discPanel.add(discountField, BorderLayout.CENTER);

        totalLabel = new JLabel("Total: Rs 0.0", SwingConstants.RIGHT);
        totalLabel.setForeground(COLOR_ACCENT);
        totalLabel.setFont(new Font("Arial", Font.BOLD, 24));

        JButton checkoutBtn = new JButton("COMPLETE SALE");
        styleButton(checkoutBtn);
        checkoutBtn.setFont(new Font("Arial", Font.BOLD, 18));
        checkoutBtn.addActionListener(e -> processCheckout());

        // Panel mein add karna (Order zaroori hai)
        checkoutPanel.add(subTotalLabel);
        checkoutPanel.add(taxLabel);
        checkoutPanel.add(discPanel);
        checkoutPanel.add(totalLabel);
        checkoutPanel.add(checkoutBtn);

        rightPanel.add(new JLabel("<html><h2 style='color:#FFD700;'>Shopping Cart</h2></html>"), BorderLayout.NORTH);
        rightPanel.add(tableScroll, BorderLayout.CENTER);
        rightPanel.add(checkoutPanel, BorderLayout.SOUTH);

        mainPanel.add(leftPanel, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);
        add(mainPanel, BorderLayout.CENTER);

        // Listeners for Filters
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                filterProducts(searchField.getText(), categoryBox.getSelectedItem().toString());
            }
        });
        
        categoryBox.addActionListener(e -> 
            filterProducts(searchField.getText(), categoryBox.getSelectedItem().toString())
        );

        // Initial Load
        renderProducts(allProducts);
    }

    private void filterProducts(String query, String category) {
        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            boolean matchCategory = category.equals("All Categories") || p.getCategory().equals(category);
            boolean matchQuery = p.getName().toLowerCase().contains(query.toLowerCase());
            
            if (matchCategory && matchQuery) {
                filtered.add(p);
            }
        }
        renderProducts(filtered);
    }
    
    private void updateProductDisplay() {
        // Refresh the current view with updated stock
        if (searchField == null || categoryBox == null) {
            // If fields not initialized, just reload all products
            renderProducts(allProducts);
            return;
        }
        String currentQuery = searchField.getText();
        String currentCategory = categoryBox.getSelectedItem().toString();
        filterProducts(currentQuery, currentCategory);
    }

    private void renderProducts(List<Product> products) {
        productsPanel.removeAll();
        for (Product p : products) {
            productsPanel.add(createProductCard(p));
        }
        productsPanel.revalidate();
        productsPanel.repaint();
    }

    private JPanel createProductCard(Product product) {
        JPanel card = new JPanel(new BorderLayout(0, 5));
        card.setBackground(COLOR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_ACCENT, 1),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel imgLabel = new JLabel(scaleImage(product.getImagePath()));
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JPanel infoPanel = new JPanel(new GridLayout(4, 1));
        infoPanel.setBackground(COLOR_CARD);
        JLabel nameLabel = new JLabel(product.getName(), SwingConstants.CENTER);
        nameLabel.setForeground(COLOR_TEXT);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JLabel priceLabel = new JLabel("Rs " + product.getPrice(), SwingConstants.CENTER);
        priceLabel.setForeground(COLOR_ACCENT);
        // Barcode ke liye label
JLabel itemBarcode = new JLabel();
itemBarcode.setHorizontalAlignment(SwingConstants.CENTER);

// Nayi file BarcodeGenerator ko call karein
// product.getName() use kar rahe hain taake har product ka naam barcode ban jaye
BarcodeGenerator.setBarcode(itemBarcode, product.getName());

// Isay panel mein add karein
infoPanel.add(itemBarcode);
        
        // Stock label
        JLabel stockLabel = new JLabel("Stock: " + product.getStock(), SwingConstants.CENTER);
        stockLabel.setForeground(product.getStock() > 0 ? Color.GREEN : Color.RED);
        stockLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        
        infoPanel.add(nameLabel);
        infoPanel.add(priceLabel);
        infoPanel.add(stockLabel);

        JButton addBtn = new JButton("Add to Cart");
        styleButton(addBtn);
        
        // Disable button if out of stock
        if (product.getStock() <= 0) {
            addBtn.setEnabled(false);
            addBtn.setText("Out of Stock");
            addBtn.setBackground(new Color(80, 80, 80));
        }
        
        addBtn.addActionListener(e -> {
            boolean added = cartManager.addProduct(product);
            if (added) {
                updateCartUI();
            } else {
                JOptionPane.showMessageDialog(this, "Cannot add more - stock limit reached!", "Stock Limit", JOptionPane.WARNING_MESSAGE);
            }
        });

        card.add(imgLabel, BorderLayout.NORTH);
        card.add(infoPanel, BorderLayout.CENTER);
        card.add(addBtn, BorderLayout.SOUTH);

        return card;
    }

    private void updateCartUI() {
        cartTableModel.setRowCount(0); // Clear table
        Object[][] data = cartManager.getCartDataForTable();
        for (Object[] row : data) {
            cartTableModel.addRow(row);
        }

        // 1. Discount value input field se uthana
        double discVal = 0;
        try {
            // Agar box khali ho ya galat ho to 0 lega
            discVal = Double.parseDouble(discountField.getText());
        } catch (Exception e) {
            discVal = 0;
        }
        
        // 2. CartManager ko batana ke kitna discount hai
        cartManager.setDiscount(discVal);

        // 3. Screen par Labels update karna (Professional formatting ke sath)
        subTotalLabel.setText("Subtotal: Rs " + String.format("%.2f", cartManager.getSubTotal()));
        taxLabel.setText("GST (16%): Rs " + String.format("%.2f", cartManager.getTaxAmount()));
        totalLabel.setText("Grand Total: Rs " + String.format("%.2f", cartManager.getFinalTotal()));
        
        // UI ko refresh karna
        revalidate();
        repaint();
    }

    private void processCheckout() {
        if (cartManager.getCartItems().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 1. Detailed Receipt to Console
        System.out.println("\n--- Crown Mill Receipt ---");
        System.out.println("Item\t\tQty\tPrice\tTotal");
        for (CartManager.CartItem item : cartManager.getCartItems().values()) {
            System.out.println(item.getProduct().getName() + "\t" + 
                               item.getQuantity() + "\t" + 
                               item.getProduct().getPrice() + "\t" + 
                               (item.getQuantity() * item.getProduct().getPrice()));
        }
        System.out.println("--------------------------");
        System.out.println("Subtotal:    Rs " + String.format("%.2f", cartManager.getSubTotal()));
        System.out.println("GST (16%):   Rs " + String.format("%.2f", cartManager.getTaxAmount()));
        System.out.println("Discount:    Rs " + String.format("%.2f", cartManager.getDiscountAmount()));
        System.out.println("Grand Total: Rs " + String.format("%.2f", cartManager.getFinalTotal()));
        System.out.println("--------------------------\n");

        // 2. Save sales to database
        saveSaleToDatabase();

        // 3. Update stock in database (decrease stock for each item sold)
        updateStockAfterSale();

        // 4. Popup Receipt (Final Total dikhane ke liye)
    String receiptHTML = "<html>" +
        "<div style='text-align: center; width: 220px; font-family: sans-serif; padding: 10px;'>" +
        "<h2 style='color: #CC9900; margin: 0;'>CROWN MILL</h2>" +
        "<p style='font-size: 9px; margin: 2px 0;'>Premium Quality Store</p>" +
        "<div style='border-top: 1px dashed #000; margin: 5px 0;'></div>" +
        "<table width='100%' style='font-size: 11px;'>" +
        "<tr><th align='left'>Item</th><th align='right'>Total</th></tr>";

    for (CartManager.CartItem item : cartManager.getCartItems().values()) {
        receiptHTML += "<tr><td>" + item.getProduct().getName() + " (x" + item.getQuantity() + ")</td>" +
                       "<td align='right'>Rs " + (int)(item.getQuantity() * item.getProduct().getPrice()) + "</td></tr>";
    }

    receiptHTML += "</table>" +
        "<div style='border-top: 1px dashed #000; margin: 5px 0;'></div>" +
        "<table width='100%' style='font-size: 11px;'>" +
        "<tr><td>Subtotal:</td><td align='right'>Rs " + String.format("%.0f", cartManager.getSubTotal()) + "</td></tr>" +
        "<tr><td>GST (16%):</td><td align='right'>Rs " + String.format("%.0f", cartManager.getTaxAmount()) + "</td></tr>" +
        "<tr><td>Discount:</td><td align='right'>-Rs " + String.format("%.0f", cartManager.getDiscountAmount()) + "</td></tr>" +
        "</table>" +
        "<div style='margin: 5px 0; background-color: #000; padding: 5px;'>" +
        "<table width='100%'>" +
        "<tr style='color: #FFD700; font-weight: bold; font-size: 14px;'>" +
        "<td>TOTAL:</td><td align='right'>Rs " + String.format("%.0f", cartManager.getFinalTotal()) + "</td></tr>" +
        "</table></div>" +
        "<p style='font-size: 10px; margin-top: 10px;'><i>*** Thank You For Shopping ***</i></p>" +
        "</div></html>";

    JOptionPane.showMessageDialog(this, receiptHTML, "Crown Mill Receipt", JOptionPane.PLAIN_MESSAGE);

        // Ask user if they want to print the receipt
        int printChoice = JOptionPane.showConfirmDialog(this, 
            "Do you want to print this receipt?", 
            "Print Receipt", 
            JOptionPane.YES_NO_OPTION);

        if (printChoice == JOptionPane.YES_OPTION) {
            printReceipt(receiptHTML);
        }

        // 5. Clear Cart and Reset UI
        cartManager.clearCart();
        discountField.setText("0");
        updateCartUI();
    }

    private void saveSaleToDatabase() {
        try {
            if (dbConnection == null) {
                System.out.println(">>> WARNING: Database connection is null, sale not saved");
                return;
            }
            
            for (CartManager.CartItem item : cartManager.getCartItems().values()) {
                double itemPrice = item.getProduct().getPrice();
                int qty = item.getQuantity();
                double itemTotal = itemPrice * qty;
                
                String insertSQL = "INSERT INTO sales (item_name, quantity, price, total, discount, tax) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement pstmt = dbConnection.prepareStatement(insertSQL);
                pstmt.setString(1, item.getProduct().getName());
                pstmt.setInt(2, qty);
                pstmt.setDouble(3, itemPrice);
                pstmt.setDouble(4, itemTotal);
                pstmt.setDouble(5, cartManager.getDiscountAmount());
                pstmt.setDouble(6, cartManager.getTaxAmount());
                pstmt.executeUpdate();
                pstmt.close();
            }
            
            System.out.println(">>> SALE SAVED TO DATABASE!");
        } catch (SQLException e) {
            System.out.println(">>> ERROR saving sale to database: " + e.getMessage());
        }
    }

    private void updateStockAfterSale() {
        try {
            if (dbConnection == null) return;
            
            for (CartManager.CartItem item : cartManager.getCartItems().values()) {
                String productName = item.getProduct().getName();
                int quantitySold = item.getQuantity();
                
                // Decrease stock by quantity sold
                String updateSQL = "UPDATE products SET stock = stock - ? WHERE name = ? AND stock > 0";
                PreparedStatement pstmt = dbConnection.prepareStatement(updateSQL);
                pstmt.setInt(1, quantitySold);
                pstmt.setString(2, productName);
                int updated = pstmt.executeUpdate();
                pstmt.close();
                
                if (updated > 0) {
                    System.out.println(">>> Stock updated for: " + productName + " (-" + quantitySold + ")");
                }
            }
            
            // Reload products to reflect new stock values
            loadProductsFromDatabase();
            updateProductDisplay();
            
        } catch (SQLException e) {
            System.out.println(">>> ERROR updating stock: " + e.getMessage());
        }
    }

    private void printReceipt(String receiptHTML) {
        try {
            // Create a JTextPane to render the HTML
            JTextPane textPane = new JTextPane();
            textPane.setContentType("text/html");
            textPane.setText(receiptHTML);
            textPane.setEditable(false);
            
            // Create a print job
            PrinterJob printerJob = PrinterJob.getPrinterJob();
            printerJob.setPrintable(new Printable() {
                @Override
                public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                    if (pageIndex > 0) {
                        return NO_SUCH_PAGE;
                    }
                    
                    Graphics2D g2d = (Graphics2D) graphics;
                    g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                    
                    // Scale to fit the page
                    double scaleX = pageFormat.getImageableWidth() / textPane.getPreferredSize().getWidth();
                    double scaleY = pageFormat.getImageableHeight() / textPane.getPreferredSize().getHeight();
                    double scale = Math.min(scaleX, scaleY);
                    
                    if (scale < 1.0) {
                        g2d.scale(scale, scale);
                    }
                    
                    textPane.print(g2d);
                    return PAGE_EXISTS;
                }
            });
            
            // Show print dialog
            if (printerJob.printDialog()) {
                printerJob.print();
                JOptionPane.showMessageDialog(this, "Receipt printed successfully!", "Print Complete", JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this, "Error printing receipt: " + e.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int showThemedConfirmDialog(String message, String title) {
        // Create custom dialog with theme colors
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(COLOR_BG);
        
        // Message label
        JLabel messageLabel = new JLabel(message, SwingConstants.CENTER);
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setFont(new Font("Arial", Font.BOLD, 14));
        messageLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(COLOR_BG);
        buttonPanel.setBorder(new EmptyBorder(10, 20, 20, 20));
        
        JButton yesButton = new JButton("Yes");
        styleThemeButton(yesButton);
        
        JButton noButton = new JButton("No");
        styleThemeButton(noButton);
        
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        
        dialog.add(messageLabel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Handle button clicks
        final int[] result = {JOptionPane.CLOSED_OPTION};
        
        yesButton.addActionListener(e -> {
            result[0] = JOptionPane.YES_OPTION;
            dialog.dispose();
        });
        
        noButton.addActionListener(e -> {
            result[0] = JOptionPane.NO_OPTION;
            dialog.dispose();
        });
        
        // Handle window close
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                result[0] = JOptionPane.CLOSED_OPTION;
            }
        });
        
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        
        return result[0];
    }

    private void styleThemeButton(JButton button) {
        button.setBackground(COLOR_ACCENT);
        button.setForeground(Color.BLACK);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(80, 30));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(COLOR_ACCENT_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(COLOR_ACCENT);
            }
        });
    }


    //  Image Handling with Fallback
    private ImageIcon scaleImage(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                Image img = ImageIO.read(file);
                Image scaled = img.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception e) {
            System.out.println("Image not found: " + path);
        }
        
       
        // Fallback Placeholder if image doesn't exist
        BufferedImage placeholder = new BufferedImage(150, 150, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = placeholder.createGraphics();
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, 150, 150);
        g.setColor(COLOR_ACCENT);
        g.setStroke(new BasicStroke(2));
        g.drawRect(5, 5, 140, 140);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        String text = "NO IMAGE";
        int x = (150 - fm.stringWidth(text)) / 2;
        int y = (150 - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, x, y);
        g.dispose();
        
        return new ImageIcon(placeholder);
    }

    //  Custom Button Styling (Hover effects)
    private void styleButton(JButton btn) {
        btn.setBackground(COLOR_ACCENT);
        btn.setForeground(COLOR_BG);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(COLOR_ACCENT_HOVER);
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(COLOR_ACCENT);
            }
        });
    }

    private void startClock() {
        Timer timer = new Timer(1000, e -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a");
            timeLabel.setText(sdf.format(new Date()));
        });
        timer.start();
    }

    private void openAdminPanel() {
        SwingUtilities.invokeLater(() -> new AdminPanel());
    }

    public static void main(String[] args) {
        // Set Look and Feel to CrossPlatform to ensure button colors apply perfectly
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            // Show login dialog first
            if (showLoginDialog()) {
                new POSUI().setVisible(true);
            }
        });
    }
    
    private static boolean showLoginDialog() {
        Color bgColor = new Color(0, 0, 0);
        Color accentColor = new Color(255, 215, 0);
        
        JDialog loginDialog = new JDialog((JFrame) null, "Crown Mill Login", true);
        loginDialog.setLayout(new BorderLayout());
        loginDialog.getContentPane().setBackground(bgColor);
        loginDialog.setSize(350, 250);
        loginDialog.setLocationRelativeTo(null);
        loginDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // Title
        JLabel titleLabel = new JLabel("Crown Mill Store", SwingConstants.CENTER);
        titleLabel.setForeground(accentColor);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBorder(new EmptyBorder(20, 20, 10, 20));
        
        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(bgColor);
        formPanel.setBorder(new EmptyBorder(10, 40, 10, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Username
        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Arial", Font.BOLD, 12));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        formPanel.add(userLabel, gbc);
        
        JTextField userField = new JTextField(15);
        userField.setBackground(new Color(40, 40, 40));
        userField.setForeground(Color.WHITE);
        userField.setCaretColor(Color.WHITE);
        userField.setFont(new Font("Arial", Font.PLAIN, 12));
        userField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 1),
            new EmptyBorder(5, 8, 5, 8)));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.7;
        formPanel.add(userField, gbc);
        
        // Password
        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(Color.WHITE);
        passLabel.setFont(new Font("Arial", Font.BOLD, 12));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        formPanel.add(passLabel, gbc);
        
        JPasswordField passField = new JPasswordField(15);
        passField.setBackground(new Color(40, 40, 40));
        passField.setForeground(Color.WHITE);
        passField.setCaretColor(Color.WHITE);
        passField.setFont(new Font("Arial", Font.PLAIN, 12));
        passField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 1),
            new EmptyBorder(5, 8, 5, 8)));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.7;
        formPanel.add(passField, gbc);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(bgColor);
        buttonPanel.setBorder(new EmptyBorder(10, 20, 20, 20));
        
        JButton loginBtn = new JButton("Login");
        loginBtn.setBackground(accentColor);
        loginBtn.setForeground(Color.BLACK);
        loginBtn.setFont(new Font("Arial", Font.BOLD, 12));
        loginBtn.setFocusPainted(false);
        loginBtn.setBorderPainted(false);
        loginBtn.setPreferredSize(new Dimension(100, 35));
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(new Color(60, 60, 60));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFont(new Font("Arial", Font.BOLD, 12));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setPreferredSize(new Dimension(100, 35));
        
        buttonPanel.add(loginBtn);
        buttonPanel.add(cancelBtn);
        
        loginDialog.add(titleLabel, BorderLayout.NORTH);
        loginDialog.add(formPanel, BorderLayout.CENTER);
        loginDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        final boolean[] loginSuccess = {false};
        
        // Login button action
         loginBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());
            
            // Default credentials: admin / admin123
            if (username.equals("a") && password.equals("a")) {
                loginSuccess[0] = true;
                loginDialog.dispose();
            } else {
                JOptionPane.showMessageDialog(loginDialog, 
                    "Invalid username or password!", 
                    "Login Failed", 
                    JOptionPane.ERROR_MESSAGE);
                passField.setText("");
                userField.requestFocus();
            }
        });
        
        // Cancel button action
        cancelBtn.addActionListener(e -> {
            loginDialog.dispose();
        });
        
        // Enter key to login
        passField.addActionListener(e -> {
            loginBtn.doClick();
        });
        
        loginDialog.pack();
        loginDialog.setLocationRelativeTo(null);
        loginDialog.setVisible(true);
        
        return loginSuccess[0];
    }
    private void updateCartTable() {
    // 1. Table ko saaf karein
    DefaultTableModel model = (DefaultTableModel) cartTable.getModel();
    model.setRowCount(0);

    // 2. CartManager se items utha kar table mein daalein
    // Note: Check karein ke cartManager.getCartItems() aapke code mein mojood hai
    // 2. CartManager se items utha kar table mein daalein
    // Hum .values() use karenge kyunke aapka cartItems aik Map hai
    for (CartManager.CartItem item : cartManager.getCartItems().values()) {
        model.addRow(new Object[]{
            item.getProduct().getName(),
            item.getQuantity(),
            item.getProduct().getPrice(),
            (item.getProduct().getPrice() * item.getQuantity())
        });
    }

    // 3. Bill wale labels update karein (Aapki file ke methods)
    totalLabel.setText("Total: Rs " + cartManager.getFinalTotal());
}
}