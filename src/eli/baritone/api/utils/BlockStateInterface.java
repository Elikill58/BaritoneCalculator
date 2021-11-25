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

import java.util.HashMap;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;

import eli.baritone.Baritone;
import eli.baritone.api.nms.ChunkPos;
import eli.baritone.api.nms.block.BlockPos;
import eli.baritone.api.nms.block.BlockState;
import eli.baritone.api.utils.player.PlayerContext;
import eli.baritone.cache.CachedRegion;
import eli.baritone.cache.WorldData;

/**
 * Wraps get for chuck caching capability
 *
 * @author leijurv
 */
public class BlockStateInterface {

    private final HashMap<Long, Chunk> loadedChunks = new HashMap<>();
    private final WorldData worldData;
    protected final World world;
    public final BlockPos.MutableBlockPos isPassableBlockPos;

    private Chunk prev = null;
    private CachedRegion prevCached = null;

    private final boolean useTheRealWorld;

    public BlockStateInterface(PlayerContext ctx) {
        this(ctx, false);
    }

    public BlockStateInterface(PlayerContext ctx, boolean copyLoadedChunks) {
        this(ctx.world(), ctx.worldData(), copyLoadedChunks);
    }

    public BlockStateInterface(World world, WorldData worldData, boolean copyLoadedChunks) {
        this.world = world;
        this.worldData = worldData;
        for(Chunk c : world.getLoadedChunks()) {
        	this.loadedChunks.put(getLong(c.getX(), c.getZ()), c);
        }
        this.useTheRealWorld = !Baritone.settings().pathThroughCachedOnly.value;
        this.isPassableBlockPos = new BlockPos.MutableBlockPos();
    }

    private long getLong(int blockX, int blockZ) {
    	return ChunkPos.asLong(blockX >> 4, blockZ >> 4);
    }
    
    public boolean worldContainsLoadedChunk(int blockX, int blockZ) {
        return loadedChunks.containsKey(getLong(blockX >> 4, blockZ >> 4));
    }

    public static Block getBlock(PlayerContext ctx, BlockPos pos) { // won't be called from the pathing thread because the pathing thread doesn't make a single blockpos pog
        return get(ctx, pos).getBlock();
    }

    public static BlockState get(PlayerContext ctx, BlockPos pos) {
        return new BlockStateInterface(ctx).get0(pos.getX(), pos.getY(), pos.getZ()); // immense iq
        // can't just do world().get because that doesn't work for out of bounds
        // and toBreak and stuff fails when the movement is instantiated out of load range but it's not able to BlockStateInterface.get what it's going to walk on
    }

    public BlockState get0(int x, int y, int z) { // Mickey resigned

        // Invalid vertical position
        if (y < 0 || y >= 256) {
            return BlockState.getFrom(new BlockPos(x, y, z), world);
        }

        if (useTheRealWorld) {
            Chunk cached = prev;
            BlockPos blockPos = new BlockPos(x, y, z);
            // there's great cache locality in block state lookups
            // generally it's within each movement
            // if it's the same chunk as last time
            // we can just skip the mc.world.getChunk lookup
            // which is a Long2ObjectOpenHashMap.get
            // see issue #113
            if (cached != null && cached.getX() == x >> 4 && cached.getZ() == z >> 4) {
                return BlockState.getFrom(blockPos, cached.getWorld());
            }
            Chunk chunk = loadedChunks.get(ChunkPos.asLong(x >> 4, z >> 4));

            if (chunk != null && chunk.isLoaded()) {
                prev = chunk;
                return BlockState.getFrom(blockPos, chunk.getWorld());
            }
        }
        // same idea here, skip the Long2ObjectOpenHashMap.get if at all possible
        // except here, it's 512x512 tiles instead of 16x16, so even better repetition
        CachedRegion cached = prevCached;
        if (cached == null || cached.getX() != x >> 9 || cached.getZ() != z >> 9) {
            if (worldData == null) {
                return BlockState.getFrom(world.getBlockAt(x, y, z));
            }
            CachedRegion region = worldData.cache.getRegion(x >> 9, z >> 9);
            if (region == null) {
                return BlockState.getFrom(world.getBlockAt(x, y, z));
            }
            prevCached = region;
            cached = region;
        }
        BlockState type = cached.getBlock(x & 511, y, z & 511);
        if (type == null) {
            return BlockState.getFrom(world.getBlockAt(x, y, z));
        }
        return type;
    }

    public boolean isLoaded(int x, int z) {
        Chunk prevChunk = prev;
        if(prev != null && prev.isLoaded())
        	return true;
        if (prevChunk != null && prevChunk.getX() == x >> 4 && prevChunk.getZ() == z >> 4) {
            return true;
        }
        prevChunk = loadedChunks.get(ChunkPos.asLong(x >> 4, z >> 4));
        if (prevChunk != null && prevChunk.isLoaded()) {
            prev = prevChunk;
            return true;
        }
        CachedRegion prevRegion = prevCached;
        if (prevRegion != null && prevRegion.getX() == x >> 9 && prevRegion.getZ() == z >> 9) {
            return prevRegion.isCached(x & 511, z & 511);
        }
        if (worldData == null) {
            return false;
        }
        prevRegion = worldData.cache.getRegion(x >> 9, z >> 9);
        if (prevRegion == null) {
            return false;
        }
        prevCached = prevRegion;
        return prevRegion.isCached(x & 511, z & 511);
    }
}
