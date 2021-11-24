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

package baritone.pathing.movement.movements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.block.Block;

import com.google.common.collect.ImmutableSet;

import baritone.Baritone;
import baritone.api.nms.EnumFacing;
import baritone.api.nms.PlayerContext;
import baritone.api.nms.block.BlockPos;
import baritone.api.nms.block.BlockState;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockStateInterface;
import baritone.api.utils.BlockUtils;
import baritone.api.utils.pathing.MutableMoveResult;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;

public class MovementDiagonal extends Movement {

    private static final double SQRT_2 = Math.sqrt(2);

    public MovementDiagonal(Baritone baritone, BetterBlockPos start, EnumFacing dir1, EnumFacing dir2, int dy) {
        this(baritone, start, start.offset(dir1), start.offset(dir2), dir2, dy);
        // super(start, start.offset(dir1).offset(dir2), new BlockPos[]{start.offset(dir1), start.offset(dir1).up(), start.offset(dir2), start.offset(dir2).up(), start.offset(dir1).offset(dir2), start.offset(dir1).offset(dir2).up()}, new BlockPos[]{start.offset(dir1).offset(dir2).down()});
    }

    private MovementDiagonal(Baritone baritone, BetterBlockPos start, BetterBlockPos dir1, BetterBlockPos dir2, EnumFacing drr2, int dy) {
        this(baritone, start, dir1.offset(drr2).up(dy), dir1, dir2);
    }

    private MovementDiagonal(Baritone baritone, BetterBlockPos start, BetterBlockPos end, BetterBlockPos dir1, BetterBlockPos dir2) {
        super(baritone, start, end);
    }

    @Override
    protected boolean safeToCancel(PlayerContext ctx, MovementState state) {
    	double offset = 0.25;
        double x = ctx.locX();
        double y = ctx.locY() - 1;
        double z = ctx.locZ();
        //standard
        if (ctx.playerFeet().equals(src)) {
            return true;
        }
        //both corners are walkable
        if (MovementHelper.canWalkOn(ctx, new BlockPos(src.x, src.y - 1, dest.z))
                && MovementHelper.canWalkOn(ctx, new BlockPos(dest.x, src.y - 1, src.z))) {
            return true;
        }
        //we are in a likely unwalkable corner, check for a supporting block
        if (ctx.playerFeet().equals(new BetterBlockPos(src.x, src.y, dest.z))
                || ctx.playerFeet().equals(new BetterBlockPos(dest.x, src.y, src.z))) {
            return (MovementHelper.canWalkOn(ctx, new BetterBlockPos(x + offset, y, z + offset))
                    || MovementHelper.canWalkOn(ctx, new BetterBlockPos(x + offset, y, z - offset))
                    || MovementHelper.canWalkOn(ctx, new BetterBlockPos(x - offset, y, z + offset))
                    || MovementHelper.canWalkOn(ctx, new BetterBlockPos(x - offset, y, z - offset)));
        }
        return true;
    }

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult result = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, dest.x, dest.z, result);
        if (result.y != dest.y) {
            return COST_INF; // doesn't apply to us, this position is incorrect
        }
        return result.cost;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        BetterBlockPos diagA = new BetterBlockPos(src.x, src.y, dest.z);
        BetterBlockPos diagB = new BetterBlockPos(dest.x, src.y, src.z);
        if (dest.y < src.y) {
            return ImmutableSet.of(src, dest.up(), diagA, diagB, dest, diagA.down(), diagB.down());
        }
        if (dest.y > src.y) {
            return ImmutableSet.of(src, src.up(), diagA, diagB, dest, diagA.up(), diagB.up());
        }
        return ImmutableSet.of(src, dest, diagA, diagB);
    }

    public static void cost(CalculationContext context, int x, int y, int z, int destX, int destZ, MutableMoveResult res) {
        if (!MovementHelper.canWalkThrough(context.bsi, destX, y + 1, destZ)) {
            return;
        }
        BlockState destInto = context.get(destX, y, destZ);
        boolean ascend = false;
        BlockState destWalkOn;
        boolean descend = false;
        if (!MovementHelper.canWalkThrough(context.bsi, destX, y, destZ, destInto)) {
            ascend = true;
            if (!context.allowDiagonalAscend || !MovementHelper.canWalkThrough(context.bsi, x, y + 2, z) || !MovementHelper.canWalkOn(context.bsi, destX, y, destZ, destInto) || !MovementHelper.canWalkThrough(context.bsi, destX, y + 2, destZ)) {
                return;
            }
            destWalkOn = destInto;
        } else {
            destWalkOn = context.get(destX, y - 1, destZ);
            if (!MovementHelper.canWalkOn(context.bsi, destX, y - 1, destZ, destWalkOn)) {
                descend = true;
                if (!context.allowDiagonalDescend || !MovementHelper.canWalkOn(context.bsi, destX, y - 2, destZ) || !MovementHelper.canWalkThrough(context.bsi, destX, y - 1, destZ, destWalkOn)) {
                    return;
                }
            }
        }
        double multiplier = WALK_ONE_BLOCK_COST;
        // For either possible soul sand, that affects half of our walking
        if (BlockUtils.is(destWalkOn.getBlock(), "SOUL_SAND")) {
            multiplier += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
        } else if (destWalkOn.getBlock().isLiquid()) {
            multiplier += context.walkOnWaterOnePenalty * SQRT_2;
        }
        Block fromDown = context.get(x, y - 1, z).getBlock();
        if (BlockUtils.is(fromDown, "LADDER", "VINE")) {
            return;
        }
        if (BlockUtils.is(fromDown, "SOUL_SAND")) {
            multiplier += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
        }
        Block cuttingOver1 = context.get(x, y - 1, destZ).getBlock();
        if (BlockUtils.is(cuttingOver1, "MAGMA_BLOCK") || MovementHelper.isLava(cuttingOver1)) {
            return;
        }
        Block cuttingOver2 = context.get(destX, y - 1, z).getBlock();
        if (BlockUtils.is(cuttingOver2, "MAGMA_BLOCK") || MovementHelper.isLava(cuttingOver2)) {
            return;
        }
        Block startIn = context.getBlock(x, y, z);
        boolean water = false;
        if (MovementHelper.isWater(startIn) || MovementHelper.isWater(destInto.getBlock())) {
            if (ascend) {
                return;
            }
            // Ignore previous multiplier
            // Whatever we were walking on (possibly soul sand) doesn't matter as we're actually floating on water
            // Not even touching the blocks below
            multiplier = context.waterWalkSpeed;
            water = true;
        }
        BlockState pb0 = context.get(x, y, destZ);
        BlockState pb2 = context.get(destX, y, z);
        if (ascend) {
            boolean ATop = MovementHelper.canWalkThrough(context.bsi, x, y + 2, destZ);
            boolean AMid = MovementHelper.canWalkThrough(context.bsi, x, y + 1, destZ);
            boolean ALow = MovementHelper.canWalkThrough(context.bsi, x, y, destZ, pb0);
            boolean BTop = MovementHelper.canWalkThrough(context.bsi, destX, y + 2, z);
            boolean BMid = MovementHelper.canWalkThrough(context.bsi, destX, y + 1, z);
            boolean BLow = MovementHelper.canWalkThrough(context.bsi, destX, y, z, pb2);
            if ((!(ATop && AMid && ALow) && !(BTop && BMid && BLow)) // no option
                    || MovementHelper.avoidWalkingInto(pb0.getBlock()) // bad
                    || MovementHelper.avoidWalkingInto(pb2.getBlock()) // bad
                    || (ATop && AMid && MovementHelper.canWalkOn(context.bsi, x, y, destZ, pb0)) // we could just ascend
                    || (BTop && BMid && MovementHelper.canWalkOn(context.bsi, destX, y, z, pb2)) // we could just ascend
                    || (!ATop && AMid && ALow) // head bonk A
                    || (!BTop && BMid && BLow)) { // head bonk B
                return;
            }
            res.cost = multiplier * SQRT_2 + JUMP_ONE_BLOCK_COST;
            res.x = destX;
            res.z = destZ;
            res.y = y + 1;
            return;
        }
        double optionA = MovementHelper.getMiningDurationTicks(context, x, y, destZ, pb0, false);
        double optionB = MovementHelper.getMiningDurationTicks(context, destX, y, z, pb2, false);
        if (optionA != 0 && optionB != 0) {
            // check these one at a time -- if pb0 and pb2 were nonzero, we already know that (optionA != 0 && optionB != 0)
            // so no need to check pb1 as well, might as well return early here
            return;
        }
        BlockState pb1 = context.get(x, y + 1, destZ);
        optionA += MovementHelper.getMiningDurationTicks(context, x, y + 1, destZ, pb1, true);
        if (optionA != 0 && optionB != 0) {
            // same deal, if pb1 makes optionA nonzero and option B already was nonzero, pb3 can't affect the result
            return;
        }
        BlockState pb3 = context.get(destX, y + 1, z);
        if (optionA == 0 && ((MovementHelper.avoidWalkingInto(pb2.getBlock()) && !pb2.getBlock().isLiquid()) || MovementHelper.avoidWalkingInto(pb3.getBlock()))) {
            // at this point we're done calculating optionA, so we can check if it's actually possible to edge around in that direction
            return;
        }
        optionB += MovementHelper.getMiningDurationTicks(context, destX, y + 1, z, pb3, true);
        if (optionA != 0 && optionB != 0) {
            // and finally, if the cost is nonzero for both ways to approach this diagonal, it's not possible
            return;
        }
        if (optionB == 0 && ((MovementHelper.avoidWalkingInto(pb0.getBlock()) && !pb0.getBlock().isLiquid()) || MovementHelper.avoidWalkingInto(pb1.getBlock()))) {
            // and now that option B is fully calculated, see if we can edge around that way
            return;
        }
        if (optionA != 0 || optionB != 0) {
            multiplier *= SQRT_2 - 0.001; // TODO tune
            if (BlockUtils.is(startIn, "LADDER", "VINE")) {
                // edging around doesn't work if doing so would climb a ladder or vine instead of moving sideways
                return;
            }
        } else {
            // only can sprint if not edging around
            if (context.canSprint && !water) {
                // If we aren't edging around anything, and we aren't in water
                // We can sprint =D
                // Don't check for soul sand, since we can sprint on that too
                multiplier *= SPRINT_MULTIPLIER;
            }
        }
        res.cost = multiplier * SQRT_2;
        if (descend) {
            res.cost += Math.max(FALL_N_BLOCKS_COST[1], CENTER_AFTER_FALL_COST);
            res.y = y - 1;
        } else {
            res.y = y;
        }
        res.x = destX;
        res.z = destZ;
    }

    @Override
    public List<BlockPos> toWalkInto(BlockStateInterface bsi) {
        if (toWalkIntoCached == null) {
            toWalkIntoCached = new ArrayList<>();
        }
        return toWalkIntoCached;
    }
}
