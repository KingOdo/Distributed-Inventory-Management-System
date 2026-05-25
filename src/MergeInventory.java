import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MergeInventory {
    private static final String FOLDER_NAME = "InventoryFolder";
    private static final String MERGED_FILE_NAME = "MergedInventory.txt";

    public static String generateOverview() {
        File inventoryFolder = new File(FOLDER_NAME);

        if (!inventoryFolder.exists()) {
            return "Error: Inventory folder was not found.";
        }

        File[] inventoryFiles = inventoryFolder.listFiles((folder, name) ->
                name.endsWith(".txt") && !name.equals(MERGED_FILE_NAME)
        );

        if (inventoryFiles == null || inventoryFiles.length == 0) {
            return "Error: No inventory files found.";
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(4);

        List<Future<List<Products>>> futures = new ArrayList<>();

        for (File file : inventoryFiles) {
            Callable<List<Products>> task = () -> readProductsFromFile(file);
            futures.add(threadPool.submit(task));
        }

        Map<String, Products> mergedProducts = new HashMap<>();

        try {
            for (Future<List<Products>> future : futures) {
                List<Products> productList = future.get();

                for (Products product : productList) {
                    String productName = product.getProductName();

                    if (!mergedProducts.containsKey(productName)) {
                        mergedProducts.put(productName, product);
                    } else {
                        Products existingProduct = mergedProducts.get(productName);

                        if (product.getPrice() > existingProduct.getPrice()) {
                            mergedProducts.put(productName, product);
                        }
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            return "Error while merging inventory files: " + e.getMessage();
        } finally {
            threadPool.shutdown();
        }

        List<Products> finalProducts = new ArrayList<>(mergedProducts.values());

        writeMergedInventoryFile(finalProducts);

        return calculateStatistics(finalProducts);
    }

    private static List<Products> readProductsFromFile(File file) {
        List<Products> products = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                Products product = parseProductLine(line);

                if (product != null) {
                    products.add(product);
                }
            }

        } catch (IOException e) {
            System.out.println("Error reading file: " + file.getName());
            e.printStackTrace();
        }

        return products;
    }

    private static Products parseProductLine(String line) {
        try {
            String[] parts = line.split(",");

            if (parts.length != 3) {
                return null;
            }

            String productId = parts[0].trim();
            String productName = parts[1].trim();
            double price = Double.parseDouble(parts[2].trim());

            return new Products(productId, productName, price);

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void writeMergedInventoryFile(List<Products> products) {
        File inventoryFolder = new File(FOLDER_NAME);
        File mergedFile = new File(inventoryFolder, MERGED_FILE_NAME);

        try (FileWriter writer = new FileWriter(mergedFile)) {
            for (Products product : products) {
                writer.write(product.toFileFormat() + "\n");
            }

            System.out.println("MergedInventory.txt created successfully.");

        } catch (IOException e) {
            System.out.println("Error writing merged inventory file.");
            e.printStackTrace();
        }
    }

    private static String calculateStatistics(List<Products> products) {
        if (products.isEmpty()) {
            return "No products available for statistics.";
        }

        double totalPrice = 0;
        Products highestProduct = products.get(0);
        Products lowestProduct = products.get(0);

        for (Products product : products) {
            totalPrice += product.getPrice();

            if (product.getPrice() > highestProduct.getPrice()) {
                highestProduct = product;
            }

            if (product.getPrice() < lowestProduct.getPrice()) {
                lowestProduct = product;
            }
        }

        double averagePrice = totalPrice / products.size();

        return "Inventory Overview\n" +
                "----------------------------\n" +
                "Total Unique Products: " + products.size() + "\n" +
                "Average Price= " + String.format("%.2f", averagePrice) + "\n" +
                "Highest Price= " + highestProduct.getProductName() +
                " - " + String.format("%.2f", highestProduct.getPrice()) + "\n" +
                "Lowest Price=  " + lowestProduct.getProductName() +
                " - " + String.format("%.2f", lowestProduct.getPrice()) + "\n";
    }

    public static void main(String[] args) {
        String overview = generateOverview();
        System.out.println(overview);
    }
}