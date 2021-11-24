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

import java.util.Set;

import org.bukkit.block.Block;

import com.google.common.collect.ImmutableSet;

import baritone.Baritone;
import baritone.api.nms.block.BlockState;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockUtils;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;

public class MovementPillar extends Movement {

    public MovementPillar(Baritone baritone, BetterBlockPos start, BetterBlockPos end) {
        super(baritone, start, end);
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
        boolean ladder = BlockUtils.is(from, "LADDER", "VINE");
        BlockState fromDown = context.get(x, y - 1, z);
        if (!ladder) {
            if (BlockUtils.is(fromDown.getBlock(), "LADDER", "VINE")) {
                return COST_INF; // can't pillar from a ladder or vine onto something that isn't also climbable
            }
        }
        if (BlockUtils.is(from, "VINE") && !hasAgainst(context, x, y, z)) { // TODO this vine can't be climbed, but we could place a pillar still since vines are replacable, no? perhaps the pillar jump would be impossible because of the slowdown actually.
            return COST_INF;
        }
        BlockState toBreak = context.get(x, y + 2, z);
        Block toBreakBlock = toBreak.getBlock();
        if (BlockUtils.is(toBreakBlock, "FENCE")) { // see issue #172
            return COST_INF;
        }
        Block srcUp = null;
        if (MovementHelper.isWater(toBreakBlock) && MovementHelper.isWater(from)) {
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
            if (BlockUtils.is(fromDown.getBlock(), "AIR")) {
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
}
