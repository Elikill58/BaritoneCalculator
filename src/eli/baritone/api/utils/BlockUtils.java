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

package eli.baritone.api.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public class BlockUtils {
	
	public static boolean is(Block b, String... name) {
		return is(b.getType(), name);
	}
	
	public static boolean is(Material type, String... name) {
		String blockName = type.name();
		for(String s : name)
			if(blockName.contains(s))
				return true;
		return false;
	}

    public static String blockToString(Block block) {
        return block.toString();
    }

    public static BlockData stringToBlockRequired(String name) {
        BlockData block = stringToBlockNullable(name);

        if (block == null) {
            throw new IllegalArgumentException(String.format("Invalid block name %s", name));
        }

        return block;
    }

    public static BlockData stringToBlockNullable(String name) {
    	return Bukkit.createBlockData(name);
    	//return ((CraftBlockData) Bukkit.createBlockData(name)).getState().getBlock();
    	/*
        // do NOT just replace this with a computeWithAbsent, it isn't thread safe
        Block block = resourceCache.get(name); // map is never mutated in place so this is safe
        if (block != null) {
            return block;
        }
        if (resourceCache.containsKey(name)) {
            return null; // cached as null
        }
        block = Block.getBlockFromName(name.contains(":") ? name : "minecraft:" + name);
        Map<String, Block> copy = new HashMap<>(resourceCache); // read only copy is safe, wont throw concurrentmodification
        copy.put(name, block);
        resourceCache = copy;
        return block;*/
    }

    private BlockUtils() {}
}
