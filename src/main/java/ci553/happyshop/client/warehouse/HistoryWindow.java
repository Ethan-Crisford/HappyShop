package ci553.happyshop.client.warehouse;

import ci553.happyshop.utility.UIStyle;
import ci553.happyshop.utility.WindowBounds;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.ArrayList;

/**
 * This class provides a simple history window to display a list of events (successfully delete, edit and add a new product).
 *
 * - The scene is created only once to avoid unnecessary scene recreation.
 * - The window is created only when needed. If the window is already visible, it will not be recreated.
 * - The history text is updated dynamically in the TextArea when new data is provided.
 * - The window is positioned relative to the warehouse window for a consistent UI experience.
 *
 * This design ensures that the history view is efficient by not recreating the scene and only displaying the window when required.
 */

public class HistoryWindow {
    private static int WIDTH = UIStyle.HistoryWinWidth;
    private static int HEIGHT = UIStyle.HistoryWinHeight;

    public WarehouseView warehouseView;
    private  Stage window = new Stage();
    private  Scene scene;
    private  TextArea taHistory;

    // Create the scene only once (to avoid recreating it multiple times)
    private  void createScene() {
        // a TextArea to show stock management history
        taHistory = new TextArea();
        taHistory.setPrefSize(150,150);
        taHistory.setEditable(false);
        taHistory.setStyle(UIStyle.textFiledStyle);
        VBox vbHistory = new VBox(taHistory);
        scene = new Scene(vbHistory,WIDTH,HEIGHT);
    }

    // Create the window only when needed (i.e., when the window is not created or closed by user but we need it again)
    private  void createWindow(){
        if (scene == null) {
            createScene(); // create the scene only once
        }

        window = new Stage();
        window.setScene(scene);
        window.setTitle("\uD83C\uDFEC Warehouse Management History"); // for icon 🏬
        window.show();
        //get the bounds of warehouse window which triggers the history window
        //so that we can put the history window next to the warehouse window
        WindowBounds bounds = warehouseView.getWindowBounds();
        window.setX(bounds.x + bounds.width - 20);
        window.setY(bounds.y); // align vertically
    }

    public  void showManageHistory(ArrayList<String> history){
        if(window ==null ||!window.isShowing() ) {
            createWindow();  // Only create window if it's not created or invisible
        }
        // Create a single string with each item on a new line
        StringBuilder result = new StringBuilder();
        for (String his : history) {
            result.append(his).append("\n"); // Append item followed by a newline
        }
        taHistory.setText(result.toString());
        System.out.println(result.toString());
    }
}
