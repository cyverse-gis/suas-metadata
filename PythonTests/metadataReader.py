import exiftool
import json
import os
import datetime

# Map of actual EXIF metadata tags to our index metadata tags
indexedFieldNames = \
[
    "EXIF:CreateDate",
    "EXIF:DateTimeOriginal",
    "EXIF:GPSAltitude",
    "EXIF:GPSLatitude",
    "EXIF:GPSLongitude"
]

def main():
    # Read the directory to index from the command line, won't be used later
    dirToIndex = input("Enter the path to the directory to read image metadata from:\n")
    if not os.path.exists(dirToIndex):
        print("Path doesnt exist!")
        exit(1)
    if not os.path.isdir(dirToIndex):
        print("Path is not a valid directory!")
        exit(1)
    # The bulk insert to return from the program
    bulkInsert = createBulkInsertForDirectory(dirToIndex)
    # Write the jsons to a file
    with open("insert.txt", "w") as text_file:
        text_file.write(bulkInsert)

def createBulkInsertForDirectory(directory):
    # Create the index
    filesToIndex = getIndexableFiles(directory)
    # A list of indexed files in the form of a dictionary
    indexedFiles = createIndexEntriesForFiles(filesToIndex)

    # Function that testes a file to see if there's at least one non-null field
    def fileValid(indexedFile):
        for key, value in indexedFile.items():
            if (value is not None):
                return True
        return False

    # Only keep files that actually have non-null fields. Indexing documents with all null fields is useless
    validIndexedFiles = list(filter(lambda indexedFile: fileValid(indexedFile), indexedFiles))


    bulkInsertBody = ""
    for indexedFile in validIndexedFiles:
        header = \
        {
            "index":
            {
                "_index": "drone",
                "_type": "_doc"
            }
        }
        jsonForHeader = json.dumps(header)
        jsonForIndexedFile = json.dumps(indexedFile)
        bulkInsertBody = bulkInsertBody + jsonForHeader + "\n" + jsonForIndexedFile + "\n"

    # Return the results
    return bulkInsertBody

def getIndexableFiles(currentDirectory, filesToIndex = []):
    # Raw files names is a list of files in a directory that need to be indexed. They are without path
    rawFileNamesInDir = listFiles(currentDirectory, includeDirectories=False, includeFiles=True)
    # Index all image files in the directory and add them to the current list of indexed files
    filesToIndex = filesToIndex + list(map(lambda rawFileName: os.path.join(currentDirectory, rawFileName), rawFileNamesInDir))
    # Iterate over all directories (not files) in the current directory
    for nextDirectory in listFiles(currentDirectory, includeDirectories=True, includeFiles=False):
        # Compute the full path of the subdirectory
        nextDirectoryPath = os.path.join(currentDirectory, nextDirectory)
        # Make sure we can read the directory
        filesToIndex = getIndexableFiles(nextDirectoryPath, filesToIndex)
    return filesToIndex

def listFiles(directory, includeDirectories=True, includeFiles=True):
    # Return all files in a directory including sub directories if specified and including files if specified
    try:
        return [file for file in os.listdir(directory) if
                os.access(os.path.join(directory, file), os.R_OK)
                and
                    ((includeDirectories and os.path.isdir(os.path.join(directory, file))
                     or
                    (includeFiles        and os.path.isfile(os.path.join(directory, file)))))]
    except PermissionError:
        return []

def createIndexEntriesForFiles(files):
    # Ensure we dont have 0 files in a directory which crashes ExifTool
    if len(files) == 0:
        return []

    # Exif tool instance used to read image metadata
    exiftoolInstance = None
    if os.name == "nt":
        exiftoolInstance = exiftool.ExifTool("./exiftool.exe")
    else:
        exiftoolInstance = exiftool.ExifTool()
    # Start the exif tool (used for bulk optimization)
    exiftoolInstance.start()
    # Pull a list of tags off of a list of files
    exifInfoList = exiftoolInstance.get_tags_batch(indexedFieldNames, files)
    # Terminate the exif tool
    exiftoolInstance.terminate()

    # Remove any entries from the exif info list that we don't need by recreating it
    indexedInfoList = []
    # Iterate over our exif info
    for exifInfo in exifInfoList:
        # Create a new map of exif -> value with only key/value pairs we want
        indexedInfo = {}
        # Assume date is in Year:Month:Day Hour:Minute:Second
        indexedInfo["createDate"] = exifInfo.get("EXIF:CreateDate", None)
        # Assume date is in Year:Month:Day Hour:Minute:Second
        indexedInfo["originalDate"] = exifInfo.get("EXIF:DateTimeOriginal", None)
        # Assume altitude in meters?
        indexedInfo["altitude"] = exifInfo.get("EXIF:GPSAltitude", None)
        # The date uploaded is the current date
        indexedInfo["dateUploaded"] = datetime.datetime.now().strftime("%Y:%m:%d %H:%M:%S")
        lat = exifInfo.get("EXIF:GPSLatitude", None)
        lon = exifInfo.get("EXIF:GPSLongitude", None)
        if lat is not None and lon is not None:
            # Append location as an object
            indexedInfo["location"] = \
            {
                "lat": lat,
                "lon": lon
            }
        else:
            indexedInfo["location"] = None
        indexedInfoList.append(indexedInfo)

    return indexedInfoList

main()