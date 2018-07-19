# Calliope User Manual

Calliope is a program developed by CyVerse for the School of Natural Resources and the Environment. It allows users to tag drone metadata, transfer it onto a database for safe keeping, and then query using a map based interface.

[TOC]

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