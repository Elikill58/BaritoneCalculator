package eli.baritone.api.nms.block;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import eli.baritone.api.nms.EnumFacing;
import eli.baritone.api.nms.NmsHelper;
import eli.baritone.api.nms.Vec3i;

public class BlockPos extends Vec3i {

	public BlockPos(int x, int y, int z) {
		super(x, y, z);
	}

	public BlockPos(Block b) {
		this(b.getX(), b.getY(), b.getZ());
	}

	public BlockPos(Object pos) {
		this(NmsHelper.getX(pos), NmsHelper.getY(pos), NmsHelper.getZ(pos));
	}

	public BlockPos(double x, double y, double z) {
		super(x, y, z);
	}

	public BlockPos(Vec3i source) {
		this(source.getX(), source.getY(), source.getZ());
	}

	public Location toBukkitLocation(World w) {
		return new Location(w, getX(), getY(), getZ());
	}

	/**
	 * Add the given coordinates to the coordinates of this BlockPos
	 */
	public BlockPos add(double x, double y, double z) {
		return x == 0.0D && y == 0.0D && z == 0.0D ? this
				: new BlockPos((double) this.getX() + x, (double) this.getY() + y, (double) this.getZ() + z);
	}

	/**
	 * Add the given coordinates to the coordinates of this BlockPos
	 */
	public BlockPos add(int x, int y, int z) {
		return x == 0 && y == 0 && z == 0 ? this : new BlockPos(this.getX() + x, this.getY() + y, this.getZ() + z);
	}

	/**
	 * Add the given Vector to this BlockPos
	 */
	public BlockPos add(Vec3i vec) {
		return this.add(vec.getX(), vec.getY(), vec.getZ());
	}

	/**
	 * Subtract the given Vector from this BlockPos
	 */
	public BlockPos subtract(Vec3i vec) {
		return this.add(-vec.getX(), -vec.getY(), -vec.getZ());
	}

	/**
	 * Offset this BlockPos 1 block up
	 */
	public BlockPos up() {
		return this.up(1);
	}

	/**
	 * Offset this BlockPos n blocks up
	 */
	public BlockPos up(int n) {
		return this.offset(EnumFacing.UP, n);
	}

	/**
	 * Offset this BlockPos 1 block down
	 */
	public BlockPos down() {
		return this.down(1);
	}

	/**
	 * Offset this BlockPos n blocks down
	 */
	public BlockPos down(int n) {
		return this.offset(EnumFacing.DOWN, n);
	}

	/**
	 * Offset this BlockPos 1 block in northern direction
	 */
	public BlockPos north() {
		return this.north(1);
	}

	/**
	 * Offset this BlockPos n blocks in northern direction
	 */
	public BlockPos north(int n) {
		return this.offset(EnumFacing.NORTH, n);
	}

	/**
	 * Offset this BlockPos 1 block in southern direction
	 */
	public BlockPos south() {
		return this.south(1);
	}

	/**
	 * Offset this BlockPos n blocks in southern direction
	 */
	public BlockPos south(int n) {
		return this.offset(EnumFacing.SOUTH, n);
	}

	/**
	 * Offset this BlockPos 1 block in western direction
	 */
	public BlockPos west() {
		return this.west(1);
	}

	/**
	 * Offset this BlockPos n blocks in western direction
	 */
	public BlockPos west(int n) {
		return this.offset(EnumFacing.WEST, n);
	}

	/**
	 * Offset this BlockPos 1 block in eastern direction
	 */
	public BlockPos east() {
		return this.east(1);
	}

	/**
	 * Offset this BlockPos n blocks in eastern direction
	 */
	public BlockPos east(int n) {
		return this.offset(EnumFacing.EAST, n);
	}

	/**
	 * Offset this BlockPos 1 block in the given direction
	 */
	public BlockPos offset(EnumFacing facing) {
		return this.offset(facing, 1);
	}

	/**
	 * Offsets this BlockPos n blocks in the given direction
	 */
	public BlockPos offset(EnumFacing facing, int n) {
		return n == 0 ? this
				: new BlockPos(this.getX() + facing.getXOffset() * n, this.getY() + facing.getYOffset() * n,
						this.getZ() + facing.getZOffset() * n);
	}

	/**
	 * Calculate the cross product of this and the given Vector
	 */
	public BlockPos crossProduct(Vec3i vec) {
		return new BlockPos(this.getY() * vec.getZ() - this.getZ() * vec.getY(),
				this.getZ() * vec.getX() - this.getX() * vec.getZ(),
				this.getX() * vec.getY() - this.getY() * vec.getX());
	}

	/**
	 * Returns a version of this BlockPos that is guaranteed to be immutable.
	 * 
	 * <p>
	 * When storing a BlockPos given to you for an extended period of time, make
	 * sure you use this in case the value is changed internally.
	 * </p>
	 */
	public BlockPos toImmutable() {
		return this;
	}

	public static class MutableBlockPos extends BlockPos {
		/** Mutable X Coordinate */
		protected int x;
		/** Mutable Y Coordinate */
		protected int y;
		/** Mutable Z Coordinate */
		protected int z;

		public MutableBlockPos() {
			this(0, 0, 0);
		}

		public MutableBlockPos(int x_, int y_, int z_) {
			super(0, 0, 0);
			this.x = x_;
			this.y = y_;
			this.z = z_;
		}

		/**
		 * Add the given coordinates to the coordinates of this BlockPos
		 */
		public BlockPos add(double x, double y, double z) {
			return super.add(x, y, z).toImmutable();
		}

		/**
		 * Add the given coordinates to the coordinates of this BlockPos
		 */
		public BlockPos add(int x, int y, int z) {
			return super.add(x, y, z).toImmutable();
		}

		/**
		 * Offsets this BlockPos n blocks in the given direction
		 */
		public BlockPos offset(EnumFacing facing, int n) {
			return super.offset(facing, n).toImmutable();
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
		 * None
		 */
		public BlockPos.MutableBlockPos setPos(int xIn, int yIn, int zIn) {
			this.x = xIn;
			this.y = yIn;
			this.z = zIn;
			return this;
		}

		public BlockPos.MutableBlockPos setPos(double xIn, double yIn, double zIn) {
			return this.setPos(NmsHelper.floor(xIn), NmsHelper.floor(yIn), NmsHelper.floor(zIn));
		}

		public BlockPos.MutableBlockPos move(EnumFacing facing) {
			return this.move(facing, 1);
		}

		public BlockPos.MutableBlockPos move(EnumFacing facing, int n) {
			return this.setPos(this.x + facing.getXOffset() * n, this.y + facing.getYOffset() * n,
					this.z + facing.getZOffset() * n);
		}

		public void setY(int yIn) {
			this.y = yIn;
		}

		/**
		 * Returns a version of this BlockPos that is guaranteed to be immutable.
		 * 
		 * <p>
		 * When storing a BlockPos given to you for an extended period of time, make
		 * sure you use this in case the value is changed internally.
		 * </p>
		 */
		public BlockPos toImmutable() {
			return new BlockPos(this);
		}
	}
}