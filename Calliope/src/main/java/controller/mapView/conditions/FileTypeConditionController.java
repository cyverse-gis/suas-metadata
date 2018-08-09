package controller.mapView.conditions;

import controller.mapView.IConditionController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import model.CalliopeData;
import model.elasticsearch.query.QueryCondition;
import model.elasticsearch.query.conditions.FileTypeCondition;
import model.threading.ErrorTask;
import org.controlsfx.control.MaskerPane;

import java.util.List;

/**
 * Data model used by the "File Type filter" query condition
 */
public class FileTypeConditionController implements IConditionController
{
	///
	/// FXML Bound Fields Start
	///

	// A list view of file types to select
	@FXML
	public ListView<String> lvwFileType;
	// A masker pane that is shown while we are re-loading the file type list
	@FXML
	public MaskerPane mpnRefreshing;

	///
	/// FXML Bound Fields End
	///

	// A reference to our data model
	private FileTypeCondition fileTypeCondition;

	/**
	 * Given a file type query condition this function initializes the controller's fields with the data object
	 *
	 * @param queryCondition The query condition data model to bind to this controller
	 */
	@Override
	public void initializeData(QueryCondition queryCondition)
	{
		// Make sure the query condition is the right type
		if (queryCondition instanceof FileTypeCondition)
		{
			// Store the data model
			this.fileTypeCondition = (FileTypeCondition) queryCondition;
			this.lvwFileType.setItems(fileTypeCondition.getFileTypes());
			this.lvwFileType.setCellFactory(CheckBoxListCell.forListView(fileTypeCondition::fileTypeSelectedProperty));
			this.lvwFileType.setEditable(true);
			// If the condition was not yet initialized, initialize it
			if (!this.fileTypeCondition.wasInitializedOnce())
				this.refreshFileTypes();
		}
	}

	/**
	 * When refreshed is pressed, download a new list of file types
	 *
	 * @param actionEvent consumed
	 */
	public void refreshPressed(ActionEvent actionEvent)
	{
		this.refreshFileTypes();
		actionEvent.consume();
	}

	/**
	 * Called to execute a task that downloads a new set of files
	 */
	private void refreshFileTypes()
	{
		if (this.fileTypeCondition != null)
		{
			// Show the refreshing masker pane
			this.mpnRefreshing.setVisible(true);
			// Create a new error task that downloads the list file types
			ErrorTask<List<String>> fileTypeDownloader = new ErrorTask<List<String>>()
			{
				@Override
				protected List<String> call()
				{
					return CalliopeData.getInstance().getEsConnectionManager().downloadFileTypeList();
				}
			};
			// Once the task finishes, update our data model and hide the refreshing masker pane
			fileTypeDownloader.setOnSucceeded(event ->
			{
				this.fileTypeCondition.updateFileTypeList(fileTypeDownloader.getValue());
				this.mpnRefreshing.setVisible(false);
			});
			// Execute the task
			CalliopeData.getInstance().getExecutor().getBackgroundExecutor().addTask(fileTypeDownloader);
		}
	}
}
