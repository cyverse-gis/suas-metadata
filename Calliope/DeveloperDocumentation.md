# Calliope Developer Documentation

This documentation outlines some design decisions made in the development of Calliope. A description of important classes can be found as well. Calliope is written in Java using JavaFX as well as numerous other libraries. 

## Important Design Patterns

### Model - View - Controller (MVC)

Calliope is built using the standard MVC design pattern. 

In this design pattern, the model is created first with complete independence from the view or controller. The model tends to be made up of simple classes that make up the data of the program and functions to manipulate that data. Any database connections or network threads are managed by the model. 

The view is completely independent from the model, but is linked or 'bound' to the controller. The view is made up of FXML and CSS files that define each user interface component. Each FXML file begins with a component that has an `fx:controller="x.y.z"` attribute that defines the controller the FXML file is bound to. Any actions done to the UI are forwarded to the controller through action listeners.

The controller is dependent on both the model and controller. Any UI components such as text fields and labels are represented in the controller as fields. The controller contains method definitions that represent action listeners for actions that are fired from the view. The controller contains an important `public void initialize() {}` method that is automatically called upon UI creation. This method is used to initialize any UI fields and setup data bindings to the model. 

### Dependency Injection

Calliope makes use of dependency injection to link a FXML file to its controller. This is automatically performed by JavaFX, so you won't need to do anything special. FXML elements with the tag `<X fx:id="abc"/>` tell JavaFX to 'dependency inject' this element into the field `private X abc;` found in the controller. If this field is not present an error will arise. 

### Singleton

The singleton design pattern ensures only one instance of an object is ever created. This is used to ensure one publicly accessible data model is available to all controller files without needing to pass object references around. This data can be accessed with:
```java
CalliopeData.getInstance()...
```
While this makes referencing the data model very easy, it can be dangerous. A publicly accessible data model is typically frowned upon in traditional object oriented programming and is not the best design decision, but it does work for now. In the future this should be refactored to pass references around to each individual file.

### Observer

The observer design pattern is seen in JavaFX as data bindings. The model (as previously described in MVC) stores its variables as properties instead of primitive types. As an example, a person's name would be stored using a `StringProperty` instead of a `String`. A property has the ability to notify listeners of any changes. These properties are bound to the view through the controller and vice versa. When the model changes the view will automatically reflect the changes, and when the view updates the model will also reflect those changes. 

## Important Model Classes

Model classes are found in: ` /src/main/java/model`

##### Calliope Data (/CalliopeData.java)

This object contains all data used by Calliope. This data contains the list of sites, list of collections, currently imported images, CyVerse connection manager, ElasticSearch connection manager, threaded executor, and much more. To access the data model from anywhere in the program use `CalliopeData.getInstance()`. Any additional classes that need a single instance and a global presence should be added to this class.

##### CyVerse Connection Manager (/cyverse/CyVerseConnectionManager.java)

This class contains all method definitions for connecting to CyVerse using Jargon. It lets users authenticate their account, upload images, download files, and much more. Any logic that interfaces with iRODS or the CyVerse datastore should be found in this file or at least in the `/cyverse/` package.

##### Elastic Search Connection Manager (/elasticsearch/ElasticSearchConnectionManager.java)

This class contains all method definitions for connecting to the ElasticSearch index. It lets users authenticate their account, index images, and perform metadata queries. Any logic that interfaces with ElasticSearch should be found in this file or at least in the `/elasticsearch/` package.

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

## Libraries Used

Click any of the library titles to find out more!

### [JFXtras](http://jfxtras.org/)

A library used for its high quality UI controls. This project relys heavily on the 'DateTimePicker' which allows for users to easily pick a date and/or time without having to type it in a special format.

### [EasyBind](https://github.com/TomasMikula/EasyBind)

One of the most important libraries in this project that provides helper classes for creating data bindings. In vanilla JavaFX it is challenging to create a listener that listens for changes in sub-properties of an object and this library makes that much easier with `EasyBind.monadic()`.

### [ControlsFX](http://fxexperience.com/controlsfx/)

ControlsFX arguably contains the highest quality set of controls ever for JavaFX. Calliope specifically makes use of the `PropertySheet`, `TaskProgressView`, `NotificationPane`, `HyperlinkLabel`, `StatusBar`, `Validators`, and much more.

### [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/)

This library provides utility classes that allow for advanced string manipulation, number parsing, and exception parsing. It is used to format exceptions in popups and parsing strings into numbers.

### [Apache Commons IO](https://commons.apache.org/proper/commons-io/)

Much like Apache Commons Lang, this library provides utility classes for manipulating IO streams and files. It is used to retrieve file extensions and copy input streams to files.

### [Apache Commons BeanUtils](http://commons.apache.org/proper/commons-beanutils/)

An optional dependency that is required for apache commons configuration manager.

### [Apache Commons Compress](https://commons.apache.org/proper/commons-compress/)

Much like the other Apache Commons libraries, this one assists in the creation of TAR files as well as the extraction of KMZ files. This library lets us optimize uploads by 'tarring' images first.

### [Apache Commons Configuration](https://commons.apache.org/proper/commons-configuration/)

Once again another Apache Commons library, this time for managing and parsing configuration files. These are used to keep private details like passwords out of the code and in separate files.

### [Gson](https://github.com/google/gson)

A utility library by Google to help serialize Java objects into JSON and vice versa. This is incredibly useful when working with ElasticSearch which heavily makes use of JSON. 

### [FXGson](https://github.com/joffrey-bion/fx-gson)

An addon to Gson that allows for cleaner serialization and deserialization of JavaFX properties.

### [Jargon](https://github.com/DICE-UNC/jargon)

Arguably the most important library in this list, Jargon is a Java implementation of the iRODS protocol which allows for connection to CyVerse's iRODS-based Data Store. This allows users to authenticate and then upload and download images.

### [ElasticSearch](https://www.elastic.co/)

This library allows users to connect to an ElasticSearch index which enables fast queries and efficient data indexing. 

### [Log4j](https://logging.apache.org/log4j/)

A utility library used by ElasticSearch to print out errors and warnings. These are turned off by Calliope, but can be useful when debugging.

### [JavaAPIforKml](https://labs.micromata.de/projects/jak.html)

This library allows Java to parse and import KML files. These contain shape and boundary information of NEON sites ready to be drawn on the map.

### [ExifToolLib](https://github.com/rkalla/exiftool)

A fantastic Java interface to the ExifTool software used to read image file's metadata. This library starts the ExifTool process and keeps it alive while running multiple images through it.

### [Java Advanced Imaging](https://www.oracle.com/technetwork/java/iio-141084.html)

An advanced imaging library provided by Oracle to enable swing's `ImageIO` class to parse ".tif" image files. These can then be converted into JavaFX image files.

### [Spatial4j](https://github.com/locationtech/spatial4j) and [JTS](https://github.com/locationtech/jts)

Two utility libraries used by ElasticSearch for geo-queries.

### [SLF4j](https://www.slf4j.org/)

Another simple logging library used as a dependency to disable library logging.

### [Logback](https://logback.qos.ch/)

Intended successor to Log4j used to log information. This is used by the Jargon library and also disabled to remove debug messages.

### [FX Map Control](https://github.com/ClemensFischer/FX-Map-Control)

Advanced map rendering library for JavaFX that supports multiple tile providers and the addition of JavaFX nodes on the map.
