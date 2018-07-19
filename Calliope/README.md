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

This is the default tab users land when logging in. The top left contains the Calliope logo, followed by a welcome message with your CyVerse username. The credits button can be used to see details about who worked on the project. The logout and exit buttons both close the program. This project was funded by the School of Natural Resources and the Environment (SNRE), and a link to their home page can be accessed by clicking the SNRE logo in the bottom left. The project was developed by CyVerse, and a link to their home page can be found by clicking the CyVerse logo in the bottom right.

#### Import Tab
![Import Tab](/Calliope/screenshots/importStart.PNG)

The import tab is where you can begin importing your drone data for processing. To begin, click the 'Import Images' button found in the bottom right corner. You will be prompted to pick a data source to import images from. There are currently 3 different options:
1. CyVerse Data Store - If you have already uploaded your image data to the CyVerse Data Store you can use this option to import those files. This can happen if you use a third party tool to upload images such as CyberDuck or upload images straight to the Data Store using the Discovery Environment. 
2. Local PC File(s) - This option lets you pick specific image files on your local PC to import. You may import a single image or multiple images if you desire. If you import multiple images, they must all be found in the same directory. 
3. Local PC Directory - This option lets you pick a specific directory on your local PC to import. The directory will be recursively searched for any image files while ignoring unknown files. 

After importing images into Calliope you may view them by clicking on them on the right panel. Depending on the speed of the PC, storage medium, and file size the preview may take some time to load. A loading circle will appear in the top left of the central image preview window indicating an image is loading. Left and right arrows are found on the left and right side of the central window allowing you to navigate to the next and previous image respectively.

Once an image is selected, the bottom metadata panel becomes populated. Here you may update any incorrect metadata about a specific image file. If the image file is missing a piece of metadata, the field will be blank, or 0. The metadata fields are described below:
- Date Taken - The day and time the image was taken. This may be incorrect if the camera was setup improperly, and can be adjusted by either typing into the 'Date Taken' box or selecting the clock and picking a time. The value must be a valid date formatted in the same way as initially shown.
- Latitude - The exact latitude coordinate the drone was at the second the image was taken.  The value must be a valid number.
- Longitude - The exact longitude coordinate the drone was at the second the image was taken.  The value must be a valid number.
- Elevation - The elevation the drone was at the second the image was taken. This must be a valid number.
- Drone Brand - The brand of the drone, may be something like 'DJI'. This will be blank if the drone did not embed brand information on the image file. The brand name can contain any characters.
- Camera Model - The camera model used to take the image, may be something like 'FC330'. This will be blank if the drone did not embed camera information on the image file. The camera model name can contain any characters.
- Drone Speed - A 3-dimensional vector representing this drone's x, y, and z velocities the moment the image was taken. These will all be 0 if the drone did not embed any speed information on the image. All three fields must contain valid numbers.
- Drone Rotation - A 3-dimensional vector representing this drone's roll, pitch, and yaw. velocities the moment the image was taken. These will all be 0 if the drone did not embed any rotational information on the image. All three fields must contain valid numbers.

The left panel of the import window has two tabs, one for NEON sites and one for Raw Metadata. 

The NEON sites panel shows one entry per NEON site. These sites can be viewed in more detail on https://www.neonscience.org/field-sites/field-sites-map. Some site names may be repeated if they are nearby without a common geo-graphical boundary. You can search for a NEON site by typing into the 'Search Sites' box below the list of NEON sites. The 'X' button will clear the current search. 

The Raw Metadata tab will show you all the raw metadata that was extracted from the image file. It is not editable, but can be useful for seeing what other data your images have stored. 

If you would like to specify that an imported image was taken at a NEON site you can do one of two things:
1. To manually specify which site an image was taken at, drag a NEON site from the left panel onto the central image preview or onto the image on the right panel to tag it with the site. You may also drag it to a directory, indicating that all images in that directory were taken at the given NEON site. 
2. To automatically detect which site an image was taken at, select an image or directory, and then click on the globe icon in the top right corner of the central image preview window. You can then pick either:
    - Detect if the image was taken at NEON site is within <input> km of the center point
    - Detect if image was taken inside of a NEON site based on the site's boundary.
The first option will test each image to see if it is within a certain distance of the center point of the site. If it is close to multiple sites, the closest one is picked. The second option will test if the image was taken inside of a know NEON site's boundary. 




