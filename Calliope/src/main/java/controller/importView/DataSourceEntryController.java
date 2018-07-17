package controller.importView;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import model.dataSources.IDataSource;

public class DataSourceEntryController extends ListCell<IDataSource>
{
	///
	/// FXML Bound Fields Start
	///

	// A reference to the main pane object
	@FXML
	public BorderPane mainPane;

	// The title at the top of the entry
	@FXML
	public Label lblTitle;
	// The image icon
	@FXML
	public ImageView imgIcon;
	// The description of the import method
	@FXML
	public Label lblDescription;

	///
	/// FXML Bound Fields End
	///

	/**
	 * Update item gets called whenever we receive a new item to display.
	 *
	 * @param importableDataFormat The item to display
	 * @param empty If the cell should be empty
	 */
	@Override
	protected void updateItem(IDataSource importableDataFormat, boolean empty)
	{
		// Update the cell first
		super.updateItem(importableDataFormat, empty);

		// Set the text to null
		this.setText(null);

		// If the cell is empty we have no graphic
		if (empty && importableDataFormat == null)
		{
			this.setGraphic(null);
		}
		// if the cell is not empty, set the field's values and set the graphic
		else
		{
			// Update title, description, icon, and finally set the main graphic to be the main pane
			this.lblTitle.setText(importableDataFormat.getName());
			this.lblDescription.setText(importableDataFormat.getDescription());
			this.imgIcon.setImage(importableDataFormat.getIcon());
			this.setGraphic(mainPane);
		}
	}
}
