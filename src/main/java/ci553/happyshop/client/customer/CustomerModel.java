package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.ProductListFormatter;
import ci553.happyshop.client.customer.RemoveProductNotifier;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CustomerModel
{
    public CustomerView cusView;
    public DatabaseRW databaseRW; //Interface type, not specific implementation
                                  //Benefits: Flexibility: Easily change the database implementation.

    private Product theProduct =null; // product found from search
    private ArrayList<Product> trolley =  new ArrayList<>(); // a list of products in trolley

    // Four UI elements to be passed to CustomerView for display updates.
    private String imageName = "imageHolder.jpg";                // Image to show in product preview (Search Page)
    private String displayLaSearchResult = "No Product was searched yet"; // Label showing search result message (Search Page)
    private String displayTaTrolley = "";                                // Text area content showing current trolley items (Trolley Page)
    private String displayTaReceipt = "";                                // Text area content showing receipt after checkout (Receipt Page)

    //SELECT productID, description, image, unitPrice,inStock quantity
    void search() throws SQLException
    {
        String productId = cusView.tfId.getText().trim();
        if(!productId.isEmpty())
        {
            theProduct = databaseRW.searchByProductId(productId); //search database
            if(theProduct != null && theProduct.getStockQuantity()>0)
            {
                double unitPrice = theProduct.getUnitPrice();
                String description = theProduct.getProductDescription();
                int stock = theProduct.getStockQuantity();

                String baseInfo = String.format("Product_Id: %s\n%s,\nPrice: £%.2f", productId, description, unitPrice);
                String quantityInfo = stock < 100 ? String.format("\n%d units left.", stock) : "";
                displayLaSearchResult = baseInfo + quantityInfo;
                System.out.println(displayLaSearchResult);
            }
            else
            {
                theProduct=null;
                displayLaSearchResult = "No Product was found with ID " + productId;
                System.out.println("No Product was found with ID " + productId);
            }
        }
        else
        {
            theProduct=null;
            displayLaSearchResult = "Please type ProductID";
            System.out.println("Please type ProductID.");
        }
        updateView();
    }

    /**
     * Organised trolley.
     * 1. Merges items with the same product ID (combining their quantities).
     * 2. Sorts the products in the trolley by product ID.
     * Uses merge bool to assign false to products unless they have the same ID of a product already in the trolley.
     * Uses trolley.Sort to compare product to product ID.
     */

    void addToTrolley()
    {
        if (theProduct != null)
        {
            String idToAdd = theProduct.getProductId();

            // Find existing line in trolley (if any)
            Product existing = null;
            for (Product p : trolley)
            {
                if (p.getProductId().equals(idToAdd))
                {
                    existing = p;
                    break;
                }
            }

            // Stock limit check:
            // If already in trolley, do not allow quantity to exceed stockQuantity.
            if (existing != null)
            {
                int currentQty = existing.getOrderedQuantity();
                int stock = existing.getStockQuantity(); // stock stored on the product line

                if (currentQty + 1 > stock)
                {
                    displayLaSearchResult = "Cannot add more. Only " + stock + " units available for product " + idToAdd + ".";
                    displayTaReceipt = "";
                    updateView();
                    return;
                }

                existing.setOrderedQuantity(currentQty + 1);

            }
            else
            {
                //add if at least 1 in stock
                int stock = theProduct.getStockQuantity();
                if (stock < 1)
                {
                    displayLaSearchResult = "This product is out of stock.";
                    displayTaReceipt = "";
                    updateView();
                    return;
                }

                // Create a copy so trolley doesn’t share references with theProduct
                Product toAdd = new Product(
                        theProduct.getProductId(),
                        theProduct.getProductDescription(),
                        theProduct.getProductImageName(),
                        theProduct.getUnitPrice(),
                        theProduct.getStockQuantity()
                );
                toAdd.setOrderedQuantity(1);
                trolley.add(toAdd);
            }

            // Keep trolley sorted by productId
            trolley.sort(java.util.Comparator.comparing(Product::getProductId));

            displayTaTrolley = ProductListFormatter.buildString(trolley);

        }
        else
        {
            displayLaSearchResult = "Please search for an available product before adding it to the trolley";
            System.out.println("Please search for an available product before adding it to the trolley");
        }

        displayTaReceipt = "";
        updateView();
    }


    /**
     * Remove products with insufficient stock from the trolley.
     * Trigger a message window to notify the customer about the insufficient stock.
     * Close the message window where appropriate (using method closeNotifierWindow() of RemoveProductNotifier class)
     */

     void checkOut() throws IOException, SQLException
     {
        {
            if (!trolley.isEmpty())
            {
                //ArrayList<Product> groupedTrolley = groupProductsById(trolley); not needed
                ArrayList<Product> insufficientProducts =
                        databaseRW.purchaseStocks(new ArrayList<>(trolley));

                if (insufficientProducts.isEmpty())
                {
                    // If stock is enough for all products in trolley then create new order
                    OrderHub orderHub = OrderHub.getOrderHub();
                    Order theOrder = orderHub.newOrder(trolley);

                    trolley.clear();
                    displayTaTrolley = "";

                    displayTaReceipt = String.format(
                            "Order_ID: %s\nOrdered_Date_Time: %s\n%s",
                            theOrder.getOrderId(),
                            theOrder.getOrderedDateTime(),
                            ProductListFormatter.buildString(theOrder.getProductList())
                    );

                    System.out.println(displayTaReceipt);
                }
                else
                {
                    // Build error message
                    StringBuilder errorMsg = new StringBuilder();
                    for (Product p : insufficientProducts)
                    {
                        errorMsg.append("\u2022 ")
                                .append(p.getProductId()).append(", ")
                                .append(p.getProductDescription())
                                .append(" (Only ")
                                .append(p.getStockQuantity())
                                .append(" available, ")
                                .append(p.getOrderedQuantity())
                                .append(" requested)\n");
                    }

                    theProduct = null;

                    // Remove products with insufficient stock from the trolley
                    java.util.Set<String> insufficientIds = new java.util.HashSet<>();
                    for (Product p : insufficientProducts)
                    {
                        insufficientIds.add(p.getProductId());
                    }

                    trolley.removeIf(p -> insufficientIds.contains(p.getProductId()));

                    // Rebuild trolley display after removals
                    displayTaTrolley = ProductListFormatter.buildString(trolley);

                    // Notify the customer using a message window rather than in the text field.
                    RemoveProductNotifier notifier = new RemoveProductNotifier();
                    notifier.showRemovalMsg
                            (
                            "Checkout failed: insufficient stock",
                            "The following products were removed from your trolley:\n\n" + errorMsg
                    );

                    // Close the notifier window where appropriate
                    notifier.closeNotifierWindow();

                    // Keep UI messaging neutral after popup
                    displayLaSearchResult =
                            "Some items were removed from your trolley due to insufficient stock. Please review your trolley.";

                    System.out.println("Checkout failed due to insufficient stock.");
                }
            }
            else
            {
                displayTaTrolley = "Your trolley is empty";
                System.out.println("Your trolley is empty");
            }
            updateView();
        }
    }

    /**
     * Groups products by their productId to optimize database queries and updates.
     * By grouping products, we can check the stock for a given `productId` once, rather than repeatedly.
     * Needed to be reworked so that purchaseStocks(groupedTrolley) receives the correct quantities.
     */
    private ArrayList<Product> groupProductsById(ArrayList<Product> proList)
    {
        Map<String, Product> grouped = new HashMap<>();

        for (Product p : proList)
        {
            String id = p.getProductId();

            if (grouped.containsKey(id))
            {
                Product existing = grouped.get(id);
                existing.setOrderedQuantity(existing.getOrderedQuantity() + p.getOrderedQuantity());
            }
            else
            {
                // Copy the product and copy the ordered quantity.
                Product copy = new Product(
                        p.getProductId(),
                        p.getProductDescription(),
                        p.getProductImageName(),
                        p.getUnitPrice(),
                        p.getStockQuantity()
                );
                copy.setOrderedQuantity(p.getOrderedQuantity());
                grouped.put(id, copy);
            }
        }

        return new ArrayList<>(grouped.values());
    }


    void cancel()
    {
        trolley.clear();
        displayTaTrolley="";
        updateView();
    }
    void closeReceipt()
    {
        displayTaReceipt="";
    }

    void updateView()
    {
        if(theProduct != null)
        {
            imageName = theProduct.getProductImageName();
            String relativeImageUrl = StorageLocation.imageFolder +imageName; //relative file path, eg images/0001.jpg
            // Get the full absolute path to the image
            Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
            imageName = imageFullPath.toUri().toString(); //get the image full Uri then convert to String
            System.out.println("Image absolute path: " + imageFullPath); // Debugging to ensure path is correct
        }
        else
        {
            imageName = "imageHolder.jpg";
        }
        cusView.update(imageName, displayLaSearchResult, displayTaTrolley,displayTaReceipt);
    }

    //for test only
    public ArrayList<Product> getTrolley()
    {
        return trolley;
    }
}
