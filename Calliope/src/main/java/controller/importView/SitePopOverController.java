package controller.importView;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import model.neon.jsonPOJOs.Site;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Simple class for displaying site information as a pop-over
 */
public class SitePopOverController implements Initializable
{
	///
	/// FXML Bound Fields Start
	///

	// The NEON site name
	@FXML
	public Label lblName;
	// The NEON domain name
	@FXML
	public Label lblDomainName;
	// The NEON domain code
	@FXML
	public Label lblDomainCode;
	// The NEON site code
	@FXML
	public Label lblSiteCode;
	// The NEON site description
	@FXML
	public Label lblSiteDescription;
	// The NEON site centroid latitude
	@FXML
	public Label lblSiteLatitude;
	// The NEON site centroid longitude
	@FXML
	public Label lblSiteLongitude;
	// The NEON site type
	@FXML
	public Label lblSiteType;
	// The NEON site state code
	@FXML
	public Label lblStateCode;
	// The NEON site state name
	@FXML
	public Label lblStateName;

	///
	/// FXML Bound Fields End
	///

	/**
	 * Nothing to initialize
	 *
	 * @param location ignored
	 * @param resources ignored
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
	}

	/**
	 * Called to update the popover with new data
	 *
	 * @param site The new site to display
	 */
	public void updateSite(Site site)
	{
		// Just set all the fields to new values
		this.lblName.setText(site.getSiteName());
		this.lblDomainName.setText(site.getDomainName());
		this.lblDomainCode.setText(site.getDomainCode());
		this.lblSiteCode.setText(site.getSiteCode());
		this.lblSiteDescription.setText(site.getSiteDescription());
		this.lblSiteLatitude.setText(Double.toString(site.getSiteLatitude()));
		this.lblSiteLongitude.setText(Double.toString(site.getSiteLongitude()));
		this.lblSiteType.setText(site.getSiteType());
		this.lblStateCode.setText(site.getStateCode());
		this.lblStateName.setText(site.getStateName());
	}
}