# OpenDistro

https://opendistro.github.io/for-elasticsearch-docs/docs/install/

```
docker pull amazon/opendistro-for-elasticsearch:0.7.1
docker pull amazon/opendistro-for-elasticsearch-kibana:0.7.1
```

```
docker run -p 9200:9200 -p 9600:9600 -e "discovery.type=single-node" amazon/opendistro-for-elasticsearch:0.7.1
```


```

```
