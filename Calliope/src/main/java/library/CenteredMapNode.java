package library;

import fxmapcontrol.MapNode;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

/**
 * Utility class used to center a map node on the map on its given latitude and longitude coordinate
 */
public class CenteredMapNode extends MapNode
{
	/**
	 * Constructor does nothing
	 */
	public CenteredMapNode()
	{
		super();
	}

	/**
	 * This function is called whenever the viewport is changed
	 *
	 * @param viewportPosition The new viewport position
	 */
	@Override
	protected void viewportPositionChanged(Point2D viewportPosition)
	{
		// The body of this function is mostly copied except for the content of this if statement
		if (viewportPosition != null)
		{
			// Here we grab the width and height of our component as a bounds object
			Bounds boundsInParent = this.getBoundsInParent();
			// Instead of just translating by X and Y, we translate by x - width/2 and y - width/2 which ensures our marker is centered
			setTranslateX(viewportPosition.getX() - boundsInParent.getWidth() / 2);
			setTranslateY(viewportPosition.getY() - boundsInParent.getHeight() / 2);
		}
		else
		{
			// Same as the base function
			setTranslateX(0d);
			setTranslateY(0d);
		}
	}
}
