package controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.CalliopeData;
import model.util.FXMLLoaderUtils;
import org.fxmisc.easybind.EasyBind;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Controller class for the main program
 */
public class CalliopeHomeController
{
	///
	/// FXML bound fields start
	///

	// The credits button
	@FXML
	public Button btnCredits;
	// The help button opens up the github page
	@FXML
	public Button btnHelp;
	// The exit button to close the program
	@FXML
	public Button btnExit;
	// After logging in this shows the username of the logged in person
	@FXML
	public Label lblUsername;

	// The main anchor pane in the background
	@FXML
	public AnchorPane mainPane;

	// The background image containing the camera trap image
	@FXML
	public ImageView backgroundImage;


	///
	/// FXML bound fields end
	///

	/**
	 * Initialize sets up the analysis window and bindings
	 */
	@FXML
	public void initialize()
	{
		// If we're logged in show the logged in person's username
		this.lblUsername.textProperty().bind(EasyBind.monadic(CalliopeData.getInstance().usernameProperty()).map(username -> "Welcome " + username + "!").orElse(""));

		// Grab the logged in property
		ReadOnlyBooleanProperty loggedIn = CalliopeData.getInstance().loggedInProperty();

		// Hide the logout button and text when not logged in
		this.lblUsername.visibleProperty().bind(loggedIn);
		this.btnCredits.visibleProperty().bind(loggedIn);
		this.btnHelp.visibleProperty().bind(loggedIn);
		this.btnExit.visibleProperty().bind(loggedIn);
	}

	/**
	 * When exit is pressed close the program
	 *
	 * @param actionEvent ignored
	 */
	@FXML
	public void exitPressed(ActionEvent actionEvent)
	{
		// Shutdown any executing threads
		CalliopeData.getInstance().getExecutor().shutdown();
		// Shutdown ExifTool
		CalliopeData.getInstance().getMetadataManager().shutdown();
		// Clear any temp files made
		CalliopeData.getInstance().getTempDirectoryManager().shutdown();
		// Kill the Application
		Platform.exit();
		// Exit JVM
		System.exit(0);
	}

	/**
	 * When the user clicks the cyverse logo
	 *
	 * @param mouseEvent consumed
	 */
	public void showCyverseWebsite(MouseEvent mouseEvent)
	{
		try
		{
			Desktop.getDesktop().browse(new URI("http://www.cyverse.org"));
		}
		catch (IOException | URISyntaxException ignored)
		{
		}
		mouseEvent.consume();
	}

	/**
	 * When the user clicks the UA SNRE logo
	 *
	 * @param mouseEvent consumed
	 */
	public void showSNREWebsite(MouseEvent mouseEvent)
	{
		try
		{
			Desktop.getDesktop().browse(new URI("https://snre.arizona.edu/"));
		}
		catch (IOException | URISyntaxException ignored)
		{
		}
		mouseEvent.consume();
	}

	/**
	 * When the user clicks the ARS logo
	 *
	 * @param mouseEvent consumed
	 */
	public void showARSWebsite(MouseEvent mouseEvent)
	{
		try
		{
			Desktop.getDesktop().browse(new URI("https://www.usda.gov/"));
		}
		catch (IOException | URISyntaxException ignored)
		{
		}
		mouseEvent.consume();
	}

	/**
	 * When the user clicks the credits button
	 *
	 * @param actionEvent consumed
	 */
	public void creditsPressed(ActionEvent actionEvent)
	{
		// Load the FXML file of the editor window
		FXMLLoader loader = FXMLLoaderUtils.loadFXML("homeView/Credits.fxml");

		if (!CalliopeData.getInstance().getSettings().getDisablePopups())
		{
			// Create the stage that will have the species creator/editor
			Stage dialogStage = new Stage();
			// Set the title
			dialogStage.setTitle("Credits");
			// Set the modality and initialize the owner to be this current window
			dialogStage.initModality(Modality.WINDOW_MODAL);
			dialogStage.initOwner(this.mainPane.getScene().getWindow());
			// Set the scene to the root of the FXML file
			Scene scene = new Scene(loader.getRoot());
			// Set the scene of the stage, and show it!
			dialogStage.setScene(scene);
			dialogStage.setResizable(false);
			dialogStage.showAndWait();
		}
		else
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Popups must be enabled to see credits");
		}

		actionEvent.consume();
	}

	/**
	 * When the help button is pressed we show the github user manual
	 *
	 * @param actionEvent consumed
	 */
	public void helpPressed(ActionEvent actionEvent)
	{
		try
		{
			Desktop.getDesktop().browse(new URI("https://github.com/cyverse-gis/suas-metadata/tree/master/Calliope"));
		}
		catch (IOException | URISyntaxException ignored)
		{
		}
		actionEvent.consume();
	}
}
