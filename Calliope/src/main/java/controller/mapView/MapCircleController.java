package controller.mapView;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.paint.*;
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

	// Two static color objects, one for if the circle is highlighted and one if it is not
	private static final Paint REGULAR_COLOR = new RadialGradient(0, 0, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.ORANGE), new Stop(0.5, Color.WHEAT));
	private static final Paint HIGHLIT_COLOR = new RadialGradient(0, 0, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.YELLOW), new Stop(0.5, Color.WHITESMOKE));

	/**
	 * Initialize doesn't do anything here
	 */
	@FXML
	public void initialize()
	{
		this.crlBackground.setFill(REGULAR_COLOR);
	}

	/**
	 * Setter for image geo-bucket just updates the label
	 *
	 * @param geoBucket The bucket to update the label with
	 */
	public void updateItem(GeoBucket geoBucket)
	{
		// We hide the highlighting if the new geo-bucket contains a different amount of images or the lat/long are different
		if (this.geoBucket == null || !this.geoBucket.getDocumentCount().equals(geoBucket.getDocumentCount()) || !this.geoBucket.getCenterLatitude().equals(geoBucket.getCenterLatitude()) || !this.geoBucket.getCenterLongitude().equals(geoBucket.getCenterLongitude()))
			this.setHighlighted(false);

		this.geoBucket = geoBucket;
		// Use a dynamic radius that grows as the number of images increases
		this.crlBackground.setRadius(10 + 6 * (this.geoBucket.getDocumentCount().toString().length() - 1));
		this.lblImageCount.setText(this.geoBucket.getDocumentCount().toString());
	}

	/**
	 * Makes the circle yellow or white depending on if it's selected or not
	 *
	 * @param highlighted If true the circle will be white, yellow otherwise
	 */
	public void setHighlighted(Boolean highlighted)
	{
		if (highlighted)
			this.crlBackground.setFill(HIGHLIT_COLOR);
		else
			this.crlBackground.setFill(REGULAR_COLOR);
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
