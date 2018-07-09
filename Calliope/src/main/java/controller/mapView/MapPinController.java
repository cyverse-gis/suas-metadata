package controller.mapView;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class MapPinController implements Initializable
{
	///
	/// FXML Bound Fields Start
	///

	@FXML
	public Label lblImageCount;

	///
	/// FXML Bound Fields End
	///

	@Override
	public void initialize(URL location, ResourceBundle resources)
	{

	}

	public void setImageCount(Long count)
	{
		this.lblImageCount.setText(count.toString());
	}
}
