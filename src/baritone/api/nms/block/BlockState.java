package baritone.api.nms.block;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class BlockState {

	private Block b;

	public static BlockState getFromBlockData(Block b) {
		return new BlockState(b);
	}
	
	public BlockState(Block b) {
		this.b = b;
	}
	
	public Material getMaterial() {
		return b.getType();
	}

	public boolean isFullBlock() {
		return getMaterial().isSolid();
	}

	public boolean isFullCube() {
		return isFullBlock();
	}

	public boolean isBlockNormalCube() {
		return getMaterial().isSolid();
	}
	
	public boolean isTopSolid() {
		return b.getRelative(BlockFace.UP).getType().isSolid();
	}

	public Block getBlock() {
		return b;
	}

	public float getBlockHardness(World worldIn, BlockPos pos) {
		return 0;
	}
}
