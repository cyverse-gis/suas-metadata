import exiftool
import json
import os
import datetime

# Map of actual EXIF metadata tags to our index metadata tags
indexedFieldNames = \
{
    "EXIF:CreateDate": lambda exifInfo: ("createDate", exifInfo.get("EXIF:CreateDate", None)),
    "EXIF:DateTimeOriginal": lambda exifInfo: ("originalDate", exifInfo.get("EXIF:DateTimeOriginal", None)),
    "EXIF:GPSAltitude": lambda exifInfo: ("altitude", exifInfo.get("EXIF:GPSAltitude", None)),
    "EXIF:GPSLatitude": lambda exifInfo: ("dateUploaded", datetime.datetime.now().strftime("%Y:%m:%d %H:%M:%S")),
    "EXIF:GPSLongitude": lambda exifInfo: ("location", { "lat": exifInfo.get("EXIF:GPSLatitude"), "lon": exifInfo.get("EXIF:GPSLongitude") }
                                           if exifInfo.get("EXIF:GPSLatitude",  None) is not None and
                                              exifInfo.get("EXIF:GPSLongitude", None) is not None
                                           else None)
}

def createMetaDictionaryForDirectory(directory):
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

    return validIndexedFiles


def getIndexableFiles(currentDirectory, filesToIndex=None):
    # If we weren't given this argument, just use the empty list
    if filesToIndex is None:
        filesToIndex = []

    # Raw files names is a list of files in a directory that need to be indexed. They are without path
    rawFileNamesInDir = listFiles(currentDirectory, includeDirectories=False, includeFiles=True)
    # Index all image files in the directory and add them to the current list of indexed files
    filesToIndex = filesToIndex + list(
        map(lambda rawFileName: os.path.join(currentDirectory, rawFileName), rawFileNamesInDir))
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
        # Go over all files in a directory and all subdirectories
        return [file for file in os.listdir(directory) if
                # Make sure we have read access
                os.access(os.path.join(directory, file), os.R_OK)
                and
                # If we are supposed to include directories, include them, if not, dont include them
                ((includeDirectories and os.path.isdir(os.path.join(directory, file))
                  or
                  (includeFiles and os.path.isfile(os.path.join(directory, file)))))]
    # If we get a permission error we can't do anything, just return
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
    exifInfoList = exiftoolInstance.get_tags_batch(indexedFieldNames.keys(), files)
    # Terminate the exif tool
    exiftoolInstance.terminate()

    # Remove any entries from the exif info list that we don't need by recreating it
    indexedInfoList = []
    # Iterate over our exif info
    for exifInfo in exifInfoList:
        # Create a new map of exif -> value with only key/value pairs we want
        indexedInfo = {}
        # Iterate over all index field names
        for tag, converter in indexedFieldNames.items():
            # Convert the field from metadata format to indexable format
            (indexFieldName, indexFieldValue) = converter(exifInfo)
            indexedInfo[indexFieldName] = indexFieldValue
        # Store the indexable format
        indexedInfoList.append(indexedInfo)

    return indexedInfoList
