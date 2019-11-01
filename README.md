[![DOI](https://zenodo.org/badge/132954148.svg)](https://zenodo.org/badge/latestdoi/132954148) [![license](https://img.shields.io/badge/license-GPLv3-blue.svg)](https://opensource.org/licenses/GPL-3.0) 

This repository maintains software used by **Project Calliope**, a sUAS-based imagery/video/lidar upload|download program [Powered by CyVerse](https://www.cyverse.org/powered-by-cyverse). 

## Contents

### Calliope

Desktop Java application used to import, tag, organize, upload, and visualize drone images. More details can be found [here](./Calliope)

### CalliopeAuth

Backend Java application used to accept and forward HTTP basic authentication requests from ElasticSearch to CyVerse using iRODS. More details can be found [here](./CalliopeAuth) 

### Java Metadata Processor & Python Sandbox

A Java and Python implementation of a program that could run on CentOS7 along side iRODS. iRODS rules could trigger automatic metadata indexing through this java program or python script. Both are currently not used, but much of the code from the Java metadata processor is reflected in [Calliope](./Calliope)

### Developer Documentation

If you would like to contribute to this project and learn about the code base in detail, see [Developer Documentation](./DeveloperDocumentation.md)

### Credits

This program is currently being developed by [CyVerse](https://github.com/DavidM1A2/). It is heavily based on previous code from [Sanimal](https://github.com/DavidM1A2/Sanimal). 

- Software Developers 
  - Creator: [David Slovikosky](https://github.com/DavidM1A2/), 
  - Past Dev: [Pooja Narayan](https://github.com/poojalnarayan), 
  - Current Dev(s): [Caleb Prigge](https://github.com/priggec), [Jackson Lindsay](https://github.com/jlhonors)
  - Project Supervisor: [Tyson L. Swetnam](https://github.com/tyson-swetnam)
- Funding Sources 
  - The University of Arizona [School of Natural Resources and the Environment](https://snre.arizona.edu/) 
  - [USDA Agricultural Research Service Southwest Watershed Research Center](https://www.ars.usda.gov/pacific-west-area/tucson-az/southwest-watershed-research-center/) 
  - National Science Foundation via [CyVerse](https://cyverse.org)

**This material is based upon work supported by the U.S. Department of Agriculture, Agricultural Research Service, under Agreement no. 58-2022-5-13.**

**Any opinions, findings, conclusion, or recommendations expressed in this publication are those of the author(s) and do not necessarily reflect the view of the U.S. Department of Agriculture.**
