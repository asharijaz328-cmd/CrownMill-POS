import java.io.*;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DatabaseBackup {
    public static void main(String[] args) {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");

            java.nio.file.Path dbPath = Paths.get("crownmill_db.db").toAbsolutePath();
            String url = "jdbc:sqlite:" + dbPath.toString();

            System.out.println("=== DATABASE BACKUP ===");
            System.out.println("Original database: " + dbPath);
            conn = DriverManager.getConnection(url);

            // Create backup filename with timestamp
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
            String backupFileName = "crownmill_db_backup_" + now.format(formatter) + ".db";
            
            File backupFile = new File(backupFileName);
            
            System.out.println("\nBacking up to: " + backupFile.getAbsolutePath());

            // Get all data from database
            Statement stmt = conn.createStatement();
            
            // Export products
            System.out.println("\n=== PRODUCTS ===");
            ResultSet rs = stmt.executeQuery("SELECT * FROM products");
            int productCount = 0;
            while (rs.next()) {
                productCount++;
                System.out.println(productCount + ". " + rs.getString("name") + 
                                 " (Rs " + rs.getDouble("price") + ") - " + 
                                 rs.getString("category"));
            }
            
            // Export sales
            System.out.println("\n=== SALES ===");
            rs = stmt.executeQuery("SELECT * FROM sales ORDER BY sale_date DESC");
            int saleCount = 0;
            while (rs.next()) {
                saleCount++;
                System.out.println(saleCount + ". " + rs.getString("item_name") + 
                                 " x" + rs.getInt("quantity") + 
                                 " = Rs " + rs.getDouble("total") +
                                 " (Date: " + rs.getString("sale_date") + ")");
            }
            
            stmt.close();

            // Copy database file for backup
            java.nio.file.Files.copy(
                java.nio.file.Paths.get(dbPath.toString()),
                java.nio.file.Paths.get(backupFileName)
            );

            System.out.println("\n=== BACKUP COMPLETE ===");
            System.out.println("Backup file: " + backupFileName);
            System.out.println("Total Products: " + productCount);
            System.out.println("Total Sales: " + saleCount);

            conn.close();

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
