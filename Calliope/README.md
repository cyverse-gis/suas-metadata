# Calliope User Manual

Calliope is a program developed by CyVerse for the School of Natural Resources and the Environment. It allows users to tag drone metadata, transfer it onto a database for safe keeping, and then query using a map based interface.

## Installation

### Getting Started
This repository should be cloned and then built using maven. All dependencies will be automatically fetched. through maven 

#### Prerequisites
Java 8:
http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
Maven:
https://maven.apache.org/install.html

#### Build from Source
Clone the github repository into a directory:
```shell
git clone https://github.com/cyverse-gis/suas-metadata <directory>
```
Build the project into an executable JAR file to run:
```shell
cd '<directory>/Calliope/'
mvn -U compile package
```
Run the program:
```shell
java -jar '<directory>/Calliope/target/Calliope-1.0-SNAPSHOT-jar-with-dependencies.jar'
```

## Usage and Documentation

### Logging In
The CyVerse infrastructure is heavily used by Calliope to both store data and authenticate users. In order to use Calliope, you will need a CyVerse account. Please make a free account here: https://user.cyverse.org/register. After registering you can open Calliope where you should be prompted with a login screen.

![Login Screen](/Calliope/screenshots/login.PNG)

At this point you can enter your CyVerse account username and password into the respective fields. If you would like Calliope to remember your username in the future you can check the box. If you have not yet registered for an account click on the blue 'Register' text. If you have trouble logging in and would like to change your password click on 'Forgot Password'. 

### Calliope Interface Overview

Calliope's interface is broken up by tabs found on the top of the window. A short description of each tab is described below:
- Home - This tab allows you to exit the program and read credits.
- Import - This tab lets you import image data and modify any metadata that may be found on those images.
- Collections - This tab lets you manage uploading data to and from your PC as well as showing what data has already been uploaded.
- Map - This tab allows you to see all uploaded images on an interactive map as well as perform queries to filter what data you see.
- Settings - This tab lets you specify various settings, such as date and time format.

Each tab will be described in detail below.

### Calliope Interface in Detail

#### Home Tab
![Home Tab](/Calliope/screenshots/home.PNG)

This is the default tab users land when logging in. The top left contains the Calliope logo, followed by a welcome message with your CyVerse username. The credits button can be used to see details about who worked on the project. The logout and exit button both close the program. This project was funded by the School of Natural Resources and the Environment (SNRE), and a link to their home page can be accessed by clicking the SNRE logo in the bottom left. The project was developed by CyVerse, and a link to their home page can be found by clicking the CyVerse logo in the bottom right.

#### Import Tab
![Import Tab](/Calliope/screenshots/importStart.PNG)













