FROM ubuntu:16.04

MAINTAINER Tyson Swetnam <tswetnam@cyverse.org>

USER root

RUN apt-get update -y && apt-get upgrade -y

RUN apt-get install -y openjdk-8-jdk openjfx git maven wget make

WORKDIR /opt/

RUN cd /opt && wget https://exiftool.org/Image-ExifTool-12.01.tar.gz \
    && gzip -dc Image-ExifTool-12.01.tar.gz | tar -xf - \
    && cd /opt/Image-ExifTool-12.01 \
    && perl Makefile.PL \
    && make install 

RUN cd /opt && wget https://data.cyverse.org/dav-anon/iplant/home/shared/aes/calliope/Calliope-1.5-SNAPSHOT-jar-with-dependencies.jar 

ENTRYPOINT ["java", "-jar", "/opt/Calliope-1.5-SNAPSHOT-jar-with-dependencies.jar"]
