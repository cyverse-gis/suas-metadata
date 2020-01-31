package controller.importView;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import model.CalliopeData;
import model.image.DataContainer;
import model.image.ImageEntry;
import model.site.Site;
import model.threading.ErrorTask;
import model.util.AnalysisUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SiteDetectorController
{
	///
	/// FXML bound fields start
	///

	// The radio button to select distance from center site detection
	@FXML
	public RadioButton rbnByDistance;
	// The radio button to select distance from center site detection
	@FXML
	public RadioButton rbnByBoundary;
	// The text field used by the distance select site detection
	@FXML
	public TextField txtDistance;
	// The button used to fire off our site detection
	@FXML
	public Button btnDetectSites;

	// Two HBoxes containing distance and boundary input. We hold these references so we can disable them if needed using radio buttons
	@FXML
	public HBox hbxDistance;
	@FXML
	public HBox hbxBoundary;

	///
	/// FXML bound fields end
	///

	// The list of entries this site detector should use
	private List<DataContainer> imageEntries = Collections.emptyList();

	/**
	 * Initializes this site detector form
	 */
	@FXML
	public void initialize()
	{
		// We disable the detect sites button if we have the distance radio button selected and the text is an invalid double
		this.btnDetectSites.disableProperty().bind(this.rbnByBoundary.selectedProperty().not().and(Bindings.createBooleanBinding(() -> !NumberUtils.isNumber(this.txtDistance.getText()), this.txtDistance.textProperty())));
		// Hide the boundary box if the boundary radio button is not selected
		//this.hbxBoundary.disableProperty().bind(this.rbnByBoundary.selectedProperty().not());
		// Hide the distance box if the distance radio button is not selected
		//this.hbxDistance.disableProperty().bind(this.rbnByDistance.selectedProperty().not());
	}

	/**
	 * Called to initialize our neon site detector with data
	 *
	 * @param entries The images/videos to process
	 */
	public void updateItems(List<DataContainer> entries)
	{
		this.imageEntries = entries;
		// Clear our text field because we have new data and the old distance value might no longer be valid
		this.txtDistance.clear();
	}

	/**
	 * When we close the window we intercept the event and hide the window instead of closing it
	 *
	 * @param actionEvent consumed to keep the window from closing
	 */
	public void closeWindow(ActionEvent actionEvent)
	{
		this.rbnByDistance.getScene().getWindow().hide();
		actionEvent.consume();
	}

	/**
	 * Called to detect which sites each image was taken at
	 *
	 * @param actionEvent consumed
	 */
	public void detectSites(ActionEvent actionEvent)
	{
		// If we are querying by distance, we can use our local version of the site data instead of querying our DB
		if (this.rbnByDistance.isSelected())
		{
			// Parse the distance text field
			Double maxDistance = NumberUtils.toDouble(this.txtDistance.getText(), 0);

			// Do this in a thread so we don't slow down the program
			ErrorTask<Site[]> detectTask = new ErrorTask<Site[]>()
			{
				/**
				 * Return a list of sites parallel to the list of images
				 *
				 * @return A list of sites parallel to the image list where each pair contains the image and the site for that image
				 */
				@Override
				protected Site[] call()
				{
					this.updateMessage("Detecting sites for images...");
					// Create the parallel array
					Site[] toReturn = new Site[imageEntries.size()];
					// Grab the raw sites from our cache
					List<Site> rawSites = CalliopeData.getInstance().getSiteManager().getSites();
					// For each image, find the closest site
					for (Integer i = 0; i < imageEntries.size(); i++)
					{
						if (i % 20 == 0)
							this.updateProgress(i, imageEntries.size());
						// Grab the image
						DataContainer toProcess = imageEntries.get(i);
						// Find the closest site to the image
						Site closest = CalliopeData.getInstance().getSiteManager().closestSiteTo(rawSites, toProcess.getPositionTaken().getLatitude(), toProcess.getPositionTaken().getLongitude());
						// Compute the distance to that site
						Double distance = AnalysisUtils.distanceBetween(closest.getCenter().getLat(), closest.getCenter().getLon(), toProcess.getPositionTaken().getLatitude(), toProcess.getPositionTaken().getLongitude());
						// If the distance is less than the one required, we found the right spot
						if (distance <= maxDistance)
							toReturn[i] = closest;
					}
					return toReturn;
				}
			};

			detectTask.setOnSucceeded(event ->
			{
				// When we finish set each image's site to the site we detected. Nulls are OK
				for (Integer i = 0; i < imageEntries.size(); i++)
					imageEntries.get(i).getSiteTaken().add(detectTask.getValue()[i]);
			});

			// Execute the task
			CalliopeData.getInstance().getExecutor().getQueuedExecutor().addTask(detectTask);
			// Hide the window now that it's done its job
			this.rbnByBoundary.getScene().getWindow().hide();
		}
		// If we are searching by boundary, we need ES to help us
		else if (this.rbnByBoundary.isSelected())
		{
			ErrorTask<Site[]> detectTask = new ErrorTask<Site[]>()
			{
				@Override
				protected Site[] call()
				{
					this.updateMessage("Detecting sites for images...");
					// Create an array of results to return
					Site[] toReturn = new Site[imageEntries.size()];

					// Grab the global site list
					List<Site> sites = CalliopeData.getInstance().getSiteManager().getSites();
					// Ask ES to give us a parallel array of site codes to our image entries
					String[] siteCodes = CalliopeData.getInstance().getEsConnectionManager().detectSites(imageEntries);
					// For each parallel entry, process it
					for (Integer i = 0; i < siteCodes.length; i++)
					{
						// Grab the site code
						String siteCode = siteCodes[i];
						// If the site code is null, set the value to return as null, if not, process the code
						if (siteCode != null)
						{
							// Grab the site associated with the site code
							Optional<Site> correctSite = sites.stream().filter(site -> site.getCode().equals(siteCode)).findFirst();
							// If we got a site, store it
							if (correctSite.isPresent())
								toReturn[i] = correctSite.get();
						}
					}

					return toReturn;
				}
			};

			detectTask.setOnSucceeded(event ->
			{
				// When we finish set each image's site to the site we detected. Nulls are OK
				for (Integer i = 0; i < imageEntries.size(); i++)
					imageEntries.get(i).getSiteTaken().add(detectTask.getValue()[i]);
			});


			// Execute the task
			CalliopeData.getInstance().getExecutor().getQueuedExecutor().addTask(detectTask);
			// Hide the window now that it's done its job
			this.rbnByBoundary.getScene().getWindow().hide();
		}
		actionEvent.consume();
	}

	/**
	 * Called when the clear sites button is pressed, removes all sites off of the images
	 *
	 * @param actionEvent consumed
	 */
	public void clearSites(ActionEvent actionEvent)
	{
		imageEntries.forEach(imageEntry -> imageEntry.getSiteTaken().clear());
		this.rbnByBoundary.getScene().getWindow().hide();
		actionEvent.consume();
	}
}
