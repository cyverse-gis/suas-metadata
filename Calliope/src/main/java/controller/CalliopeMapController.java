package controller;

import fxmapcontrol.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

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

	/**
	 * Initialize sets up the analysis window and bindings
	 *
	 * @param location ignored
	 * @param resources ignored
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		TileImageLoader.setCache(new ImageFileCache());

		MapTileLayer openStreetMapLayer = MapTileLayer.getOpenStreetMapLayer();
		this.map.getChildren().add(openStreetMapLayer);
		this.map.setTargetCenter(new Location(32.2226, -110.9747));
		this.map.setZoomLevel(15);

		MapPolygon mapPolygon = new MapPolygon();
		mapPolygon.getLocations().add(new Location(5, 5));
		mapPolygon.getLocations().add(new Location(-5, 5));
		mapPolygon.getLocations().add(new Location(5, -5));
		mapPolygon.getLocations().add(new Location(-5, -5));
		mapPolygon.setLocation(new Location(0, 0));
		mapPolygon.setFill(Color.RED);

		this.map.getChildren().add(mapPolygon);
	}
}
