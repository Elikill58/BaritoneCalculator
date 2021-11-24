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

import org.bukkit.Chunk;
import org.bukkit.World;

import baritone.api.nms.PlayerContext;
import baritone.api.nms.block.BlockState;

/**
 * Wraps get for chuck caching capability
 *
 * @author leijurv
 */
public class BlockStateInterface {

    protected final World world;
    private Chunk prev = null;
    private final boolean useTheRealWorld;
    private PlayerContext ctx;
    
    public BlockStateInterface(PlayerContext ctx) {
        this(ctx, false);
    }

    public BlockStateInterface(PlayerContext ctx, boolean copyLoadedChunks) {
    	this.ctx = ctx;
        this.world = ctx.world();
        this.useTheRealWorld = true;
    }

    public boolean worldContainsLoadedChunk(int blockX, int blockZ) {
        return true;
    }
    
    public PlayerContext getPlayer() {
		return ctx;
	}

    public BlockState get0(int x, int y, int z) { // Mickey resigned

        // Invalid vertical position
        if (y < 0 || y >= 256) {
            return null; // should not append
        }

        if (useTheRealWorld) {
            Chunk cached = prev;
            // there's great cache locality in block state lookups
            // generally it's within each movement
            // if it's the same chunk as last time
            // we can just skip the mc.world.getChunk lookup
            // which is a Long2ObjectOpenHashMap.get
            // see issue #113
            if (cached != null && cached.getX() == x >> 4 && cached.getZ() == z >> 4) {
                return new BlockState(cached.getBlock(x, y, z));
            }
        }
        return new BlockState(world.getBlockAt(x, y, z));
    }

    public boolean isLoaded(int x, int z) {
        Chunk prevChunk = prev;
        return (prevChunk != null && prevChunk.getX() == x >> 4 && prevChunk.getZ() == z >> 4);
    }
}
