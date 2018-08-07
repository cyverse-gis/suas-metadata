# Calliope Developer Documentation

This is an in-depth overview of all the pieces and parts working together to make the Calliope project a reality.

## I have no idea what Calliope is, where do I start?

So you just got tasked to continue work on the Calliope project and your first thoughts might be something like:

> What's Calliope, what does Calliope depend on?
> What's the CyVerse Data Store, what's ElasticSearch, and what's Calliope Auth?
> How do I set these things up, and where do I start adding features?

Well, this is the place for you. Calliope is a project with the goal of developing a set of tools to help drone researchers collect, store, and analyze their drone data. More specifically researchers fly their drones over special `sites` and return with an SD card full of photos. These photos need to move through the following pipeline:

1. Raw images must be stored on some sort of database
2. The raw image metadata must be stored on a database, and indexed to allow for searchability
3. Images must be queryable by multiple users and retrievable
4. Images must be visualizable using a map, such as OpenDroneMap. This requires a High Performance Computing (HPC) system to convert thousands of individual images to a single orthophoto
 
## Important terms to understand

### What is `image metadata`?

Image metadata is just additional data added onto an image file by the camera. It is "data about the data" stored as key -> value pairs as part of an image file. These key -> value pairs are typically referred to as `tags`. Examples of standard image metadata tags include date taken, focal length, and image width/height in pixels. Depending on the camera, image metadata can also be much more complex with attributes such as camera model, height above ground, and much more.

### What is a `site`?

Drones are flown in special areas known as sites. Sites are known ahead of time and are specified by various sources. Sites from any source have common attributes that must include a name, a geographical boundary, and a unique site identifier known as a site code. Many site providers provide additional site attributes, but since these are not common to all sites they are not relied on. Calliope currently makes use of two different site providers, described below:

1. NEON Sites - These sites are specified by the National Ecological Observatory Network (NEON). All site names and codes can be found through their [data api](http://data.neonscience.org/data-api#/) as JSON and boundaries must be manually downloaded on their website as a KMZ file.
2. LTAR Sites - These sites are specified by Long-Term Agroecosystem Research (LTAR) project. They provide site name, code, and boundary all through a single ESRI shape file. This shape file must be manually downloaded on the LTAR site.

### What is the `CyVerse Data Store`?

The CyVerse Data Store is the heart of CyVerse infrustracture. The `Data Store` is a term that refers to the database CyVerse keeps to store all files and information. It is managed by a protocol called iRODS. iRODS enables data to be secured with user permissions, allows data to be uploaded/downloaded, and can store image metadata. It organizes its files similarly to a classic Unix system where the root directory is `/` and user accounts are under the directory `/iplant/home/<username>`. 

Anyone can make a free CyVerse account to begin accessing the Data Store. If you haven't done so, you should make an account since all Calliope products require logging in before use. You can access the Data Store using any iRODS capable client, of which some are listed below:

- The Discovery Environment (DE) - The DE is a web platform hosted by CyVerse to view your data in the Data Store. To access it, see https://user.cyverse.org/dashboard/ and select `Request Access` under the Discovery Environment. After a little while, you should be approved to use the DE, and can click `Launch`.
- CyberDuck - This is a third party program that supports the iRODS protocol. To setup CyberDuck google `cyverse cyberduck` and follow the offical CyVerse tutorial. It is great for uploading and downloading files quickly.
- iCommands - If you prefer to use a command line interface to acccess the datastore, you can do so using iCommands. This tool supports most of the basic Unix commands such as `cd` but proceeded with an `i`, so `icd`. Full iCommands help can be found here: https://docs.irods.org/master/icommands/user/
- Programming libraries - iRODS provides many libraries for various programming languages to connect to an iRODS server and execute commands on it. Since Calliope is written in Java, the library "Jargon" is used.