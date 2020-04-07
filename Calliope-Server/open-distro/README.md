# OpenDistro ElasticSearch

These files are used to initial the [Docker container](https://hub.docker.com/r/amazon/opendistro-for-elasticsearch) for [Amazon OpenDistro for ElasticSearch](https://opendistro.github.io/for-elasticsearch/)

```
docker pull amazon/opendistro-for-elasticsearch:1.6.0
docker pull amazon/opendistro-for-elasticsearch-kibana:1.6.0
```

[Setting up with Docker](https://opendistro.github.io/for-elasticsearch-docs/docs/install/docker/)

## Create the Security configuration (HTTPS)

Following [these instructions](https://opendistro.github.io/for-elasticsearch-docs/docs/security-configuration/generate-certificates/)

```
# go into the directory with the OpenDistro docker-compose.yml
cd ~/github/suas-metadata/Calliope-Server/open-distro/

# create the private key
openssl genrsa -out root-ca-key.pem 2048

#  generate a new self-signed certificate
openssl req -new -x509 -sha256 -key root-ca-key.pem -out root-ca.pem

# generate the admin certificate
openssl genrsa -out admin-key-temp.pem 2048

# convert to pkcs8
openssl pkcs8 -inform PEM -outform PEM -in admin-key-temp.pem -topk8 -nocrypt -v1 PBE-SHA1-3DES -out admin-key.pem

# create a certificate signing request (.csr)
openssl req -new -key admin-key.pem -out admin.csr

# now generate the actual certificate
openssl x509 -req -in admin.csr -CA root-ca.pem -CAkey root-ca-key.pem -CAcreateserial -sha256 -out admin.pem
```

Each node that ElasticSearch runs on needs its own certificates.

```
# Node cert
openssl genrsa -out node-key-temp.pem 2048
openssl pkcs8 -inform PEM -outform PEM -in node-key-temp.pem -topk8 -nocrypt -v1 PBE-SHA1-3DES -out node-key.pem
openssl req -new -key node-key.pem -out node.csr
openssl x509 -req -in node.csr -CA root-ca.pem -CAkey root-ca-key.pem -CAcreateserial -sha256 -out node.pem
```

Clean up `*-temp.pem`
```
# Cleanup
rm admin-key-temp.pem
rm admin.csr
rm node-key-temp.pem
rm node.csr
```

## Docker compose

We are launching our OpenDistro ElasticSearch and Kibana containers using `docker-compose` 

```
docker-compose up
```
