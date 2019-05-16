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

Image metadata is just additional data added onto an image file by the camera. It is "data about the data" stored as key -> value pairs as part of an image file. These key -> value pairs are typically referred to as `tags`. Examples of standard image metadata tags include date taken, focal length, and image width/height in pixels. Depending on the camera, image metadata can also be customized with attributes such as camera model, height above ground, and much more.

### What is a `site`?

Research drones are often flown in special recognized locations known as sites. Sites are published by some organizations that fly drones every few months to assist in grouping image data. Each organization has their own method of storing sites, but most have common attributes like a name, a geographical boundary, and a unique site identifier known as a site code. Many site providing organizations provide additional site attributes which are stored as well, but can't be relied on. Calliope currently makes use of two different site providers, described below:

1. NEON Sites - These sites are specified by the National Ecological Observatory Network (NEON). All site names and codes can be found through their [data api](http://data.neonscience.org/data-api#/) as JSON and boundaries must be manually downloaded on their website as a KMZ file.
2. LTAR Sites - These sites are specified by Long-Term Agroecosystem Research (LTAR) project. They provide site name, code, and boundary all through a single ESRI shape file. This shape file must be manually downloaded on the LTAR site.

### What is the `CyVerse data store`?

The CyVerse data store is the heart of CyVerse infrustracture. The `data store` refers to the database CyVerse keeps to store all files and information. This massive database is managed by a protocol called iRODS. iRODS enables data to be secured with user permissions, allows data to be uploaded/downloaded, can store file metadata, and provides encrypted user authentication. It organizes its files similarly to a classic Unix system where the root directory is `/` and user accounts are under the directory `/iplant/home/<username>`. Each new user has full permission over their home directory but nothing else.

Anyone can make a free CyVerse account to begin accessing the data store. If you haven't done so, you should make an account since all Calliope products require logging in before use. You can access the data store using any iRODS capable client, of which some are listed below:

- The Discovery Environment (DE) - The DE is a web platform hosted by CyVerse to view your data in the data store. To access it, see https://user.cyverse.org/dashboard/ and select `Request Access` under the Discovery Environment. You need to wait to be approved for DE use, which can take some time. You can email a CyVerse administrator if you need to be approved more quickly. Once you are approved to use the DE, and can click `Launch`. Here you can see the Unix-like directory structure provided by iRODS as well as manage file permissions manually.
- CyberDuck - This is a third party program that supports the iRODS protocol. To setup CyberDuck google `cyverse cyberduck` and follow the offical CyVerse tutorial. It is great for uploading and downloading files quickly.
- iCommands - If you prefer to use a command line interface to acccess the data store, you can do so using iCommands. This tool supports many of the basic Unix commands such as `cd`, except that the command is proceeded with an `i`, so you would execute `icd <dir>` to change directories. Full iCommands documentation can be found here: https://docs.irods.org/master/icommands/user/
- Programming libraries - iRODS provides many libraries for various programming languages to connect to an iRODS server and execute commands on it. Since Calliope is written in Java, the library "Jargon" is used. If you want to use a different language like Python, that's also supported. 

### What is `ElasticSearch`?

ElasticSearch is a database management system that is optimized for fast searches. If you want details about how ElasticSearch works, read more [here](https://www.elastic.co/guide/index.html). Simply put, ElasticSearch accepts HTTP REST requests with JSON bodies to decide what you want it to do, and then returns a response as JSON. For example, if you wanted to list all indices currently stored, the http request would be:
```json
Request:
GET http://<elasticsearch-server-ip>/_cat/indices?format=json
Response:
[
  {
    "health": "green",
    "status": "open",
    "index": "sites",
    "uuid": "b6h7tH3_QyuXmwnG_YbcNw",
    "pri": "1",
    "rep": "0",
    "docs.count": "291",
    "docs.deleted": "0",
    "store.size": "9.2mb",
    "pri.store.size": "9.2mb"
  }
]
```

In this example, our ElasticSearch database had a single index named "sites" with 291 "documents" which are similar to rows in a traditional SQL database. Please note that an `index` in a ElasticSearch is the same thing as a `table` in a traditional SQL database. Notice that we didn't specify a JSON body for the GET request. That is because this specific endpoint does not require any additional data to execute, it can simply take all its arguments in the URL. We will need to use the JSON body, however, if we want to insert a new site into this index, for example:

```json
Request:
POST http://<elasticsearch-server-ip>/sites/_doc/0
{
  "name": "Test site",
  "code": "352ge"
}
Response:
{
  "_index": "sites",
  "_type": "_doc",
  "_id": "0",
  "_version": 1,
  "result": "created",
  "_shards": {
    "total": 2,
    "successful": 1,
    "failed": 0
  },
  "_seq_no": 0,
  "_primary_term": 1
}
```

As we can see, this request was successful and we inserted a site with name "Test site" and code "352ge" into our theoretical sites index. ElasticSearch also supports a variety of libraries that abstract away the HTTP REST api into a simple interface for a given language. Calliope makes use of the Java and Python ElasticSearch libraries.

### What is `Kibana`?

Kibana is a visual way to interact with an ElasticSearch index. It runs side-by-side with ElasticSearch and allows you to easily execute queries, perform queries, and create analysis charts through a web-based interface. 

### What is `docker`?

Docker is a way of containerizing, securing, and building a piece of software without needing to create an entire VM for it. Programs are built and dependencies fetched like a Unix Makefile or build script, except that it's done inside of a "vm-like" container. Docker containers are built using a `Dockerfile`, which is just a text file. It begins by specifying a base image which may look like `FROM ubuntu:latest`. A base image is simply an operating system or environment to "start" from. You can assume the image is clean and minimal. You then specify a list of run instructions which executes commands on that base image such as `RUN apt-get install openjdk-8-jdk -y`. This command installs Java onto the Ubuntu base image. Finally, you specify what executable to run, and the docker container is complete. This may look something like `ENTRYPOINT ["java", "-jar", "/MyProgram.jar"]`.

## Tying all the components together

The Calliope project makes use of many different programs and apis to accomplish its goal, and those are described below.

### Calliope ElasticSearch and Kibana

To begin, the Calliope backend requires an ElasticSearch instance to store its image metadata. Currently, this database is hosted on Tyson's server, however the end goal is to host this on the CyVerse data store. You can also create your own ElasticSearch cluster if you want to, using these commands to install it with docker:

```shell
# Download elasticsearch image:
docker pull docker.elastic.co/elasticsearch/elasticsearch-oss:6.2.4
# Download kibana image:
docker pull docker.elastic.co/kibana/kibana-oss:6.2.4
# Run & Install elasticsearch:
docker run -d -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch-oss:6.2.4
# Run & Install kibana:
docker run -d --net=host -e "ELASTICSEARCH_URL=http://localhost:9200" docker.elastic.co/kibana/kibana-oss:6.2.4
```

You should now be able to access your ElasticSearch index at localhost:9200 and kibana index visualization at localhost:5601.

### Calliope iRODS system

Normally the CyVerse data store is used as the iRODS backend system for storing image files. It may be useful to setup a custom temporary iRODS system to test out custom iRODS rules, and this can be done with the following series of commands on a new CentOS7 installation:

```shell
# Run each of these commands one by one into the command line:

# Update the OS
sudo yum update
# Install postgres dependency
sudo yum install postgresql-server postgresql-contrib
# Setup postgres DB and make sure it automatically starts
sudo postgresql-setup initdb
sudo systemctl start postgresql
# Sudo as the postgres user to setup the tables
sudo su - postgres
# Enter the DBMS 
psql
# Create the iRODS user, iCAT table, and let iRODS manage iCAT
CREATE USER irods WITH PASSWORD 'password';
CREATE DATABASE "ICAT";
GRANT ALL PRIVILEGES ON DATABASE "ICAT" TO irods;
# Quit the DBMS
\q
# Leave the postgres user
exit
# Install required irods keys
sudo rpm --import https://packages.irods.org/irods-signing-key.asc
# Make sure we have wget for the next command
sudo yum install wget
# Add the irods repo to download from
wget -qO - https://packages.irods.org/renci-irods.yum.repo | sudo tee /etc/yum.repos.d/renci-irods.yum.repo
# Install iRODS server and postgres db plugin
sudo yum install epel-release
sudo yum install irods-server irods-database-plugin-postgres
# A bit of a hack... removes lines 164/165 from the iRODS setup script that was throwing an error...
sudo sed -i '164,165d' /var/lib/irods/scripts/setup_irods.py
# Replace indent with md5 authentication so that we don't get any auth errors
sudo sed -i 's/ident/md5/g' /var/lib/pgsql/data/pg_hba.conf
# Reboot postgres db
sudo systemctl restart postgresql
# Call our setup script
sudo python /var/lib/irods/scripts/setup_irods.py
# At this point you will be promted to enter a bunch of information about the iRODS install. Feel free to change any of it you want,
# but the defautls are fine for the most part. When the script asks for the following fields below, you can use the suggested values
# (because defaults are not given):
# Database password: password
# iRODS server's zone key: TEMPORARY_zone_key
# iRODS server's negotiation key: TEMPORARY_32byte_negotiation_key
# Control Plane key: TEMPORARY__32byte_ctrl_plane_key
# iRODS server's administrator password: password
sudo sed -i 's/CS_NEG_DONT_CARE/CS_NEG_REFUSE/g' /etc/irods/core.re
# Become the irods user
sudo su irods
# Reboot the server
/var/lib/irods/irodsctl restart
```

Once iRODS is setup you can add custom rules to the file `/etc/irods/core.re`. 

### Calliope Auth

Unfortunately, ElasticSearch does not come with any method of authenticating users by default, and gives any user full permissions. To circumvent this, a plugin must be used instead to forward authentication requests through Calliope Auth to iRODS. Calliope auth is documented in the [Calliope Auth directory](./CalliopeAuth/README.md).

### Calliope

The Calliope client is the core of the Calliope project. It is written in Java using JavaFX and runs as a downloadable desktop application. To see more Calliope documentation click [here](./Calliope/README.md).