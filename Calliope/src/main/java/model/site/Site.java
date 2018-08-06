package model.site;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.util.Pair;
import org.elasticsearch.common.geo.GeoPoint;

/**
 * Base class to be used by all site types
 */
public abstract class Site
{
	// The name of the site
	private StringProperty name = new SimpleStringProperty(null);
	// The boundary of the site
	private ObjectProperty<Boundary> boundary = new SimpleObjectProperty<>(null);
	// The center point of the site
	private ObjectProperty<GeoPoint> center = new SimpleObjectProperty<>(null);
	// The site's code
	private StringProperty code = new SimpleStringProperty(null);
	// The type of the site (NEON, LTAR, etc)
	private StringProperty type = new SimpleStringProperty(null);

	/**
	 * Constructor takes the site's name, code, type, and boundary as input. The center is computed automatically
	 *
	 * @param name The name of the site
	 * @param code The code of the site
	 * @param type The type of the site
	 * @param boundary The boundary of the site
	 */
	public Site(String name, String code, String type, Boundary boundary)
	{
		// Set the four simple fields
		this.setName(name);
		this.setCode(code);
		this.setType(type);
		this.setBoundary(boundary);
	}

	/**
	 * Getter for icon of this site
	 *
	 * @param hovered If the site is hovered or not
	 * @return An image based on if the site was hovered or not
	 */
	public abstract Image getIcon(Boolean hovered);

	/**
	 * Getter for list of details that this site provides
	 *
	 * @return A list of details that this site provides as key, value pairs. Default is an empty list
	 */
	public ObservableList<Pair<String, ?>> getDetails()
	{
		return FXCollections.emptyObservableList();
	}

	/**
	 * To string is used by the site filter.
	 *
	 * @return A string of 'sitename (sitecode)'
	 */
	@Override
	public String toString()
	{
		return this.getName() + " (" + this.getCode() + ")";
	}

	///
	/// Getters/Setters
	///

	public void setName(String name)
	{
		this.name.setValue(name);
	}
	public String getName()
	{
		return this.name.getValue();
	}
	public StringProperty nameProperty()
	{
		return this.name;
	}

	/**
	 * Set boundary is special in that it also updates the center
	 *
	 * @param boundary The new boundary
	 */
	public void setBoundary(Boundary boundary)
	{
		this.boundary.setValue(boundary);

		// Recompute the center if boundary is not null
		if (boundary != null)
		{
			// Average the latitudes and longitudes
			double avgLat = 0;
			double avgLon = 0;
			// Go over the outer boundary, and add up all lat longs
			ObservableList<GeoPoint> outerBoundary = boundary.getOuterBoundary();
			for (GeoPoint geoPoint : outerBoundary)
			{
				avgLat = avgLat + geoPoint.getLat();
				avgLon = avgLon + geoPoint.getLon();
			}
			// Divide by the number of points to get the average lat and long
			avgLat = avgLat / outerBoundary.size();
			avgLon = avgLon / outerBoundary.size();
			// Set the center point's lat and long
			this.center.setValue(new GeoPoint(avgLat, avgLon));
		}
	}
	public Boundary getBoundary()
	{
		return this.boundary.getValue();
	}
	public ObjectProperty<Boundary> boundaryProperty()
	{
		return this.boundary;
	}

	public void setCenter(GeoPoint center)
	{
		this.center.setValue(center);
	}
	public GeoPoint getCenter()
	{
		return center.getValue();
	}
	public ObjectProperty<GeoPoint> centerProperty()
	{
		return center;
	}

	public void setCode(String code)
	{
		this.code.setValue(code);
	}
	public String getCode()
	{
		return this.code.getValue();
	}
	public StringProperty codeProperty()
	{
		return this.code;
	}

	public void setType(String type)
	{
		this.type.setValue(type);
	}
	public String getType()
	{
		return type.getValue();
	}
	public StringProperty typeProperty()
	{
		return type;
	}
}
