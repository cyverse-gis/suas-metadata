package model;

import com.google.gson.Gson;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import model.cyverse.CyVerseConnectionManager;
import model.cyverse.ImageCollection;
import model.elasticsearch.ElasticSearchConnectionManager;
import model.image.ImageDirectory;
import model.image.ImageEntry;
import model.neon.BoundedSite;
import model.neon.NeonData;
import model.query.QueryEngine;
import model.species.Species;
import model.threading.CalliopeExecutor;
import model.threading.ErrorService;
import model.threading.ErrorTask;
import model.util.*;
import org.hildan.fxgson.FxGson;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * A singleton class containing all data Calliope needs
 */
public class CalliopeData
{
	// The one instance of the data
	private static final CalliopeData INSTANCE = new CalliopeData();

	// Get the one instance
	public static CalliopeData getInstance()
	{
		return CalliopeData.INSTANCE;
	}

	// The sensitive data configuration file so we don't put up sensitive info on Github
	private SensitiveConfigurationManager sensitiveConfigurationManager = new SensitiveConfigurationManager();

	// A global list of species
	private final ObservableList<Species> speciesList;
	private AtomicBoolean needSpeciesSync = new AtomicBoolean(false);
	private AtomicBoolean speciesSyncInProgress = new AtomicBoolean(false);

	// A global list of neon locations
	private final ObservableList<BoundedSite> siteList;

	// A global list of image collections
	private final ObservableList<ImageCollection> collectionList;


	// A base directory to which we add all extra directories
	private final ImageDirectory imageTree;
	private static final Integer NUM_IMAGES_AT_A_TIME = 100;
	private AtomicBoolean metadataSyncInProgress = new AtomicBoolean(false);

	// A username property which we can bind to in the rest of the program
	private StringProperty usernameProperty = new SimpleStringProperty("");
	// A logged in property which we can bind to in the rest of the program, is set to true when a user is logged in
	private BooleanProperty loggedInProperty = new SimpleBooleanProperty(false);

	// Executor used to thread off long tasks
	private CalliopeExecutor calliopeExecutor = new CalliopeExecutor();

	// GSon object used to serialize data. We register a local date time adapter to ensure dates are serialized correctly
	private final Gson gson = FxGson.fullBuilder().setPrettyPrinting().serializeNulls().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()).create();

	// The connection manager used to authenticate the CyVerse user
	private CyVerseConnectionManager cyConnectionManager = new CyVerseConnectionManager();

	// The connection manager used to manage the database
	private ElasticSearchConnectionManager esConnectionManager = new ElasticSearchConnectionManager(this.sensitiveConfigurationManager);

	// Preferences used to save the user's username
	private final Preferences calliopePreferences = Preferences.userNodeForPackage(CalliopeData.class);

	// Manager of all temporary files used by the Calliope software
	private final TempDirectoryManager tempDirectoryManager = new TempDirectoryManager();

	// List of Calliope settings
	private final SettingsData settings = new SettingsData();
	private AtomicBoolean needSettingsSync = new AtomicBoolean(false);
	private AtomicBoolean settingsSyncInProgress = new AtomicBoolean(false);

	// Class used to display errors as popups
	private final ErrorDisplay errorDisplay = new ErrorDisplay(this);

	// Query engine used in storing the current query setup
	private QueryEngine queryEngine = new QueryEngine();

	// NEON data api connection
	private NeonData neonData = new NeonData();

	// Class to handle metadata management
	private MetadataManager metadataManager = new MetadataManager();

	/**
	 * Private constructor since we're using the singleton design pattern
	 */
	private CalliopeData()
	{
		// Create the species list, and add some default species
		this.speciesList = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(species -> new Observable[]{species.commonNameProperty(), species.scientificNameProperty(), species.speciesIconURLProperty(), species.keyBindingProperty()}));

		// Create the location list and add some default locations
		this.siteList = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(site -> new Observable[]{ site.siteProperty(), site.boundaryProperty() }));

		// Create the image collection list
		this.collectionList = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(collection -> new Observable[]{collection.nameProperty(), collection.getPermissions(), collection.organizationProperty(), collection.contactInfoProperty(), collection.descriptionProperty(), collection.idProperty() }));

		// The tree just starts in the current directory which is a dummy directory
		this.imageTree = new ImageDirectory(new File("./"));

		// When the settings change, we sync them
		this.setupAutoSettingsSync();
	}

	/**
	 * Ensures that when settings change they get uploaded to CyVerse
	 */
	private void setupAutoSettingsSync()
	{
		ErrorService<Void> syncService = new ErrorService<Void>()
		{
			@Override
			protected Task<Void> createTask()
			{
				return new ErrorTask<Void>()
				{
					@Override
					protected Void call()
					{
						// Perform the push of the settings data
						this.updateMessage("Syncing new settings to CyVerse...");
						CalliopeData.getInstance().getEsConnectionManager().pushLocalSettings(CalliopeData.getInstance().getSettings());
						return null;
					}
				};
			}
		};
		// When we finish syncing...
		syncService.setOnSucceeded(event -> {
			// After finishing the sync, check if we need to sync again. If so set the flag to false and sync once again
			if (this.needSettingsSync.get())
			{
				this.needSettingsSync.set(false);
				syncService.restart();
			}
			// If we don't need to sync again set the sync in progress flag to false
			else
			{
				this.settingsSyncInProgress.set(false);
			}
		});
		this.calliopeExecutor.getQueuedExecutor().registerService(syncService);

		// When the settings change...
		Runnable onSettingChange = () ->
		{
			// If a sync is already in progress, we set a flag telling the current sync to perform another sync right after it finishes
			if (this.settingsSyncInProgress.get())
			{
				this.needSettingsSync.set(true);
			}
			// If a sync is not in progress, go ahead and sync
			else
			{
				this.settingsSyncInProgress.set(true);
				// Perform the task
				syncService.restart();
			}
		};
		this.settings.getSettingList().addListener((ListChangeListener<CustomPropertyItem<?>>) c -> onSettingChange.run());
	}

	/**
	 * @return The global species list
	 */
	public ObservableList<Species> getSpeciesList()
	{
		return speciesList;
	}

	/**
	 * @return The global site list
	 */
	public ObservableList<BoundedSite> getSiteList()
	{
		return siteList;
	}

	/**
	 * @return The global collection list
	 */
	public ObservableList<ImageCollection> getCollectionList()
	{
		return collectionList;
	}

	/**
	 * @return The root of the data tree
	 */
	public ImageDirectory getImageTree()
	{
		return imageTree;
	}

	/**
	 * @return The tree of images as a list
	 */
	public List<ImageEntry> getAllImages()
	{
		return this.getImageTree()
				.flattened()
				.filter(container -> container instanceof ImageEntry)
				.map(imageEntry -> (ImageEntry) imageEntry)
				.collect(Collectors.toList());
	}

	/**
	 * @return The Cyverse connection manager used to authenticate and upload the user's images
	 */
	public CyVerseConnectionManager getCyConnectionManager()
	{
		return cyConnectionManager;
	}

	/**
	 * @return The ElasticSearch connection manager used to manage the DB
	 */
	public ElasticSearchConnectionManager getEsConnectionManager()
	{
		return esConnectionManager;
	}

	/**
	 * @return The CyVerse Calliope executor service
	 */
	public CalliopeExecutor getExecutor()
	{
		return calliopeExecutor;
	}

	/**
	 * @return The Gson serializer used to serialize properties
	 */
	public Gson getGson()
	{
		return this.gson;
	}

	/**
	 * @return Preference file used to store usernames and passwords
	 */
	public Preferences getPreferences()
	{
		return calliopePreferences;
	}

	public void setUsername(String username)
	{
		this.usernameProperty.setValue(username);
	}

	public String getUsername()
	{
		return this.usernameProperty.getValue();
	}

	public StringProperty usernameProperty()
	{
		return usernameProperty;
	}

	public void setLoggedIn(Boolean loggedIn)
	{
		this.loggedInProperty.setValue(loggedIn);
	}

	public Boolean isLoggedIn()
	{
		return this.loggedInProperty.getValue();
	}

	public BooleanProperty loggedInProperty()
	{
		return loggedInProperty;
	}

	public TempDirectoryManager getTempDirectoryManager()
	{
		return tempDirectoryManager;
	}

	public ErrorDisplay getErrorDisplay()
	{
		return this.errorDisplay;
	}

	public SettingsData getSettings()
	{
		return this.settings;
	}

	public QueryEngine getQueryEngine() { return this.queryEngine; }

	public SensitiveConfigurationManager getSensitiveConfigurationManager()
	{
		return this.sensitiveConfigurationManager;
	}

	public NeonData getNeonData()
	{
		return this.neonData;
	}

	public MetadataManager getMetadataManager()
	{
		return this.metadataManager;
	}
}
