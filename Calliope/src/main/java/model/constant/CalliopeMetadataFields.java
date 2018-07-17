package model.constant;

import java.time.format.DateTimeFormatter;

/**
 * A class containing any metadata fields to be used by the ElasticSearch index
 */
public class CalliopeMetadataFields
{
	public static final DateTimeFormatter INDEX_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
}
