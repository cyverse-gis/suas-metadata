package controller.mapView;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;

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

	/**
	 * Initialize doesn't do anything here
	 */
	@FXML
	public void initialize()
	{
	}

	/**
	 * Setter for image count just updates the count label
	 *
	 * @param count The number of images in the bucket to update the label with
	 */
	public void setImageCount(Long count)
	{
		this.crlBackground.setRadius(10 + 6 * (count.toString().length() - 1));
		this.lblImageCount.setText(count.toString());
	}
}
