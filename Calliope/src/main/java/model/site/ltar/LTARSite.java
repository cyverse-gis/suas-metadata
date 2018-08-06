package model.site.ltar;

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
public class LTARSite extends Site
{
	// Extra fields that a LTAR site will give us
	private StringProperty acronym = new SimpleStringProperty(null);
	private StringProperty city = new SimpleStringProperty(null);
	private StringProperty state = new SimpleStringProperty(null);

	// A constant ltar pin icon that we cache so we don't have to load the image over and over again
	private static final Image LTAR_ICON = new Image(ImageEntry.class.getResource("/images/mapWindow/ltarIcon32.png").toString());
	// A constant highlighted ltar pin icon that we cache so we dont have to load the image over and over again
	private static final Image LTAR_HIGHLIGHTED_ICON = new Image(ImageEntry.class.getResource("/images/mapWindow/highlightedLtarIcon32.png").toString());

	/**
	 * Constructor does nothing other than set type. It waits for the rest of the data to be added with setters
	 */
	public LTARSite()
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
	public LTARSite(String name, String code, Boundary boundary)
	{
		super(name, code,"LTAR", boundary);
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
			return LTAR_HIGHLIGHTED_ICON;
		else
			return LTAR_ICON;
	}

	/**
	 * @return Returns a list of details that this LTAR site provides. Details are unique to each site type
	 */
	@Override
	public ObservableList<Pair<String, ?>> getDetails()
	{
		return FXCollections.observableArrayList(
			new Pair<>("acronym", this.getAcronym()),
			new Pair<>("city", this.getCity()),
			new Pair<>("state", this.getState())
		);
	}

	///
	/// Getters/Setters for the LTAR site fields
	///

	public void setAcronym(String acronym)
	{
		this.acronym.setValue(acronym);
	}
	public String getAcronym()
	{
		return acronym.getValue();
	}
	public StringProperty acronymProperty()
	{
		return acronym;
	}

	public void setCity(String city)
	{
		this.city.setValue(city);
	}
	public String getCity()
	{
		return city.getValue();
	}
	public StringProperty cityProperty()
	{
		return city;
	}

	public void setState(String state)
	{
		this.state.setValue(state);
	}
	public String getState()
	{
		return state.getValue();
	}
	public StringProperty stateProperty()
	{
		return state;
	}
}
