package eli.baritone.api.nms;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

public enum EnumFacing {
	DOWN(0, 1, -1, "down", EnumFacing.AxisDirection.NEGATIVE, EnumFacing.Axis.Y, new Vec3i(0, -1, 0)),
	UP(1, 0, -1, "up", EnumFacing.AxisDirection.POSITIVE, EnumFacing.Axis.Y, new Vec3i(0, 1, 0)),
	NORTH(2, 3, 2, "north", EnumFacing.AxisDirection.NEGATIVE, EnumFacing.Axis.Z, new Vec3i(0, 0, -1)),
	SOUTH(3, 2, 0, "south", EnumFacing.AxisDirection.POSITIVE, EnumFacing.Axis.Z, new Vec3i(0, 0, 1)),
	WEST(4, 5, 1, "west", EnumFacing.AxisDirection.NEGATIVE, EnumFacing.Axis.X, new Vec3i(-1, 0, 0)),
	EAST(5, 4, 3, "east", EnumFacing.AxisDirection.POSITIVE, EnumFacing.Axis.X, new Vec3i(1, 0, 0));

	/** Ordering index for D-U-N-S-W-E */
	private final int index;
	/** Index of the opposite Facing in the VALUES array */
	private final int opposite;
	/** Ordering index for the HORIZONTALS field (S-W-N-E) */
	private final int horizontalIndex;
	private final String name;
	private final EnumFacing.Axis axis;
	private final EnumFacing.AxisDirection axisDirection;
	/** Normalized Vector that points in the direction of this Facing */
	private final Vec3i directionVec;
	/** All facings in D-U-N-S-W-E order */
	private static final EnumFacing[] VALUES = new EnumFacing[6];
	/** All Facings with horizontal axis in order S-W-N-E */
	private static final EnumFacing[] HORIZONTALS = new EnumFacing[4];
	private static final Map<String, EnumFacing> NAME_LOOKUP = Maps.<String, EnumFacing>newHashMap();

	private EnumFacing(int indexIn, int oppositeIn, int horizontalIndexIn, String nameIn,
			EnumFacing.AxisDirection axisDirectionIn, EnumFacing.Axis axisIn, Vec3i directionVecIn) {
		this.index = indexIn;
		this.horizontalIndex = horizontalIndexIn;
		this.opposite = oppositeIn;
		this.name = nameIn;
		this.axis = axisIn;
		this.axisDirection = axisDirectionIn;
		this.directionVec = directionVecIn;
	}

	/**
	 * Get the Index of this Facing (0-5). The order is D-U-N-S-W-E
	 */
	public int getIndex() {
		return this.index;
	}

	/**
	 * Get the index of this horizontal facing (0-3). The order is S-W-N-E
	 */
	public int getHorizontalIndex() {
		return this.horizontalIndex;
	}

	/**
	 * Get the AxisDirection of this Facing.
	 */
	public EnumFacing.AxisDirection getAxisDirection() {
		return this.axisDirection;
	}

	/**
	 * Get the opposite Facing (e.g. DOWN => UP)
	 */
	public EnumFacing getOpposite() {
		return byIndex(this.opposite);
	}

	/**
	 * Rotate this Facing around the Y axis counter-clockwise (NORTH => WEST =>
	 * SOUTH => EAST => NORTH)
	 */
	public EnumFacing rotateYCCW() {
		switch (this) {
		case NORTH:
			return WEST;
		case EAST:
			return NORTH;
		case SOUTH:
			return EAST;
		case WEST:
			return SOUTH;
		default:
			throw new IllegalStateException("Unable to get CCW facing of " + this);
		}
	}

	/**
	 * Gets the offset in the x direction to the block in front of this facing.
	 */
	public int getXOffset() {
		return this.axis == EnumFacing.Axis.X ? this.axisDirection.getOffset() : 0;
	}

	/**
	 * Gets the offset in the y direction to the block in front of this facing.
	 */
	public int getYOffset() {
		return this.axis == EnumFacing.Axis.Y ? this.axisDirection.getOffset() : 0;
	}

	/**
	 * Gets the offset in the z direction to the block in front of this facing.
	 */
	public int getZOffset() {
		return this.axis == EnumFacing.Axis.Z ? this.axisDirection.getOffset() : 0;
	}

	/**
	 * Same as getName, but does not override the method from Enum.
	 */
	public String getName2() {
		return this.name;
	}

	public EnumFacing.Axis getAxis() {
		return this.axis;
	}

	/**
	 * Get the facing specified by the given name
	 */
	@Nullable
	public static EnumFacing byName(String name) {
		return name == null ? null : (EnumFacing) NAME_LOOKUP.get(name.toLowerCase(Locale.ROOT));
	}

	/**
	 * Gets the EnumFacing corresponding to the given index (0-5). Out of bounds
	 * values are wrapped around. The order is D-U-N-S-W-E.
	 */
	public static EnumFacing byIndex(int index) {
		return VALUES[Math.abs(index % VALUES.length)];
	}

	/**
	 * Gets the EnumFacing corresponding to the given horizontal index (0-3). Out of
	 * bounds values are wrapped around. The order is S-W-N-E.
	 */
	public static EnumFacing byHorizontalIndex(int horizontalIndexIn) {
		return HORIZONTALS[Math.abs(horizontalIndexIn % HORIZONTALS.length)];
	}

	/**
	 * Gets the angle in degrees corresponding to this EnumFacing.
	 */
	public float getHorizontalAngle() {
		return (float) ((this.horizontalIndex & 3) * 90);
	}

	public String toString() {
		return this.name;
	}

	public String getName() {
		return this.name;
	}
	
	/**
	 * Get a normalized Vector that points in the direction of this Facing.
	 */
	public Vec3i getDirectionVec() {
		return this.directionVec;
	}

	static {
		for (EnumFacing enumfacing : values()) {
			VALUES[enumfacing.index] = enumfacing;

			if (enumfacing.getAxis().isHorizontal()) {
				HORIZONTALS[enumfacing.horizontalIndex] = enumfacing;
			}

			NAME_LOOKUP.put(enumfacing.getName2().toLowerCase(Locale.ROOT), enumfacing);
		}
	}

	public static enum Axis implements Predicate<EnumFacing> {
		X("x", EnumFacing.Plane.HORIZONTAL), Y("y", EnumFacing.Plane.VERTICAL), Z("z", EnumFacing.Plane.HORIZONTAL);

		private static final Map<String, EnumFacing.Axis> NAME_LOOKUP = Maps.<String, EnumFacing.Axis>newHashMap();
		private final String name;
		private final EnumFacing.Plane plane;

		private Axis(String name, EnumFacing.Plane plane) {
			this.name = name;
			this.plane = plane;
		}

		/**
		 * Get the axis specified by the given name
		 */
		@Nullable
		public static EnumFacing.Axis byName(String name) {
			return name == null ? null : (EnumFacing.Axis) NAME_LOOKUP.get(name.toLowerCase(Locale.ROOT));
		}

		/**
		 * Like getName but doesn't override the method from Enum.
		 */
		public String getName2() {
			return this.name;
		}

		/**
		 * If this Axis is on the vertical plane (Only true for Y)
		 */
		public boolean isVertical() {
			return this.plane == EnumFacing.Plane.VERTICAL;
		}

		/**
		 * If this Axis is on the horizontal plane (true for X and Z)
		 */
		public boolean isHorizontal() {
			return this.plane == EnumFacing.Plane.HORIZONTAL;
		}

		public String toString() {
			return this.name;
		}

		public boolean apply(@Nullable EnumFacing p_apply_1_) {
			return p_apply_1_ != null && p_apply_1_.getAxis() == this;
		}

		/**
		 * Get this Axis' Plane (VERTICAL for Y, HORIZONTAL for X and Z)
		 */
		public EnumFacing.Plane getPlane() {
			return this.plane;
		}

		public String getName() {
			return this.name;
		}

		static {
			for (EnumFacing.Axis enumfacing$axis : values()) {
				NAME_LOOKUP.put(enumfacing$axis.getName2().toLowerCase(Locale.ROOT), enumfacing$axis);
			}
		}
	}

	public static enum AxisDirection {
		POSITIVE(1, "Towards positive"), NEGATIVE(-1, "Towards negative");

		private final int offset;
		private final String description;

		private AxisDirection(int offset, String description) {
			this.offset = offset;
			this.description = description;
		}

		/**
		 * Get the offset for this AxisDirection. 1 for POSITIVE, -1 for NEGATIVE
		 */
		public int getOffset() {
			return this.offset;
		}

		public String toString() {
			return this.description;
		}
	}

	public static enum Plane implements Predicate<EnumFacing>, Iterable<EnumFacing> {
		HORIZONTAL, VERTICAL;

		/**
		 * All EnumFacing values for this Plane
		 */
		public EnumFacing[] facings() {
			switch (this) {
			case HORIZONTAL:
				return new EnumFacing[] { EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST };
			case VERTICAL:
				return new EnumFacing[] { EnumFacing.UP, EnumFacing.DOWN };
			default:
				throw new Error("Someone's been tampering with the universe!");
			}
		}

		/**
		 * Choose a random Facing from this Plane using the given Random
		 */
		public EnumFacing random(Random rand) {
			EnumFacing[] aenumfacing = this.facings();
			return aenumfacing[rand.nextInt(aenumfacing.length)];
		}

		public boolean apply(@Nullable EnumFacing p_apply_1_) {
			return p_apply_1_ != null && p_apply_1_.getAxis().getPlane() == this;
		}

		public Iterator<EnumFacing> iterator() {
			return Iterators.<EnumFacing>forArray(this.facings());
		}
	}
}