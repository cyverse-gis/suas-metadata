FROM ubuntu:16.04

MAINTAINER Tyson Swetnam <tswetnam@cyverse.org>

USER root

RUN apt-get update -y && apt-get upgrade -y

RUN apt-get install -y openjdk-8-jdk openjfx git maven wget make

WORKDIR /opt/

RUN cd /opt && wget https://exiftool.org/Image-ExifTool-12.00.tar.gz \
    && gzip -dc Image-ExifTool-12.00.tar.gz | tar -xf - \
    && cd /opt/Image-ExifTool-12.00 \
    && perl Makefile.PL \
    && make install 

ADD https://api.github.com/repos/cyverse-gis/suas-metadata/git/refs/heads/master version.json
RUN cd /opt && git clone https://github.com/cyverse-gis/suas-metadata \
    && cd /opt/suas-metadata/Calliope \
    && mvn -U compile package

ENTRYPOINT ["java", "-jar", "/opt/suas-metadata/Calliope/target/Calliope-1.0-SNAPSHOT-jar-with-dependencies.jar"]
