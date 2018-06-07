import metadataReader
import os
from elasticsearch import Elasticsearch, helpers
import sys

# The address of the host of ElasticSearch
ELASTIC_HOST = "128.196.38.73"
# The index name to store metadata in
DRONE_INDEX_NAME = "drone"


def main():
    # The directory to index
    dirToIndex = None
    # If we got a command line argument, store it
    if len(sys.argv) > 1:
        dirToIndex = sys.argv[1]
    # If we did not get a command line argument, ask for one
    else:
        # Read the directory to index from the stdin
        dirToIndex = input("Enter the path to the directory to read image metadata from:\n")

    # Make sure the path exists
    if not os.path.exists(dirToIndex):
        print("Path doesnt exist!")
        exit(1)
    # Make sure the path is a directory
    if not os.path.isdir(dirToIndex):
        print("Path is not a valid directory!")
        exit(1)

    # Index the directory
    indexDirectory(dirToIndex)


def indexDirectory(directory):
    # Create metadata from the directory ready for indexing
    metadataToIndexList = metadataReader.createMetaDictionaryForDirectory(directory)

    # Open a connection to elasticsearch
    es = Elasticsearch(hosts=[ELASTIC_HOST])
    # Make sure our connection is good
    if not es.ping():
        print("ElasticSearch connection failed! Is it not currently running on this system?")
        exit(1)
    else:
        print("ElasticSearch connection successful.")

    # Convert from our raw metadata to a list of bulk insert actions
    actions = list(map(lambda metadataToIndex: createInsertFor(metadataToIndex), metadataToIndexList))

    # Perform the insert, store the result
    (numSuccessful, errors) = helpers.bulk(es, actions, stats_only=False)
    # Compute number of errors
    numErrors = len(actions) - numSuccessful

    # Print the number of successful/failed insertions and total errors
    print("Number of attempted insertions: " + str(len(actions)))
    print("Number of successful insertions: " + str(numSuccessful))
    print("Number of failed insertions: " + str(numErrors))
    if numErrors is not 0:
        print(*errors, sep=", ")


def createInsertFor(metadataToIndex):
    return \
        {
            # The index to put the data into
            "_index": DRONE_INDEX_NAME,
            # The type to put the data into
            "_type": "_doc",
            # The operation type which is "index"
            "_op_type": "index",
            # The actual document to index
            "_source": metadataToIndex
        }


if __name__ == '__main__':
    main()