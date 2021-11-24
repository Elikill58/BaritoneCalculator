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

public class MovementDownward extends Movement {

    public MovementDownward(Baritone baritone, BetterBlockPos start, BetterBlockPos end) {
        super(baritone, start, end);
    }

    @Override
    public void reset() {
        super.reset();
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
        if (!context.allowDownward) {
        	//Helper.HELPER.logDebug("MovementDownward: Not allowed.");
            return COST_INF;
        }
        if (!MovementHelper.canWalkOn(context.bsi, x, y - 2, z)) {
        	//Helper.HELPER.logDebug("MovementDownward: Can't walk.");
            return COST_INF;
        }
        BlockState down = context.get(x, y - 1, z);
        Block downBlock = down.getBlock();
        if (BlockUtils.is(downBlock, "LADDER") || BlockUtils.is(downBlock, "VINE")) {
        	//Helper.HELPER.logDebug("MovementDownward: Ladder or vine: " + downBlock);
            return LADDER_DOWN_ONE_COST;
        } else {
        	//Helper.HELPER.logDebug("MovementDownward: Checking for block, mining tick: " + MovementHelper.getMiningDurationTicks(context, x, y - 1, z, down, false));
            // we're standing on it, while it might be block falling, it'll be air by the time we get here in the movement
            return FALL_N_BLOCKS_COST[1] + MovementHelper.getMiningDurationTicks(context, x, y - 1, z, down, false);
        }
    }
}
