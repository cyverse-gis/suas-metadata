package controller.uploadView;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import model.CalliopeData;
import model.image.UploadedEntry;

/**
 * Controller for the download entry which allows downloading/saving of image files
 */
public class ImageUploadDownloadListEntryController extends ListCell<UploadedEntry>
{
	///
	/// FXML Bound fields start
	///

	// The primary pane reference
	@FXML
	public HBox mainPane;

	// List of labels used to display edits
	@FXML
	public Label lblDate;
	@FXML
	public Label lblUsername;
	@FXML
	public Label lblCount;

	///
	/// FXML Bound fields end
	///

	/**
	 * Nothing needs to be done to initialize this cell
	 */
	@FXML
	public void initialize()
	{
		// If the date or time settings are changed, recompute the label
		CalliopeData.getInstance().getSettings().dateFormatProperty().addListener((observable, oldValue, newValue) ->
		{
			if (this.getItem() != null)
				this.lblDate.setText(CalliopeData.getInstance().getSettings().formatDateTime(this.getItem().getUploadDate(), " at "));
		});
		CalliopeData.getInstance().getSettings().timeFormatProperty().addListener((observable, oldValue, newValue) ->
		{
			if (this.getItem() != null)
				this.lblDate.setText(CalliopeData.getInstance().getSettings().formatDateTime(this.getItem().getUploadDate(), " at "));
		});
	}

	/**
	 * Called when we get a new item to display
	 *
	 * @param uploadedEntry The image directory to show
	 * @param empty If the cell is empty
	 */
	@Override
	public void updateItem(UploadedEntry uploadedEntry, boolean empty)
	{
		// Update the underlying item
		super.updateItem(uploadedEntry, empty);

		// Set the text of the cell to nothing
		this.setText(null);

		// If no image directory was given and the cell was empty, clear the graphic
		if (empty && uploadedEntry == null)
		{
			this.setGraphic(null);
		}
		else
		{
			// Update the labels
			this.lblUsername.setText(uploadedEntry.getUploadUser());
			this.lblCount.setText(uploadedEntry.getImageCount() + " images uploaded");
			this.lblDate.setText(CalliopeData.getInstance().getSettings().formatDateTime(this.getItem().getUploadDate(), " at "));
			// Set the graphic to display
			this.setGraphic(mainPane);
		}
	}
}
