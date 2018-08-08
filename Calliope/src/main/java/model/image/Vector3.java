package model.image;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * 3D vector class used to store X, Y, Z triples
 */
public class Vector3
{
	// The X,Y,Z fields
	private DoubleProperty x = new SimpleDoubleProperty();
	private DoubleProperty y = new SimpleDoubleProperty();
	private DoubleProperty z = new SimpleDoubleProperty();

	/**
	 * Default constructor sets all fields to 0
	 */
	public Vector3()
	{
		this.x.setValue(0);
		this.y.setValue(0);
		this.z.setValue(0);
	}

	/**
	 * Overloaded constructor sets all fields using parameters
	 *
	 * @param x The X component of the vector
	 * @param y The Y component of the vector
	 * @param z The Z component of the vector
	 */
	public Vector3(Double x, Double y, Double z)
	{
		this.x.setValue(x);
		this.y.setValue(y);
		this.z.setValue(z);
	}

	/**
	 * @return Returns the vector in the format [x, y, z]
	 */
	@Override
	public String toString()
	{
		return "[" + this.getX() + ", " + this.getY() + ", " + this.getZ() + "]";
	}

	///
	/// Setters/Getters
	///

	public void setX(double x)
	{
		this.x.set(x);
	}

	public double getX()
	{
		return x.get();
	}

	public DoubleProperty xProperty()
	{
		return x;
	}

	public void setY(double y)
	{
		this.y.set(y);
	}

	public double getY()
	{
		return y.get();
	}

	public DoubleProperty yProperty()
	{
		return y;
	}

	public void setZ(double z)
	{
		this.z.set(z);
	}

	public double getZ()
	{
		return z.get();
	}

	public DoubleProperty zProperty()
	{
		return z;
	}
}
