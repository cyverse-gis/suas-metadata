package controller;

import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import model.CalliopeData;
import model.threading.CalliopeExecutor;
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
        FXMLLoader loader = FXMLLoaderUtils.loadFXML("CalliopeView.fxml");
        // Create the scene
        Parent root = loader.getRoot();
        assert root instanceof Pane : "ERROR: Base layer of CalliopeView must be a Pane for scaling purposes.";
        Scene scene = new Scene(new Group(root));

        // Put the scene on the stage
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image("images/mainMenu/drone.png"));
        primaryStage.setTitle("Calliope");
        // When we click exit...
        primaryStage.setOnCloseRequest(event ->
        {
            // If a task is still running ask for confirmation to exit
            CalliopeExecutor calliopeExecutor = CalliopeData.getInstance().getExecutor();
            if (calliopeExecutor.anyTaskRunning())
            {
                CalliopeData.getInstance().getErrorDisplay().notify("Calliope is still cleaning up background tasks and exiting now may cause data corruption. Are you sure you want to exit?",
					new Action("Exit Anyway", actionEvent -> System.exit(0)));
				event.consume();
            }
            else
            {
                // Shutdown any executing threads
                calliopeExecutor.shutdown();
                // Shutdown ExifTool
                CalliopeData.getInstance().getMetadataManager().shutdown();
                // Clear any temp files made
                CalliopeData.getInstance().getTempDirectoryManager().shutdown();
                // Kill the Application
                Platform.exit();
                // Force java exit (We need this because the map tile provider starts a thread that we don't have access to and can't be stopped
                System.exit(0);
            }
        });

        // Set the scaling function to be run after the scene has been initialized
        Platform.runLater(() -> letterbox(scene, (Pane)root, primaryStage));

        // Show it
        //primaryStage.setMaximized(true);
        primaryStage.show();
    }

    private void letterbox(final Scene scene, final Pane contentPane, final Stage primaryStage) {
        Scale scale = new Scale(1,1,0,0);
        Translate translate = new Translate(0, 0);
        // Add listener to the scale setting
        CalliopeData.getInstance().getSettings().scalePercentProperty().addListener((observable, oldValue, newValue) -> {
            Double scaleFactor = newValue.scaleFactor();
            scale.setX(scaleFactor);
            scale.setY(scaleFactor);
            contentPane.setPrefWidth(scene.getWidth() * 1/scaleFactor);
            contentPane.setPrefHeight(scene.getHeight() * 1/scaleFactor);
        });
        // Add listeners to scene dimensions
        scene.widthProperty().addListener(observable -> {
            Double scaleFactor = CalliopeData.getInstance().getSettings().getScalePercent().scaleFactor();
            contentPane.setPrefWidth(scene.getWidth() * 1/scaleFactor);
        });
        scene.heightProperty().addListener(observable -> {
            Double scaleFactor = CalliopeData.getInstance().getSettings().getScalePercent().scaleFactor();
            contentPane.setPrefHeight(scene.getHeight() * 1/scaleFactor);
        });
        primaryStage.maximizedProperty().addListener(observable -> {
        });

        contentPane.getTransforms().addAll(scale, translate);
    }
}
