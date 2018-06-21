FROM ubuntu:latest

MAINTAINER David Sloviksoky <dslovikosky@email.arizona.edu>

RUN apt-get update -y
RUN apt-get upgrade -y
RUN apt-get autoremove -y

RUN apt-get install gtk2-engines -y
RUN apt-get install libswt-gtk-3-java -y
RUN apt-get install libxslt1.1 -y
RUN apt-get install libxtst6 -y
RUN apt-get install libxxf86vm1 -y

RUN apt-get install openjdk-8-jdk -y
RUN apt-get install openjfx -y

RUN apt-get install git -y

WORKDIR /
RUN git clone https://github.com/DavidM1A2/Sanimal

ENTRYPOINT ["java", "-jar", "/Sanimal/tmp/SanimalFX.jar"]