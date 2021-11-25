package eli.baritone.api.nms.block;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import eli.baritone.api.nms.AxisAlignedBB;
import eli.baritone.api.utils.BlockUtils;

public class BlockState {

	private Material type;
	private Block b;
	private BlockPos pos;

	public static BlockState getFromType(Material type, double x, double y, double z) {
		return getFromType(type, new BlockPos(x, y, z));
	}

	public static BlockState getFromType(Material type, BlockPos pos) {
		return new BlockState(type, pos);
	}

	public static BlockState getFromBlock(Block b) {
		return new BlockState(b);
	}

	public static BlockState getFromBlockData(BlockPos pos, World world) {
		return new BlockState(world.getBlockAt(pos.getX(), pos.getY(), pos.getZ()));
	}

	private BlockState(Material type, BlockPos pos) {
		this.type = type;
		this.pos = pos;
	}

	private BlockState(Block b) {
		this.b = b;
		this.type = b.getType();
		this.pos = new BlockPos(b);
	}

	private void checkType() {
		if (b != null)
			type = b.getType();
	}

	public Material getMaterial() {
		checkType();
		return type;
	}

	public boolean isFullBlock() {
		checkType();
		return type.isSolid();
	}

	public boolean isFullCube() {
		return isFullBlock();
	}

	public boolean isReplaceable() {
		checkType();
		return type.isInteractable();
	}

	public boolean isBlockNormalCube() {
		checkType();
		return type.isSolid();
	}

	public AxisAlignedBB getBoundingBox() {
		return new AxisAlignedBB(pos);
	}

	public boolean isTopSolid() {
		return b.getWorld().getBlockAt(b.getLocation().add(0, 1, 0)).getType().isSolid();
	}

	public Block getBlock() {
		return b;
	}

	public boolean isAir() {
		checkType();
		return type.isAir();
	}

	public boolean isWater() {
		checkType();
		return type.name().contains("WATER");
	}

	public boolean isLadderOrVine() {
		return BlockUtils.is(b, "LADDER", "VINE");
	}

	public boolean isStairs() {
		return BlockUtils.is(b, "STAIRS");
	}

	public float getBlockHardness(World worldIn, BlockPos pos) {
		return 0;
	}
}
