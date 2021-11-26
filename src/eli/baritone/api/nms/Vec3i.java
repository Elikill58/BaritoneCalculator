package eli.baritone.api.nms;

import com.google.common.base.MoreObjects;

import eli.baritone.api.utils.VecUtils;

public class Vec3i implements Comparable<Vec3i> {
	/** X coordinate */
	private final int x;
	/** Y coordinate */
	private final int y;
	/** Z coordinate */
	private final int z;

	public Vec3i(int xIn, int yIn, int zIn) {
		this.x = xIn;
		this.y = yIn;
		this.z = zIn;
	}

	public Vec3i(double xIn, double yIn, double zIn) {
		this(VecUtils.floor(xIn), VecUtils.floor(yIn), VecUtils.floor(zIn));
	}

	public boolean equals(Object p_equals_1_) {
		if (this == p_equals_1_) {
			return true;
		} else if (!(p_equals_1_ instanceof Vec3i)) {
			return false;
		} else {
			Vec3i vec3i = (Vec3i) p_equals_1_;

			if (this.getX() != vec3i.getX()) {
				return false;
			} else if (this.getY() != vec3i.getY()) {
				return false;
			} else {
				return this.getZ() == vec3i.getZ();
			}
		}
	}

	public int hashCode() {
		return (this.getY() + this.getZ() * 31) * 31 + this.getX();
	}

	public int compareTo(Vec3i p_compareTo_1_) {
		if (this.getY() == p_compareTo_1_.getY()) {
			return this.getZ() == p_compareTo_1_.getZ() ? this.getX() - p_compareTo_1_.getX()
					: this.getZ() - p_compareTo_1_.getZ();
		} else {
			return this.getY() - p_compareTo_1_.getY();
		}
	}

	/**
	 * Gets the X coordinate.
	 */
	public int getX() {
		return this.x;
	}

	/**
	 * Gets the Y coordinate.
	 */
	public int getY() {
		return this.y;
	}

	/**
	 * Gets the Z coordinate.
	 */
	public int getZ() {
		return this.z;
	}

	/**
	 * Calculate the cross product of this and the given Vector
	 */
	public Vec3i crossProduct(Vec3i vec) {
		return new Vec3i(this.getY() * vec.getZ() - this.getZ() * vec.getY(),
				this.getZ() * vec.getX() - this.getX() * vec.getZ(),
				this.getX() * vec.getY() - this.getY() * vec.getX());
	}

	public String toString() {
		return MoreObjects.toStringHelper(this).add("x", this.getX()).add("y", this.getY()).add("z", this.getZ())
				.toString();
	}
}
