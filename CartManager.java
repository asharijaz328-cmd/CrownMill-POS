import java.util.LinkedHashMap;
import java.util.Map;

public class CartManager {
    // K: Product Name, V: CartItemInfo (Product, Quantity)
    private Map<String, CartItem> cartItems = new LinkedHashMap<>();
private double discountPercentage = 0.0;
    private final double TAX_RATE = 0.16;
    public boolean addProduct(Product product) {
        // Check if product has stock available
        if (product.getStock() <= 0) {
            return false; // Cannot add out of stock items
        }
        
        // Check if already in cart - cannot exceed available stock
        if (cartItems.containsKey(product.getName())) {
            CartItem existing = cartItems.get(product.getName());
            if (existing.getQuantity() >= product.getStock()) {
                return false; // Cannot add more than available stock
            }
            existing.incrementQuantity();
        } else {
            cartItems.put(product.getName(), new CartItem(product, 1));
        }
        return true;
    }

    public void clearCart() {
        cartItems.clear();
    }

    public double getTotal() {
        double total = 0;
        for (CartItem item : cartItems.values()) {
            total += item.getProduct().getPrice() * item.getQuantity();
        }
        return total;
    }

    public Object[][] getCartDataForTable() {
        Object[][] data = new Object[cartItems.size()][4];
        int i = 0;
        for (CartItem item : cartItems.values()) {
            data[i][0] = item.getProduct().getName();
            data[i][1] = item.getQuantity();
            data[i][2] = item.getProduct().getPrice();
            data[i][3] = item.getProduct().getPrice() * item.getQuantity();
            i++;
        }
        return data;
    }
    
    public Map<String, CartItem> getCartItems() {
        return cartItems;
    }
// CartManager class ke andar ye methods add/update karein:

public void setDiscount(double percentage) {
    this.discountPercentage = percentage;
}

public double getSubTotal() {
    double subtotal = 0;
    for (CartItem item : cartItems.values()) {
        subtotal += item.getProduct().getPrice() * item.getQuantity();
    }
    return subtotal;
}

public double getTaxAmount() {
    return getSubTotal() * TAX_RATE;
}

public double getDiscountAmount() {
    return getSubTotal() * (discountPercentage / 100);
}

public double getFinalTotal() {
    return getSubTotal() + getTaxAmount() - getDiscountAmount();
}

    // Inner class for Cart Item
    public static class CartItem {
        private Product product;
        private int quantity;

        public CartItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        public Product getProduct() { return product; }
        public int getQuantity() { return quantity; }
        public void incrementQuantity() { this.quantity++; }
    }
}