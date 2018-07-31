FROM ubuntu:latest

MAINTAINER David Sloviksoky <dslovikosky@email.arizona.edu>

RUN apt-get update -y
RUN apt-get upgrade -y
RUN apt-get autoremove -y

RUN apt-get install openjdk-8-jdk -y

RUN apt-get install git -y

WORKDIR /
RUN git clone https://github.com/cyverse-gis/suas-metadata



ENTRYPOINT ["java", "-jar", "/suas-metadata/tmp/SanimalFX.jar"]