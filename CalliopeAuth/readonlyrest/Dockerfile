FROM docker.elastic.co/elasticsearch/elasticsearch-oss:6.5.4

# must have copy of readonly rest from https://github.com/beshu-tech/readonlyrest-docs/blob/master/elasticsearch.md#installing-the-plugin
COPY readonlyrest-1.17.3_es6.5.4.zip /usr/share/elasticsearch/readonlyrest-1.17.3_es6.5.4.zip

# Install ReadOnlyRest
RUN cd /usr/share/elasticsearch && \
    bin/elasticsearch-plugin install -b file:///usr/share/elasticsearch/readonlyrest-1.17.3_es6.5.4.zip && \
    rm /usr/share/elasticsearch/readonlyrest-1.17.3_es6.5.4.zip

COPY readonlyrest.yml /usr/share/elasticsearch/config/readonlyrest.yml

# Append XPACK security update to elasticsearch yaml
# RUN echo "xpack.security.enabled: false" >> /usr/share/elasticsearch/config/elasticsearch.yml
# RUN echo "tribe.cluster1.xpack.security.enabled: false" >> /usr/share/elasticsearch/config/elasticsearch.yml
