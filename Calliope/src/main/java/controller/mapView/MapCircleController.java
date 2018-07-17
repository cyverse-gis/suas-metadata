package controller.mapView;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import model.elasticsearch.GeoBucket;

/**
 * Simple controller file used to control pin behaviour when placed on the map
 */
public class MapCircleController
{
	///
	/// FXML Bound Fields Start
	///

	// The label containing the number of images in the pin aggregation
	@FXML
	public Label lblImageCount;
	// The background circle that will dynamically change its size
	@FXML
	public Circle crlBackground;

	///
	/// FXML Bound Fields End
	///

	// A reference to our data model source
	private GeoBucket geoBucket = null;

	/**
	 * Initialize doesn't do anything here
	 */
	@FXML
	public void initialize()
	{
	}

	/**
	 * Setter for image geo-bucket just updates the label
	 *
	 * @param geoBucket The bucket to update the label with
	 */
	public void updateItem(GeoBucket geoBucket)
	{
		this.geoBucket = geoBucket;
		this.crlBackground.setRadius(10 + 6 * (this.geoBucket.getDocumentCount().toString().length() - 1));
		this.lblImageCount.setText(this.geoBucket.getDocumentCount().toString());
	}

	/**
	 * Getter for geo-bucket for a given circle
	 *
	 * @return The bucket associated with a given circle
	 */
	public GeoBucket getGeoBucket()
	{
		return this.geoBucket;
	}
}
