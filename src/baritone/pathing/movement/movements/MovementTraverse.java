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

import java.util.Optional;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;

import com.google.common.collect.ImmutableSet;

import baritone.Baritone;
import baritone.api.nms.Vec3d;
import baritone.api.nms.block.BlockPos;
import baritone.api.nms.block.BlockState;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockStateInterface;
import baritone.api.utils.BlockUtils;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.VecUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;

public class MovementTraverse extends Movement {

    /**
     * Did we have to place a bridge block or was it always there
     */
    private boolean wasTheBridgeBlockAlwaysThere = true;

    public MovementTraverse(Baritone baritone, BetterBlockPos from, BetterBlockPos to) {
        super(baritone, from, to, new BetterBlockPos[]{to.up(), to}, to.down());
    }

    @Override
    public void reset() {
        super.reset();
        wasTheBridgeBlockAlwaysThere = true;
    }

    @Override
    public double calculateCost(CalculationContext context) {
        return cost(context, src.x, src.y, src.z, dest.x, dest.z);
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        return ImmutableSet.of(src, dest);
    }

    public static double cost(CalculationContext context, int x, int y, int z, int destX, int destZ) {
        BlockState pb0 = context.get(destX, y + 1, destZ);
        BlockState pb1 = context.get(destX, y, destZ);
        BlockState destOn = context.get(destX, y - 1, destZ);
        Block srcDown = context.getBlock(x, y - 1, z);
        if (MovementHelper.canWalkOn(context.bsi, destX, y - 1, destZ, destOn)) {//this is a walk, not a bridge
            double WC = WALK_ONE_BLOCK_COST;
            boolean water = false;
            if (pb0.isWater() || pb1.isWater()) {
                WC = context.waterWalkSpeed;
                water = true;
            } else {
                if (destOn.getMaterial().equals(Material.SOUL_SAND)) {
                    WC += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
                } else if (destOn.isWater()) {
                    WC += context.walkOnWaterOnePenalty;
                }
                if (srcDown.getType().equals(Material.SOUL_SAND)) {
                    WC += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
                }
            }
            double hardness1 = MovementHelper.getMiningDurationTicks(context, destX, y, destZ, pb1, false);
            if (hardness1 >= COST_INF) {
                return COST_INF;
            }
            double hardness2 = MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, pb0, true); // only include falling on the upper block to break
            if (hardness1 == 0 && hardness2 == 0) {
                if (!water && context.canSprint) {
                    // If there's nothing in the way, and this isn't water, and we aren't sneak placing
                    // We can sprint =D
                    // Don't check for soul sand, since we can sprint on that too
                    WC *= SPRINT_MULTIPLIER;
                }
                return WC;
            }
            if (BlockUtils.is(srcDown, "LADDER", "VINE")) {
                hardness1 *= 5;
                hardness2 *= 5;
            }
            return WC + hardness1 + hardness2;
        } else {//this is a bridge, so we need to place a block
            if (BlockUtils.is(srcDown, "LADDER", "VINE")) {
                return COST_INF;
            }
            if (MovementHelper.isReplaceable(destX, y - 1, destZ, destOn, context.bsi)) {
                boolean throughWater = pb0.isWater() || pb1.isWater();
                if (destOn.isWater() && throughWater) {
                    // this happens when assume walk on water is true and this is a traverse in water, which isn't allowed
                    return COST_INF;
                }
                double placeCost = context.costOfPlacingAt(destX, y - 1, destZ, destOn);
                if (placeCost >= COST_INF) {
                    return COST_INF;
                }
                double hardness1 = MovementHelper.getMiningDurationTicks(context, destX, y, destZ, pb1, false);
                if (hardness1 >= COST_INF) {
                    return COST_INF;
                }
                double hardness2 = MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, pb0, true); // only include falling on the upper block to break
                double WC = throughWater ? context.waterWalkSpeed : WALK_ONE_BLOCK_COST;
                for (int i = 0; i < 5; i++) {
                    int againstX = destX + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i].getXOffset();
                    int againstY = y - 1 + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i].getYOffset();
                    int againstZ = destZ + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i].getZOffset();
                    if (againstX == x && againstZ == z) { // this would be a backplace
                        continue;
                    }
                    if (MovementHelper.canPlaceAgainst(context.bsi, againstX, againstY, againstZ)) { // found a side place option
                        return WC + placeCost + hardness1 + hardness2;
                    }
                }
                if (srcDown.getType().name().contains("WATER")) {
                    return COST_INF; // this is obviously impossible
                }
                WC = WC * (SNEAK_ONE_BLOCK_COST / WALK_ONE_BLOCK_COST);//since we are sneak backplacing, we are sneaking lol
                return WC + placeCost + hardness1 + hardness2;
            }
            return COST_INF;
        }
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        BlockState pb0 = BlockStateInterface.get(ctx, positionsToBreak[0]);
        BlockState pb1 = BlockStateInterface.get(ctx, positionsToBreak[1]);
        if (state.getStatus() != MovementStatus.RUNNING) {
            // if the setting is enabled
            if (!Baritone.settings().walkWhileBreaking.value) {
                return state;
            }
            // and if we're prepping (aka mining the block in front)
            if (state.getStatus() != MovementStatus.PREPPING) {
                return state;
            }
            // and if it's fine to walk into the blocks in front
            if (MovementHelper.avoidWalkingInto(pb0.getBlock())) {
                return state;
            }
            if (MovementHelper.avoidWalkingInto(pb1.getBlock())) {
                return state;
            }
            // and we aren't already pressed up against the block
            double dist = Math.max(Math.abs(ctx.player().locX() - (dest.getX() + 0.5D)), Math.abs(ctx.player().locZ() - (dest.getZ() + 0.5D)));
            if (dist < 0.83) {
                return state;
            }
            if (!state.getTarget().getRotation().isPresent()) {
                // this can happen rarely when the server lags and doesn't send the falling sand entity until you've already walked through the block and are now mining the next one
                return state;
            }

            // combine the yaw to the center of the destination, and the pitch to the specific block we're trying to break
            // it's safe to do this since the two blocks we break (in a traverse) are right on top of each other and so will have the same yaw
            float yawToDest = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.calculateBlockCenter(ctx.world(), dest), ctx.playerRotations()).getYaw();
            float pitchToBreak = state.getTarget().getRotation().get().getPitch();
            if ((pb0.isFullCube() || pb0.isAir() && (pb1.isFullCube() || pb1.isAir()))) {
                // in the meantime, before we're right up against the block, we can break efficiently at this angle
                pitchToBreak = 26;
            }

            return state.setTarget(new MovementState.MovementTarget(new Rotation(yawToDest, pitchToBreak)))
                    .setInput(Input.MOVE_FORWARD, true)
                    .setInput(Input.SPRINT, true);
        }

        //sneak may have been set to true in the PREPPING state while mining an adjacent block
        state.setInput(Input.SNEAK, false);
        boolean ladder = BlockStateInterface.get(ctx, src.down()).isLadderOrVine();

        if (BlockUtils.is(pb0.getBlock(), "DOOR") || BlockUtils.is(pb1.getBlock(), "DOOR")) {

            boolean notPassable = pb0.getMaterial().name().contains("DOOR") && !MovementHelper.isDoorPassable(ctx, src, dest) || pb1.getMaterial().name().contains("DOOR") && !MovementHelper.isDoorPassable(ctx, dest, src);
            boolean canOpen = !(pb0.getMaterial().equals(Material.IRON_DOOR) || pb1.getMaterial().equals(Material.IRON_DOOR));

            if (notPassable && canOpen) {
                return state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.calculateBlockCenter(ctx.world(), positionsToBreak[0]), ctx.playerRotations())))
                        .setInput(Input.CLICK_RIGHT, true);
            }
        }

        if (pb0.getMaterial().name().contains("FENCE") || pb1.getMaterial().name().contains("FENCE")) {
            BlockPos blocked = !MovementHelper.isGatePassable(ctx, positionsToBreak[0], src.up()) ? positionsToBreak[0]
                    : !MovementHelper.isGatePassable(ctx, positionsToBreak[1], src) ? positionsToBreak[1]
                    : null;
            if (blocked != null) {
                Optional<Rotation> rotation = RotationUtils.reachable(ctx, blocked);
                if (rotation.isPresent()) {
                    return state.setTarget(new MovementState.MovementTarget(rotation.get())).setInput(Input.CLICK_RIGHT, true);
                }
            }
        }

        boolean isTheBridgeBlockThere = MovementHelper.canWalkOn(ctx, positionToPlace) || ladder;
        BlockPos feet = ctx.playerFeet();
        if (feet.getY() != dest.getY() && !ladder) {
            logDebug("Wrong Y coordinate");
            if (feet.getY() < dest.getY()) {
                return state.setInput(Input.JUMP, true);
            }
            return state;
        }

        if (isTheBridgeBlockThere) {
            if (feet.equals(dest)) {
                return state.setStatus(MovementStatus.SUCCESS);
            }
            if (Baritone.settings().overshootTraverse.value && (feet.equals(dest.add(getDirection())) || feet.equals(dest.add(getDirection()).add(getDirection())))) {
                return state.setStatus(MovementStatus.SUCCESS);
            }
            BlockState low = BlockStateInterface.get(ctx, src);
            BlockState high = BlockStateInterface.get(ctx, src.up());
            if (ctx.player().locY() > src.y + 0.1D && !ctx.onGround() && (low.isLadderOrVine() || high.isLadderOrVine())) {
                // hitting W could cause us to climb the ladder instead of going forward
                // wait until we're on the ground
                return state;
            }
            BlockPos into = dest.subtract(src).add(dest);
            Block intoBelow = BlockStateInterface.get(ctx, into).getBlock();
            Block intoAbove = BlockStateInterface.get(ctx, into.up()).getBlock();
            if (wasTheBridgeBlockAlwaysThere && (!MovementHelper.isLiquid(ctx, feet) || Baritone.settings().sprintInWater.value) && (!MovementHelper.avoidWalkingInto(intoBelow) || MovementHelper.isWater(intoBelow)) && !MovementHelper.avoidWalkingInto(intoAbove)) {
                state.setInput(Input.SPRINT, true);
            }

            BlockState destDown = BlockStateInterface.get(ctx, dest.down());
            BlockPos against = positionsToBreak[0];
            if (feet.getY() != dest.getY() && ladder && destDown.isLadderOrVine()) {
                against = destDown.getMaterial().name().contains("VINE") ? MovementPillar.getAgainst(new CalculationContext(baritone), dest.down()) : MovementPillar.getAgainst(new CalculationContext(baritone), dest.up());
                if (against == null) {
                    logDirect("Unable to climb vines. Consider disabling allowVines.");
                    return state.setStatus(MovementStatus.UNREACHABLE);
                }
            }
            MovementHelper.moveTowards(ctx, state, against);
            return state;
        } else {
            wasTheBridgeBlockAlwaysThere = false;
            Block standingOn = BlockStateInterface.get(ctx, feet.down()).getBlock();
            if (standingOn.getType().equals(Material.SOUL_SAND)) { // see issue #118
                double dist = Math.max(Math.abs(dest.getX() + 0.5 - ctx.player().locX()), Math.abs(dest.getZ() + 0.5 - ctx.player().locZ()));
                if (dist < 0.85) { // 0.5 + 0.3 + epsilon
                    MovementHelper.moveTowards(ctx, state, dest);
                    return state.setInput(Input.MOVE_FORWARD, false)
                            .setInput(Input.MOVE_BACK, true);
                }
            }
            double dist1 = Math.max(Math.abs(ctx.player().locX() - (dest.getX() + 0.5D)), Math.abs(ctx.player().locZ() - (dest.getZ() + 0.5D)));
            PlaceResult p = MovementHelper.attemptToPlaceABlock(state, baritone, dest.down(), false, true);
            if ((p == PlaceResult.READY_TO_PLACE || dist1 < 0.6) && !Baritone.settings().assumeSafeWalk.value) {
                state.setInput(Input.SNEAK, true);
            }
            switch (p) {
                case READY_TO_PLACE: {
                    if (ctx.player().isSneaking() || Baritone.settings().assumeSafeWalk.value) {
                        state.setInput(Input.CLICK_RIGHT, true);
                    }
                    return state;
                }
                case ATTEMPTING: {
                    if (dist1 > 0.83) {
                        // might need to go forward a bit
                        float yaw = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.getBlockPosCenter(dest), ctx.playerRotations()).getYaw();
                        if (Math.abs(state.getTarget().rotation.getYaw() - yaw) < 0.1) {
                            // but only if our attempted place is straight ahead
                            return state.setInput(Input.MOVE_FORWARD, true);
                        }
                    } else if (ctx.playerRotations().isReallyCloseTo(state.getTarget().rotation)) {
                        // well i guess theres something in the way
                        return state.setInput(Input.CLICK_LEFT, true);
                    }
                    return state;
                }
                default:
                    break;
            }
            if (feet.equals(dest)) {
                // If we are in the block that we are trying to get to, we are sneaking over air and we need to place a block beneath us against the one we just walked off of
                // Out.log(from + " " + to + " " + faceX + "," + faceY + "," + faceZ + " " + whereAmI);
                double faceX = (dest.getX() + src.getX() + 1.0D) * 0.5D;
                double faceY = (dest.getY() + src.getY() - 1.0D) * 0.5D;
                double faceZ = (dest.getZ() + src.getZ() + 1.0D) * 0.5D;
                // faceX, faceY, faceZ is the middle of the face between from and to
                BlockPos goalLook = src.down(); // this is the block we were just standing on, and the one we want to place against

                Rotation backToFace = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), new Vec3d(faceX, faceY, faceZ), ctx.playerRotations());
                float pitch = backToFace.getPitch();
                double dist2 = Math.max(Math.abs(ctx.player().locX() - faceX), Math.abs(ctx.player().locZ() - faceZ));
                if (dist2 < 0.29) { // see issue #208
                    float yaw = RotationUtils.calcRotationFromVec3d(VecUtils.getBlockPosCenter(dest), ctx.playerHead(), ctx.playerRotations()).getYaw();
                    state.setTarget(new MovementState.MovementTarget(new Rotation(yaw, pitch)));
                    state.setInput(Input.MOVE_BACK, true);
                } else {
                    state.setTarget(new MovementState.MovementTarget(backToFace));
                }
                if (ctx.isLookingAt(goalLook)) {
                    return state.setInput(Input.CLICK_RIGHT, true); // wait to right click until we are able to place
                }
                // Out.log("Trying to look at " + goalLook + ", actually looking at" + Baritone.whatAreYouLookingAt());
                if (ctx.playerRotations().isReallyCloseTo(state.getTarget().rotation)) {
                    state.setInput(Input.CLICK_LEFT, true);
                }
                return state;
            }
            MovementHelper.moveTowards(ctx, state, positionsToBreak[0]);
            return state;
            // TODO MovementManager.moveTowardsBlock(to); // move towards not look at because if we are bridging for a couple blocks in a row, it is faster if we dont spin around and walk forwards then spin around and place backwards for every block
        }
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        // if we're in the process of breaking blocks before walking forwards
        // or if this isn't a sneak place (the block is already there)
        // then it's safe to cancel this
        return state.getStatus() != MovementStatus.RUNNING || MovementHelper.canWalkOn(ctx, dest.down());
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (ctx.playerFeet().equals(src) || ctx.playerFeet().equals(src.down())) {
            Block block = BlockStateInterface.getBlock(ctx, src.down());
            if (BlockUtils.is(block, "LADDER", "VINE")) {
                state.setInput(Input.SNEAK, true);
            }
        }
        return super.prepared(state);
    }
}
