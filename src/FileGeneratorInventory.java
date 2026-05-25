import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;


public class FileGeneratorInventory {
    private static final String myStudentID= "22205122";
    private  static final String FolderName= "InventoryFolder";

    private static  final  String[] File_names={
                   "Clothing.txt", "Electronics.txt", "Groceries.txt", "Books.txt"
    };
public static void GenerateInventoryFIles(){
    File inventoryFolder = new File(FolderName);

    //check if inventory folder already exists, if not create one.
    if(!inventoryFolder.exists()) {
        inventoryFolder.mkdir();
    }
    long seed= Long.parseLong("22205122");
    Random random= new Random(seed);

    for(String fileName : File_names){
        File file= new File(inventoryFolder, fileName);
        try (FileWriter writer= new FileWriter(file))
        {
            for(int i=1; i<=30; i++){
                String productId= String.format("P%04d", 1000 + i);
                String productName= generateproductname(fileName, i);
                double price=  5 + (495 * random.nextDouble());
                writer.write(productId + ", " + productName + ", " + String.format("%.2f", price) + "\n");
            }
            //my id record
            writer.write("P9999, StudentID_" + myStudentID + "_Demo, 0.01\n");
            System.out.println(fileName + " successfully generated");
        }
        catch (IOException e){
            System.out.println("Error while creating file: " + fileName);
            e.printStackTrace();
        }
    }
        System.out.println("Inventory files have successfully been generated");
}

private static String generateproductname(String fileName, int index)
{
     if (fileName.equals("Clothing.txt")) {
        return "Clothing Item" + index;
    } else if (fileName.equals("Electronics.txt")) {
        return "Electronic Item " + index;
    } else if (fileName.equals("Groceries.txt")) {
        return "Grocery Item" + index;
    } else if (fileName.equals("Books.txt")) {
        return "Book Item " + index;
    } else {
        return "Product Item " + index;
    }
}
    public static void main(String[] args){
            GenerateInventoryFIles();
    }
}
