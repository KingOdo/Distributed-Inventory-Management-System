public class Products {
    private String productId;
    private String productName;
    private double price;

    public Products(String productId, String productName, double price) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public double getPrice() {
        return price;
    }

    public String toFileFormat() {
        return productId + ", " + productName + ", " + String.format("%.2f", price);
    }

    @Override
    public String toString() {
        return toFileFormat();
    }
}
