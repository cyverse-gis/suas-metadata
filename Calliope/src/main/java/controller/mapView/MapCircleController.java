package controller.mapView;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Simple controller file used to control pin behaviour when placed on the map
 */
public class MapCircleController implements Initializable
{
	///
	/// FXML Bound Fields Start
	///

	// The label containing the number of images in the pin aggregation
	@FXML
	public Label lblImageCount;
	@FXML
	public Circle crlBackground;

	///
	/// FXML Bound Fields End
	///

	/**
	 * Initialize doesn't do anything here
	 *
	 * @param location ignored
	 * @param resources ignored
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources)
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
