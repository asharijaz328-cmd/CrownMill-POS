import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class TestDb {
    public static void main(String[] args) {
        // Database - use file: URI format  
        String url = "jdbc:sqlite:file:crownmill_db.db?mode=rwc";

        System.out.println("Starting Database Connection Test...");

        try {
            // 1. Forcefully load the SQLite Driver (Dhakka lagana)
            Class.forName("org.sqlite.JDBC");
            
            // 2. Connection build karna
            Connection conn = DriverManager.getConnection(url);

            if (conn != null) {
                System.out.println("---------------------------------------");
                System.out.println("MUBARAK HO! Database Connect Ho Gaya Hai.");
                System.out.println("Windows 10 par SQLite sahi kaam kar raha hai.");
                System.out.println("---------------------------------------");
                
                // Simple test - just execute a simple pragma
                System.out.println("\n>>> Testing database with PRAGMA...");
                Statement testStmt = conn.createStatement();
                testStmt.execute("PRAGMA journal_mode=WAL");
                testStmt.close();
                System.out.println(">>> PRAGMA successful!");
                
                // Create tables
                System.out.println("\n>>> Creating tables...");
                Statement stmt = conn.createStatement();
                
                String createProductsTable = "CREATE TABLE IF NOT EXISTS products (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "price REAL NOT NULL," +
                    "image_path TEXT," +
                    "category TEXT" +
                    ")";
                
                stmt.execute(createProductsTable);
                System.out.println(">>> Products table created!");
                
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
                
                stmt.execute(createSalesTable);
                System.out.println(">>> Sales table created!");
                
                stmt.close();
                System.out.println(">>> Database initialization SUCCESSFUL!\n");
                
                // Connection band karna zaroori hai (Good Practice)
                conn.close();
            }
        } catch (ClassNotFoundException e) {
            System.out.println("ERROR: SQLite Driver nahi mila! Referenced Libraries check karein.");
        } catch (SQLException e) {
            System.out.println("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("GENERAL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}