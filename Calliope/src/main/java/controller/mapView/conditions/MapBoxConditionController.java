package controller.mapView.conditions;

import controller.mapView.IConditionController;
import controller.mapView.LayeredMap;
import fxmapcontrol.Location;
import fxmapcontrol.Map;
import fxmapcontrol.MapPolygon;
import fxmapcontrol.MapProjection;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseEvent;
import javafx.util.StringConverter;
import jfxtras.scene.control.ListView;
import model.elasticsearch.query.QueryCondition;
import model.elasticsearch.query.conditions.MapBoxCondition;
import org.controlsfx.control.MaskerPane;
import org.controlsfx.control.ToggleSwitch;

import java.util.EnumSet;

/**
 * Condition controller for the map box condition
 */
public class MapBoxConditionController implements IConditionController
{
	///
	/// FXML Bound Fields Start
	///

	// The button that allows us to draw on the map
	@FXML
	public Button btnDraw;
	// The toggle switch that shows and hides the bounding box
	@FXML
	public ToggleSwitch tswShow;
	// The masker pane that appears when we are waiting for a bounding box to be drawn
	@FXML
	public MaskerPane mpnWaiting;
	// The list of coordinates that make up the bounding box
	@FXML
	public ListView<Location> lvwCoordinates;

	///
	/// FXML Bound Fields End
	///

	// Property that lets us know if we are awaiting the user to draw a box
	private Boolean awaitingBox = false;
	// Boolean that lets us know if we are currently drawing a box
	private Boolean drawingBox = false;
	// The X and Y coordinates of the starting position of the box
	private double startX;
	private double startY;
	// The data model containing the polygon condition
	private MapBoxCondition mapBoxCondition;

	// 3 action listeners that allow us to draw the box on the map
	private EventHandler<MouseEvent> onDragDetected;
	private EventHandler<MouseEvent> onMouseDragged;
	private EventHandler<MouseEvent> onMouseReleased;

	/**
	 * Initialize sets up the list view of coordinates with a cell factory
	 */
	@FXML
	public void initialize()
	{
		// The cell factory provides a one way conversion from location -> string
		this.lvwCoordinates.setCellFactory(x -> new TextFieldListCell<>(new StringConverter<Location>()
		{
			@Override
			public String toString(Location location)
			{
				return location.getLatitude() + ", " + location.getLongitude();
			}
			@Override
			public Location fromString(String string)
			{
				return null;
			}
		}));
	}

	/**
	 * Given a query condition this function initializes the controller's fields with the data object
	 *
	 * @param queryCondition The query condition data model to bind to this controller
	 */
	@Override
	public void initializeData(QueryCondition queryCondition)
	{
		// If the map box condition is non-null wipe out the previous event listeners
		if (mapBoxCondition != null)
		{
			LayeredMap map = mapBoxCondition.getMap();
			map.removeEventHandler(MouseEvent.DRAG_DETECTED, onDragDetected);
			map.removeEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDragged);
			map.removeEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleased);
		}

		// If we have new valid data, update this cell to display that data
		if (queryCondition instanceof MapBoxCondition)
		{
			// Store the data model
			this.mapBoxCondition = (MapBoxCondition) queryCondition;
			// Grab the map and polygon from the data model
			MapPolygon polygon = this.mapBoxCondition.getPolygon();
			LayeredMap map = this.mapBoxCondition.getMap();

			// Unbind the polygon's visibility property
			polygon.visibleProperty().unbind();
			// Update the toggle switch based on the polygon's visibility property
			this.tswShow.setSelected(polygon.isVisible());
			// Then bind the polygon's visibility property to the toggle switch
			polygon.visibleProperty().bind(this.tswShow.selectedProperty());

			// Update the list of coordinates to be the polygon's locations
			this.lvwCoordinates.setItems(polygon.getLocations());

			// Setup our listeners

			// When we drag, we store the start X,Y and set flags
			this.onDragDetected = event ->
			{
				// If we're awaiting a bounding box, we're good to go
				if (awaitingBox)
				{
					// We're not longer awaiting a bounding box
					this.awaitingBox = false;
					// Allow the map to be moved normally again, this only takes effect after we drop the drag
					map.setManipulationModes(Map.ManipulationModes.DEFAULT);
					// Compute the start X and Y and store it
					Point2D point2D = map.screenToLocal(event.getScreenX(), event.getScreenY());
					this.startX = point2D.getX();
					this.startY = point2D.getY();
					// Set the drawing box flag to true because we are now drawing
					this.drawingBox = true;
				}
			};
			// When we drag the mouse...
			this.onMouseDragged = event ->
			{
				// If we're drawing a box...
				if (this.drawingBox)
				{
					// Compute the current mouse X and Y
					Point2D point2D = map.screenToLocal(event.getScreenX(), event.getScreenY());
					double currentX = point2D.getX();
					double currentY = point2D.getY();
					// Grab the map projection and list of locations
					MapProjection mapProjection = map.getProjection();
					ObservableList<Location> locations = polygon.getLocations();
					// Clear the list of locations and add the 4 corners of the box
					locations.clear();
					locations.add(mapProjection.viewportPointToLocation(new Point2D(this.startX, this.startY)));
					locations.add(mapProjection.viewportPointToLocation(new Point2D(this.startX + (currentX - this.startX), this.startY)));
					locations.add(mapProjection.viewportPointToLocation(new Point2D(this.startX + (currentX - this.startX), this.startY + (currentY - this.startY))));
					locations.add(mapProjection.viewportPointToLocation(new Point2D(this.startX, this.startY + (currentY - this.startY))));
				}
			};
			// When releasing the mouse we stop the drawing box flag and hide the masker pane
			this.onMouseReleased = event ->
			{
				if (this.drawingBox)
				{
					this.drawingBox = false;
					this.mpnWaiting.setVisible(false);
				}
			};

			// We add our default action listeners to the map
			map.addEventHandler(MouseEvent.DRAG_DETECTED, onDragDetected);
			map.addEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDragged);
			map.addEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleased);
		}
	}

	/**
	 * When we click the draw bounding box flag set flags and don't allow map dragging
	 *
	 * @param actionEvent consumed
	 */
	public void drawBoundingBox(ActionEvent actionEvent)
	{
		// Set the waiting flag to true
		this.awaitingBox = true;
		// Don't allow the map to be dragged
		this.mapBoxCondition.getMap().setManipulationModes(EnumSet.noneOf(Map.ManipulationModes.class));
		// Hide the waiting masker pane
		this.mpnWaiting.setVisible(true);
		actionEvent.consume();
	}
}
