import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.collect.Tuple;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MetadataIndexer
{
	private MetadataConverter metadataConverter = new MetadataConverter();

	public void index(Map<String, String> rawMetadata)
	{
		Map<String, Object> cleanedMetadata = this.convertRawToIndexable(rawMetadata);
		cleanedMetadata.put("uploadDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")));

		try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("128.196.38.73", 9200, "http"))))
		{
			IndexRequest request = new IndexRequest()
					.index("drone")
					.type("_doc")
					.source(cleanedMetadata);

			IndexResponse response = client.index(request);

			System.out.println("Index response: " + response.status());
		}
		catch (IOException e)
		{
			System.err.println("Error connecting to the elasticsearch client!");
			e.printStackTrace();
		}
	}

	private Map<String, Object> convertRawToIndexable(Map<String, String> rawMetadata)
	{
		Map<String, Object> metadata = new HashMap<>();

		for (Map.Entry<String, String> metadataEntry : rawMetadata.entrySet())
		{
			if (metadataConverter.canConvert(metadataEntry))
			{
				Tuple<String, Object> converted = metadataConverter.convert(rawMetadata, metadataEntry);
				metadata.put(converted.v1(), converted.v2());
			}
		}

		return metadata;
	}
}
