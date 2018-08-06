# suas-metadata

This repository maintains the software used for the Project Calliope sUAS (small unmanned aerial system) data upload/download program [Powered by Cyverse](https://www.cyverse.org/powered-by-cyverse). 

## Contents

### Calliope

Desktop Java application used to import, tag, organize, upload, and visualize drone images. More details can be found [here](./Calliope)

### CalliopeAuth

Backend Java application used to accept and forward HTTP basic authentication requests from ElasticSearch to CyVerse using iRODS. More details can be found [here](./CalliopeAuth) 

### Java Metadata Processor & Python Sandbox

A Java and Python implementation of a program that could run on CentOS7 along side iRODS. iRODS rules could trigger automatic metadata indexing through this java program or python script. Both are currently not used, but much of the code from the Java metadata processor is reflected in [Calliope](./Calliope)

### iRODS Server

Simple directory containing two files, `iRODS Installation Steps CentOS7.txt` and `iRODS rules.txt`. The first file contains a list of instructions to install a custom iRODS server on a fresh CentOS7 install. This can be used to test iRODS rules. Test iRODS rules are found in the second text file, even though none of them are actually used.

### Photoscan-scripts

### Credits

This program is currently being developed by [CyVerse](https://github.com/DavidM1A2/). It is heavily based on previous code from [Sanimal](https://github.com/DavidM1A2/Sanimal). 

- Software Developer - [David Slovikosky](https://github.com/DavidM1A2/)
- Project Supervisor - [Tyson Swetnam](https://github.com/tyson-swetnam)
- Funding Sources: University of Arizona School of Natural Resources and the Environment ([SNRE](https://snre.arizona.edu/)), & [USDA Agricultural Research Service Southwest Watershed Research Center](https://www.ars.usda.gov/pacific-west-area/tucson-az/southwest-watershed-research-center/)

**This material is based upon work supported by the U.S. Department of Agriculture, Agricultural Research Service, under Agreement no. 58-2022-5-13.**

**Any opinions, findings, conclusion, or recommendations expressed in this publication are those of the author(s) and do not necessarily reflect the view of the U.S. Department of Agriculture.**
