import exiftool
import json
import os

# Exif tool instance used to read image metadata
exiftoolInstance = exiftool.ExifTool()

# Map of actual EXIF metadata tags to our index metadata tags
indexedFieldNames = \
{
    "EXIF:CreateDate": "createDate",
    "EXIF:DateTimeOriginal": "originalDate",
    "EXIF:GPSAltitude": "altitude",
    "EXIF:GPSLatitude": "latitude",
    "EXIF:GPSLongitude": "longitude"
}

def main():
    # Read the directory to index from the command line, won't be used later
    dirToIndex = input("Enter the path to the directory to read image metadata from:\n")
    if not os.path.exists(dirToIndex):
        print("Path doesnt exist!")
        exit(1)
    if not os.path.isdir(dirToIndex):
        print("Path is not a valid directory!")
        exit(1)
    # The JSON to return from the program
    jsonOut = createJSONIndexForDirectory(dirToIndex)
    # Write the json to a file
    with open("index.json", "w") as text_file:
        text_file.write(jsonOut)

def createJSONIndexForDirectory(directory):
    # Start the exif tool (used for bulk optimization)
    exiftoolInstance.start()
    # Create the index
    indexedFiles = recursivelyCreateIndexEntries(directory)
    # Terminate the exif tool
    exiftoolInstance.terminate()
    # Return the results
    return json.dumps(indexedFiles)


def recursivelyCreateIndexEntries(currentDirectory, indexedFiles = []):
    # Index all image files in the directory and add them to the current list of indexed files
    indexedFiles = indexedFiles + createIndexEntriesForDir(currentDirectory)
    # Iterate over all directories (not files) in the current directory
    for nextDirectory in listFiles(currentDirectory, includeDirectories=True, includeFiles=False):
        # Compute the full path of the subdirectory
        nextDirectoryPath = os.path.join(currentDirectory, nextDirectory)
        # Make sure we can read the directory
        indexedFiles = recursivelyCreateIndexEntries(nextDirectoryPath, indexedFiles)
    return indexedFiles

def createIndexEntriesForDir(directory):
    # Raw files names is a list of files in a directory that need to be indexed. They are without path
    rawFileNames = listFiles(directory, includeDirectories=False, includeFiles=True)
    # Convert this list of raw files to a list of files with path appended. Also ensure all files are readable
    filesToIndex = list(map(lambda rawFileName: os.path.join(directory, rawFileName), rawFileNames))
    # A list of indexed files in the form of a dictionary
    indexedFiles = createIndexEntryForFiles(filesToIndex)

    # Function that testes a file to see if there's at least one non-null field
    def fileValid(indexedFile):
        for key, value in indexedFile.items():
            if (value is not None):
                return True
        return False

    # Only keep files that actually have non-null fields. Indexing documents with all null fields is useless
    validIndexedFiles = list(filter(lambda indexedFile: fileValid(indexedFile), indexedFiles))
    return validIndexedFiles

def createIndexEntryForFiles(files):
    # Ensure we dont have 0 files in a directory which crashes ExifTool
    if len(files) == 0:
        return []

    # Pull a list of tags off of a list of files
    exifInfoList = exiftoolInstance.get_tags_batch(indexedFieldNames, files)

    # Remove any entries from the exif info list that we don't need by recreating it
    indexedInfoList = []
    # Iterate over our exif info
    for exifInfo in exifInfoList:
        # Create a new map of exif -> value with only key/value pairs we want
        indexedInfo = {}
        # Iterate over all keys we want to store
        for metadataFieldName, indexFieldName in indexedFieldNames.items():
            # Grab the keys from the exif info or store none if it's not present
            indexedInfo[indexFieldName] = exifInfo.get(metadataFieldName, None)
        indexedInfoList.append(indexedInfo)

    return indexedInfoList

def listFiles(directory, includeDirectories=True, includeFiles=True):
    # Return all files in a directory including sub directories if specified and including files if specified
    try:
        return [file for file in os.listdir(directory) if
                os.access(os.path.join(directory, file), os.R_OK)
                and
                    ((includeDirectories and os.path.isdir(os.path.join(directory, file))
                     or
                    (includeFiles       and os.path.isfile(os.path.join(directory, file)))))]
    except PermissionError:
        return []


main()