package controller.mapView;

import javafx.animation.FillTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
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

	// If this circle is selected
	private BooleanProperty selected = new SimpleBooleanProperty(false);

	// Four static color objects, one for if the circle is selected and one if it is not, and one highlighted version of each
	private static final Color REGULAR_COLOR = Color.color(1.0, 0.64705884, 0.0);
	private static final Color HIGHLIGHTED_COLOR = Color.color(0.996, 0.815, 0.486);
	private static final Color SELECTED_REGULAR_COLOR = Color.color(0.984, 0.941, 0.015);
	private static final Color SELECTED_HIGHLIGHTED_COLOR = Color.color(0.996, 0.980, 0.725);

	/**
	 * Initialize doesn't do anything here
	 */
	@FXML
	public void initialize()
	{
		// Set the default color
		this.crlBackground.setFill(REGULAR_COLOR);

		// Create a fill transition used to perform color fades
		FillTransition fillTransition = new FillTransition();
		fillTransition.setDuration(Duration.seconds(0.2));
		fillTransition.setShape(this.crlBackground);
		// When we hover the circle, change the color
		this.crlBackground.hoverProperty().addListener((observable, oldValue, hovered) ->
		{
			// Set the from and to color of the circle
			Color to = null;
			Color from = null;

			// Pick to and from colors based on if this circle is selected and if the circle is hovered or not
			if      (hovered && this.isSelected())   { to = SELECTED_HIGHLIGHTED_COLOR; from = SELECTED_REGULAR_COLOR; }
			else if (hovered && !this.isSelected())  { to = HIGHLIGHTED_COLOR;          from = REGULAR_COLOR; }
			else if (!hovered && this.isSelected())  { to = SELECTED_REGULAR_COLOR;     from = SELECTED_HIGHLIGHTED_COLOR; }
			else if (!hovered && !this.isSelected()) { to = REGULAR_COLOR;              from = HIGHLIGHTED_COLOR; }

			// Set the to and from colors, and then play the animation from the start
			fillTransition.setFromValue(from);
			fillTransition.setToValue(to);
			fillTransition.playFromStart();
		});
		// When the circle gets selected/deselected
		this.selectedProperty().addListener((observable, oldValue, selected) ->
		{
			// Set the from and to color of the circle
			Color to;
			Color from;

			// Pick to and from colors based on if this circle is selected
			if      (selected) { to = SELECTED_HIGHLIGHTED_COLOR; from = HIGHLIGHTED_COLOR; }
			else               { to = REGULAR_COLOR;              from = SELECTED_REGULAR_COLOR; }

			// Set the to and from colors, and then play the animation from the start
			fillTransition.setFromValue(from);
			fillTransition.setToValue(to);
			fillTransition.playFromStart();
		});
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
			this.setSelected(false);

		this.geoBucket = geoBucket;
		// Use a dynamic radius that grows as the number of images increases
		this.crlBackground.setRadius(10 + 6 * (this.geoBucket.getDocumentCount().toString().length() - 1));
		this.lblImageCount.setText(this.geoBucket.getDocumentCount().toString());
	}

	/**
	 * @param selected Set if the circle is selected or not
	 */
	public void setSelected(Boolean selected)
	{
		this.selected.setValue(selected);
	}

	/**
	 * @return True if the circle is selected, false otherwise
	 */
	public boolean isSelected()
	{
		return this.selected.getValue();
	}

	/**
	 * @return The selected property
	 */
	public BooleanProperty selectedProperty()
	{
		return this.selected;
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
