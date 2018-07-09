package controller.uploadView;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import model.CalliopeData;
import model.image.CloudUploadEntry;

/**
 * Controller for the download entry which allows downloading/saving of image files
 */
public class ImageUploadDownloadListEntryController extends ListCell<CloudUploadEntry>
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
	 * @param cloudUploadEntry The image directory to show
	 * @param empty If the cell is empty
	 */
	@Override
	public void updateItem(CloudUploadEntry cloudUploadEntry, boolean empty)
	{
		// Update the underlying item
		super.updateItem(cloudUploadEntry, empty);

		// Set the text of the cell to nothing
		this.setText(null);

		// If no image directory was given and the cell was empty, clear the graphic
		if (empty && cloudUploadEntry == null)
		{
			this.setGraphic(null);
		}
		else
		{
			// Update the labels
			this.lblUsername.setText(cloudUploadEntry.getUploadUser());
			this.lblCount.setText(cloudUploadEntry.getImageCount() + " images uploaded");
			this.lblDate.setText(CalliopeData.getInstance().getSettings().formatDateTime(this.getItem().getUploadDate(), " at "));
			// Set the graphic to display
			this.setGraphic(mainPane);
		}
	}

	/**
	 * The runnable code to execute when we click download
	 *
	 * @param onDownload Code to run
	 */
	public void setOnDownload(Runnable onDownload)
	{
	}

	/**
	 * The runnable code to execute when we click upload
	 *
	 * @param onUpload Code to run
	 */
	public void setOnUpload(Runnable onUpload)
	{
	}
}
