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

import java.util.Set;

import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;

import com.google.common.collect.ImmutableSet;

import eli.baritone.Baritone;
import eli.baritone.api.nms.Vec3d;
import eli.baritone.api.nms.block.BlockPos;
import eli.baritone.api.nms.block.BlockState;
import eli.baritone.api.pathing.movement.MovementStatus;
import eli.baritone.api.utils.BetterBlockPos;
import eli.baritone.api.utils.BlockStateInterface;
import eli.baritone.api.utils.BlockUtils;
import eli.baritone.api.utils.Rotation;
import eli.baritone.api.utils.RotationUtils;
import eli.baritone.api.utils.VecUtils;
import eli.baritone.api.utils.input.Input;
import eli.baritone.pathing.movement.CalculationContext;
import eli.baritone.pathing.movement.Movement;
import eli.baritone.pathing.movement.MovementHelper;
import eli.baritone.pathing.movement.MovementState;

public class MovementPillar extends Movement {

    public MovementPillar(Baritone baritone, BetterBlockPos start, BetterBlockPos end) {
        super(baritone, start, end, new BetterBlockPos[]{start.up(2)}, start);
    }

    @Override
    public double calculateCost(CalculationContext context) {
        return cost(context, src.x, src.y, src.z);
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        return ImmutableSet.of(src, dest);
    }

    public static double cost(CalculationContext context, int x, int y, int z) {
    	BlockState fromState = context.get(x, y, z);
        Block from = fromState.getBlock();
        boolean ladder = fromState.isLadderOrVine();
        BlockState fromDown = context.get(x, y - 1, z);
        if (!ladder) {
            if (fromDown.isLadderOrVine()) {
                return COST_INF; // can't pillar from a ladder or vine onto something that isn't also climbable
            }
        }
        if (from.getType().name().contains("VINE") && !hasAgainst(context, x, y, z)) { // TODO this vine can't be climbed, but we could place a pillar still since vines are replacable, no? perhaps the pillar jump would be impossible because of the slowdown actually.
            return COST_INF;
        }
        BlockState toBreak = context.get(x, y + 2, z);
        Block toBreakBlock = toBreak.getBlock();
        if (toBreakBlock.getType().name().contains("FENCE")) { // see issue #172
            return COST_INF;
        }
        Block srcUp = null;
        if (MovementHelper.isWater(toBreakBlock) && MovementHelper.isWater(from)) { // TODO should this also be allowed if toBreakBlock is air?
            srcUp = context.get(x, y + 1, z).getBlock();
            if (MovementHelper.isWater(srcUp)) {
                return LADDER_UP_ONE_COST; // allow ascending pillars of water, but only if we're already in one
            }
        }
        double placeCost = 0;
        if (!ladder) {
            // we need to place a block where we started to jump on it
            placeCost = context.costOfPlacingAt(x, y, z, fromState);
            if (placeCost >= COST_INF) {
                return COST_INF;
            }
            if (fromDown.isAir()) {
                placeCost += 0.1; // slightly (1/200th of a second) penalize pillaring on what's currently air
            }
        }
        double hardness = MovementHelper.getMiningDurationTicks(context, x, y + 2, z, toBreak, true);
        if (hardness >= COST_INF) {
            return COST_INF;
        }
        if (hardness != 0) {
            if (BlockUtils.is(toBreakBlock, "LADDER", "VINE")) {
                hardness = 0; // we won't actually need to break the ladder / vine because we're going to use it
            } else {
            	BlockState check = context.get(x, y + 3, z); // the block on top of the one we're going to break, could it fall on us?
                if (check.getBlock() instanceof FallingBlock) {
                    // see MovementAscend's identical check for breaking a falling block above our head
                    if (srcUp == null) {
                        srcUp = context.get(x, y + 1, z).getBlock();
                    }
                    if (!(toBreakBlock instanceof FallingBlock) || !(srcUp instanceof FallingBlock)) {
                        return COST_INF;
                    }
                }
                // this is commented because it may have had a purpose, but it's very unclear what it was. it's from the minebot era.
                //if (!MovementHelper.canWalkOn(chkPos, check) || MovementHelper.canWalkThrough(chkPos, check)) {//if the block above where we want to break is not a full block, don't do it
                // TODO why does canWalkThrough mean this action is COST_INF?
                // BlockFalling makes sense, and !canWalkOn deals with weird cases like if it were lava
                // but I don't understand why canWalkThrough makes it impossible
                //    return COST_INF;
                //}
            }
        }
        if (ladder) {
            return LADDER_UP_ONE_COST + hardness * 5;
        } else {
            return JUMP_ONE_BLOCK_COST + placeCost + context.jumpPenalty + hardness;
        }
    }

    public static boolean hasAgainst(CalculationContext context, int x, int y, int z) {
        return context.get(x + 1, y, z).isBlockNormalCube() ||
                context.get(x - 1, y, z).isBlockNormalCube() ||
                context.get(x, y, z + 1).isBlockNormalCube() ||
                context.get(x, y, z - 1).isBlockNormalCube();
    }

    public static BlockPos getAgainst(CalculationContext context, BetterBlockPos vine) {
        if (context.get(vine.north()).isBlockNormalCube()) {
            return vine.north();
        }
        if (context.get(vine.south()).isBlockNormalCube()) {
            return vine.south();
        }
        if (context.get(vine.east()).isBlockNormalCube()) {
            return vine.east();
        }
        if (context.get(vine.west()).isBlockNormalCube()) {
            return vine.west();
        }
        return null;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        if (ctx.playerFeet().y < src.y) {
            return state.setStatus(MovementStatus.UNREACHABLE);
        }

        BlockState fromDown = BlockStateInterface.get(ctx, src);
        if (MovementHelper.isWater(fromDown.getBlock()) && MovementHelper.isWater(ctx, dest)) {
            // stay centered while swimming up a water column
            state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.getBlockPosCenter(dest), ctx.playerRotations())));
            Vec3d destCenter = VecUtils.getBlockPosCenter(dest);
            if (Math.abs(ctx.player().locX() - destCenter.x) > 0.2 || Math.abs(ctx.player().locZ() - destCenter.z) > 0.2) {
                state.setInput(Input.MOVE_FORWARD, true);
            }
            if (ctx.playerFeet().equals(dest)) {
                return state.setStatus(MovementStatus.SUCCESS);
            }
            return state;
        }
        boolean ladder = fromDown.isLadderOrVine();
        boolean vine = BlockUtils.is(fromDown.getBlock(), "VINE");
        Rotation rotation = RotationUtils.calcRotationFromVec3d(ctx.playerHead(),
                VecUtils.getBlockPosCenter(positionToPlace),
                new Rotation(ctx.player().getYaw(), ctx.player().getPitch()));
        if (!ladder) {
            state.setTarget(new MovementState.MovementTarget(new Rotation(ctx.player().getYaw(), rotation.getPitch())));
        }

        boolean blockIsThere = MovementHelper.canWalkOn(ctx, src) || ladder;
        if (ladder) {
            BlockPos against = vine ? getAgainst(new CalculationContext(baritone), src) : src;//src.offset(fromDown.getValue(BlockLadder.FACING).getOpposite());
            if (against == null) {
                logDirect("Unable to climb vines. Consider disabling allowVines.");
                return state.setStatus(MovementStatus.UNREACHABLE);
            }

            if (ctx.playerFeet().equals(against.up()) || ctx.playerFeet().equals(dest)) {
                return state.setStatus(MovementStatus.SUCCESS);
            }
            if (MovementHelper.isBottomSlab(BlockStateInterface.get(ctx, src.down()))) {
                state.setInput(Input.JUMP, true);
            }
            /*
            if (thePlayer.getPosition0().getX() != from.getX() || thePlayer.getPosition0().getZ() != from.getZ()) {
                Baritone.moveTowardsBlock(from);
            }
             */

            MovementHelper.moveTowards(ctx, state, against);
            return state;
        } else {
            
            state.setInput(Input.SNEAK, ctx.player().locY() > dest.getY() || ctx.player().locY() < src.getY() + 0.2D); // delay placement by 1 tick for ncp compatibility
            // since (lower down) we only right click once player.isSneaking, and that happens the tick after we request to sneak

            double diffX = ctx.player().locX() - (dest.getX() + 0.5);
            double diffZ = ctx.player().locZ() - (dest.getZ() + 0.5);
            double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
            double flatMotion = Math.sqrt(ctx.motionX() * ctx.motionX() + ctx.motionZ() * ctx.motionZ());
            if (dist > 0.17) {//why 0.17? because it seemed like a good number, that's why
                //[explanation added after baritone port lol] also because it needs to be less than 0.2 because of the 0.3 sneak limit
                //and 0.17 is reasonably less than 0.2

                // If it's been more than forty ticks of trying to jump and we aren't done yet, go forward, maybe we are stuck
                state.setInput(Input.MOVE_FORWARD, true);

                // revise our target to both yaw and pitch if we're going to be moving forward
                state.setTarget(new MovementState.MovementTarget(rotation));
            } else if (flatMotion < 0.05) {
                // If our Y coordinate is above our goal, stop jumping
                state.setInput(Input.JUMP, ctx.player().locY() < dest.getY());
            }


            if (!blockIsThere) {
            	BlockState frState = BlockStateInterface.get(ctx, src);
                // TODO: Evaluate usage of getMaterial().isReplaceable()
                if (!(frState.isAir() || frState.isReplaceable())) {
                    RotationUtils.reachable(ctx.player(), src, ctx.getBlockReachDistance())
                            .map(rot -> new MovementState.MovementTarget(rot))
                            .ifPresent(state::setTarget);
                    state.setInput(Input.JUMP, false); // breaking is like 5x slower when you're jumping
                    state.setInput(Input.CLICK_LEFT, true);
                    blockIsThere = false;
                /*} else if (ctx.player().isSneaking() && (Objects.equals(src.down(), ctx.objectMouseOver().getBlockPos()) || Objects.equals(src, ctx.objectMouseOver().getBlockPos())) && ctx.player().locY() > dest.getY() + 0.1) {
                    state.setInput(Input.CLICK_RIGHT, true);*/
                }
            }
        }

        // If we are at our goal and the block below us is placed
        if (ctx.playerFeet().equals(dest) && blockIsThere) {
            return state.setStatus(MovementStatus.SUCCESS);
        }

        return state;
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (ctx.playerFeet().equals(src) || ctx.playerFeet().equals(src.down())) {
            Block block = BlockStateInterface.getBlock(ctx, src.down());
            if (BlockUtils.is(block, "LADDER", "VINE")) {
                state.setInput(Input.SNEAK, true);
            }
        }
        if (MovementHelper.isWater(ctx, dest.up())) {
            return true;
        }
        return super.prepared(state);
    }
}
