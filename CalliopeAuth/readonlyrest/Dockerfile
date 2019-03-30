FROM docker.elastic.co/elasticsearch/elasticsearch-oss:6.2.4

#must have copy of readonly rest from https://github.com/beshu-tech/readonlyrest-docs/blob/master/elasticsearch.md#installing-the-plugin
COPY readonlyrest-1.17.0_es6.2.4.zip /usr/share/elasticsearch/readonlyrest-1.17.0_es6.2.4.zip
COPY readonlyrest.yml /usr/share/elasticsearch/configs/readonlyrest.yml

#Install ReadOnlyRest
RUN cd /usr/share/elasticsearch && \
    bin/elasticsearch-plugin install -b file:///usr/share/elasticsearch/readonlyrest-1.17.0_es6.2.4.zip

# Append XPACK security update to elasticsearch yaml
RUN echo "xpack.security.enabled: false" >> /usr/share/elasticsearch/config/elasticsearch.yml
