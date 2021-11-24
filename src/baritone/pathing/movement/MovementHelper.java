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

package baritone.pathing.movement;

import org.bukkit.block.Block;

import baritone.api.nms.PlayerContext;
import baritone.api.nms.block.BlockPos;
import baritone.api.nms.block.BlockState;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockStateInterface;
import baritone.api.utils.BlockUtils;
import baritone.api.utils.Helper;

public interface MovementHelper extends ActionCosts, Helper {

    static boolean avoidBreaking(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        return BlockUtils.is(state.getBlock(), "ICE");
    }

    static boolean canWalkThrough(PlayerContext ctx, BetterBlockPos pos) {
        return canWalkThrough(new BlockStateInterface(ctx), pos.x, pos.y, pos.z);
    }

    static boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z) {
        return canWalkThrough(bsi, x, y, z, bsi.get0(x, y, z));
    }

    static boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
    	if(bsi.getPlayer().isAllowedFly()) {
    		return true;
    	}
        Block block = state.getBlock();
        if (BlockUtils.is(block, "AIR")) { // early return for most common case
            return true;
        }
        if (BlockUtils.is(block, "FIRE", "TRIPWIRE", "COBWEB", "END_PORTAL", "COCOA", "SKULL", "END_ROD", "IRON_DOOR")) {
            return false;
        }
        if (BlockUtils.is(block, "CARPET")) {
            return canWalkOn(bsi, x, y - 1, z);
        }
        if (BlockUtils.is(block, "SNOW")) {
            // we've already checked doors and fence gates
            // so the only remaining dynamic isPassables are snow and trapdoor
            // if they're cached as a top block, we don't know their metadata
            // default to true (mostly because it would otherwise make long distance pathing through snowy biomes impossible)
            if (!bsi.worldContainsLoadedChunk(x, z)) {
                return true;
            }
            // ok, it's low enough we could walk through it, but is it supported?
            return canWalkOn(bsi, x, y - 1, z);
        }
        if (isFlowing(x, y, z, state, bsi)) {
            return false; // Don't walk through flowing liquids
        }

        return !block.getType().isSolid();//block.isPassable(bsi.access, bsi.isPassableBlockPos.setPos(x, y, z));
    }

    /**
     * canWalkThrough but also won't impede movement at all. so not including doors or fence gates (we'd have to right click),
     * not including water, and not including ladders or vines or cobwebs (they slow us down)
     *
     * @param context Calculation context to provide block state lookup
     * @param x       The block's x position
     * @param y       The block's y position
     * @param z       The block's z position
     * @return Whether or not the block at the specified position
     */
    static boolean fullyPassable(CalculationContext context, int x, int y, int z) {
        return fullyPassable(context.bsi.get0(x, y, z).getBlock());
    }

    static boolean fullyPassable(Block block) {
        // exceptions - blocks that are isPassable true, but we can't actually jump through
        if (BlockUtils.is(block, "AIR", "FIRE", "TRIPWIRE", "COBWEB", "VINE", "LADDER", "COCOA", "DOOR", "FENCE", "SNOW", "SKULL")) {
            return false;
        }
        // door, fence gate, liquid, trapdoor have been accounted for, nothing else uses the world or pos parameters
        return block.getType().isSolid();
    }

    static boolean isReplaceable(int x, int y, int z, BlockState state, BlockStateInterface bsi) {
        // for MovementTraverse and MovementAscend
        // block double plant defaults to true when the block doesn't match, so don't need to check that case
        // all other overrides just return true or false
        // the only case to deal with is snow
        /*
         *  public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos)
         *     {
         *         return ((Integer)worldIn.getBlockState(pos).getValue(LAYERS)).intValue() == 1;
         *     }
         */
        Block block = state.getBlock();
        if (BlockUtils.is(block, "AIR", "WATER")) {
            // early return for common cases hehe
            return true;
        }
        return !state.getMaterial().isBlock();
    }

    static boolean avoidWalkingInto(Block block) {
        return BlockUtils.is(block, "CACTUS", "FIRE", "END_PORTAL", "COBWEB");
    }

    /**
     * Can I walk on this block without anything weird happening like me falling
     * through? Includes water because we know that we automatically jump on
     * water
     *
     * @param bsi   Block state provider
     * @param x     The block's x position
     * @param y     The block's y position
     * @param z     The block's z position
     * @param state The state of the block at the specified location
     * @return Whether or not the specified block can be walked on
     */
    static boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
    	if(bsi.getPlayer().isAllowedFly()) {
    		return true;
    	}
        Block block = state.getBlock();
        if (state.isBlockNormalCube()) {
            return true;
        }
        if (BlockUtils.is(block, "AIR", "LADDER", "FARMLAND", "GRASS_PATH", "ENDER_CHEST", "CHEST", "TRAPPED_CHEST")) {
            return true;
        }
        if (isWater(block)) {
            // since this is called literally millions of times per second, the benefit of not allocating millions of useless "pos.up()"
            // BlockPos s that we'd just garbage collect immediately is actually noticeable. I don't even think its a decrease in readability
            Block up = bsi.get0(x, y + 1, z).getBlock();
            if (up.getType().name().contains("CARPET")) {
                return true;
            }
            if (isFlowing(x, y, z, state, bsi)) {
                // the only scenario in which we can walk on flowing water is if it's under still water with jesus off
                return isWater(up);
            }
            // if assumeWalkOnWater is on, we can only walk on water if there isn't water above it
            // if assumeWalkOnWater is off, we can only walk on water if there is water above it
            return isWater(up) ^ false;
        }
        if (BlockUtils.is(block, "GLASS")) {
            return true;
        }
        return BlockUtils.is(block, "STAIRS");
    }

    static boolean canWalkOn(PlayerContext ctx, BlockPos pos) {
        return canWalkOn(new BlockStateInterface(ctx), pos.getX(), pos.getY(), pos.getZ());
    }

    static boolean canWalkOn(PlayerContext ctx, BetterBlockPos pos) {
        return canWalkOn(new BlockStateInterface(ctx), pos.x, pos.y, pos.z);
    }

    static boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z) {
        return canWalkOn(bsi, x, y, z, bsi.get0(x, y, z));
    }

    static boolean canPlaceAgainst(BlockStateInterface bsi, int x, int y, int z) {
        return canPlaceAgainst(bsi, x, y, z, bsi.get0(x, y, z));
    }

    static boolean canPlaceAgainst(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
    	if(bsi.getPlayer().isAllowedFly()) {
    		return true;
    	}
        // can we look at the center of a side face of this block and likely be able to place?
        // (thats how this check is used)
        // therefore dont include weird things that we technically could place against (like carpet) but practically can't
        return state.isBlockNormalCube() || state.isFullBlock() || BlockUtils.is(state.getBlock(), "GLASS");
    }

    static double getMiningDurationTicks(CalculationContext context, int x, int y, int z, boolean includeFalling) {
        return getMiningDurationTicks(context, x, y, z, context.get(x, y, z), includeFalling);
    }

    static double getMiningDurationTicks(CalculationContext context, int x, int y, int z, BlockState state, boolean includeFalling) {
        Block block = state.getBlock();
        if (!canWalkThrough(context.bsi, x, y, z, state)) {
            if (isLiquid(block)) {
                return COST_INF;
            }
            double mult = context.breakCostMultiplierAt(x, y, z, state);
            if (mult >= COST_INF) {
                return COST_INF;
            }
            if (avoidBreaking(context.bsi, x, y, z, state)) {
                return COST_INF;
            }
            double strVsBlock = context.toolSet.getStrVsBlock(state);
            if (strVsBlock <= 0) {
                return COST_INF;
            }
            double result = 1 / strVsBlock;
            result += context.breakBlockAdditionalCost;
            result *= mult;
            return result;
        }
        return 0; // we won't actually mine it, so don't check fallings above
    }

    static boolean isBottomSlab(BlockState state) {
        return false; // TODO fix bottom slab
    }

    /**
     * Returns whether or not the specified block is
     * water, regardless of whether or not it is flowing.
     *
     * @param b The block
     * @return Whether or not the block is water
     */
    static boolean isWater(Block b) {
        return b.getType().name().contains("WATER");
    }

    static boolean isLava(Block b) {
        return b.getType().name().contains("LAVA");
    }
    static boolean isLiquid(Block b) {
        return isWater(b) || isLava(b);
    }

    static boolean possiblyFlowing(BlockState state) {
        // Will be IFluidState in 1.13
        return isWater(state.getBlock());
    }

    static boolean isFlowing(int x, int y, int z, BlockState state, BlockStateInterface bsi) {
        return possiblyFlowing(bsi.get0(x + 1, y, z))
                || possiblyFlowing(bsi.get0(x - 1, y, z))
                || possiblyFlowing(bsi.get0(x, y, z + 1))
                || possiblyFlowing(bsi.get0(x, y, z - 1));
    }
}
