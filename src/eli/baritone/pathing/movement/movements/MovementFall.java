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

package eli.baritone.pathing.movement.movements;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.bukkit.block.Block;

import eli.baritone.Baritone;
import eli.baritone.api.nms.EnumFacing;
import eli.baritone.api.nms.Vec3d;
import eli.baritone.api.nms.Vec3i;
import eli.baritone.api.nms.block.BlockPos;
import eli.baritone.api.pathing.movement.MovementStatus;
import eli.baritone.api.utils.BetterBlockPos;
import eli.baritone.api.utils.BlockUtils;
import eli.baritone.api.utils.Rotation;
import eli.baritone.api.utils.RotationUtils;
import eli.baritone.api.utils.VecUtils;
import eli.baritone.api.utils.input.Input;
import eli.baritone.api.utils.pathing.MutableMoveResult;
import eli.baritone.pathing.movement.CalculationContext;
import eli.baritone.pathing.movement.Movement;
import eli.baritone.pathing.movement.MovementHelper;
import eli.baritone.pathing.movement.MovementState;
import eli.baritone.pathing.movement.MovementState.MovementTarget;

public class MovementFall extends Movement {

    public MovementFall(Baritone baritone, BetterBlockPos src, BetterBlockPos dest) {
        super(baritone, src, dest, MovementFall.buildPositionsToBreak(src, dest));
    }

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult result = new MutableMoveResult();
        MovementDescend.cost(context, src.x, src.y, src.z, dest.x, dest.z, result);
        if (result.y != dest.y) {
            return COST_INF; // doesn't apply to us, this position is a descend not a fall
        }
        return result.cost;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        Set<BetterBlockPos> set = new HashSet<>();
        set.add(src);
        for (int y = src.y - dest.y; y >= 0; y--) {
            set.add(dest.up(y));
        }
        return set;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        BlockPos playerFeet = ctx.playerFeet();
        Rotation toDest = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.getBlockPosCenter(dest), ctx.playerRotations());
        Rotation targetRotation = null;
        Block destBlock = ctx.world().getBlockAt(dest.x, dest.y, dest.z);
        boolean isWater = BlockUtils.is(destBlock, "WATER");
        /*if (targetRotation != null) {
            state.setTarget(new MovementTarget(targetRotation, true));
        } else {*/
            state.setTarget(new MovementTarget(toDest));
        //}
        if (playerFeet.equals(dest) && (ctx.player().locY() - playerFeet.getY() < 0.094 || isWater)) { // 0.094 because lilypads
             return state.setStatus(MovementStatus.SUCCESS);
        }
        Vec3d destCenter = VecUtils.getBlockPosCenter(dest); // we are moving to the 0.5 center not the edge (like if we were falling on a ladder)
        if (Math.abs(ctx.player().locX() + ctx.motionX() - destCenter.x) > 0.1 || Math.abs(ctx.player().locZ() + ctx.motionZ() - destCenter.z) > 0.1) {
            if (!ctx.onGround() && Math.abs(ctx.motionY()) > 0.4) {
                state.setInput(Input.SNEAK, true);
            }
            state.setInput(Input.MOVE_FORWARD, true);
        }
        Vec3i avoid = Optional.ofNullable(avoid()).map(EnumFacing::getDirectionVec).orElse(null);
        if (avoid == null) {
            avoid = src.subtract(dest);
        } else {
            double dist = Math.abs(avoid.getX() * (destCenter.x - avoid.getX() / 2.0 - ctx.player().locX())) + Math.abs(avoid.getZ() * (destCenter.z - avoid.getZ() / 2.0 - ctx.player().locZ()));
            if (dist < 0.6) {
                state.setInput(Input.MOVE_FORWARD, true);
            } else if (!ctx.onGround()) {
                state.setInput(Input.SNEAK, false);
            }
        }
        if (targetRotation == null) {
            Vec3d destCenterOffset = new Vec3d(destCenter.x + 0.125 * avoid.getX(), destCenter.y, destCenter.z + 0.125 * avoid.getZ());
            state.setTarget(new MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), destCenterOffset, ctx.playerRotations())));
        }
        return state;
    }

    private EnumFacing avoid() {
        /*for (int i = 0; i < 15; i++) {
        	BlockPosition pos = ctx.playerFeet().down(i).toBlockPosition();
        	BlockState state = BlockState.getFromBlockData(ctx.world().getType(pos), pos, ctx.world());
            if (state.getBlock() == Blocks.LADDER) {
            	// TODO fix ladder
                //return state.getValue(BlockLadder.FACING);
            }
        }*/
        return null;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        // if we haven't started walking off the edge yet, or if we're in the process of breaking blocks before doing the fall
        // then it's safe to cancel this
        return ctx.playerFeet().equals(src) || state.getStatus() != MovementStatus.RUNNING;
    }

    private static BetterBlockPos[] buildPositionsToBreak(BetterBlockPos src, BetterBlockPos dest) {
        BetterBlockPos[] toBreak;
        int diffX = src.getX() - dest.getX();
        int diffZ = src.getZ() - dest.getZ();
        int diffY = src.getY() - dest.getY();
        toBreak = new BetterBlockPos[diffY + 2];
        for (int i = 0; i < toBreak.length; i++) {
            toBreak[i] = new BetterBlockPos(src.getX() - diffX, src.getY() + 1 - i, src.getZ() - diffZ);
        }
        return toBreak;
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        // only break if one of the first three needs to be broken
        // specifically ignore the last one which might be water
        for (int i = 0; i < 4 && i < positionsToBreak.length; i++) {
            if (!MovementHelper.canWalkThrough(ctx, positionsToBreak[i])) {
                return super.prepared(state);
            }
        }
        return true;
    }
}
