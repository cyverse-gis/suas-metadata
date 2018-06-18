import os
import PIL.ExifTags
import PIL.Image
import exifread
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

    exifInfoList = []

    # Exif tool instance used to read image metadata
    #exiftoolInstance = None
    # If we're on windows, use the included executable. If we're on a different OS, we have to assume exiftool is in the $PATH
    #if os.name == "nt":
    #    exiftoolInstance = exiftool.ExifTool("./exiftool.exe")
    #else:
    #    exiftoolInstance = exiftool.ExifTool("exiftool")
    # Start the exif tool (used for bulk optimization)
    #exiftoolInstance.start()
    # Pull a list of tags off of a list of files
    #exifInfoList = exiftoolInstance.get_metadata_batch(files)
    # Terminate the exif tool
    #exiftoolInstance.terminate()

    #for file in files:
    #    try:
    #        with PIL.Image.open(file) as image:
    #            exifInfoList.append(get_exif_data(image))
    #    except OSError:
    #        pass

    #for file in files:
    #    with open(file, 'rb') as image:
    #        exifInfoList.append(exifread.process_file(image))

    #for key, value in exifInfoList[0].items():
    #    print(key + ": " + value.__str__())

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


def get_exif_data(image):
    """Returns a dictionary from the exif data of an PIL Image item. Also converts the GPS Tags"""
    exif_data = {}
    info = image._getexif()
    if info:
        for tag, value in info.items():
            decoded = PIL.ExifTags.TAGS.get(tag, tag)
            if decoded == "GPSInfo":
                gps_data = {}
                for t in value:
                    sub_decoded = PIL.ExifTags.GPSTAGS.get(t, t)
                    gps_data[sub_decoded] = value[t]

                exif_data[decoded] = gps_data
            else:
                exif_data[decoded] = value

    return exif_data


def _get_if_exist(data, key):
    if key in data:
        return data[key]

    return None


def _convert_to_degress(value):
    """Helper function to convert the GPS coordinates stored in the EXIF to degress in float format"""
    d0 = value[0][0]
    d1 = value[0][1]
    d = float(d0) / float(d1)

    m0 = value[1][0]
    m1 = value[1][1]
    m = float(m0) / float(m1)

    s0 = value[2][0]
    s1 = value[2][1]
    s = float(s0) / float(s1)

    return d + (m / 60.0) + (s / 3600.0)


def get_lat_lon(exif_data):
    """Returns the latitude and longitude, if available, from the provided exif_data (obtained through get_exif_data above)"""
    lat = None
    lon = None

    if "GPSInfo" in exif_data:
        gps_info = exif_data["GPSInfo"]

        gps_latitude = _get_if_exist(gps_info, "GPSLatitude")
        gps_latitude_ref = _get_if_exist(gps_info, 'GPSLatitudeRef')
        gps_longitude = _get_if_exist(gps_info, 'GPSLongitude')
        gps_longitude_ref = _get_if_exist(gps_info, 'GPSLongitudeRef')

        if gps_latitude and gps_latitude_ref and gps_longitude and gps_longitude_ref:
            lat = _convert_to_degress(gps_latitude)
            if gps_latitude_ref != "N":
                lat = 0 - lat

            lon = _convert_to_degress(gps_longitude)
            if gps_longitude_ref != "E":
                lon = 0 - lon

    return lat, lon

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

