package model.transitions;

import javafx.animation.Transition;
import javafx.scene.layout.Region;
import javafx.util.Duration;

public class HeightTransition extends Transition
{
	private Region region;
	private double startHeight;
	private double heightDiff;

	public HeightTransition(Duration duration, Region region, double endHeight)
	{
		this.setCycleDuration(duration);
		this.region = region;
		this.startHeight = this.region.getHeight();
		this.heightDiff = endHeight - this.startHeight;
	}

	@Override
	protected void interpolate(double fraction)
	{
		this.region.setMaxHeight(this.startHeight + (this.heightDiff * fraction));
	}
}

