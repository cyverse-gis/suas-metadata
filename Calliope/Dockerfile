FROM ubuntu:latest

MAINTAINER David Sloviksoky <dslovikosky@email.arizona.edu>

RUN apt-get update -y
RUN apt-get upgrade -y
RUN apt-get autoremove -y

RUN apt-get install openjdk-8-jdk -y
RUN apt-get install openjfx -y

RUN apt-get install git -y

RUN apt-get install maven -y

RUN apt-get install wget -y

RUN apt-get install make -y

WORKDIR /

RUN wget https://www.sno.phy.queensu.ca/~phil/exiftool/Image-ExifTool-11.10.tar.gz
RUN gzip -dc Image-ExifTool-11.10.tar.gz | tar -xf -
WORKDIR /Image-ExifTool-11.10
RUN perl Makefile.PL
RUN make install

WORKDIR /

WORKDIR /
ADD https://api.github.com/repos/cyverse-gis/suas-metadata/git/refs/heads/master version.json
RUN git clone https://github.com/cyverse-gis/suas-metadata

WORKDIR /suas-metadata/Calliope

RUN mvn compile package

ENTRYPOINT ["java", "-jar", "/suas-metadata/Calliope/target/Calliope-1.0-SNAPSHOT-jar-with-dependencies.jar"]