package model.elasticsearch.query;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.function.BiFunction;

/**
 * Enum of numeric comparison operators
 */
public enum NumericComparisonOperator
{
	Equal("Equal To", QueryBuilders::termQuery),
	GreaterThan("Greater Than", (fieldName, numericValue) -> QueryBuilders.rangeQuery(fieldName).gt(numericValue)),
	GreaterThanOrEqual("Greater Than or Equal To", (fieldName, numericValue) -> QueryBuilders.rangeQuery(fieldName).gte(numericValue)),
	LessThan("Less Than", (fieldName, numericValue) -> QueryBuilders.rangeQuery(fieldName).lt(numericValue)),
	LessThanOrEqual("Less Than or Equal To", (fieldName, numericValue) -> QueryBuilders.rangeQuery(fieldName).lte(numericValue));

	private String displayName;
	private BiFunction<String, Double, QueryBuilder> conditionCreator;

	/**
	 * Constructor takes the name to display and an operator that is the query condition operator equivelant
	 *
	 * @param displayName The name to visually display
	 * @param conditionCreator The mapping of (field name & number) -> query condition
	 */
	NumericComparisonOperator(String displayName, BiFunction<String, Double, QueryBuilder> conditionCreator)
	{
		this.displayName = displayName;
		this.conditionCreator = conditionCreator;
	}

	/**
	 * Create the condition based on operator
	 *
	 * @param fieldName The name of the field to apply to
	 * @return The query builder representing the operator
	 */
	public QueryBuilder createCondition(String fieldName, Double numericValue)
	{
		return conditionCreator.apply(fieldName, numericValue);
	}

	/**
	 * Returns the display name as the altitude condition toString
	 *
	 * @return The display name
	 */
	@Override
	public String toString()
	{
		return this.displayName;
	}
}
