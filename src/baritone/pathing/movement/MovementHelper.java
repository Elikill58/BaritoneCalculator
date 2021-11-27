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

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;

import baritone.Baritone;
import baritone.api.nms.block.BlockPos;
import baritone.api.nms.block.BlockState;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockStateInterface;
import baritone.api.utils.BlockUtils;
import baritone.api.utils.Helper;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.VecUtils;
import baritone.api.utils.input.Input;
import baritone.api.utils.player.PlayerContext;
import baritone.pathing.movement.MovementState.MovementTarget;

/**
 * Static helpers for cost calculation
 *
 * @author leijurv
 */
public interface MovementHelper extends ActionCosts, Helper {

	static boolean avoidBreaking(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
		return BlockUtils.is(state.getMaterial(), "ICE") // ice becomes water, and water can mess up the path
				// call context.get directly with x,y,z. no need to make 5 new BlockPos for no
				// reason
				|| avoidAdjacentBreaking(bsi, x, y + 1, z, true) || avoidAdjacentBreaking(bsi, x + 1, y, z, false)
				|| avoidAdjacentBreaking(bsi, x - 1, y, z, false) || avoidAdjacentBreaking(bsi, x, y, z + 1, false)
				|| avoidAdjacentBreaking(bsi, x, y, z - 1, false);
	}

	static boolean avoidAdjacentBreaking(BlockStateInterface bsi, int x, int y, int z, boolean directlyAbove) {
		// returns true if you should avoid breaking a block that's adjacent to this one
		// (e.g. lava that will start flowing if you give it a path)
		// this is only called for north, south, east, west, and up. this is NOT called
		// for down.
		// we assume that it's ALWAYS okay to break the block thats ABOVE liquid
		if (!directlyAbove
				&& bsi.get0(x, y, z).getBlock() instanceof FallingBlock
				&& Baritone.settings().avoidUpdatingFallingBlocks.value // and if the setting is enabled
				&& canFallThrough(bsi.get0(x, y - 1, z))) { // and if it would fall (i.e.
																						// it's unsupported)
			return true; // dont break a block that is adjacent to unsupported gravel because it can
							// cause really weird stuff
		}
		return false;
	}
	
	static boolean canFallThrough(BlockState b) {
		return b.getBlock().isLiquid() || b.getBlock().isPassable() || b.isReplaceable();
	}

	static boolean canWalkThrough(PlayerContext ctx, BetterBlockPos pos) {
		return canWalkThrough(new BlockStateInterface(ctx), pos.x, pos.y, pos.z);
	}

	static boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z) {
		return canWalkThrough(bsi, x, y, z, bsi.get0(x, y, z));
	}

	static boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
		if (state.isAir()) { // early return for most common case
			return true;
		}
		Material block = state.getMaterial();
		if (BlockUtils.is(block, "FIRE", "TRIPWIRE", "COBWEB", "END_PORTAL", "COCOA", "SKULL", "END_ROD")) {
			return false;
		}
		if (BlockUtils.is(block, "DOOR", "FENCE")) {
			// Because there's no nice method in vanilla to check if a door is openable or
			// not, we just have to assume
			// that anything that isn't an iron door isn't openable, ignoring that some
			// doors introduced in mods can't
			// be opened by just interacting.
			return !block.equals(Material.IRON_DOOR);
		}
		if (block.name().contains("CARPET")) {
			return canWalkOn(bsi, x, y - 1, z);
		}
		if (block.name().contains("SNOW")) {
			// we've already checked doors and fence gates
			// so the only remaining dynamic isPassables are snow and trapdoor
			// if they're cached as a top block, we don't know their metadata
			// default to true (mostly because it would otherwise make long distance pathing
			// through snowy biomes impossible)
			if (!bsi.worldContainsLoadedChunk(x, z)) {
				return true;
			}
			// ok, it's low enough we could walk through it, but is it supported?
			return canWalkOn(bsi, x, y - 1, z);
		}
		if (isFlowing(x, y, z, state, bsi)) {
			return false; // Don't walk through flowing liquids
		}

		return !block.isSolid();// block.isPassable(bsi.access,
																// bsi.isPassableBlockPos.setPos(x, y, z));
	}

	/**
	 * canWalkThrough but also won't impede movement at all. so not including doors
	 * or fence gates (we'd have to right click), not including water, and not
	 * including ladders or vines or cobwebs (they slow us down)
	 *
	 * @param context
	 *            Calculation context to provide block state lookup
	 * @param x
	 *            The block's x position
	 * @param y
	 *            The block's y position
	 * @param z
	 *            The block's z position
	 * @return Whether or not the block at the specified position
	 */
	static boolean fullyPassable(CalculationContext context, int x, int y, int z) {
		return fullyPassable(context.bsi.get0(x, y, z));
	}

	static boolean fullyPassable(PlayerContext ctx, BlockPos pos) {
		return fullyPassable(BlockState.getFrom(pos, ctx.world()));
	}

	static boolean fullyPassable(BlockState state) {
		Material block = state.getMaterial();
		if (state.isAir()) { // early return for most common case
			return true;
		}
		// exceptions - blocks that are isPassable true, but we can't actually jump
		// through
		if (BlockUtils.is(block, "FIRE", "TRIPWIRE", "COBWEB", "VINE", "LADDER", "COCOA", "DOOR", "FENCE", "SNOW",
				"SKULL")) {
			return false;
		}
		// door, fence gate, liquid, trapdoor have been accounted for, nothing else uses
		// the world or pos parameters
		return block.isSolid();
	}

	static boolean isReplaceable(int x, int y, int z, BlockState state, BlockStateInterface bsi) {
		// for MovementTraverse and MovementAscend
		// block double plant defaults to true when the block doesn't match, so don't
		// need to check that case
		// all other overrides just return true or false
		// the only case to deal with is snow
		/*
		 * public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) { return
		 * ((Integer)worldIn.getBlockState(pos).getValue(LAYERS)).intValue() == 1; }
		 */
		if (BlockUtils.is(state.getMaterial(), "AIR") || state.isWater()) {
			// early return for common cases hehe
			return true;
		}
		return state.isReplaceable();
	}

	static boolean isDoorPassable(PlayerContext ctx, BlockPos doorPos, BlockPos playerPos) {
		if (playerPos.equals(doorPos)) {
			return false;
		}

		BlockState state = BlockStateInterface.get(ctx, doorPos);
		if (!(state.getMaterial().name().contains("DOOR"))) {
			return true;
		}

		return !state.getMaterial().isSolid();
	}

	static boolean isGatePassable(PlayerContext ctx, BlockPos gatePos, BlockPos playerPos) {
		if (playerPos.equals(gatePos)) {
			return false;
		}

		BlockState state = BlockStateInterface.get(ctx, gatePos);
		if (!(state.getMaterial().name().contains("FENCE"))) {
			return true;
		}

		return !state.getMaterial().isSolid();
	}

	static boolean avoidWalkingInto(Block block) {
		return BlockUtils.is(block, "CACTUS", "FIRE", "END_PORTAL", "COBWEB");
	}

	/**
	 * Can I walk on this block without anything weird happening like me falling
	 * through? Includes water because we know that we automatically jump on water
	 *
	 * @param bsi
	 *            Block state provider
	 * @param x
	 *            The block's x position
	 * @param y
	 *            The block's y position
	 * @param z
	 *            The block's z position
	 * @param state
	 *            The state of the block at the specified location
	 * @return Whether or not the specified block can be walked on
	 */
	static boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
		if (state.getMaterial().equals(Material.AIR)) {
			// early return for most common case (air)
			// plus magma, which is a normal cube but it hurts you
			return false;
		}
		if (state.isBlockNormalCube() || state.isLadderOrVine()) {
			return true;
		}
		Material block = state.getMaterial();
		if (BlockUtils.is(block, "FARMLAND", "GRASS_PATH", "CHEST")) {
			return true;
		}
		if (state.isWater()) {
			// since this is called literally millions of times per second, the benefit of
			// not allocating millions of useless "pos.up()"
			// BlockPos s that we'd just garbage collect immediately is actually noticeable.
			// I don't even think its a decrease in readability
			BlockState up = bsi.get0(x, y + 1, z);
			if (up.getMaterial().name().contains("CARPET")) {
				return true;
			}
			if (isFlowing(x, y, z, state, bsi)) {
				// the only scenario in which we can walk on flowing water is if it's under
				// still water with jesus off
				return up.isWater() && !Baritone.settings().assumeWalkOnWater.value;
			}
			// if assumeWalkOnWater is on, we can only walk on water if there isn't water
			// above it
			// if assumeWalkOnWater is off, we can only walk on water if there is water
			// above it
			return up.isWater() ^ Baritone.settings().assumeWalkOnWater.value;
		}
		if (Baritone.settings().assumeWalkOnLava.value && BlockUtils.is(block, "LAVA") && !isFlowing(x, y, z, state, bsi)) {
			return true;
		}
		if (BlockUtils.is(block, "GLASS")) {
			return true;
		}
		return state.isStairs();
	}

	static boolean canWalkOn(PlayerContext ctx, BetterBlockPos pos, BlockState state) {
		return canWalkOn(new BlockStateInterface(ctx), pos.x, pos.y, pos.z, state);
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
		// can we look at the center of a side face of this block and likely be able to
		// place?
		// (thats how this check is used)
		// therefore dont include weird things that we technically could place against
		// (like carpet) but practically can't
		return state.isBlockNormalCube() || state.isFullBlock() || state.getMaterial().name().contains("GLASS");
	}

	static double getMiningDurationTicks(CalculationContext context, int x, int y, int z, boolean includeFalling) {
		return getMiningDurationTicks(context, x, y, z, context.get(x, y, z), includeFalling);
	}

	static double getMiningDurationTicks(CalculationContext context, int x, int y, int z, BlockState state,
			boolean includeFalling) {
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
			if (includeFalling) {
				BlockState above = context.get(x, y + 1, z);
				if (above.getBlock() instanceof FallingBlock) {
					result += getMiningDurationTicks(context, x, y + 1, z, above, true);
				}
			}
			return result;
		}
		return 0; // we won't actually mine it, so don't check fallings above
	}

	static boolean isBottomSlab(BlockState state) {
		return false; // TODO fix bottom slab
	}

	static void moveTowards(PlayerContext ctx, MovementState state, BlockPos pos) {
		state.setTarget(new MovementTarget(new Rotation(RotationUtils
				.calcRotationFromVec3d(ctx.playerHead(), VecUtils.getBlockPosCenter(pos), ctx.playerRotations())
				.getYaw(), ctx.player().getPitch()))).setInput(Input.MOVE_FORWARD, true);
	}

	/**
	 * Returns whether or not the specified block is water, regardless of whether or
	 * not it is flowing.
	 *
	 * @param b
	 *            The block
	 * @return Whether or not the block is water
	 */
	static boolean isWater(Block b) {
		return b.getType().name().contains("WATER");
	}

	/**
	 * Returns whether or not the block at the specified pos is water, regardless of
	 * whether or not it is flowing.
	 *
	 * @param ctx
	 *            The player context
	 * @param bp
	 *            The block pos
	 * @return Whether or not the block is water
	 */
	static boolean isWater(PlayerContext ctx, BlockPos bp) {
		return isWater(BlockStateInterface.getBlock(ctx, bp));
	}

	static boolean isLava(Block b) {
		return b.getType().name().contains("LAVA");
	}

	static boolean isLiquid(Block b) {
		return isWater(b) || isLava(b);
	}

	/**
	 * Returns whether or not the specified pos has a liquid
	 *
	 * @param ctx
	 *            The player context
	 * @param p
	 *            The pos
	 * @return Whether or not the block is a liquid
	 */
	static boolean isLiquid(PlayerContext ctx, BlockPos p) {
		return isWater(BlockStateInterface.getBlock(ctx, p)) || isLava(BlockStateInterface.getBlock(ctx, p));
	}

	static boolean possiblyFlowing(Block state) {
		// Will be IFluidState in 1.13
		return isWater(state);
	}

	static boolean isFlowing(int x, int y, int z, BlockState state, BlockStateInterface bsi) {
		return bsi.get0(x + 1, y, z).isWater() || bsi.get0(x - 1, y, z).isWater()
				|| bsi.get0(x, y, z + 1).isWater() || bsi.get0(x, y, z - 1).isWater();
	}

	static PlaceResult attemptToPlaceABlock(MovementState state, Baritone baritone, BlockPos placeAt,
			boolean preferDown, boolean wouldSneak) {
		PlayerContext ctx = baritone.getPlayerContext();
		Optional<Rotation> direct = RotationUtils.reachable(ctx, placeAt, wouldSneak); // we assume that if there is a
																						// block there, it must be
																						// replacable
		boolean found = false;
		if (direct.isPresent()) {
			state.setTarget(new MovementState.MovementTarget(direct.get()));
			found = true;
		}
		if (found) {
			return PlaceResult.ATTEMPTING;
		}
		return PlaceResult.NO_OPTION;
	}

	enum PlaceResult {
		READY_TO_PLACE, ATTEMPTING, NO_OPTION;
	}
}
