import json
import urllib
import enum


class TaxonTypeCode(enum.Enum):
    ALGAE = 1
    BEETLE = 2
    BIRD = 3
    FISH = 4
    HERPETOLOGY = 5
    MACROINVERTEBRATE = 6
    MOSQUITO = 7
    MOSQUITO_PATHOGENS = 8
    SMALL_MAMMAL = 9
    PLANT = 10
    TICK = 11


BASE_API_URL = "http://data.neonscience.org/api/v0"


def getProductCodes():
    url = urllib.urlopen(BASE_API_URL + "/products")
    jsonData = json.loads(url.read())
    if "data" not in jsonData:
        return []
    products = jsonData["data"]
    return set(map(lambda product: product["productCode"] if "productCode" in product else None, products))


def getProduct(productCode):
    url = urllib.urlopen(BASE_API_URL + "/products/" + productCode)
    jsonData = json.loads(url.read())
    return jsonData


def getSiteCodes():
    url = urllib.urlopen(BASE_API_URL + "/sites")
    jsonData = json.loads(url.read())
    if "data" not in jsonData:
        return []
    sites = jsonData["data"]
    return set(map(lambda site: site["siteCode"] if "siteCode" in site else None, sites))


def getSite(siteCode):
    url = urllib.urlopen(BASE_API_URL + "/sites/" + siteCode)
    jsonData = json.loads(url.read())
    return jsonData


def getLocationNames():
    url = urllib.urlopen(BASE_API_URL + "/locations/sites")
    jsonData = json.loads(url.read())
    if "data" not in jsonData:
        return []
    locations = jsonData["data"]
    return set(map(lambda location: location["locationName"] if "locationName" in location else None, locations))


def getLocation(locationName):
    url = urllib.urlopen(BASE_API_URL + "/locations/" + locationName)
    jsonData = json.loads(url.read())
    return jsonData


def getData(productCode, siteCode, yearMonth, fileName=None):
    url = urllib.urlopen(BASE_API_URL + "/data/" + productCode + "/" + siteCode + "/" + yearMonth + (
        "/" + fileName if fileName is not None else ""))
    jsonData = json.loads(url.read())
    return jsonData


def getTaxonomyWithTypeCode(taxonTypeCode, limit=None):
    url = urllib.urlopen(BASE_API_URL + "/taxonomy?taxonTypeCode=" + taxonTypeCode.name + (
        "&limit=" + str(limit) if limit is not None else ""))
    jsonData = json.loads(url.read())
    return jsonData


def getTaxonomy(kingdom=None, phylum=None, division=None, order=None, clazz=None, family=None, genus=None,
                scientificName=None, limit=None):
    arguments = []
    if kingdom is not None:
        arguments.append("kingdom=" + kingdom)
    if phylum is not None:
        arguments.append("phylum=" + phylum)
    if division is not None:
        arguments.append("division=" + division)
    if order is not None:
        arguments.append("order=" + order)
    if clazz is not None:
        arguments.append("class=" + clazz)
    if family is not None:
        arguments.append("family=" + family)
    if genus is not None:
        arguments.append("genus=" + genus)
    if scientificName is not None:
        arguments.append("scientificName=" + scientificName)
    if limit is not None:
        arguments.append("limit=" + str(limit))

    if len(arguments) == 0:
        return []
    urlToQuery = "/taxonomy?" + "&".join(arguments)
    url = urllib.urlopen(BASE_API_URL + urlToQuery)
    jsonData = json.loads(url.read())
    return jsonData


def prettyPrint(jsonData):
    return json.dumps(jsonData, indent=4)
