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
import model.cyverse.CyVerseConnectionManager;
import model.cyverse.ImageCollection;
import model.dataSources.IDataSource;
import model.dataSources.ImageDirectory;
import model.dataSources.ImageEntry;
import model.dataSources.cyverseDataStore.CyVerseDSDataSource;
import model.dataSources.localPC.directory.LocalPCDirectoryDataSource;
import model.dataSources.localPC.image.LocalPCImageDataSource;
import model.elasticsearch.ElasticSearchConnectionManager;
import model.elasticsearch.query.QueryEngine;
import model.neon.BoundedSite;
import model.neon.NeonData;
import model.threading.CalliopeExecutor;
import model.threading.ErrorTask;
import model.threading.ReRunnableService;
import model.util.*;
import org.hildan.fxgson.FxGson;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
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
	private SensitiveConfigurationManager sensitiveConfigurationManager;

	// A global list of neon locations
	private final ObservableList<BoundedSite> siteList;

	// A global list of image collections
	private final ObservableList<ImageCollection> collectionList;


	// A base directory to which we add all extra directories
	private final ImageDirectory imageTree;

	// A username property which we can bind to in the rest of the program
	private StringProperty usernameProperty;
	// A logged in property which we can bind to in the rest of the program, is set to true when a user is logged in
	private BooleanProperty loggedInProperty;

	// Executor used to thread off long tasks
	private CalliopeExecutor calliopeExecutor;

	// GSon object used to serialize data. We register a local date time adapter to ensure dates are serialized correctly
	private final Gson gson;

	// The connection manager used to authenticate the CyVerse user
	private CyVerseConnectionManager cyConnectionManager;

	// The connection manager used to manage the database
	private ElasticSearchConnectionManager esConnectionManager;

	// Preferences used to save the user's username
	private final Preferences calliopePreferences;

	// Manager of all temporary files used by the Calliope software
	private final TempDirectoryManager tempDirectoryManager;

	// List of Calliope settings
	private final SettingsData settings;

	// Class used to display errors as popups
	private final ErrorDisplay errorDisplay;

	// Query engine used in storing the current query setup
	private QueryEngine queryEngine;

	// NEON data api connection
	private NeonData neonData;

	// Class to handle metadata management
	private MetadataManager metadataManager;

	private ObservableList<IDataSource> dataSources;

	/**
	 * Private constructor since we're using the singleton design pattern
	 */
	private CalliopeData()
	{
		// Setup our sensitive config manager
		this.sensitiveConfigurationManager = new SensitiveConfigurationManager();

		// Initialize our username and logged in properties
		this.usernameProperty = new SimpleStringProperty("");
		this.loggedInProperty = new SimpleBooleanProperty(false);

		// Start up our thread executor
		this.calliopeExecutor = new CalliopeExecutor();

		// Initialize our JSON parser
		this.gson = FxGson.fullBuilder().setPrettyPrinting().serializeNulls().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()).create();

		// Establish a connection to the CyVerse datastore
		this.cyConnectionManager = new CyVerseConnectionManager();

		// Load our program settings
		this.settings = new SettingsData();

		// Setup our debug/error logger
		this.errorDisplay = new ErrorDisplay(this.settings);

		// Establish a connection to the ElasticSearch cluster
		this.esConnectionManager = new ElasticSearchConnectionManager(this.sensitiveConfigurationManager, this.errorDisplay);

		// Setup our preferences store which stores our username
		this.calliopePreferences = Preferences.userNodeForPackage(CalliopeData.class);

		// Create a temporary directory to dump any temporary files into
		this.tempDirectoryManager = new TempDirectoryManager(this.errorDisplay);

		// Create the query engine which executes ES queries
		this.queryEngine = new QueryEngine();

		// Download and setup NEON data
		this.neonData = new NeonData();

		// Setup our metadata management class
		this.metadataManager = new MetadataManager(this.errorDisplay);

		// Create the location list and add some default locations
		this.siteList = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(site -> new Observable[]{ site.siteProperty(), site.boundaryProperty() }));

		// Create the image collection list
		this.collectionList = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(collection -> new Observable[]{collection.nameProperty(), collection.getPermissions(), collection.organizationProperty(), collection.contactInfoProperty(), collection.descriptionProperty(), collection.idProperty() }));

		// The tree just starts in the current directory which is a dummy directory
		this.imageTree = new ImageDirectory(new File("./"));

		// When the settings change, we sync them
		this.setupAutoSettingsSync();

		// Setup our list of data sources
		this.dataSources = FXCollections.observableArrayList(dataSource -> new Observable[] {dataSource.descriptionProperty(), dataSource.iconProperty(), dataSource.nameProperty() });

		// Add all default data sources to the list
		this.addDefaultDataSources();
	}

	/**
	 * Ensures that when settings change they get uploaded to CyVerse
	 */
	private void setupAutoSettingsSync()
	{
		ReRunnableService reRunnableService = new ReRunnableService<>(() ->
			new ErrorTask<Void>()
			{
				@Override
				protected Void call()
				{
					// Perform the push of the settings data
					this.updateMessage("Syncing new settings to CyVerse...");
					CalliopeData.getInstance().getEsConnectionManager().pushLocalSettings(CalliopeData.getInstance().getSettings());
					return null;
				}
			}, this.calliopeExecutor.getImmediateExecutor());

		// When the settings change...
		this.settings.getSettingList().addListener((ListChangeListener<CustomPropertyItem<?>>) c -> reRunnableService.requestAnotherRun());
	}

	/**
	 * Adds all default data sources to our list of sources
	 */
	private void addDefaultDataSources()
	{
		this.dataSources.add(new CyVerseDSDataSource());
		this.dataSources.add(new LocalPCImageDataSource());
		this.dataSources.add(new LocalPCDirectoryDataSource());
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

	public ObservableList<IDataSource> getDataSources()
	{
		return dataSources;
	}
}
