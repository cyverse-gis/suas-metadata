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

	private List<MapNode> currentPins = new ArrayList<>();
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
		TileImageLoader.setCache(new ImageFileCache(new File(System.getProperty("user.home") + File.separator + "CalliopeMapCache").toPath()));

		MapTileLayer openStreetMapLayer = MapTileLayer.getOpenStreetMapLayer();
		this.map.getChildren().add(openStreetMapLayer);

		PopOver popOver = new PopOver();
		popOver.setHeaderAlwaysVisible(false);
		popOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
		popOver.setArrowSize(20);
		popOver.setCloseButtonEnabled(true);
		FXMLLoader fxmlLoader = FXMLLoaderUtils.loadFXML("importView/SitePopOver.fxml");
		SitePopOverController sitePopOverController = fxmlLoader.getController();
		popOver.setContentNode(fxmlLoader.getRoot());

		HashMap<BoundedSite, MapPolygon> siteToPolygon = new HashMap<>();
		CalliopeData.getInstance().getSiteList().addListener((ListChangeListener<? super BoundedSite>) change ->
		{
			while (change.next())
				if (change.wasAdded())
				{
					for (BoundedSite boundedSite : change.getAddedSubList())
					{
						Polygon polygon = boundedSite.getBoundary();
						MapPolygon mapPolygon = new MapPolygon();
						mapPolygon.getLocations().addAll(polygon.getOuterBoundaryIs().getLinearRing().getCoordinates().stream().map(coordinate -> new Location(coordinate.getLatitude(), coordinate.getLongitude())).collect(Collectors.toList()));
						mapPolygon.setLocation(new Location(boundedSite.getSite().getSiteLatitude(), boundedSite.getSite().getSiteLongitude()));
						mapPolygon.getStyleClass().add("neon-site-boundary");
						mapPolygon.setOnMouseClicked(event ->
						{
							sitePopOverController.updateSite(boundedSite.getSite());
							popOver.show(mapPolygon);
							event.consume();
						});
						this.map.getChildren().add(mapPolygon);
						siteToPolygon.put(boundedSite, mapPolygon);
					}
				}
				else if (change.wasRemoved())
				{
					for (BoundedSite boundedSite : change.getRemoved())
						if (siteToPolygon.containsKey(boundedSite))
							this.map.getChildren().remove(siteToPolygon.get(boundedSite));
				}
		});

		this.map.mouseDraggingProperty().addListener((observable, oldValue, newValue) ->
		{
			if (newValue == null || newValue)
				return;
			this.replacePins();
		});

		this.map.zoomLevelProperty().addListener((observable, oldValue, newValue) -> this.replacePins());
	}

	private void replacePins()
	{
		Bounds boundsInParent = this.map.getBoundsInParent();
		Location topLeft = this.map.getProjection().viewportPointToLocation(new Point2D(boundsInParent.getMinX(), boundsInParent.getMinY()));
		Location bottomRight = this.map.getProjection().viewportPointToLocation(new Point2D(boundsInParent.getMaxX(), boundsInParent.getMaxY()));
		List<GeoBucket> geoBuckets = CalliopeData.getInstance().getEsConnectionManager().performGeoAggregation(
				MathUtil.clamp(topLeft.getLatitude(), -90.0, 90.0),
				MathUtil.clamp(topLeft.getLongitude(), -180.0, 180.0),
				MathUtil.clamp(bottomRight.getLatitude(), -90.0, 90.0),
				MathUtil.clamp(bottomRight.getLongitude(), -180.0, 180.0),
				this.depthForCurrentZoom());

		while (geoBuckets.size() > currentPins.size())
		{
			MapNode newPin = this.createPin();
			this.map.getChildren().add(newPin);
		}
		while (currentPins.size() > geoBuckets.size())
		{
			MapNode toRemove = currentPins.remove(currentPins.size() - 1);
			currentPinControllers.remove(currentPinControllers.size() - 1);
			this.map.getChildren().remove(toRemove);
		}

		if (currentPins.size() == geoBuckets.size())
		{
			for (Integer i = 0; i < currentPins.size(); i++)
			{
				GeoBucket geoBucket = geoBuckets.get(i);
				MapNode mapNode = currentPins.get(i);
				MapPinController mapPinController = currentPinControllers.get(i);
				mapNode.setLocation(new Location(geoBucket.getCenterLatitude(), geoBucket.getCenterLongitude()));
				mapPinController.setImageCount(geoBucket.getDocumentCount());
			}
		}
		else
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Pin bucket and geo bucket were not the same size after normalization, this is an error!");
		}
	}

	private MapNode createPin()
	{
		MapNode pin = new MapNode()
		{
			@Override
			protected void viewportPositionChanged(Point2D viewportPosition)
			{
				if (viewportPosition != null)
				{
					Bounds boundsInParent = this.getBoundsInParent();
					setTranslateX(viewportPosition.getX() - boundsInParent.getWidth() / 2);
					setTranslateY(viewportPosition.getY() - boundsInParent.getHeight() / 2);
				}
				else
				{
					setTranslateX(0d);
					setTranslateY(0d);
				}
			}
		};
		FXMLLoader fxmlLoader = FXMLLoaderUtils.loadFXML("mapView/MapPin.fxml");
		pin.getChildren().add(fxmlLoader.getRoot());
		this.currentPins.add(pin);
		this.currentPinControllers.add(fxmlLoader.getController());
		return pin;
	}

	private Integer depthForCurrentZoom()
	{
		Double zoom = this.map.getZoomLevel();
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
