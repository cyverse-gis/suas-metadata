package controller.importView;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import model.neon.jsonPOJOs.Site;
import model.util.FXMLLoaderUtils;
import org.controlsfx.control.PopOver;

public class SiteListEntryController extends ListCell<Site>
{
	///
	/// FXML Bound Fields Start
	///

	@FXML
	public Label lblName;
	@FXML
	public Label lblLatitude;
	@FXML
	public Label lblLongitude;

	@FXML
	public VBox mainPane;

	///
	/// FXML Bound Fields End
	///

	private PopOver popOver = new PopOver();
	private SitePopOverController popOverController;

	/**
	 * Initializes the location list entry by adding a listener to the format property
	 */
	@FXML
	public void initialize()
	{
		this.popOver = new PopOver();
		this.popOver.setHeaderAlwaysVisible(false);
		this.popOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
		this.popOver.setArrowSize(20);
		FXMLLoader fxmlLoader = FXMLLoaderUtils.loadFXML("importView/SitePopOver.fxml");
		this.popOverController = fxmlLoader.getController();
		this.popOver.setContentNode(fxmlLoader.getRoot());
	}

	/**
	 * Update item is called whenever the cell gets updated
	 *
	 * @param site The new site
	 * @param empty If the cell is empty
	 */
	@Override
	protected void updateItem(Site site, boolean empty)
	{
		// Update the cell first
		super.updateItem(site, empty);

		// Set the text to null
		this.setText(null);

		// If the cell is empty we have no graphic
		if (empty && site == null)
		{
			this.setGraphic(null);
		}
		// if the cell is not empty, set the field's values and set the graphic
		else
		{
			this.lblName.setText(site.getSiteName());
			this.lblLatitude.setText(Double.toString(site.getSiteLatitude()));
			this.lblLongitude.setText(Double.toString(site.getSiteLongitude()));
			this.popOverController.updateSite(site);
			this.setGraphic(mainPane);
		}
	}

	public void mouseEntered(MouseEvent mouseEvent)
	{
		if (this.getItem() != null)
		{
			popOver.show(this);
		}
	}

	public void mouseExited(MouseEvent mouseEvent)
	{
		popOver.hide();
	}
}
