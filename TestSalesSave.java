import java.nio.file.Paths;
import java.sql.*;

public class TestSalesSave {
    public static void main(String[] args) {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");

            // Use absolute path for database file
            java.nio.file.Path dbPath = Paths.get("crownmill_db.db").toAbsolutePath();
            String url = "jdbc:sqlite:" + dbPath.toString();

            System.out.println("Connecting to database: " + url);
            conn = DriverManager.getConnection(url);

            // Test saving a sale
            System.out.println("Testing sale save...");
            String insertSQL = "INSERT INTO sales (item_name, quantity, price, total, discount, tax) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertSQL);
            pstmt.setString(1, "Test Product");
            pstmt.setInt(2, 2);
            pstmt.setDouble(3, 1000.0);
            pstmt.setDouble(4, 2000.0);
            pstmt.setDouble(5, 200.0); // discount
            pstmt.setDouble(6, 320.0); // tax
            pstmt.executeUpdate();
            pstmt.close();

            System.out.println("SALE SAVED SUCCESSFULLY!");

            // Check how many sales are in database
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM sales");
            rs.next();
            int saleCount = rs.getInt("count");
            rs.close();
            stmt.close();

            System.out.println("Total sales in database: " + saleCount);

            conn.close();
            System.out.println("Database connection closed successfully!");

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}