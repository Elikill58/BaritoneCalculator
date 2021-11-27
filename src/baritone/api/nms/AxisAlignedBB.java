package baritone.api.nms;

import org.bukkit.block.Block;

import baritone.api.nms.block.BlockPos;

public class AxisAlignedBB {

	public final double minX, minY, minZ;
	public final double maxX, maxY, maxZ;

	public AxisAlignedBB(Block pos) {
		this(pos.getX(), pos.getY(), pos.getZ(), (pos.getX() + 1), (pos.getY() + 1), (pos.getZ() + 1));
	}
	
	public AxisAlignedBB(BlockPos pos) {
		this(pos.getX(), pos.getY(), pos.getZ(), (pos.getX() + 1), (pos.getY() + 1), (pos.getZ() + 1));
	}

	public AxisAlignedBB(double minX, double var2, double var4, double var6, double var8, double var10) {
		this.minX = Math.min(minX, var6);
		this.minY = Math.min(var2, var8);
		this.minZ = Math.min(var4, var10);
		this.maxX = Math.max(minX, var6);
		this.maxY = Math.max(var2, var8);
		this.maxZ = Math.max(var4, var10);
	}
	
	public double getMaxX() {
		return maxX;
	}
	
	public double getMaxY() {
		return maxY;
	}
	
	public double getMaxZ() {
		return maxZ;
	}
	
	public double getMinX() {
		return minX;
	}
	
	public double getMinY() {
		return minY;
	}
	
	public double getMinZ() {
		return minZ;
	}
}
