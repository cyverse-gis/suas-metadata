package controller;

import com.panemu.tiwulfx.control.DetachableTab;
import com.panemu.tiwulfx.control.DetachableTabPane;
import controller.analysisView.VisCSVController;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import model.SanimalData;
import model.image.ImageEntry;
import model.query.CyVerseQuery;
import model.query.IQueryCondition;
import model.query.QueryEngine;
import model.threading.ErrorTask;
import model.util.FXMLLoaderUtils;
import org.controlsfx.control.MaskerPane;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Controller class for the analysis page
 */
public class SanimalAnalysisController implements Initializable
{
	///
	/// FXML bound fields start
	///

	@FXML
	public VisCSVController visCSVController;

	// The list of query conditions
	@FXML
	public ListView<IQueryCondition> lvwQueryConditions;
	// The event interval used for Dr. Sanderson's output
	@FXML
	public TextField txtEventInterval;

	// The detachable tab containing the Dr. Sanderson output
	@FXML
	public DetachableTab dtbDrSanderson;

	// Tab pane full of visualizations
	@FXML
	public DetachableTabPane tpnVisualizations;

	// The list of possible filters
	@FXML
	public ListView<QueryEngine.QueryFilters> lvwFilters;

	// The Vbox with the query parameters, used to hide the event interval
	@FXML
	public VBox vbxQuery;

	// The imageview with the arrow divider
	@FXML
	public ImageView imgArrow;

	// If the query is happening
	@FXML
	public MaskerPane mpnQuerying;

	///
	/// FXML bound fields end
	///

	private Integer eventIntervalIndex = 0;

	private Image standardArrow = new Image("/images/analysisWindow/arrowDivider.png");
	private Image highlightedArrow = new Image("/images/analysisWindow/arrowDividerSelected.png");

	private UUID sessionID;

	/**
	 * Initialize sets up the analysis window and bindings
	 *
	 * @param location ignored
	 * @param resources ignored
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{}

	public void init(UUID sessionID)
	{
		this.sessionID = sessionID;

		// Set the query conditions to be specified by the data model
		this.lvwQueryConditions.setItems(SanimalData.getInstance(sessionID).getQueryEngine().getQueryConditions());
		// Set the cell factory to be our custom query condition cell which adapts itself to the specific condition
		this.lvwQueryConditions.setCellFactory(x -> FXMLLoaderUtils.loadFXML("analysisView/QueryConditionsListCell.fxml").getController());

		// Hide the Dr. Sanderson tab and the event interval if we don't have Dr. Sanderson's compatibility
		if (!SanimalData.getInstance(sessionID).getSettings().getDrSandersonOutput())
		{
			this.tpnVisualizations.getTabs().remove(this.dtbDrSanderson);
			this.eventIntervalIndex = this.vbxQuery.getChildren().indexOf(this.txtEventInterval);
			this.vbxQuery.getChildren().remove(this.txtEventInterval);
		}

		// Hide the Dr. Sanderson tab if compatibility is not enabled
		SanimalData.getInstance(sessionID).getSettings().drSandersonOutputProperty().addListener((observable, oldValue, newValue) ->
		{
			if (newValue)
			{
				if (!this.tpnVisualizations.getTabs().contains(this.dtbDrSanderson))
					this.tpnVisualizations.getTabs().add(0, this.dtbDrSanderson);
				if (!this.vbxQuery.getChildren().contains(this.txtEventInterval))
					this.vbxQuery.getChildren().add(this.eventIntervalIndex, this.txtEventInterval);
			}
			else
			{
				this.tpnVisualizations.getTabs().remove(this.dtbDrSanderson);
				this.eventIntervalIndex = this.vbxQuery.getChildren().indexOf(this.txtEventInterval);
				this.vbxQuery.getChildren().remove(this.txtEventInterval);
			}
		});

		// Set the items in the list to be the list of possible query filters
		this.lvwFilters.setItems(SanimalData.getInstance(sessionID).getQueryEngine().getQueryFilters());

		this.mpnQuerying.setVisible(false);
	}

	/**
	 * Called when the refresh button is pressed
	 *
	 * @param actionEvent consumed
	 */
	public void query(ActionEvent actionEvent)
	{
		this.mpnQuerying.setVisible(true);

		// Default 60s event interval
		Integer eventInterval = 60;
		try
		{
			// Check if a different interval was given
			eventInterval = Integer.parseInt(this.txtEventInterval.getText());
		}
		catch (NumberFormatException ignored) {}

		// Create a query
		CyVerseQuery query = new CyVerseQuery();
		// For each condition listed in the listview, apply that to the overall query
		for (IQueryCondition queryCondition : SanimalData.getInstance(sessionID).getQueryEngine().getQueryConditions())
			queryCondition.appendConditionToQuery(query);

		Task<List<String>> queryTask = new ErrorTask<List<String>>(this.sessionID)
		{
			@Override
			protected List<String> call()
			{
				this.updateMessage("Performing query...");
				// Grab the result of the query
				return SanimalData.getInstance(sessionID).getConnectionManager().performQuery(query);
			}
		};
		Integer finalEventInterval = eventInterval;

		// Once finished with the task, we test if the user wants to continue
		queryTask.setOnSucceeded(event ->
		{
			// Get the result of the first query
			List<String> irodsAbsolutePaths = queryTask.getValue();

			// Ask the user if they would like to continue to part 2 of the query where we retrieve metadata. This takes a while
			Optional<ButtonType> buttonTypeOpt = SanimalData.getInstance(sessionID).getErrorDisplay().showPopup(
					Alert.AlertType.CONFIRMATION,
					this.lvwFilters.getScene().getWindow(),
					"Query Count",
					null,
					"This query will return " + irodsAbsolutePaths.size() + " results at approximately 6 results per second, continue?",
					true);

			// If they press OK, query, otherwise just jump out
			if (buttonTypeOpt.isPresent() && buttonTypeOpt.get() == ButtonType.OK)
			{
				// Create a second task to perform the next query
				Task<List<ImageEntry>> queryImageTask = new ErrorTask<List<ImageEntry>>(this.sessionID)
				{
					@Override
					protected List<ImageEntry> call()
					{
						this.updateMessage("Performing image query...");
						// Grab the result of the image query
						return SanimalData.getInstance(sessionID).getConnectionManager().fetchMetadataFor(irodsAbsolutePaths);
					}
				};

				queryImageTask.setOnSucceeded(event1 ->
				{
					visCSVController.visualize();
					this.mpnQuerying.setVisible(false);
				});

				// Execute the second query
				SanimalData.getInstance(sessionID).getSanimalExecutor().getQueuedExecutor().addTask(queryImageTask);
			}
			else
			{
				this.mpnQuerying.setVisible(false);
			}

		});
		SanimalData.getInstance(sessionID).getSanimalExecutor().getQueuedExecutor().addTask(queryTask);

		actionEvent.consume();
	}

	/**
	 * Called to add athe current filter to the analysis
	 *
	 * @param mouseEvent consumed
	 */
	public void clickedAdd(MouseEvent mouseEvent)
	{
		// If a filter was clicked, we instantiate it and append it to the end of the list (-1 so that the + is at the end)
		ObservableList<IQueryCondition> queryConditions = SanimalData.getInstance(sessionID).getQueryEngine().getQueryConditions();
		if (this.lvwFilters.getSelectionModel().selectedItemProperty().getValue() != null)
			queryConditions.add(this.lvwFilters.getSelectionModel().selectedItemProperty().getValue().createInstance(sessionID));
		mouseEvent.consume();
	}

	/**
	 * Called when the mouse enters the arrow image
	 *
	 * @param mouseEvent consumed
	 */
	public void mouseEnteredArrow(MouseEvent mouseEvent)
	{
		imgArrow.setImage(highlightedArrow);
		mouseEvent.consume();
	}

	/**
	 * Called when the mouse exits the arrow image
	 *
	 * @param mouseEvent consumed
	 */
	public void mouseExitedArrow(MouseEvent mouseEvent)
	{
		imgArrow.setImage(standardArrow);
		mouseEvent.consume();
	}

	/**
	 * Called whenever a filter is clicked on the filters list view
	 *
	 * @param mouseEvent consumed
	 */
	public void clickedFilters(MouseEvent mouseEvent)
	{
		if (mouseEvent.getClickCount() == 2)
			this.clickedAdd(mouseEvent);
		mouseEvent.consume();
	}
}
