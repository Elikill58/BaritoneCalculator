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

package baritone.api.utils.pathing;

import org.bukkit.World;
import org.bukkit.WorldBorder;

/**
 * Essentially, a "rule" for the path finder, prevents proposed movements from attempting to venture
 * into the world border, and prevents actual movements from placing blocks in the world border.
 */
public class BetterWorldBorder {

    private final WorldBorder border;

    public BetterWorldBorder(WorldBorder border, World world) {
    	this.border = border;
    }

    public boolean entirelyContains(int x, int z) { // Y is 10 because we don't care about it
    	double size = border.getSize();
    	double minX = border.getCenter().getX() - size;
    	double maxX = border.getCenter().getX() + size;
    	double minZ = border.getCenter().getZ() - size;
    	double maxZ = border.getCenter().getZ() + size;
        return (minX < x && maxX > x) && (minZ < z && maxZ > z);
    }

    public boolean canPlaceAt(int x, int z) {
        // move it in 1 block on all sides
        // because we can't place a block at the very edge against a block outside the border
        // it won't let us right click it
        return entirelyContains(x, z);
    }
}
