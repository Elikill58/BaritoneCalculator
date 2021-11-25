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

import java.util.Optional;

import eli.baritone.Baritone;
import eli.baritone.api.BaritoneAPI;
import eli.baritone.api.nms.AxisAlignedBB;
import eli.baritone.api.nms.Vec3d;
import eli.baritone.api.nms.block.BlockPos;
import eli.baritone.api.nms.block.BlockState;
import eli.baritone.api.utils.player.PlayerContext;

/**
 * @author Brady
 * @since 9/25/2018
 */
public final class RotationUtils {

    /**
     * Constant that a radian value is multiplied by to get the equivalent degree value
     */
    public static final double RAD_TO_DEG = 180.0 / Math.PI;

    /**
     * Offsets from the root block position to the center of each side.
     */
    private static final Vec3d[] BLOCK_SIDE_MULTIPLIERS = new Vec3d[]{
            new Vec3d(0.5, 0, 0.5), // Down
            new Vec3d(0.5, 1, 0.5), // Up
            new Vec3d(0.5, 0.5, 0), // North
            new Vec3d(0.5, 0.5, 1), // South
            new Vec3d(0, 0.5, 0.5), // West
            new Vec3d(1, 0.5, 0.5)  // East
    };

    private RotationUtils() {}

    /**
     * Wraps the target angles to a relative value from the current angles. This is done by
     * subtracting the current from the target, normalizing it, and then adding the current
     * angles back to it.
     *
     * @param current The current angles
     * @param target  The target angles
     * @return The wrapped angles
     */
    public static Rotation wrapAnglesToRelative(Rotation current, Rotation target) {
        if (current.yawIsReallyClose(target)) {
            return new Rotation(current.getYaw(), target.getPitch());
        }
        return target.subtract(current).normalize().add(current);
    }

    /**
     * Calculates the rotation from Vec<sub>dest</sub> to Vec<sub>orig</sub> and makes the
     * return value relative to the specified current rotations.
     *
     * @param orig    The origin position
     * @param dest    The destination position
     * @param current The current rotations
     * @return The rotation from the origin to the destination
     * @see #wrapAnglesToRelative(Rotation, Rotation)
     */
    public static Rotation calcRotationFromVec3d(Vec3d orig, Vec3d dest, Rotation current) {
        return wrapAnglesToRelative(current, calcRotationFromVec3d(orig, dest));
    }

    /**
     * Calculates the rotation from Vec<sub>dest</sub> to Vec<sub>orig</sub>
     *
     * @param orig The origin position
     * @param dest The destination position
     * @return The rotation from the origin to the destination
     */
    private static Rotation calcRotationFromVec3d(Vec3d orig, Vec3d dest) {
        double[] delta = {orig.x - dest.x, orig.y - dest.y, orig.z - dest.z};
        double yaw = Math.atan2(delta[0], -delta[2]);
        double dist = Math.sqrt(delta[0] * delta[0] + delta[2] * delta[2]);
        double pitch = Math.atan2(delta[1], dist);
        return new Rotation(
                (float) (yaw * RAD_TO_DEG),
                (float) (pitch * RAD_TO_DEG)
        );
    }

    /**
     * @param ctx Context for the viewing entity
     * @param pos The target block position
     * @return The optional rotation
     * @see #reachable(EntityPlayerSP, BlockPos, double)
     */
    public static Optional<Rotation> reachable(PlayerContext ctx, BlockPos pos) {
        return reachable(ctx.player(), pos, ctx.getBlockReachDistance());
    }

    public static Optional<Rotation> reachable(PlayerContext ctx, BlockPos pos, boolean wouldSneak) {
        return reachable(ctx.player(), pos, ctx.getBlockReachDistance(), wouldSneak);
    }

    /**
     * Determines if the specified entity is able to reach the center of any of the sides
     * of the specified block. It first checks if the block center is reachable, and if so,
     * that rotation will be returned. If not, it will return the first center of a given
     * side that is reachable. The return type will be {@link Optional#empty()} if the entity is
     * unable to reach any of the sides of the block.
     *
     * @param entity             The viewing entity
     * @param pos                The target block position
     * @param blockReachDistance The block reach distance of the entity
     * @return The optional rotation
     */
    public static Optional<Rotation> reachable(PlayerContext entity, BlockPos pos, double blockReachDistance) {
        return reachable(entity, pos, blockReachDistance, false);
    }

    public static Optional<Rotation> reachable(PlayerContext entity, BlockPos pos, double blockReachDistance, boolean wouldSneak) {
        Baritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(entity);
        if (baritone.getPlayerContext().isLookingAt(pos)) {
            /*
             * why add 0.0001?
             * to indicate that we actually have a desired pitch
             * the way we indicate that the pitch can be whatever and we only care about the yaw
             * is by setting the desired pitch to the current pitch
             * setting the desired pitch to the current pitch + 0.0001 means that we do have a desired pitch, it's
             * just what it currently is
             *
             * or if you're a normal person literally all this does it ensure that we don't nudge the pitch to a normal level
             */
            return Optional.of(new Rotation(entity.getYaw(), entity.getPitch() + 0.0001F));
            
        }
        Optional<Rotation> possibleRotation = reachableCenter(entity, pos, blockReachDistance, wouldSneak);
        //System.out.println("center: " + possibleRotation);
        if (possibleRotation.isPresent()) {
            return possibleRotation;
        }

        BlockState state = BlockState.getFrom(pos, entity.getWorld());
        AxisAlignedBB aabb = state.getBoundingBox();
        for (Vec3d sideOffset : BLOCK_SIDE_MULTIPLIERS) {
            double xDiff = aabb.minX * sideOffset.x + aabb.maxX * (1 - sideOffset.x);
            double yDiff = aabb.minY * sideOffset.y + aabb.maxY * (1 - sideOffset.y);
            double zDiff = aabb.minZ * sideOffset.z + aabb.maxZ * (1 - sideOffset.z);
            possibleRotation = reachableOffset(entity, pos, new Vec3d(pos).add(xDiff, yDiff, zDiff), blockReachDistance, wouldSneak);
            if (possibleRotation.isPresent()) {
                return possibleRotation;
            }
        }
        return Optional.empty();
    }

    /**
     * Determines if the specified entity is able to reach the specified block with
     * the given offsetted position. The return type will be {@link Optional#empty()} if
     * the entity is unable to reach the block with the offset applied.
     *
     * @param entity             The viewing entity
     * @param pos                The target block position
     * @param offsetPos          The position of the block with the offset applied.
     * @param blockReachDistance The block reach distance of the entity
     * @return The optional rotation
     */
    public static Optional<Rotation> reachableOffset(PlayerContext entity, BlockPos pos, Vec3d offsetPos, double blockReachDistance, boolean wouldSneak) {
        /*Vec3d eyes = wouldSneak ? RayTraceUtils.inferSneakingEyePosition(entity) : entity.getPositionEyes(1.0F);
        Rotation rotation = calcRotationFromVec3d(eyes, offsetPos, new Rotation(entity.yaw, entity.pitch));
        RayTraceResult result = RayTraceUtils.rayTraceTowards(entity, rotation, blockReachDistance, wouldSneak);
        //System.out.println(result);
        if (result != null && result.getHitBlock() != null) {
            if (new BlockPos(result.getHitPosition()).equals(pos)) {
                return Optional.of(rotation);
            }
            if (entity.world.getType(pos.toBlockPosition()).getBlock() instanceof BlockFire && new BlockPos(result.getHitPosition()).equals(pos.down())) {
                return Optional.of(rotation);
            }
        }*/
        return Optional.empty();
    }

    /**
     * Determines if the specified entity is able to reach the specified block where it is
     * looking at the direct center of it's hitbox.
     *
     * @param entity             The viewing entity
     * @param pos                The target block position
     * @param blockReachDistance The block reach distance of the entity
     * @return The optional rotation
     */
    public static Optional<Rotation> reachableCenter(PlayerContext entity, BlockPos pos, double blockReachDistance, boolean wouldSneak) {
        return reachableOffset(entity, pos, VecUtils.calculateBlockCenter(entity.getWorld(), pos), blockReachDistance, wouldSneak);
    }
}
