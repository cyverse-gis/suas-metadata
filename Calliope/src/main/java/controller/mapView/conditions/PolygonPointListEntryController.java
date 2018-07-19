package controller.mapView.conditions;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import model.constant.CalliopeDataFormats;
import model.elasticsearch.query.conditions.ObservableLocation;
import model.image.ImageEntry;
import model.util.AnalysisUtils;
import org.fxmisc.easybind.EasyBind;

/**
 * The controller for the polygon point list entry
 */
public class PolygonPointListEntryController extends ListCell<ObservableLocation>
{
	///
	/// FXML Bound Fields Start
	///

	// A reference to the main background pane
	@FXML
	public GridPane mainPane;

	// The latitude and longitude labels
	@FXML
	public Label lblLatitude;
	@FXML
	public Label lblLongitude;

	// The button used to drag onto the map and set the latitude/longitude
	@FXML
	public Button btnLocationDragger;

	///
	/// FXML Bound Fields End
	///

	/**
	 * Update item is called when the list cell gets a new polygon point
	 *
	 * @param observableLocation The new polygon point
	 * @param empty if the cell will be empty
	 */
	@Override
	protected void updateItem(ObservableLocation observableLocation, boolean empty)
	{
		// Update the cell internally
		super.updateItem(observableLocation, empty);

		// Set the text of the cell to nothing
		this.setText(null);

		// If no polygon point was given and the cell was empty, clear the graphic
		if (empty && observableLocation == null)
		{
			this.setGraphic(null);
		}
		else
		{
			// Bind the latitude and longitude labels to the data model. We map the model's raw value from a double to a rounded double and finally to a string
			this.lblLatitude.textProperty().unbind();
			this.lblLatitude.textProperty().bind(EasyBind.monadic(observableLocation.latitudeProperty()).map(number -> Double.toString(AnalysisUtils.round(number.doubleValue(), 5))));
			this.lblLongitude.textProperty().unbind();
			this.lblLongitude.textProperty().bind(EasyBind.monadic(observableLocation.longitudeProperty()).map(number -> Double.toString(AnalysisUtils.round(number.doubleValue(), 5))));
			// Set the graphic to display
			this.setGraphic(mainPane);
		}
	}

	/**
	 * When we drag the polygon location we start a drag and drop which can be dropped onto the map to set lat & long values
	 *
	 * @param mouseEvent consumed
	 */
	public void dragPolygonPoint(MouseEvent mouseEvent)
	{
		// Grab the selected polygon point, make sure it's not null
		ObservableLocation selected = this.getItem();
		if (selected != null)
		{
			// Create a dragboard and begin the drag and drop
			Dragboard dragboard = this.btnLocationDragger.startDragAndDrop(TransferMode.COPY);

			// Set the drag icon to be a pin. The image is 31x31 so we move it up 31 pixels and over by 1/2 to center the pin on the cursor
			dragboard.setDragView(new Image(ImageEntry.class.getResource("/images/mapWindow/pin.png").toString()));
			dragboard.setDragViewOffsetX(15);
			dragboard.setDragViewOffsetY(31);

			// Create a clipboard and put the location unique ID into that clipboard
			ClipboardContent content = new ClipboardContent();
			content.put(CalliopeDataFormats.AWAITING_POLYGON, true);
			// Set the dragboard's context, and then consume the event
			dragboard.setContent(content);

			// Hide the location dragger button's icon while dragging
			this.btnLocationDragger.getGraphic().setVisible(false);
		}

		mouseEvent.consume();
	}

	/**
	 * Called when the drag we started on the button is dropped somewhere
	 *
	 * @param dragEvent consumed
	 */
	public void dragFinished(DragEvent dragEvent)
	{
		Dragboard dragboard = dragEvent.getDragboard();
		// If we dropped this drag onto the map the map will fill in latitude and longitude fields
		if (dragboard.hasContent(CalliopeDataFormats.POLYGON_LATITUDE_FORMAT) && dragboard.hasContent(CalliopeDataFormats.POLYGON_LONGITUDE_FORMAT))
		{
			// Grab the latitude and longitude doubles
			Double latitude = (Double) dragboard.getContent(CalliopeDataFormats.POLYGON_LATITUDE_FORMAT);
			Double longitude = (Double) dragboard.getContent(CalliopeDataFormats.POLYGON_LONGITUDE_FORMAT);
			// Set our data model's fields
			this.getItem().setLatitude(latitude);
			this.getItem().setLongitude(longitude);
		}

		// Show the dragger button icon's again
		this.btnLocationDragger.getGraphic().setVisible(true);

		dragEvent.consume();
	}
}
