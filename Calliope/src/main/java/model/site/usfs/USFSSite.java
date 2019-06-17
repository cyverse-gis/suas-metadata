package model.site.usfs;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.util.Pair;
import model.image.ImageEntry;
import model.site.Boundary;
import model.site.Site;

/**
 * A LTAR site class that has acronym, city, and state extra fields
 */
public class USFSSite extends Site
{
	// Extra fields that a USFS site will give us
	private StringProperty forestName = new SimpleStringProperty(null);
	private StringProperty region = new SimpleStringProperty(null);

	// A constant usfs pin icon that we cache so we don't have to load the image over and over again
	private static final Image USFS_ICON = new Image(ImageEntry.class.getResource("/images/mapWindow/usfsIcon32.png").toString());
	// A constant highlighted ltar pin icon that we cache so we don't have to load the image over and over again
	private static final Image USFS_HIGHLIGHTED_ICON = new Image(ImageEntry.class.getResource("/images/mapWindow/highlightedUsfsIcon32.png").toString());

	/**
	 * Constructor does nothing other than set type. It waits for the rest of the data to be added with setters
	 */
	public USFSSite()
	{
		this("", "", null);
	}

	/**
	 * Constructor takes the site's name, code, type, and boundary as input. The center is computed automatically
	 *
	 * @param name The name of the site
	 * @param code The code of the site
	 * @param boundary The boundary of the site
	 */
	public USFSSite(String name, String code, Boundary boundary)
	{
		super(name, code,"USFS", boundary);
	}

	/**
	 * Getter for icon of this site
	 *
	 * @param hovered If the site is hovered or not
	 * @return An image based on if the site was hovered or not
	 */
	@Override
	public Image getIcon(Boolean hovered)
	{
		if (hovered)
			return USFS_HIGHLIGHTED_ICON;
		else
			return USFS_ICON;
	}

	/**
	 * @return Returns a list of details that this USFS site provides. Details are unique to each site type
	 */
	@Override
	public ObservableList<Pair<String, ?>> getDetails()
	{
		return FXCollections.observableArrayList(
			new Pair<>("forestName", this.getForestName()),
			new Pair<>("region", this.getRegion())
		);
	}

	///
	/// Getters/Setters for the USFS site fields
	///

	public void setForestName(String name)
	{
		this.forestName.setValue(name);
	}
	public String getForestName()
	{
		return forestName.getValue();
	}
	public StringProperty forestNameProperty()
	{
		return forestName;
	}

	public void setRegion(String city) { this.region.setValue(city); }
	public String getRegion()
	{
		return region.getValue();
	}
	public StringProperty regionProperty()
	{
		return region;
	}
}
