package baritone.api.nms;

import baritone.api.nms.block.BlockPos;

public class Vec3d {
	
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

	public Vec3d(BlockPos orig) {
		this(orig.getX(), orig.getY(), orig.getZ());
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
}
