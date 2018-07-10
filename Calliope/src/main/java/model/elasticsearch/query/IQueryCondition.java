package model.elasticsearch.query;

public interface IQueryCondition
{
	void appendConditionToQuery(ElasticSearchQuery query);

	String getFXMLConditionEditor();
}
