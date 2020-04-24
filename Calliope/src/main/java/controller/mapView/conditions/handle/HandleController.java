package controller.mapView.conditions.handle;

import controller.mapView.LayeredMap;
import fxmapcontrol.Location;
import fxmapcontrol.MapProjection;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;

/**
 * Controller class for the handle used by the map handle object
 */
public class HandleController
{
	// The current location the handle is at
	private ObjectProperty<Location> location = new SimpleObjectProperty<>(null);
	// A reference to the layered map
	private LayeredMap map;

	/**
	 * Action listener that listens for drag events and sets the location of the handle accordingly
	 *
	 * @param mouseEvent consumed
	 */
	public void mouseDragged(MouseEvent mouseEvent)
	{
		// Compute the current mouse X and Y
		Point2D point2D = map.screenToLocal(mouseEvent.getScreenX(), mouseEvent.getScreenY());
		double currentX = point2D.getX();
		double currentY = point2D.getY();
		// We need the map projection to go from screen coordinates to lat/long coordinates
		MapProjection mapProjection = map.getProjection();
		// Convert screen to lat/long coordinates and set the location
		// TODO: Modified this code to correct compiler errors, but I have no idea if the correct output is produced.
		// this.location.setValue(mapProjection.viewportPointToLocation(new Point2D(currentX, currentY)));
		this.location.setValue(mapProjection.mapToLocation(new Point2D(currentX, currentY)));
		mouseEvent.consume();
	}

	///
	/// Setters/Getters
	///

	public void setLocation(Location location)
	{
		this.location.setValue(location);
	}

	public Location getLocation()
	{
		return this.location.getValue();
	}

	public ObjectProperty<Location> locationProperty()
	{
		return this.location;
	}

	public void setMap(LayeredMap map)
	{
		this.map = map;
	}
}
