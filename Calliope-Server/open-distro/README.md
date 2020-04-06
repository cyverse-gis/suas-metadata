# OpenDistro ElasticSearch

These files are used to initial the [Docker container](https://hub.docker.com/r/amazon/opendistro-for-elasticsearch) for [Amazon OpenDistro for ElasticSearch](https://opendistro.github.io/for-elasticsearch/)

```
docker pull amazon/opendistro-for-elasticsearch:1.6.0
docker pull amazon/opendistro-for-elasticsearch-kibana:1.6.0
```

[Setting up with Docker](https://opendistro.github.io/for-elasticsearch-docs/docs/install/docker/)

## Docker compose

We are launching our containers using a `docker-compose` file

```
docker-compose up
```
