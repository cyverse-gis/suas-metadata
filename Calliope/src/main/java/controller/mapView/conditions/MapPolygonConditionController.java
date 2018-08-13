package controller.mapView.conditions;

import controller.mapView.IConditionController;
import controller.mapView.LayeredMap;
import controller.mapView.MapLayers;
import controller.mapView.conditions.handle.HandleController;
import fxmapcontrol.*;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseEvent;
import javafx.util.StringConverter;
import jfxtras.scene.control.ListView;
import library.AlignedMapNode;
import model.elasticsearch.query.QueryCondition;
import model.elasticsearch.query.conditions.MapPolygonCondition;
import model.util.AnalysisUtils;
import model.util.FXMLLoaderUtils;
import org.controlsfx.control.MaskerPane;
import org.controlsfx.control.ToggleSwitch;
import org.locationtech.spatial4j.distance.DistanceUtils;

import java.util.EnumSet;
import java.util.HashMap;

/**
 * Condition controller for the map polygon condition
 */
public class MapPolygonConditionController implements IConditionController
{
	///
	/// FXML Bound Fields Start
	///

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
	private MapPolygonCondition mapPolygonCondition;

	// 3 action listeners that allow us to draw the box on the map
	private EventHandler<MouseEvent> onDragDetected;
	private EventHandler<MouseEvent> onMouseDragged;
	private EventHandler<MouseEvent> onMouseReleased;

	// A hash map of locations to handle map nodes
	private java.util.Map<Location, Node> locationToHandle = new HashMap<>();

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
		// If the map box condition is non-null wipe out any changes we made to the map
		if (mapPolygonCondition != null)
		{
			LayeredMap map = mapPolygonCondition.getMap();
			// Such as map listeners.....
			map.removeEventHandler(MouseEvent.DRAG_DETECTED, onDragDetected);
			map.removeEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDragged);
			map.removeEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleased);
			// And handle nodes...
			for (Node node : this.locationToHandle.values())
				map.removeChild(node);
		}

		// If we have new valid data, update this cell to display that data
		if (queryCondition instanceof MapPolygonCondition)
		{
			// Store the data model
			this.mapPolygonCondition = (MapPolygonCondition) queryCondition;
			// Grab the map and polygon from the data model
			MapPolygon polygon = this.mapPolygonCondition.getPolygon();
			LayeredMap map = this.mapPolygonCondition.getMap();

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
					this.rebuildHandles();
					this.mpnWaiting.setVisible(false);
				}
			};

			// We add our default action listeners to the map
			map.addEventHandler(MouseEvent.DRAG_DETECTED, onDragDetected);
			map.addEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDragged);
			map.addEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleased);

			// Now that we've got a new polygon build handles for it
			this.rebuildHandles();
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
		this.mapPolygonCondition.getMap().setManipulationModes(EnumSet.noneOf(Map.ManipulationModes.class));
		// Hide the waiting masker pane
		this.mpnWaiting.setVisible(true);
		actionEvent.consume();
	}

	/**
	 * When we click the add vertex button which adds a vertex to the polygon
	 *
	 * @param actionEvent consumed
	 */
	public void addVertex(ActionEvent actionEvent)
	{
		// References to the furthest two points
		Location avgPoint1 = null;
		Location avgPoint2 = null;
		// The current
		double currentMaxDistance = 0D;

		// Grab the list of locations
		ObservableList<Location> locations = this.mapPolygonCondition.getPolygon().getLocations();
		// Iterate over all locations
		for (int i = 0; i < locations.size(); i++)
		{
			// Grab the two adjacent locations
			Location first  = locations.get(i);
			// Ensure that we wrap around if we hit the end of the list...
			Location second = (i + 1) == locations.size() ? locations.get(0) : locations.get(i + 1);
			// Compute the distance between the points
			double distance = AnalysisUtils.distanceBetween(first.getLatitude(), first.getLongitude(), second.getLatitude(), second.getLongitude());
			// Test if the distance is bigger than the current max
			if (distance > currentMaxDistance)
			{
				// Set the current max distance and points
				currentMaxDistance = distance;
				avgPoint1 = first;
				avgPoint2 = second;
			}
		}

		// If the max distance is non 0, add the location
		if (currentMaxDistance > 0)
		{
			// Add the a new vertex in the middle of the furthest two vertices.
			locations.add(locations.indexOf(avgPoint1) + 1, new Location((avgPoint1.getLatitude() + avgPoint2.getLatitude()) / 2, (avgPoint1.getLongitude() + avgPoint2.getLongitude()) / 2));
			// Rebuild any handles we've created
			this.rebuildHandles();
		}

		actionEvent.consume();
	}

	/**
	 * Takes the current polygon and builds map handles for it
	 */
	private void rebuildHandles()
	{
		// Remove any existing handles
		locationToHandle.values().forEach(handle -> this.mapPolygonCondition.getMap().removeChild(handle));
		locationToHandle.clear();

		// For each location on the box, add a handle at each position
		ObservableList<Location> locations = this.mapPolygonCondition.getPolygon().getLocations();
		for (Location location : locations)
		{
			// Create a handle at the location's point
			MapNode handle = new AlignedMapNode(Pos.CENTER);
			// Set the handle's center to be the location's center
			handle.setLocation(location);

			// Load a handle FXML document for the handle
			FXMLLoader fxmlLoader = FXMLLoaderUtils.loadFXML("mapView/conditions/handle/Handle.fxml");
			// Store the controller
			HandleController handleController = fxmlLoader.getController();
			// Set the handle controller's fields
			handleController.setLocation(location);
			handleController.setMap(this.mapPolygonCondition.getMap());
			handle.locationProperty().bind(handleController.locationProperty());
			// When the location property changes we replace the location found in the polygon's location list
			handle.locationProperty().addListener((observable, oldValue, newValue) ->
			{
				int oldIndex = locations.indexOf(oldValue);
				locations.set(oldIndex, newValue);
			});
			// Hide the handle when the toggle switch is off
			handle.visibleProperty().bind(this.tswShow.selectedProperty());

			// Add the handle to the map node
			handle.getChildren().add(fxmlLoader.getRoot());

			// Store a reference to the handle in our map
			locationToHandle.put(location, handle);
			// Add the child to our map
			this.mapPolygonCondition.getMap().addChild(handle, MapLayers.QUERY_CORNER);
		}
	}
}
