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

import static baritone.api.pathing.movement.ActionCosts.COST_INF;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import baritone.Baritone;
import baritone.api.nms.PlayerContext;
import baritone.api.nms.block.BlockState;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.utils.BlockStateInterface;
import baritone.api.utils.ToolSet;
import baritone.api.utils.pathing.BetterWorldBorder;

public class CalculationContext {

    private static final ItemStack STACK_BUCKET_WATER = new ItemStack(Material.WATER_BUCKET);

    public final boolean safeForThreadedUse;
    public final Baritone baritone;
    public final World world;
    public final BlockStateInterface bsi;
    public final ToolSet toolSet;
    public final boolean hasWaterBucket;
    public final boolean hasThrowaway;
    public final boolean canSprint;
    protected final double placeBlockCost; // protected because you should call the function instead
    public final boolean allowBreak;
    public final boolean allowParkour;
    public final boolean allowParkourPlace;
    public final boolean allowJumpAt256;
    public final boolean allowParkourAscend;
    public final boolean assumeWalkOnWater;
    public final boolean allowDiagonalDescend;
    public final boolean allowDiagonalAscend;
    public final boolean allowDownward;
    public final int maxFallHeightNoWater;
    public final int maxFallHeightBucket;
    public final double waterWalkSpeed;
    public final double breakBlockAdditionalCost;
    public double backtrackCostFavoringCoefficient;
    public double jumpPenalty;
    public final double walkOnWaterOnePenalty;
    public final BetterWorldBorder worldBorder;
    public final PlayerContext player;

    public CalculationContext(Baritone baritone, boolean forUseOnAnotherThread) {
        this.safeForThreadedUse = forUseOnAnotherThread;
        this.baritone = baritone;
        this.player = baritone.getPlayerContext();
        this.world = player.getWorld();
        this.bsi = new BlockStateInterface(player, forUseOnAnotherThread);
        this.toolSet = new ToolSet(player);
        this.hasThrowaway = true;
        this.hasWaterBucket = player.getItemInHand().isSimilar(STACK_BUCKET_WATER);// TODO fix nether check && !world..isNether();
        this.canSprint = player.getFoodLevel() > 6;
        this.placeBlockCost = 200;
        this.allowBreak = false;
        this.allowParkour = false;
        this.allowParkourPlace = false;
        this.allowJumpAt256 = false;
        this.allowParkourAscend = true;
        this.assumeWalkOnWater = false;
        this.allowDiagonalDescend = true;
        this.allowDiagonalAscend = true;
        this.allowDownward = true;
        this.maxFallHeightNoWater = 3;
        this.maxFallHeightBucket = 20;
        int depth = 0;
        if (depth > 3) {
            depth = 3;
        }
        float mult = depth / 3.0F;
        this.waterWalkSpeed = ActionCosts.WALK_ONE_IN_WATER_COST * (1 - mult) + ActionCosts.WALK_ONE_BLOCK_COST * mult;
        this.breakBlockAdditionalCost = 2D;
        this.backtrackCostFavoringCoefficient = 0.5;
        this.jumpPenalty = 20;
        this.walkOnWaterOnePenalty = 30;
        // why cache these things here, why not let the movements just get directly from settings?
        // because if some movements are calculated one way and others are calculated another way,
        // then you get a wildly inconsistent path that isn't optimal for either scenario.
        this.worldBorder = new BetterWorldBorder(world.getWorldBorder(), world);
    }

    public final Baritone getBaritone() {
        return baritone;
    }

    public BlockState get(int x, int y, int z) {
        return bsi.get0(x, y, z); // laughs maniacally
    }

    public boolean isLoaded(int x, int z) {
        return bsi.isLoaded(x, z);
    }

    public Block getBlock(int x, int y, int z) {
        return get(x, y, z).getBlock();
    }

    public double costOfPlacingAt(int x, int y, int z, BlockState current) {
        if (!hasThrowaway) { // only true if allowPlace is true, see constructor
            return COST_INF;
        }
        if (isPossiblyProtected(x, y, z)) {
            return COST_INF;
        }
        if (!worldBorder.canPlaceAt(x, z)) {
            // TODO perhaps MovementHelper.canPlaceAgainst could also use this?
            return COST_INF;
        }
        return placeBlockCost;
    }

    public double breakCostMultiplierAt(int x, int y, int z, BlockState current) {
        if (!allowBreak) {
            return COST_INF;
        }
        if (isPossiblyProtected(x, y, z)) {
            return COST_INF;
        }
        return 1;
    }

    public double placeBucketCost() {
        return placeBlockCost; // shrug
    }

    public boolean isPossiblyProtected(int x, int y, int z) {
        // TODO more protection logic here; see #220
        return false;
    }
}
