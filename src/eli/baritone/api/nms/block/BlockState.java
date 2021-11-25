package eli.baritone.api.nms.block;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import eli.baritone.api.nms.AxisAlignedBB;
import eli.baritone.api.utils.BlockUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.state.IBlockData;

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

	public static BlockState getFromBlock(Block b, BlockPosition pos, World world) {
		return new BlockState(b);
	}

	public static BlockState getFromBlockData(BlockPos pos, World world) {
		return new BlockState(world.getBlockAt(pos.getX(), pos.getY(), pos.getZ()));
	}

	public static BlockState getFromBlockData(BlockPosition pos, World world) {
		return new BlockState(world.getBlockAt(pos.getX(), pos.getY(), pos.getZ()));
	}

	public static BlockState getFromBlockData(IBlockData b, BlockPosition pos, World world) {
		return new BlockState(world.getBlockAt(pos.getX(), pos.getY(), pos.getZ()));
	}

	private BlockState(Block b, BlockPosition pos) {
		this(b);
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

	public IBlockData getBlockData() {
		net.minecraft.world.level.World nmsWorld = ((org.bukkit.craftbukkit.v1_17_R1.CraftWorld) b.getWorld())
				.getHandle().getMinecraftWorld();
		BlockPosition bp = new BlockPosition(b.getX(), b.getY(), b.getZ());
		return nmsWorld.getType(bp);
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
