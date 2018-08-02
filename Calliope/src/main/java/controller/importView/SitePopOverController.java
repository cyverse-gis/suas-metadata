package controller.importView;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import model.site.Site;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

/**
 * Simple class for displaying site information as a pop-over
 */
public class SitePopOverController
{
	///
	/// FXML Bound Fields Start
	///

	// The NEON site name
	@FXML
	public Label lblName;

	// A VBox of extra keys
	@FXML
	public VBox vbxKeys;
	// A VBox of extra values
	@FXML
	public VBox vbxValues;

	///
	/// FXML Bound Fields End
	///

	/**
	 * Nothing to initialize
	 */
	@FXML
	public void initialize()
	{
	}

	/**
	 * Called to update the popover with new data
	 *
	 * @param site The new site to display
	 */
	public void updateSite(Site site)
	{
		// Clear the existing labels in the pop over
		this.vbxKeys.getChildren().clear();
		this.vbxValues.getChildren().clear();

		// Update the name label to be the site's name
		this.lblName.setText(site.getName());
		// Add a 'site code' key with the value as the site's code
		this.vbxKeys.getChildren().add(new Label("Site Code:"));
		this.vbxValues.getChildren().add(new Label(site.getCode()));
		// For each additional detail we add one key as the detail's name and a value as the detail's actual value
		for (Pair<String, ?> detail : site.getDetails())
		{
			// We convert 'camelCaseWords' into 'Regular English Capitalized Words' using word and string utils
			this.vbxKeys.getChildren().add(new Label(WordUtils.capitalize(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(detail.getKey()), " ") + ":")));
			this.vbxValues.getChildren().add(new Label(detail.getValue().toString()));
		}
	}
}