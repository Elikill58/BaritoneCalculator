package baritone.api.nms;

import org.bukkit.Location;

import baritone.api.nms.block.BlockPos;
import baritone.api.utils.VecUtils;

public class Vec3d implements Comparable<Vec3d> {
	
	/** X coordinate */
	public int x;
	/** Y coordinate */
	public int y;
	/** Z coordinate */
	public int z;

	public Vec3d(int xIn, int yIn, int zIn) {
		this.x = xIn;
		this.y = yIn;
		this.z = zIn;
	}

	public Vec3d(double xIn, double yIn, double zIn) {
		this(VecUtils.floor(xIn), VecUtils.floor(yIn), VecUtils.floor(zIn));
	}

	public Vec3d(BlockPos orig) {
		this(orig.getX(), orig.getY(), orig.getZ());
	}

	public Vec3d(Location orig) {
		this(orig.getX(), orig.getY(), orig.getZ());
	}

	public Vec3d add(double x, double y, double z) {
		this.x += x;
		this.y += y;
		this.z += z;

		return this;
	}

	public boolean equals(Object p_equals_1_) {
		if (this == p_equals_1_) {
			return true;
		} else if (!(p_equals_1_ instanceof Vec3d)) {
			return false;
		} else {
			Vec3d vec3i = (Vec3d) p_equals_1_;

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

	public int compareTo(Vec3d p_compareTo_1_) {
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

	public double distance(int xIn, int yIn, int zIn) {
		double d0 = (double) (this.getX() - xIn);
		double d1 = (double) (this.getY() - yIn);
		double d2 = (double) (this.getZ() - zIn);
		return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
	}
}
