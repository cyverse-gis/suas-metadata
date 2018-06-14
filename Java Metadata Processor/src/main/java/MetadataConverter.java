import org.elasticsearch.common.collect.Tuple;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class used to convert metadata from file exif to usable indexable metadata
 */
public class MetadataConverter
{
	// A map of exif tag name -> metadata field used to convert each exif tag
	private final Map<String, MetadataField> converters = Arrays.stream(MetadataField.values()).collect(Collectors.toMap(MetadataField::getExifKeyName, field -> field));

	/**
	 * Given raw metadata this function converts each key and value into one ready to be indexed into elasticsearch
	 *
	 * @param rawMetadata The raw metadata to index
	 * @return A map of key->value pairs ready to be indexed
	 */
	public Map<String, Object> convertRawToIndexable(Map<String, String> rawMetadata)
	{
		// A new map of cleaned up metadata
		Map<String, Object> metadata = new HashMap<>();

		// Convert each metadata entry
		for (Map.Entry<String, String> metadataEntry : rawMetadata.entrySet())
		{
			// If we can convert the metadata entry, do so. If not, throw it away
			if (this.canConvert(metadataEntry))
			{
				// Convert the tuple, and save it into the new metadata map
				Tuple<String, Object> converted = this.convert(metadataEntry);
				metadata.put(converted.v1(), converted.v2());
			}
		}

		// Post processing

		// Add an upload date field which isn't present in the default metadata
		metadata.put("uploadDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")));

		// Fix the lat/long formatting

		// If we have GPSLatitude and GPSLongitude we need to convert it to a single "location" field
		if (metadata.containsKey(MetadataField.GPSLatitude.getIndexKeyName()) && metadata.containsKey(MetadataField.GPSLongitude.getIndexKeyName()))
		{
			// Grab the lat and long
			Object latitude = metadata.get(MetadataField.GPSLatitude.getIndexKeyName());
			Object longitude = metadata.get(MetadataField.GPSLongitude.getIndexKeyName());
			// Remove the lat and long
			metadata.remove(MetadataField.GPSLatitude.getIndexKeyName());
			metadata.remove(MetadataField.GPSLongitude.getIndexKeyName());
			// If both lat and long are non-null, put the entry 'location'->'lat,lon'
			if (latitude != null && longitude != null)
				metadata.put("location", latitude.toString() + "," + longitude.toString());
		}
		else
		{
			// If either field exists in the metadata without the other just remove both
			metadata.remove(MetadataField.GPSLatitude.getIndexKeyName());
			metadata.remove(MetadataField.GPSLongitude.getIndexKeyName());
		}

		return metadata;
	}

	/**
	 * Tests to see if a metadata entry can be processed
	 *
	 * @param metadataEntry The metadata entry to test
	 * @return True if the metadata can be converted, false otherwise
	 */
	private boolean canConvert(Map.Entry<String, String> metadataEntry)
	{
		return converters.containsKey(metadataEntry.getKey());
	}

	/**
	 * Converts a raw metadata entry into an indexable entry
	 *
	 * @param metadataEntryToConvert The entry that needs converting, must first be tested with 'canConvert()'
	 * @return A tuple where the first entry is the key and the second entry is the cleaned up value
	 */
	private Tuple<String, Object> convert(Map.Entry<String, String> metadataEntryToConvert)
	{
		// Grab the metadata field for that specific exif tag
		MetadataField converter = converters.get(metadataEntryToConvert.getKey());
		// Return a tuple with the new index key name and apply the value converter to get a new value
		return Tuple.tuple(converter.getIndexKeyName(), converter.getValueConverter().apply(metadataEntryToConvert.getValue()));
	}

	/**
	 * Given a lat or long in the format XX° YY' ZZ.ZZ" this function converts it to decimal, eg XX.XXXXX\
	 *
	 * @param latLong The lat long to parse
	 * @return A double representing the same lat long
	 */
	private static Double parseLatLong(String latLong)
	{
		// Split the lat long string by the 3 characters
		String[] degMinSec = latLong.split("[°'\"]");
		// This should yield 3 pieces, if not we return null
		if (degMinSec.length == 3)
		{
			// Try and parse each piece
			try
			{
				// Grab degrees, minutes, and seconds
				Double degrees = Double.parseDouble(degMinSec[0]);
				Double minutes = Double.parseDouble(degMinSec[1]);
				Double seconds = Double.parseDouble(degMinSec[2]);
				// If degress is less than 0, we need to subtract off minutse and seconds. Otherwise add
				if (degrees < 0)
					return degrees - minutes / 60.0 - seconds / 3600.0;
				else
					return degrees + minutes / 60.0 + seconds / 3600.0;
			}
			catch (NumberFormatException ignored) {}
		}
		return null;
	}

	/**
	 * Helper enum containing a list of all parsable fields
	 */
	private enum MetadataField
	{
		GPSAltitude("[GPS] GPS Altitude", "altitude", value -> {
			String[] numAndUnit = value.split(" ");
			if (numAndUnit.length == 2)
			{
				try
				{
					return Float.parseFloat(numAndUnit[0]);
				}
				catch (NumberFormatException ignored) {}
			}
			return null;
		}),
		GPSLatitude("[GPS] GPS Latitude", "location", MetadataConverter::parseLatLong),
		GPSLongitude("[GPS] GPS Longitude", "location", MetadataConverter::parseLatLong),
		CreateDate("[Exif IFD0] Date/Time", "createDate", value -> LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")).format(DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")));

		// The exif field name
		private String exifKeyName;
		// The index field name
		private String indexKeyName;
		// A converter used to convert from exif value to indexable value
		private Function<String, Object> valueConverter;

		/**
		 * Constructor just assigns fields
		 *
		 * @param exifKeyName The exif field's name
		 * @param indexKeyName The field's name once indexed
		 * @param valueConverter The converter to convert a string value into a usable format
		 */
		MetadataField(String exifKeyName, String indexKeyName, Function<String, Object> valueConverter)
		{
			this.exifKeyName = exifKeyName;
			this.indexKeyName = indexKeyName;
			this.valueConverter = valueConverter;
		}

		/**
		 * Getter for exif key name
		 *
		 * @return The name of the exif field's name
		 */
		public String getExifKeyName()
		{
			return this.exifKeyName;
		}

		/**
		 * Getter for index key name
		 *
		 * @return The name of the index field's name
		 */
		public String getIndexKeyName()
		{
			return this.indexKeyName;
		}

		/**
		 * Getter for value converter
		 *
		 * @return A function to convert string values into their object equivelances
		 */
		public Function<String, Object> getValueConverter()
		{
			return this.valueConverter;
		}
	}
}
