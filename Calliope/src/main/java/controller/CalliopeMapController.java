package controller;

import controller.importView.SitePopOverController;
import controller.mapView.MapCircleController;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import fxmapcontrol.*;
import fxmapcontrol.Map;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import library.AlignedMapNode;
import model.CalliopeData;
import model.elasticsearch.GeoBucket;
import model.image.ImageEntry;
import model.neon.BoundedSite;
import model.threading.ErrorTask;
import model.threading.ReRunnableService;
import model.util.FXMLLoaderUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.controlsfx.control.PopOver;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.locationtech.jts.math.MathUtil;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;
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

	@FXML
	public ComboBox<MapProviders> cbxMapProvider;

	///
	/// FXML bound fields end
	///

	// A list of current pins on the map displaying circles with an image amount inside
	private List<MapNode> currentCircles = new ArrayList<>();
	// A parallel list of controllers to that list of pins
	private List<MapCircleController> currentCircleControllers = new ArrayList<>();
	// A constant pin icon that we cache so we dont have to load the image over and over again
	private static final Image PIN_ICON = new Image(ImageEntry.class.getResource("/images/mapWindow/neonIcon32.png").toString());
	// The zoom threshold where we start to render polygons instead of pins
	private static final Double PIN_TO_POLY_THRESHOLD = 10D;

	// Next 3 variables are a bit of a hack, but they allow us to have a z-order for our map
	// A cache of each node's Z order field
	private java.util.Map<Node, Integer> zOrder;
	// A sorted list which will be sorted by z-order
	private ObservableList<Node> sortedNodes;
	// A cache to a listener to avoid early garbage collection
	private Subscription subscriptionCache;

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

		// Initialize our z-order cache
		this.zOrder = new HashMap<>();
		// Let the base list be our custom observable list, we add to this list not map.getChildren()
		this.sortedNodes = FXCollections.observableArrayList();
		// Sort this custom list of nodes by the z-order, and then assign it to the map's children field. Now any nodes added to the sorted nodes list
		// will be automatically added to the map's children list in sorted order. Store a useless reference to the subscription otherwise it will be
		// garbage collected early
		this.subscriptionCache = EasyBind.listBind(this.map.getChildren(), new SortedList<>(this.sortedNodes, Comparator.comparing(node -> zOrder.getOrDefault(node, -1))));

		// Add the tile layer to the background, use OpenStreetMap by default
		this.addNodeToMap(MapProviders.OpenStreetMaps.getMapTileProvider(), 0);

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
		java.util.Map<BoundedSite, Pair<MapPolygon, MapNode>> siteToPolygonAndPin = new HashMap<>();
		// When our site list changes, we update our map
		CalliopeData.getInstance().getSiteList().addListener((ListChangeListener<? super BoundedSite>) change ->
		{
			while (change.next())
				if (change.wasAdded())
				{
					// Iterate over all new bounded sites and add them to the map
					for (BoundedSite boundedSite : change.getAddedSubList())
					{
						Location centerPoint = new Location(boundedSite.getSite().getSiteLatitude(), boundedSite.getSite().getSiteLongitude());

						// Grab the polygon representing the boundary of this site
						Polygon polygon = boundedSite.getBoundary();
						// Create a map polygon to render this site's boundary
						MapPolygon mapPolygon = new MapPolygon();
						// Setup the polygon's boundary
						mapPolygon.getLocations().addAll(polygon.getOuterBoundaryIs().getLinearRing().getCoordinates().stream().map(coordinate -> new Location(coordinate.getLatitude(), coordinate.getLongitude())).collect(Collectors.toList()));
						// Setup the polygon's center location point
						mapPolygon.setLocation(centerPoint);
						// Add a CSS attribute to all polygons so that we can style them later
						mapPolygon.getStyleClass().add("neon-site-boundary");
						// Make sure we can drag & drop through the polygon
						mapPolygon.setMouseTransparent(true);

						// Create a map pin to render the site's center point when zoomed out
						MapNode mapPin = new AlignedMapNode(Pos.CENTER);
						// Set the pin's center to be the node's center
						mapPin.setLocation(centerPoint);
						// Add a new imageview to the pin
						ImageView pinImageView = new ImageView(PIN_ICON);
						// Add the image to the pin
						mapPin.getChildren().add(pinImageView);
						// When we click a pin, show the popover
						mapPin.setOnMouseClicked(event ->
						{
							// Call our controller's update method and then show the popup
							sitePopOverController.updateSite(boundedSite.getSite());
							popOver.show(mapPin);
							event.consume();
						});

						// If we're zoomed in far enough, show the polygon, otherwise show the pin
						if (this.map.getZoomLevel() > PIN_TO_POLY_THRESHOLD)
							this.addNodeToMap(mapPolygon, 1);
						else
							this.addNodeToMap(mapPin, 3);

						// Store a reference to the polygon in our hashmap to ensure we can retrieve it later in remove
						siteToPolygonAndPin.put(boundedSite, MutablePair.of(mapPolygon, mapPin));
					}
				}
				// If a site is removed, lookup our site in the hashmap and clear it out
				else if (change.wasRemoved())
				{
					// Iterate over bounded sites
					for (BoundedSite boundedSite : change.getRemoved())
						// If the site is present in the polygon hashmap, remove it from the FXML hierarchy
						if (siteToPolygonAndPin.containsKey(boundedSite))
						{
							Pair<MapPolygon, MapNode> polygonAndPinPair = siteToPolygonAndPin.remove(boundedSite);
							this.removeNodeFromMap(polygonAndPinPair.getLeft());
							this.removeNodeFromMap(polygonAndPinPair.getRight());
						}
				}
		});

		// Service that runs in the background drawing new circles every time the user zooms in and out
		ReRunnableService<List<GeoBucket>> circleDrawingService = new ReRunnableService<>(() ->
		{
			// Compute the bounds of the map inside of the window, this is used to compute the extent to which we can see the map
			// We do it here so that it's on the FXApplicationThread and not in our custom task thread
			Bounds boundsInParent = map.getBoundsInParent();
			return new ErrorTask<List<GeoBucket>>()
			{
				@Override
				protected List<GeoBucket> call()
				{
					// Using the bounds we compute the maximum and minimum lat/long values which we will pass to elasticsearch later
					Location topLeft = CalliopeMapController.this.map.getProjection().viewportPointToLocation(new Point2D(boundsInParent.getMinX(), boundsInParent.getMinY()));
					Location bottomRight = CalliopeMapController.this.map.getProjection().viewportPointToLocation(new Point2D(boundsInParent.getMaxX(), boundsInParent.getMaxY()));

					// This is the important line. We call ES to perform an aggregation of all uploaded images given bounds and a zoom level.
					// This call will return a list of buckets including number of images per bucket and centroids for each bucket
					return CalliopeData.getInstance().getEsConnectionManager().performGeoAggregation(
							MathUtil.clamp(topLeft.getLatitude(), -90.0, 90.0),
							MathUtil.clamp(topLeft.getLongitude(), -180.0, 180.0),
							MathUtil.clamp(bottomRight.getLatitude(), -90.0, 90.0),
							MathUtil.clamp(bottomRight.getLongitude(), -180.0, 180.0),
							CalliopeMapController.this.depthForCurrentZoom());
				}
			};
		});

		// Once the service is done with its thread, take the results and process them
		circleDrawingService.addFinishListener(geoBuckets ->
		{
			// If we have more buckets than currently existing circles, create more pins until the two buckets have the same size.
			// We do this so we avoid allocating significant chunks of memory every time this gets called. Try to re-use buckets we already have
			while (geoBuckets.size() > currentCircles.size())
			{
				MapNode newCircle = this.createCircle();
				this.addNodeToMap(newCircle, 2);
			}
			// If we have less buckets than currently existing circles, remove circles until the two buckets have the same size. We
			// do this to avoid having to allocate any memory at all
			while (currentCircles.size() > geoBuckets.size())
			{
				MapNode toRemove = currentCircles.remove(currentCircles.size() - 1);
				currentCircleControllers.remove(currentCircleControllers.size() - 1);
				this.removeNodeFromMap(toRemove);
			}

			// At this point our buckets should have the same size, test that here
			if (currentCircles.size() == geoBuckets.size())
			{
				// Iterate over all pins on the map, we're going to update all of their positions and labels
				for (Integer i = 0; i < currentCircles.size(); i++)
				{
					// Grab the bucket for that pin
					GeoBucket geoBucket = geoBuckets.get(i);
					// Grab the pin
					MapNode mapNode = currentCircles.get(i);
					// Grab the controller for that pin
					MapCircleController mapCircleController = currentCircleControllers.get(i);
					// Update the pin and the controller for that pin
					mapNode.setLocation(new Location(geoBucket.getCenterLatitude(), geoBucket.getCenterLongitude()));
					mapCircleController.setImageCount(geoBucket.getDocumentCount());
				}
			}
			else
			{
				// If the buckets are not the same size something went wrong, throw an error
				CalliopeData.getInstance().getErrorDisplay().notify("Pin bucket and geo bucket were not the same size after normalization, this is an error!");
			}
		});

		// When the mouse gets dragged and released we update our pins
		this.map.mouseDraggingProperty().addListener((observable, oldValue, newValue) ->
		{
			// If we started dragging or got a null don't do anything
			if (newValue == null || newValue)
				return;
			// Redraw all the pin circles with new values because we moved our map
			circleDrawingService.requestAnotherRun();
		});

		// When the mouse scroll is touched and the zoom is changed we update our circles too
		this.map.zoomLevelProperty().addListener((observable, oldValue, newValue) ->
		{
			this.updateSitePins(siteToPolygonAndPin, oldValue.doubleValue(), newValue.doubleValue());
			circleDrawingService.requestAnotherRun();
		});

		this.cbxMapProvider.setItems(FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(MapProviders.values())));
		this.cbxMapProvider.getSelectionModel().select(MapProviders.OpenStreetMaps);
		this.cbxMapProvider.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
		{
			if (newValue != null && oldValue != null)
			{
				MapTileLayer oldMapTileProvider = oldValue.getMapTileProvider();
				MapTileLayer newMapTileProvider = newValue.getMapTileProvider();
				this.removeNodeFromMap(oldMapTileProvider);
				this.addNodeToMap(newMapTileProvider, 0);
			}
		});
	}

	/**
	 * Updates the site pins based on the the current zoom. If the zoom is close enough, then show polygons
	 *
	 * @param siteToPolygonAndPin A map of site to the two UI elements that make it up (polygon & pin)
	 * @param oldZoom The old zoom we're changing from
	 * @param newZoom The new zoom we're moving to
	 */
	private void updateSitePins(java.util.Map<BoundedSite, Pair<MapPolygon, MapNode>> siteToPolygonAndPin, Double oldZoom, Double newZoom)
	{
		// We went from below to above the threshold
		if (newZoom > PIN_TO_POLY_THRESHOLD && oldZoom <= PIN_TO_POLY_THRESHOLD)
		{
			// Go over all UI elements and show the correct element
			siteToPolygonAndPin.values().forEach(polygonAndPinPair ->
			{
				MapPolygon mapPolygon = polygonAndPinPair.getLeft();
				MapNode mapPin = polygonAndPinPair.getRight();
				// Remove the pin, and add the polygon
				this.removeNodeFromMap(mapPin);
				if (!this.map.getChildren().contains(mapPolygon))
					this.addNodeToMap(mapPolygon, 1);
			});
		}
		// We went from above to below the threshold
		else if (newZoom <= PIN_TO_POLY_THRESHOLD && oldZoom > PIN_TO_POLY_THRESHOLD)
		{
			// Go over all UI elements and show the correct element
			siteToPolygonAndPin.values().forEach(polygonAndPinPair ->
			{
				MapPolygon mapPolygon = polygonAndPinPair.getLeft();
				MapNode mapPin = polygonAndPinPair.getRight();
				// Remove the polygon, and add the pin
				this.removeNodeFromMap(mapPolygon);
				if (!this.map.getChildren().contains(mapPin))
					this.addNodeToMap(mapPin, 3);
			});
		}
	}

	/**
	 * Allocates a new pin node and stores the required references into the lists
	 *
	 * @return Returns a newly allocated map node with its controller and reference stored into parallel lists
	 */
	private MapNode createCircle()
	{
		// Create a new pin object, call our custom class that ensures the node remains centered on the geo-location
		MapNode pin = new AlignedMapNode();
		// Load the FXML document representing this pin
		FXMLLoader fxmlLoader = FXMLLoaderUtils.loadFXML("mapView/MapPin.fxml");
		// Add FXML document to this pin
		pin.getChildren().add(fxmlLoader.getRoot());
		// Store the pin and its controller into the parallel lists
		this.currentCircles.add(pin);
		this.currentCircleControllers.add(fxmlLoader.getController());
		// Make sure we can drag & drop through the pin
		pin.setMouseTransparent(true);
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

	/**
	 * Adds a node into the map using the binding we created earlier. First we add
	 * to our z-order map and then insert into our sorted nodes list which ensures that
	 * the z-order is properly applied
	 *
	 * @param node The node to add to the list
	 * @param zOrder The z-order to assign to the node
	 */
	private void addNodeToMap(Node node, Integer zOrder)
	{
		this.zOrder.put(node, zOrder);
		this.sortedNodes.add(node);
	}

	/**
	 * Removes the node from the map as well as the z-order hashmap
	 *
	 * @param node The node to remove
	 */
	private void removeNodeFromMap(Node node)
	{
		this.sortedNodes.remove(node);
		this.zOrder.remove(node);
	}

	private enum MapProviders
	{
		OpenStreetMaps("Open Street Map", MapTileLayer.getOpenStreetMapLayer()),
		BingMapsAerial("Open Topo Map", new MapTileLayer("OpenTopoMap", "https://{c}.tile.opentopomap.org/{z}/{x}/{y}.png", 0, 17));

		private String name;
		private MapTileLayer mapTileProvider;

		MapProviders(String name, MapTileLayer mapTileProvider)
		{
			this.name = name;
			this.mapTileProvider = mapTileProvider;
		}

		@Override
		public String toString()
		{
			return this.name;
		}

		public MapTileLayer getMapTileProvider()
		{
			return this.mapTileProvider;
		}
	}
}
