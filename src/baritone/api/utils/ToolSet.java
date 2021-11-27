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

package baritone.api.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import baritone.Baritone;
import baritone.api.nms.block.BlockState;
import baritone.api.utils.player.PlayerContext;

/**
 * A cached list of the best tools on the hotbar for any block
 *
 * @author Avery, Brady, leijurv
 */
public class ToolSet {

    /**
     * A cache mapping a {@link Block} to how long it will take to break
     * with this toolset, given the optimum tool is used.
     */
    private final Map<Block, Double> breakStrengthCache;

    /**
     * My buddy leijurv owned me so we have this to not create a new lambda instance.
     */
    private final Function<Block, Double> backendCalculation;

    private final PlayerContext player;

    public ToolSet(PlayerContext player) {
        breakStrengthCache = new HashMap<>();
        this.player = player;

        if (Baritone.settings().considerPotionEffects.value) {
            double amplifier = potionAmplifier();
            Function<Double, Double> amplify = x -> amplifier * x;
            backendCalculation = amplify.compose(this::getBestDestructionTime);
        } else {
            backendCalculation = this::getBestDestructionTime;
        }
    }

    /**
     * Using the best tool on the hotbar, how fast we can mine this block
     *
     * @param state the blockstate to be mined
     * @return the speed of how fast we'll mine it. 1/(time in ticks)
     */
    public double getStrVsBlock(BlockState state) {
        return breakStrengthCache.computeIfAbsent(state.getBlock(), backendCalculation);
    }

    /**
     * Evaluate the material cost of a possible tool. The priority matches the
     * listed order in the Item.ToolMaterial enum.
     *
     * @param itemStack a possibly empty ItemStack
     * @return values range from -1 to 4
     */
    private int getMaterialCost(ItemStack itemStack) {
    	return -1;
    	/*if (itemStack.getItem() instanceof ItemTool) {
            ItemTool tool = (ItemTool) itemStack.getItem();
            return ToolMaterial.valueOf(tool.getToolMaterialName()).ordinal();
        } else {
            return -1;
        }*/
    }

    public boolean hasSilkTouch(ItemStack stack) {
        return false;//EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack) > 0;
    }

    public int getBestSlot(Block b, boolean preferSilkTouch, boolean pathingCalculation) {

        /*
        If we actually want know what efficiency our held item has instead of the best one
        possible, this lets us make pathing depend on the actual tool to be used (if auto tool is disabled)
        */
        if (Baritone.settings().disableAutoTool.value && pathingCalculation) {
            return player.getItemInHandIndex();
        }

        int best = 0;
        double highestSpeed = Double.NEGATIVE_INFINITY;
        int lowestCost = Integer.MIN_VALUE;
        boolean bestSilkTouch = false;
        BlockState blockState = BlockState.getFrom(b);
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = player.getItem(i);
            if (!Baritone.settings().useSwordToMine.value && itemStack.getType().name().contains("SWORD")) {
                continue;
            }
            double speed = calculateSpeedVsBlock(itemStack, blockState);
            boolean silkTouch = hasSilkTouch(itemStack);
            if (speed > highestSpeed) {
                highestSpeed = speed;
                best = i;
                lowestCost = getMaterialCost(itemStack);
                bestSilkTouch = silkTouch;
            } else if (speed == highestSpeed) {
                int cost = getMaterialCost(itemStack);
                if ((cost < lowestCost && (silkTouch || !bestSilkTouch)) ||
                        (preferSilkTouch && !bestSilkTouch && silkTouch)) {
                    highestSpeed = speed;
                    best = i;
                    lowestCost = cost;
                    bestSilkTouch = silkTouch;
                }
            }
        }
        return best;
    }

    /**
     * Calculate how effectively a block can be destroyed
     *
     * @param b the blockstate to be mined
     * @return A double containing the destruction ticks with the best tool
     */
    private double getBestDestructionTime(Block b) {
        ItemStack stack = player.getItem(getBestSlot(b, false, true));
        return calculateSpeedVsBlock(stack, BlockState.getFrom(b)) * avoidanceMultiplier(b);
    }

    private double avoidanceMultiplier(Block b) {
        return BlockUtils.is(b, "SIGN", "CHEST", "FURNACE", "CRAFTING_TABLE", "CRAFT_TABLE") ? 0.1 : 1;
    }

    /**
     * Calculates how long would it take to mine the specified block given the best tool
     * in this toolset is used. A negative value is returned if the specified block is unbreakable.
     *
     * @param item  the item to mine it with
     * @param state the blockstate to be mined
     * @return how long it would take in ticks
     */
    public static double calculateSpeedVsBlock(ItemStack item, BlockState state) {
        float hardness = state.getBlockHardness(null, null);
        if (hardness < 0) {
            return -1;
        }
        
        
        float speed = item.getType().getMaxDurability();
        if (speed > 1) {
            int effLevel = item.getEnchantmentLevel(Enchantment.DIG_SPEED);
            if (effLevel > 0) {
                speed += effLevel * effLevel + 1;
            }
        }

        speed /= hardness;
        /*if (state.getMaterial().isToolNotRequired() || (!item.isEmpty() && item.canHarvestBlock(state))) {
            return speed / 30;
        } else {
            return speed / 100;
        }*/
        return speed / 50;
    }

    /**
     * Calculates any modifier to breaking time based on status effects.
     *
     * @return a double to scale block breaking speed.
     */
    private double potionAmplifier() {
        double speed = 1;
        if (player.hasEffect(PotionEffectType.FAST_DIGGING)) {
            speed *= 1 + (player.getEffectAmplifier(PotionEffectType.FAST_DIGGING) + 1) * 0.2;
        }
        if (player.hasEffect(PotionEffectType.SLOW_DIGGING)) {
            switch (player.getEffectAmplifier(PotionEffectType.SLOW_DIGGING)) {
                case 0:
                    speed *= 0.3;
                    break;
                case 1:
                    speed *= 0.09;
                    break;
                case 2:
                    speed *= 0.0027; // you might think that 0.09*0.3 = 0.027 so that should be next, that would make too much sense. it's 0.0027.
                    break;
                default:
                    speed *= 0.00081;
                    break;
            }
        }
        return speed;
    }
}
