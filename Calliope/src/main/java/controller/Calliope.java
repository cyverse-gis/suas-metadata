package controller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import model.CalliopeData;
import model.util.FXMLLoaderUtils;
import org.controlsfx.control.action.Action;

/**
 * Main class entry point
 *
 * @author David Slovikosky
 * @version 1.0
 */
public class Calliope extends Application
{
    // Main just launches the application
    public static void main(String[] args)
    {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage)
    {
        Application.setUserAgentStylesheet(STYLESHEET_MODENA);

        // Load the FXML document
        FXMLLoader root = FXMLLoaderUtils.loadFXML("CalliopeView.fxml");
        // Create the scene
        Scene scene = new Scene(root.getRoot());

        // Put the scene on the stage
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image("images/mainMenu/drone.png"));
        primaryStage.setTitle("Calliope");
        // When we click exit...
        primaryStage.setOnCloseRequest(event ->
        {
            // If a task is still running ask for confirmation to exit
            if (CalliopeData.getInstance().getExecutor().anyTaskRunning())
            {
                CalliopeData.getInstance().getErrorDisplay().notify("Calliope is still cleaning up background tasks and exiting now may cause data corruption. Are you sure you want to exit?",
					new Action("Exit Anyway", actionEvent ->
					{
						System.exit(0);
					}));
				event.consume();
            }
            else
            {
                System.exit(0);
            }
        });
        primaryStage.setMaximized(true);
        // When we exit the window exit the program
        // Show it
        primaryStage.show();
    }
}
