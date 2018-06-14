import javafx.util.Pair;
import org.elasticsearch.common.collect.Tuple;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MetadataConverter
{
	private final Map<String, MetadataField> converters = Arrays.stream(MetadataField.values()).collect(Collectors.toMap(MetadataField::getExifKeyName, field -> field));

	public boolean canConvert(Map.Entry<String, String> metadataEntry)
	{
		return converters.containsKey(metadataEntry.getKey());
	}

	public Tuple<String, Object> convert(Map<String, String> rawMetadata, Map.Entry<String, String> metadataEntryToConvert)
	{
		MetadataField converter = converters.get(metadataEntryToConvert.getKey());
		return Tuple.tuple(converter.getIndexKeyName(), converter.getValueConverter().apply(rawMetadata, metadataEntryToConvert.getValue()));
	}

	private static Double parseLatLong(String latLong)
	{
		String[] degMinSec = latLong.split("[°'\"]");
		if (degMinSec.length == 3)
		{
			try
			{
				Double degrees = Double.parseDouble(degMinSec[0]);
				Double minutes = Double.parseDouble(degMinSec[1]);
				Double seconds = Double.parseDouble(degMinSec[2]);
				if (degrees < 0)
					return degrees - minutes / 60.0 - seconds / 3600.0;
				else
					return degrees + minutes / 60.0 + seconds / 3600.0;
			}
			catch (NumberFormatException ignored) {}
		}
		return null;
	}

	private enum MetadataField
	{
		GPSAltitude("[GPS] GPS Altitude", "altitude", (rawMetadata, value) -> {
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
		GPSLatitude("[GPS] GPS Latitude", "location", (rawMetadata, value) -> {
			if (rawMetadata.containsKey("[GPS] GPS Latitude") && rawMetadata.containsKey("[GPS] GPS Longitude"))
			{
				Double lat = parseLatLong(value);
				Double lon = parseLatLong(rawMetadata.get("[GPS] GPS Longitude"));
				if (lat != null && lon != null)
					return lat.toString() + "," + lon.toString();
			}
			return null;
		}),
		GPSLongitude("[GPS] GPS Longitude", "location", (rawMetadata, value) -> {
			if (rawMetadata.containsKey("[GPS] GPS Latitude") && rawMetadata.containsKey("[GPS] GPS Longitude"))
			{
				Double lat = parseLatLong(rawMetadata.get("[GPS] GPS Latitude"));
				Double lon = parseLatLong(value);
				if (lat != null && lon != null)
					return lat.toString() + "," + lon.toString();
			}
			return null;
		}),
		CreateDate("[Exif IFD0] Date/Time", "createDate", (rawMetadata, value) -> LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")).format(DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")));

		private String exifKeyName;
		private String indexKeyName;
		private BiFunction<Map<String, String>, String, Object> valueConverter;

		MetadataField(String exifKeyName, String indexKeyName, BiFunction<Map<String, String>, String, Object> valueConverter)
		{
			this.exifKeyName = exifKeyName;
			this.indexKeyName = indexKeyName;
			this.valueConverter = valueConverter;
		}

		public String getExifKeyName()
		{
			return this.exifKeyName;
		}

		public String getIndexKeyName()
		{
			return this.indexKeyName;
		}

		public BiFunction<Map<String, String>, String, Object> getValueConverter()
		{
			return this.valueConverter;
		}
	}
}
