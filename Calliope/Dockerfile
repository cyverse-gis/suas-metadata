FROM ubuntu:16.04

MAINTAINER Tyson Swetnam <tswetnam@cyverse.org>

USER root

RUN apt-get update -y && apt-get upgrade -y

RUN apt-get install -y exiftool openjdk-8-jre openjfx=8u161-b12-1ubuntu2 libopenjfx-jni=8u161-b12-1ubuntu2 libopenjfx-java=8u161-b12-1ubuntu2 git maven wget make

WORKDIR /opt/

RUN cd /opt && wget https://data.cyverse.org/dav-anon/iplant/home/shared/aes/calliope/Calliope-1.5-SNAPSHOT-jar-with-dependencies.jar 

ENTRYPOINT ["java", "-jar", "/opt/Calliope-1.5-SNAPSHOT-jar-with-dependencies.jar"]