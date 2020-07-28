# Calliope User Manual

Calliope is a program developed by [CyVerse](https://www.cyverse.org/) for the University of Arizona [School of Natural Resources and the Environment](https://snre.arizona.edu/) and [Agricultural Research Service Southwest Watershed Research Center](https://www.ars.usda.gov/pacific-west-area/tucson-az/southwest-watershed-research-center/). Calliope allows users to tag drone metadata, transfer it onto a database for safe keeping, and then query using a map based interface.

## Installation

There are different ways to run Calliope, either by building it from source using the contents of this Git repo, or by downloading a [pre-built image here](https://data.cyverse.org/dav-anon/iplant/home/shared/aes/calliope/Calliope-1.0-SNAPSHOT-jar-with-dependencies.jar), or running with [Docker]()

#### Dependencies

To run Calliope there are several prerequisite dependencies that you will need to install:

**[Java 8 & JavaFX](http://www.oracle.com/technetwork/java/javase/downloads/)**

  Note: If you are using OpenJDK instead of Oracle's JDK, you will also need OpenJFX.

**[ExifTool](https://www.sno.phy.queensu.ca/~phil/exiftool/)**

ExifTool must be accessible in your system $PATH environment variable. Calliope will run without ExifTool so you can check if you have successfully installed ExifTool by launching Calliope and going to the settings tab. If the text at the bottom reads `ExifTool Installation Status: Found` you are good to go. If the text at the bottom reads `ExifTool Installation Status: Not Found` follow the instructions at the bottom of the settings tab to install ExifTool on your system. If you attempt to import images without having ExifTool installed you will be prompted to install ExifTool first.

### Running the prebuilt `jar`

Download the [Calliope for Java 8](https://data.cyverse.org/dav/iplant/home/tswetnam/calliope/Calliope-1.0-SNAPSHOT-jar-with-dependencies.jar). It is highly recommended to use the Java 8 version if possible. 

```
java -jar Calliope-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Running Calliope on Debian Distros

These step-by-step instructions should get Calliope up and running on most, if not all debian distibutions.

1. Install Required Packages

Run the following command in the terminal.
```
apt install exiftool openjdk-8-jre openjfx=8u161-b12-1ubuntu2 libopenjfx-jni=8u161-b12-1ubuntu2 libopenjfx-java=8u161-b12-1ubuntu2
```

2. Set the Java Version

Run the following command and make sure the OpenJFX 8 installation is selected.
```
update-alternatives --config java
```

3. Retrieve Calliope JAR

Run the following command to download the Calliope JAR to the current directory.
```
wget https://data.cyverse.org/dav-anon/iplant/home/shared/aes/calliope/Calliope-1.5-SNAPSHOT-jar-with-dependencies.jar
```

4. Run Calliope

Run the following command to run Calliope.
```
java -jar Calliope-1.5-SNAPSHOT-jar-with-dependencies.jar
```
Alternatively, double-clicking the JAR should open Calliope as well.

If you encounter issues, try
```
chmod +x Calliope-1.5-SNAPSHOT-jar-with-dependencies.jar
```
Then try to run the JAR again.

### Build from Source 

#### Dependencies

**Java 8**

Note: If you are using OpenJDK instead of Oracle's JDK, you will also need [OpenJFX](http://openjdk.java.net/projects/openjfx/).

To use Java 9 or 10, you will need to edit the [pom.xml](./pom.xml) file. Find the line `<artifactId>controlsfx</artifactId>` and replace the next line with the right version of ControlsFX.

```xml
<version>8.40.14</version> <!-- Use this for Java 8 -->
<version>9.0.0</version> <!-- Use this for Java 9 or 10 -->
```

**ExifTool**

**[Apache Maven](https://maven.apache.org/install.html)**

Maven is a build automation tool used primarily for Java projects. Maven addresses two aspects of building software: first, it describes how software is built, and second, it describes its dependencies.

All Java dependencies will be automatically fetched through `maven`. Please note that you will need ExifTool installed on your system as well to import images and read metadata. 

#### Installation Commands

After installing all pre-requisites, clone the github repository into a directory:
```shell
git clone https://github.com/cyverse-gis/suas-metadata <directory>
```
Build the project into an executable JAR file to run:
```shell
cd '<directory>/Calliope/'
mvn -U compile package
```

##### Make sure to set Java version to 8

On Mac OSX:
```
alias j8="export JAVA_HOME=`/usr/libexec/java_home -v 1.8`; java -version"
j8
```

Run the program:
```shell
java -jar '<directory>/Calliope/target/Calliope-1.0-SNAPSHOT-jar-with-dependencies.jar'
```

## Usage and Documentation

### First Time Calliope Execution

When first running Calliope you will most likely see a warning popup that says, `Invalid ElasticSearch host or port, please check 'calliope.properties'!`. A `calliope.properties` file will appear next to the `Calliope-*.jar` file that was executed. This file will need to be edited to be so that Calliope knows what ElasticSearch cluster to talk to. You will need to ask the project administrator for the ElasticSearch host IP and port number. If you are a system administrator you can also setup your own ElasticSearch index and use that if you desire.


### Logging In

The CyVerse infrastructure is heavily used by Calliope to both store data and authenticate users. In order to use Calliope, you will need a CyVerse account. Please make a free account here: https://user.cyverse.org/register. After registering you can open Calliope where you should be prompted with a login screen.

![Login Screen](./screenshots/login.PNG)

At this point you can enter your CyVerse account username and password into the respective fields. If you would like Calliope to remember your username in the future you can check the box. If you have not yet registered for an account click on the blue 'Register' text. If you have trouble logging in and would like to change your password click on 'Forgot Password'. 

### Calliope Interface Overview

Calliope's interface is broken up by tabs found on the top of the window. A short description of each tab is described below:
- [Home](./Home.md) - This tab allows you to exit the program and read credits.
- [Import](./Import.md) - This tab lets you import image data and modify any metadata that may be found on those images.
- [Collections](./Collections.md) - This tab lets you manage uploading data to and from your PC as well as showing what data has already been uploaded.
- [Map](./Map.md) - This tab allows you to see all uploaded images on an interactive map as well as perform queries to filter what data you see.
- [Settings](./Settings.md) - This tab lets you specify various settings, such as date and time format.

Hint: Click on a tab above to get a detailed description of the tab.

### Developer's Documentation

If you would like to work on this project an outline of the code structure can be found [here](./DeveloperDocumentation.md).

### Credits

This program is currently being developed by [CyVerse](https://www.cyverse.org/). It is heavily based on previous code from [Sanimal](https://github.com/DavidM1A2/Sanimal). 

- Software Developer - [David Slovikosky](https://github.com/DavidM1A2/)
- Project Supervisor - [Tyson Swetnam](https://github.com/tyson-swetnam)
- Funding Sources: University of Arizona School of Natural Resources and the Environment ([SNRE](https://snre.arizona.edu/)), & [USDA Agricultural Research Service Southwest Watershed Research Center](https://www.ars.usda.gov/pacific-west-area/tucson-az/southwest-watershed-research-center/)

**This material is based upon work supported by the U.S. Department of Agriculture, Agricultural Research Service, under Agreement no. 58-2022-5-13.**

**Any opinions, findings, conclusion, or recommendations expressed in this publication are those of the author(s) and do not necessarily reflect the view of the U.S. Department of Agriculture.**
