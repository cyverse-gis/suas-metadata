package model.transitions;

import javafx.animation.Transition;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * Utility transition used to change a node's height over time
 */
public class HeightTransition extends Transition
{
	// The region to change the height of
	private Region region;
	// The initial height of the region
	private double startHeight;
	// The amount the region will change by
	private double heightDiff;

	/**
	 * Constructor initializes basic fields and computes the height differential
	 *
	 * @param duration The duration the transition should take
	 * @param region The region that should have its height updated
	 * @param endHeight The final height the region should have
	 */
	public HeightTransition(Duration duration, Region region, double startHeight, double endHeight)
	{
		// Pass down the duration
		this.setCycleDuration(duration);
		// Set the region
		this.region = region;
		// Set the start height
		this.startHeight = startHeight;
		// The difference in height will be the difference between the start and end height
		this.heightDiff = endHeight - this.startHeight;
	}

	/**
	 * Gets called every frame to update our transition. Here we set the max height property
	 *
	 * @param fraction How far through the animation we are
	 */
	@Override
	protected void interpolate(double fraction)
	{
		// Set the max height equal to the start height with the percent of height difference added in
		this.region.setMaxHeight(this.startHeight + (this.heightDiff * fraction));
	}
}

