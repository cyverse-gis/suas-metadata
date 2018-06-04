package controller.analysisView;

import javafx.fxml.Initializable;

/**
 * Interface used in all visualization tab controllers
 */
public interface VisControllerBase extends Initializable
{
	/**
	 * Function called whenever we're given a new pre-analyzed data set to visualize
	 *
	 */
	void visualize();
}
