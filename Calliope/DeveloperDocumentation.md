# Calliope Developer Documentation

This documentation outlines some design decisions made in the development of Calliope. A description of important classes can be found as well. Calliope is written in Java using JavaFX as well as numerous other libraries. 

## Important Design Patterns

### Model - View - Controller (MVC)

Calliope is built using the standard MVC design pattern. 

In this design pattern, the model is created first with complete independence from the view or controller. The model contains the data of the program and functions to manipulate that data. Any database connections or network threads are managed by the model.

The view is completely independent from the model, but is linked or 'bound' to the controller. The view is made up of FXML and CSS files that define each user interface component. Each FXML file begins with a component that has an `fx:controller="x.y.z"` attribute that defines the controller the FXML file is bound to. Any actions done to the UI are forwarded to the controller through action listeners.

The controller is dependent on both the model and controller. Any UI components such as text fields and labels are represented in the controller as fields. The controller contains method definitions that represent action listeners for actions that are fired from the view. The controller contains an important `public void initialize() {}` method that is automatically called upon UI creation. This method is used to initialize any UI fields and setup data bindings to the model. 

### Dependency Injection

Calliope makes use of dependency injection to link the FXML file to its controller. This is automatically performed by JavaFX, so you won't need to do anything special. FXML elements with the tag `fx:id="abc"` tell JavaFX to 'dependency inject' this element into the field `private X abc;` found in the controller. If this field is not present an error will arise. 

### Singleton

The singleton design pattern ensures only one instance of an object is ever created. This is used to ensure one publicly accessible data model is available to all controller files without needing to pass object references around. This can be dangerous and is not the best design decision, but it does work. 

### Observer

The observer design pattern is seen in JavaFX as data bindings. The model (as previously described in MVC) stores its variables as properties instead of primitive types. As an example, a person's name would be stored using a `StringProperty` instead of a `String`. A property has the ability to notify listeners of any changes. These properties are bound to the view through the controller and vice versa. When the model changes the view will automatically reflect the changes, and when the view updates the model will also reflect those changes. 

## Important Model Classes

Model classes are found in: ` /src/main/java/model`

##### Calliope Data (/CalliopeData.java)

This object contains all data used by Calliope. This data contains the list of sites, list of collections, currently imported images, CyVerse connection manager, ElasticSearch connection manager, threaded executor, and much more. To access the data model from anywhere in the program use `CalliopeData.getInstance()`. 

##### CyVerse Connection Manager (/cyverse/CyVerseConnectionManager.java)

This class contains all method definitions for connecting to CyVerse using Jargon. It lets users authenticate their account, upload images, download files, and much more. 

##### Elastic Search Connection Manager (/elasticsearch/ElasticSearchConnectionManager.java)

This class contains all method definitions for connecting to the ElasticSearch index. It lets users authenticate their account, index images, and perform metadata queries. 

##### Data Sources (/dataSources/IDataSource.java)

This interface defines a set of methods used for interacting with different data sources. This should be implemented if adding support for more data sources such as Amazon S3 or Google Drive.

##### Image Container (/image/ImageContainer.java)

This interface is implemented by ImageEntry and ImageDirectory. Both these classes Are used in the tree of files on the right side on the import tab. ImageEntries are the programmatic form of an image file, while ImageDirectories are the programmatic form of a directory. ImageDirectories contain a list of sub-directories and files while ImageEntries contain image metadata and icon. This interface may be extended for custom image and directory implementations for use in new data sources. 

##### Settings Data (/settings/SettingsData.java)

This class contains a list of settings to be displayed in the settings tab. All settings contain properties with getters and setters which are bound to the settings tab.

##### Calliope Executor (/threading/CalliopeExecutor.java)

This class contains all things related to threading. It contains 3 different threading executors, the BackgroundExecutor, ImmediateExecutor, and QueuedExecutor. The background executor has 25 available threads all running at once which can receive tasks without user knowledge. The ImmediateExecutor does the same thing as the background executor, but the task message and progress is displayed to the user in the Collections tab. Task progress is shown in the center column at the bottom. The QueuedExecutor is used for any operation that must be done in a specific order. Any tasks added to this executor are guaranteed to run in the order they are added. This executor is used for doing tasks such as importing images, and logging in. Progress is displayed to the user at the bottom of the central pane in the import tab. 

##### FXML Loader Utils (/util/FXMLLoaderUtils.java)

This utility class allows for easy FXML document loading. Just call `FXMLLoaderUtils.loadFXML()` with the path of FXML file to load.