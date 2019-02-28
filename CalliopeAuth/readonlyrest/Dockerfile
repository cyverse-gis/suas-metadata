FROM docker.elastic.co/elasticsearch/elasticsearch-oss:6.6.0

#must have copy of readonly rest from https://github.com/beshu-tech/readonlyrest-docs/blob/master/elasticsearch.md#installing-the-plugin
COPY readonlyrest-1.17.0_es6.6.0.zip /usr/share/elasticsearch/readonlyrest-1.17.0_es6.6.0.zip
COPY readonlyrest.yml /usr/share/elasticsearch/configs/readonlyrest.yml

#Install ReadOnlyRest
RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install -b file:///readonlyrest-1.17.0_es6.6.0.zip

# Append XPACK security update to elasticsearch yaml
RUN echo "xpack.security.enabled: false" >> /usr/share/elasticsearch/config/elasticsearch.yml
