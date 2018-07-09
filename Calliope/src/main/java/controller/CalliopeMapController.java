package controller;

import controller.importView.SitePopOverController;
import controller.mapView.MapPinController;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import fxmapcontrol.*;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import library.CenteredMapNode;
import model.CalliopeData;
import model.elasticsearch.GeoBucket;
import model.neon.BoundedSite;
import model.util.FXMLLoaderUtils;
import org.controlsfx.control.PopOver;
import org.locationtech.jts.math.MathUtil;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller class for the map page
 */
public class CalliopeMapController implements Initializable
{
	///
	/// FXML bound fields start
	///

	@FXML
	public Map map;

	///
	/// FXML bound fields end
	///

	// A list of current pins on the map displaying circles with an image amount inside
	private List<MapNode> currentPins = new ArrayList<>();
	// A parallel list of controllers to that list of pins
	private List<MapPinController> currentPinControllers = new ArrayList<>();

	/**
	 * Initialize sets up the analysis window and bindings
	 *
	 * @param location ignored
	 * @param resources ignored
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		// Store image tiles inside of the user's home directory
		TileImageLoader.setCache(new ImageFileCache(new File(System.getProperty("user.home") + File.separator + "CalliopeMapCache").toPath()));

		// Use OpenStreetMap by default
		MapTileLayer openStreetMapLayer = MapTileLayer.getOpenStreetMapLayer();
		// Add the tile layer to the background
		this.map.getChildren().add(openStreetMapLayer);

		// Add a popover that we use to display location specifics
		PopOver popOver = new PopOver();
		popOver.setHeaderAlwaysVisible(false);
		popOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
		popOver.setArrowSize(20);
		popOver.setCloseButtonEnabled(true);
		// Load the content of the popover from the FXML file once and store it
		FXMLLoader fxmlLoader = FXMLLoaderUtils.loadFXML("importView/SitePopOver.fxml");
		// Grab the controller for use later
		SitePopOverController sitePopOverController = fxmlLoader.getController();
		// Store the content into the popover
		popOver.setContentNode(fxmlLoader.getRoot());

		// Create a map of bounded sites to polygons for quick lookup later
		HashMap<BoundedSite, Node> siteToPolygon = new HashMap<>();
		// When our site list changes, we update our map
		CalliopeData.getInstance().getSiteList().addListener((ListChangeListener<? super BoundedSite>) change ->
		{
			while (change.next())
				if (change.wasAdded())
				{
					// Iterate over all new bounded sites and add them to the map
					for (BoundedSite boundedSite : change.getAddedSubList())
					{
						// Grab the polygon representing the boundary of this site
						Polygon polygon = boundedSite.getBoundary();
						// Create a map polygon to render this site's boundary
						MapPolygon mapPolygon = new MapPolygon();
						// Setup the polygon's boundary
						mapPolygon.getLocations().addAll(polygon.getOuterBoundaryIs().getLinearRing().getCoordinates().stream().map(coordinate -> new Location(coordinate.getLatitude(), coordinate.getLongitude())).collect(Collectors.toList()));
						// Setup the polygon's center location point
						mapPolygon.setLocation(new Location(boundedSite.getSite().getSiteLatitude(), boundedSite.getSite().getSiteLongitude()));
						// Add a CSS attribute to all polygons so that we can style them later
						mapPolygon.getStyleClass().add("neon-site-boundary");
						// When we click a polygon, show the popover
						mapPolygon.setOnMouseClicked(event ->
						{
							// Call our controller's update method and then show the popup
							sitePopOverController.updateSite(boundedSite.getSite());
							popOver.show(mapPolygon);
							event.consume();
						});
						// Add the polygon to the map
						this.map.getChildren().add(mapPolygon);
						// Store a reference to the polygon in our hashmap to ensure we can retrieve it later in remove
						siteToPolygon.put(boundedSite, mapPolygon);
					}
				}
				// If a site is removed, lookup our site in the hashmap and clear it out
				else if (change.wasRemoved())
				{
					// Iterate over bounded sites
					for (BoundedSite boundedSite : change.getRemoved())
						// If the site is present in the polygon hashmap, remove it from the FXML hierarchy
						if (siteToPolygon.containsKey(boundedSite))
							this.map.getChildren().remove(siteToPolygon.get(boundedSite));
				}
		});

		// When the mouse gets dragged and released we update our pins
		this.map.mouseDraggingProperty().addListener((observable, oldValue, newValue) ->
		{
			// If we started dragging or got a null don't do anything
			if (newValue == null || newValue)
				return;
			// Redraw all the pin circles with new values because we moved our map
			this.replacePins();
		});

		// When the mouse scroll is touched and the zoom is changed we update our pins too
		this.map.zoomLevelProperty().addListener((observable, oldValue, newValue) -> this.replacePins());
	}

	/**
	 * Utility function that is optimized for performance. Called whenever the map moves and the aggregated circles need to be redrawn
	 */
	private void replacePins()
	{
		// Compute the bounds of the map inside of the window, this is used to compute the extent to which we can see the map
		Bounds boundsInParent = this.map.getBoundsInParent();
		// Using the bounds we compute the maximum and minimum lat/long values which we will pass to elasticsearch later
		Location topLeft = this.map.getProjection().viewportPointToLocation(new Point2D(boundsInParent.getMinX(), boundsInParent.getMinY()));
		Location bottomRight = this.map.getProjection().viewportPointToLocation(new Point2D(boundsInParent.getMaxX(), boundsInParent.getMaxY()));
		// This is the important line. We call ES to perform an aggregation of all uploaded images given bounds and a zoom level.
		// This call will return a list of buckets including number of images per bucket and centroids for each bucket
		List<GeoBucket> geoBuckets = CalliopeData.getInstance().getEsConnectionManager().performGeoAggregation(
				MathUtil.clamp(topLeft.getLatitude(), -90.0, 90.0),
				MathUtil.clamp(topLeft.getLongitude(), -180.0, 180.0),
				MathUtil.clamp(bottomRight.getLatitude(), -90.0, 90.0),
				MathUtil.clamp(bottomRight.getLongitude(), -180.0, 180.0),
				this.depthForCurrentZoom());

		// If we have more buckets than currently existing pins, create more pins until the two buckets have the same size.
		// We do this so we avoid allocating significant chunks of memory every time this gets called. Try to re-use buckets we already have
		while (geoBuckets.size() > currentPins.size())
		{
			MapNode newPin = this.createPin();
			this.map.getChildren().add(newPin);
		}
		// If we have less buckets than currently existing pins, remove pins until the two buckets have the same size. We
		// do this to avoid having to allocate any memory at all
		while (currentPins.size() > geoBuckets.size())
		{
			MapNode toRemove = currentPins.remove(currentPins.size() - 1);
			currentPinControllers.remove(currentPinControllers.size() - 1);
			this.map.getChildren().remove(toRemove);
		}

		// At this point our buckets should have the same size, test that here
		if (currentPins.size() == geoBuckets.size())
		{
			// Iterate over all pins on the map, we're going to update all of their positions and labels
			for (Integer i = 0; i < currentPins.size(); i++)
			{
				// Grab the bucket for that pin
				GeoBucket geoBucket = geoBuckets.get(i);
				// Grab the pin
				MapNode mapNode = currentPins.get(i);
				// Grab the controller for that pin
				MapPinController mapPinController = currentPinControllers.get(i);
				// Update the pin and the controller for that pin
				mapNode.setLocation(new Location(geoBucket.getCenterLatitude(), geoBucket.getCenterLongitude()));
				mapPinController.setImageCount(geoBucket.getDocumentCount());
			}
		}
		else
		{
			// If the buckets are not the same size something went wrong, throw an error
			CalliopeData.getInstance().getErrorDisplay().notify("Pin bucket and geo bucket were not the same size after normalization, this is an error!");
		}
	}

	/**
	 * Allocates a new pin node and stores the required references into the lists
	 *
	 * @return Returns a newly allocated map node with its controller and reference stored into parallel lists
	 */
	private MapNode createPin()
	{
		// Create a new pin object, call our custom class that ensures the node remains centered on the geo-location
		MapNode pin = new CenteredMapNode();
		// Load the FXML document representing this pin
		FXMLLoader fxmlLoader = FXMLLoaderUtils.loadFXML("mapView/MapPin.fxml");
		// Add FXML document to this pin
		pin.getChildren().add(fxmlLoader.getRoot());
		// Store the pin and its controller into the parallel lists
		this.currentPins.add(pin);
		this.currentPinControllers.add(fxmlLoader.getController());
		// Return the pin
		return pin;
	}

	/**
	 * Function that returns a depth value to be used by the ES geo aggregator. This value can be between 1-12, where
	 * 1 means aggregate locations together within a few hundred miles and 12 means locations together within a meter
	 *
	 * @return A value between 1 and 12 based on the map's current zoom
 	 */
	private Integer depthForCurrentZoom()
	{
		// Get the map's current zoom level
		Double zoom = this.map.getZoomLevel();
		// Based on that zoom level, return an appropriate amount of aggregation
		if (zoom < 5)
			return 1;
		else if (zoom < 10)
			return 3;
		else if (zoom < 12)
			return 4;
		else if (zoom < 14)
			return 5;
		else if (zoom < 16)
			return 6;
		else if (zoom < 18)
			return 7;
		else return 8;
	}

}
