public class Product {
    private String name;
    private double price;
    private String imagePath;
    private String category;
    private int stock;

    public Product(String name, double price, String imagePath, String category) {
        this(name, price, imagePath, category, 0);
    }
    
    public Product(String name, double price, String imagePath, String category, int stock) {
        this.name = name;
        this.price = price;
        this.imagePath = imagePath;
        this.category = category;
        this.stock = stock;
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getImagePath() { return imagePath; }
    public String getCategory() { return category; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}