FROM ubuntu:latest

MAINTAINER David Sloviksoky <dslovikosky@email.arizona.edu>

RUN apt-get update -y
RUN apt-get upgrade -y
RUN apt-get autoremove -y

RUN apt-get install openjdk-8-jdk -y

RUN apt-get install git -y

RUN apt-get install maven -y

WORKDIR /
RUN git clone https://github.com/cyverse-gis/suas-metadata

WORKDIR /suas-metadata/CalliopeAuth

RUN mvn compile package

ENTRYPOINT ["java", "-jar", "/suas-metadata/CalliopeAuth/target/CalliopeAuth-1.0-SNAPSHOT-jar-with-dependencies.jar"]