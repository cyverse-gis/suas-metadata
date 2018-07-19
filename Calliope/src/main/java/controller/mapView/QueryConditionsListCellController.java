package controller.mapView;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import model.elasticsearch.query.QueryCondition;
import model.util.FXMLLoaderUtils;
import org.controlsfx.control.ToggleSwitch;

/**
 * Generic class used as a list cell of type QueryCondition. This cell is empty, and gets UI content from the QueryCondition
 */
public class QueryConditionsListCellController extends ListCell<QueryCondition>
{
	///
	/// FXML Bound Fields Start
	///

	// The main pane used as the background of the cell
	@FXML
	public StackPane mainPane;
	// The content pane which contains the current QueryCondition UI instance
	@FXML
	public BorderPane contentPane;
	// Button to remove the current QueryCondition UI instance
	@FXML
	public Button btnRemoveCondition;

	// The toggle switch that enables or disables this list cell
	@FXML
	public ToggleSwitch tswEnabled;

	///
	/// FXML Bound Fields End
	///

	/**
	 * There's nothing to initialize, everything is done once we get data
	 */
	@FXML
	public void initialize()
	{
		this.tswEnabled.selectedProperty().addListener((observable, oldValue, newValue) -> { if (this.getItem() != null) this.getItem().setEnabled(newValue); });
		this.contentPane.disableProperty().bind(this.tswEnabled.selectedProperty().not());
	}

	/**
	 * Called when we get data in the form of a query condition
	 *
	 * @param queryCondition The filter to be displayed in this cell
	 * @param empty If the cell is empty
	 */
	@Override
	protected void updateItem(QueryCondition queryCondition, boolean empty)
	{
		// Update the cell first
		super.updateItem(queryCondition, empty);

		// Set the text to null
		this.setText(null);

		// If the cell is empty we have no graphic
		if (empty && queryCondition == null)
		{
			this.setGraphic(null);
		}
		// if the cell is not empty, set the field's values and set the graphic
		else
		{
			this.tswEnabled.setSelected(queryCondition.isEnabled());

			// Load the FXML of the given data model UI
			FXMLLoader fxml = FXMLLoaderUtils.loadFXML("mapView/conditions/" + queryCondition.getFXMLConditionEditor());
			// Initialize the IQueryConditionController that controls the UI for the data
			fxml.<IConditionController> getController().initializeData(queryCondition);
			// The root node containing the query condition
			Node rootNode = fxml.getRoot();
			// Set our cell to display the FXML data
			contentPane.setCenter(rootNode);

			// Show the data
			this.setGraphic(mainPane);
		}
	}

	/**
	 * Clears the current filter if the X is pressed
	 *
	 * @param actionEvent consumed
	 */
	public void clearCondition(ActionEvent actionEvent)
	{
		this.getListView().getItems().remove(this.getItem());
		actionEvent.consume();
	}
}
