import os

import exiftool


def createMetaDictForDirectory(directory=None, indexableFields=None):
    # Make sure we got valid paramters
    if indexableFields is None or directory is None:
        return []

    # Create the index
    filesToIndex = _getIndexableFiles(directory)
    # A list of indexed files in the form of a dictionary
    indexedFiles = _createIndexEntriesForFiles(filesToIndex, indexableFields)

    # Function that testes a file's metadata is valid. Each required field must be non-null in the dictionary
    def fileValid(indexedFile):
        for indexableField in indexableFields:
            if indexableField.isRequiredField():
                if indexedFile[indexableField.getIndexName()] is None:
                    return False
        return True

    # Only keep files that actually have non-null fields. Indexing documents with all null fields is useless
    validIndexedFiles = list(filter(lambda indexedFile: fileValid(indexedFile), indexedFiles))

    return validIndexedFiles


def _getIndexableFiles(currentDirectory, filesToIndex=None):
    # If we weren't given this argument, just use the empty list
    if filesToIndex is None:
        filesToIndex = []

    # Raw files names is a list of files in a directory that need to be indexed. They are without path
    rawFileNamesInDir = _listFiles(currentDirectory, includeDirectories=False, includeFiles=True)
    # Index all image files in the directory and add them to the current list of indexed files
    filesToIndex = filesToIndex + list(map(lambda rawFileName: os.path.join(currentDirectory, rawFileName), rawFileNamesInDir))
    # Iterate over all directories (not files) in the current directory
    for nextDirectory in _listFiles(currentDirectory, includeDirectories=True, includeFiles=False):
        # Compute the full path of the subdirectory
        nextDirectoryPath = os.path.join(currentDirectory, nextDirectory)
        # Get all indexable files in that subdirectory recursively
        filesToIndex = _getIndexableFiles(nextDirectoryPath, filesToIndex)
    return filesToIndex


def _listFiles(directory, includeDirectories=True, includeFiles=True):
    # Return all files in a directory including directories if specified and including files if specified
    try:
        # Go over all files in a directory
        return [file for file in os.listdir(directory) if
                # Make sure we have read access
                os.access(os.path.join(directory, file), os.R_OK)
                and
                # If we are supposed to include directories, include them, if not, dont include them
                ((includeDirectories and os.path.isdir(os.path.join(directory, file))
                  or
                # If we are supposed to include files, include them, if not, dont include them
                (includeFiles and os.path.isfile(os.path.join(directory, file)))))]
    # If we get a permission error we can't do anything, just return
    except PermissionError:
        return []


def _createIndexEntriesForFiles(files, indexableFields):
    # Ensure we dont have 0 files in a directory which crashes ExifTool
    if len(files) == 0:
        return []

    # Exif tool instance used to read image metadata
    exiftoolInstance = None
    # If we're on windows, use the included executable. If we're on a different OS, we have to assume exiftool is in the $PATH
    if os.name == "nt":
        exiftoolInstance = exiftool.ExifTool("./exiftool.exe")
    else:
        exiftoolInstance = exiftool.ExifTool()
    # Start the exif tool (used for bulk optimization)
    exiftoolInstance.start()
    # Pull a list of tags off of a list of files
    exifInfoList = exiftoolInstance.get_tags_batch(list(map(lambda indexableField: indexableField.getExifName(), indexableFields)), files)
    # Terminate the exif tool
    exiftoolInstance.terminate()

    # Remove any entries from the exif info list that we don't need by recreating it
    indexedInfoList = []
    # Iterate over our exif info
    for exifInfo in exifInfoList:
        # Create a new map of exif -> value with only key/value pairs we want
        indexedInfo = {}
        # Iterate over all index field names
        for indexableField in indexableFields:
            # Convert the field from metadata format to indexable format
            indexedInfo[indexableField.getIndexName()] = indexableField.getValueMapping()(exifInfo)
        # Store the indexable format
        indexedInfoList.append(indexedInfo)

    return indexedInfoList


class IndexableMetadataField:
    def __init__(self, exifName, indexName, requiredField=False, valueMapping=None):
        self.exifName = exifName
        self.indexName = indexName
        self.requiredField = requiredField
        self.valueMapping = valueMapping

        # If we don't have a value mapping, just use a default
        if self.valueMapping is None:
            self.valueMapping = lambda exifInfo: exifInfo.get(self.exifName, None)

    def getExifName(self):
        return self.exifName

    def getIndexName(self):
        return self.indexName

    def isRequiredField(self):
        return self.requiredField

    def getValueMapping(self):
        return self.valueMapping