/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package eli.baritone.api.utils;

import org.bukkit.World;

import eli.baritone.api.nms.AxisAlignedBB;
import eli.baritone.api.nms.Vec3d;
import eli.baritone.api.nms.block.BlockPos;
import eli.baritone.api.nms.block.BlockState;
import eli.baritone.api.utils.player.PlayerContext;

/**
 * @author Brady
 * @since 10/13/2018
 */
public final class VecUtils {

    private VecUtils() {}

    /**
     * Calculates the center of the block at the specified position's bounding box
     *
     * @param world The world that the block is in, used to provide the bounding box
     * @param pos   The block position
     * @return The center of the block's bounding box
     * @see #getBlockPosCenter(BlockPos)
     */
    public static Vec3d calculateBlockCenter(World world, BlockPos pos) {
        BlockState b = BlockState.getFrom(pos, world);
        AxisAlignedBB bbox = b.getBoundingBox();
        double xDiff = (bbox.minX + bbox.maxX) / 2;
        double yDiff = (bbox.minY + bbox.maxY) / 2;
        double zDiff = (bbox.minZ + bbox.maxZ) / 2;
        return new Vec3d(
                pos.getX() + xDiff,
                pos.getY() + yDiff,
                pos.getZ() + zDiff
        );
    }

    /**
     * Gets the assumed center position of the given block position.
     * This is done by adding 0.5 to the X, Y, and Z axes.
     * <p>
     * TODO: We may want to consider replacing many usages of this method with #calculateBlockCenter(BlockPos)
     *
     * @param pos The block position
     * @return The assumed center of the position
     * @see #calculateBlockCenter(World, BlockPos)
     */
    public static Vec3d getBlockPosCenter(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /**
     * Gets the distance from the specified position to the assumed center of the specified block position.
     *
     * @param pos The block position
     * @param x   The x pos
     * @param y   The y pos
     * @param z   The z pos
     * @return The distance from the assumed block center to the position
     * @see #getBlockPosCenter(BlockPos)
     */
    public static double distanceToCenter(BlockPos pos, double x, double y, double z) {
        double xdiff = pos.getX() + 0.5 - x;
        double ydiff = pos.getY() + 0.5 - y;
        double zdiff = pos.getZ() + 0.5 - z;
        return Math.sqrt(xdiff * xdiff + ydiff * ydiff + zdiff * zdiff);
    }

    /**
     * Gets the distance from the specified entity's position to the assumed
     * center of the specified block position.
     *
     * @param entity The entity
     * @param pos    The block position
     * @return The distance from the entity to the block's assumed center
     * @see #getBlockPosCenter(BlockPos)
     */
    public static double entityDistanceToCenter(PlayerContext entity, BlockPos pos) {
        return distanceToCenter(pos, entity.locX(), entity.locY(), entity.locZ());
    }

    /**
     * Gets the distance from the specified entity's position to the assumed
     * center of the specified block position, ignoring the Y axis.
     *
     * @param entity The entity
     * @param pos    The block position
     * @return The horizontal distance from the entity to the block's assumed center
     * @see #getBlockPosCenter(BlockPos)
     */
    public static double entityFlatDistanceToCenter(PlayerContext entity, BlockPos pos) {
        return distanceToCenter(pos, entity.locX(), pos.getY() + 0.5, entity.locZ());
    }
    
    /**
     * Floor the actual the double value
     * 
     * @param d the double to floor
     * @return the floored variable
     */
	public static int floor(double d) {
	    int i = (int) d;
	    return (d < i) ? (i - 1) : i;
	}
}
