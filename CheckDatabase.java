import java.nio.file.Paths;
import java.sql.*;

public class CheckDatabase {
    public static void main(String[] args) {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");

            // Use absolute path for database file
            java.nio.file.Path dbPath = Paths.get("crownmill_db.db").toAbsolutePath();
            String url = "jdbc:sqlite:" + dbPath.toString();

            System.out.println("Connecting to database: " + url);
            conn = DriverManager.getConnection(url);

            // Check products
            System.out.println("\n=== PRODUCTS IN DATABASE ===");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, name, price, category FROM products");

            int count = 0;
            while (rs.next()) {
                count++;
                System.out.println(count + ". " + rs.getString("name") +
                                 " - Rs" + rs.getDouble("price") +
                                 " - " + rs.getString("category"));
            }
            rs.close();

            System.out.println("\nTotal products: " + count);

            // Check sales
            System.out.println("\n=== SALES IN DATABASE ===");
            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM sales");
            rs.next();
            int saleCount = rs.getInt("count");
            rs.close();
            stmt.close();

            System.out.println("Total sales: " + saleCount);

            conn.close();

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}