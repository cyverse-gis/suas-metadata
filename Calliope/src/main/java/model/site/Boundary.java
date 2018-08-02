package model.site;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.elasticsearch.common.geo.GeoPoint;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A class representing a site boundary which has an outer perimeter with inner holes
 */
public class Boundary
{
	// The list of points making up the outer boundary
	private ObservableList<GeoPoint> outerBoundary = FXCollections.observableArrayList(boundary -> new Observable[] {});
	// The list of holes in the boundary where a hole is a list of points
	private ObservableList<ObservableList<GeoPoint>> innerBoundaries = FXCollections.observableArrayList(boundaries -> new Observable[] { boundaries });

	/**
	 * Constructor takes an outer boundary and an inner boundary and makes it into an observable list
	 *
	 * @param outerBoundary A list of points that make up the outer boundary
	 * @param innerBoundaries A list of holes in the boundary where a hole is a list of points
	 */
	public Boundary(List<GeoPoint> outerBoundary, List<List<GeoPoint>> innerBoundaries)
	{
		this.outerBoundary.addAll(outerBoundary);
		this.innerBoundaries.addAll(innerBoundaries.stream().map(FXCollections::observableArrayList).collect(Collectors.toList()));
	}

	/**
	 * @return Getter for the outer boundary
	 */
	public ObservableList<GeoPoint> getOuterBoundary()
	{
		return this.outerBoundary;
	}

	/**
	 * @return Getter for inner boundaries
	 */
	public ObservableList<ObservableList<GeoPoint>> getInnerBoundaries()
	{
		return this.innerBoundaries;
	}
}
