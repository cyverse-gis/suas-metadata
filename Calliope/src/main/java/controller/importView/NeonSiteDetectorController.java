package controller.importView;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import model.CalliopeData;
import model.analysis.CalliopeAnalysisUtils;
import model.image.ImageEntry;
import model.neon.BoundedSite;
import model.threading.ErrorTask;
import org.apache.commons.lang.math.NumberUtils;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class NeonSiteDetectorController implements Initializable
{
	///
	/// FXML bound fields start
	///

	@FXML
	public RadioButton rbnByDistance;
	@FXML
	public RadioButton rbnByInterior;
	@FXML
	public TextField txtDistance;
	@FXML
	public Button btnDetectSites;

	@FXML
	public HBox hbxBoundary;
	@FXML
	public HBox hbxDistance;

	///
	/// FXML bound fields end
	///

	private List<ImageEntry> imageEntries = Collections.emptyList();

	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		this.btnDetectSites.disableProperty().bind(this.rbnByInterior.selectedProperty().not().and(Bindings.createBooleanBinding(() -> !NumberUtils.isNumber(this.txtDistance.getText()), this.txtDistance.textProperty())));
		this.hbxBoundary.disableProperty().bind(this.rbnByInterior.selectedProperty().not());
		this.hbxDistance.disableProperty().bind(this.rbnByDistance.selectedProperty().not());
	}

	public void updateItems(List<ImageEntry> imageEntries)
	{
		this.imageEntries = imageEntries;
		this.txtDistance.clear();
	}

	public void closeWindow(ActionEvent actionEvent)
	{
		this.rbnByDistance.getScene().getWindow().hide();
		actionEvent.consume();
	}

	public void detectSites(ActionEvent actionEvent)
	{
		if (this.rbnByDistance.isSelected())
		{
			Double maxDistance = NumberUtils.toDouble(this.txtDistance.getText(), 0);

			ErrorTask<BoundedSite[]> detectTask = new ErrorTask<BoundedSite[]>()
			{
				@Override
				protected BoundedSite[] call()
				{
					this.updateMessage("Detecting NEON sites for images...");
					BoundedSite[] toReturn = new BoundedSite[imageEntries.size()];
					List<BoundedSite> rawSites = CalliopeData.getInstance().getSiteList();
					for (Integer i = 0; i < imageEntries.size(); i++)
					{
						if (i % 20 == 0)
							this.updateProgress(i, imageEntries.size());
						ImageEntry toProcess = imageEntries.get(i);
						BoundedSite closest = CalliopeData.getInstance().getNeonData().closestBoundedSiteTo(rawSites, toProcess.getLocationTaken().getLatitude(), toProcess.getLocationTaken().getLongitude());
						Double distance = CalliopeAnalysisUtils.distanceBetween(closest.getSite().getSiteLatitude(), closest.getSite().getSiteLongitude(), toProcess.getLocationTaken().getLatitude(), toProcess.getLocationTaken().getLongitude());
						if (distance <= maxDistance)
							toReturn[i] = closest;
					}
					return toReturn;
				}
			};

			detectTask.setOnSucceeded(event ->
			{
				for (Integer i = 0; i < imageEntries.size(); i++)
					imageEntries.get(i).setSiteTaken(detectTask.getValue()[i]);
			});

			CalliopeData.getInstance().getExecutor().getQueuedExecutor().addTask(detectTask);
			this.rbnByInterior.getScene().getWindow().hide();
		}
		else if (this.rbnByInterior.isSelected())
		{
			ErrorTask<BoundedSite[]> detectTask = new ErrorTask<BoundedSite[]>()
			{
				@Override
				protected BoundedSite[] call()
				{
					this.updateMessage("Detecting NEON sites for images...");
					BoundedSite[] toReturn = new BoundedSite[imageEntries.size()];

					ObservableList<BoundedSite> sites = CalliopeData.getInstance().getSiteList();
					String[] siteCodes = CalliopeData.getInstance().getEsConnectionManager().detectNEONSites(imageEntries);
					for (Integer i = 0; i < siteCodes.length; i++)
					{
						String siteCode = siteCodes[i];
						ImageEntry imageEntry = imageEntries.get(i);
						if (siteCode != null)
						{
							Optional<BoundedSite> correctSite = sites.stream().filter(boundedSite -> boundedSite.getSite().getSiteCode().equals(siteCode)).findFirst();
							correctSite.ifPresent(imageEntry::setSiteTaken);
						}
						else
							imageEntry.setSiteTaken(null);
					}

					return toReturn;
				}
			};

			detectTask.setOnSucceeded(event ->
			{
				for (Integer i = 0; i < imageEntries.size(); i++)
					imageEntries.get(i).setSiteTaken(detectTask.getValue()[i]);
			});

			CalliopeData.getInstance().getExecutor().getQueuedExecutor().addTask(detectTask);
			this.rbnByInterior.getScene().getWindow().hide();
		}
	}
}
