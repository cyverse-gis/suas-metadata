package controller.importView;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import model.neon.jsonPOJOs.Site;

import java.net.URL;
import java.util.ResourceBundle;

public class SitePopOverController implements Initializable
{
	///
	/// FXML Bound Fields Start
	///

	@FXML
	public Label lblName;
	@FXML
	public Label lblDomainName;
	@FXML
	public Label lblDomainCode;
	@FXML
	public Label lblSiteCode;
	@FXML
	public Label lblSiteDescription;
	@FXML
	public Label lblSiteLatitude;
	@FXML
	public Label lblSiteLongitude;
	@FXML
	public Label lblSiteType;
	@FXML
	public Label lblStateCode;
	@FXML
	public Label lblStateName;

	///
	/// FXML Bound Fields End
	///

	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
	}

	public void updateSite(Site site)
	{
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