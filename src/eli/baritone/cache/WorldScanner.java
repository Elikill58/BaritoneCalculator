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

package eli.baritone.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.bukkit.Chunk;

import eli.baritone.api.cache.ICachedWorld;
import eli.baritone.api.cache.IWorldScanner;
import eli.baritone.api.nms.ChunkPos;
import eli.baritone.api.nms.block.BlockPos;
import eli.baritone.api.nms.block.BlockState;
import eli.baritone.api.utils.BetterBlockPos;
import eli.baritone.api.utils.BlockOptionalMetaLookup;
import eli.baritone.api.utils.player.PlayerContext;

public enum WorldScanner implements IWorldScanner {

    INSTANCE;

    private static final int[] DEFAULT_COORDINATE_ITERATION_ORDER = IntStream.range(0, 16).toArray();

    @Override
    public List<BlockPos> scanChunkRadius(PlayerContext ctx, BlockOptionalMetaLookup filter, int max, int yLevelThreshold, int maxSearchRadius) {
        ArrayList<BlockPos> res = new ArrayList<>();

        if (filter.blocks().isEmpty()) {
            return res;
        }

        int maxSearchRadiusSq = maxSearchRadius * maxSearchRadius;
        int playerChunkX = ctx.playerFeet().getX() >> 4;
        int playerChunkZ = ctx.playerFeet().getZ() >> 4;
        int playerY = ctx.playerFeet().getY();

        int playerYBlockStateContainerIndex = playerY >> 4;
        int[] coordinateIterationOrder = IntStream.range(0, 16).boxed().sorted(Comparator.comparingInt(y -> Math.abs(y - playerYBlockStateContainerIndex))).mapToInt(x -> x).toArray();

        int searchRadiusSq = 0;
        boolean foundWithinY = false;
        while (true) {
            boolean allUnloaded = true;
            boolean foundChunks = false;
            for (int xoff = -searchRadiusSq; xoff <= searchRadiusSq; xoff++) {
                for (int zoff = -searchRadiusSq; zoff <= searchRadiusSq; zoff++) {
                    int distance = xoff * xoff + zoff * zoff;
                    if (distance != searchRadiusSq) {
                        continue;
                    }
                    foundChunks = true;
                    int chunkX = xoff + playerChunkX;
                    int chunkZ = zoff + playerChunkZ;
                    Chunk chunk = ctx.world().getChunkAt(chunkX, chunkZ);
                    if (chunk == null) {
                        continue;
                    }
                    allUnloaded = false;
                    if (scanChunkInto(chunkX << 4, chunkZ << 4, chunk, filter, res, max, yLevelThreshold, playerY, coordinateIterationOrder)) {
                        foundWithinY = true;
                    }
                }
            }
            if ((allUnloaded && foundChunks)
                    || (res.size() >= max
                    && (searchRadiusSq > maxSearchRadiusSq || (searchRadiusSq > 1 && foundWithinY)))
            ) {
                return res;
            }
            searchRadiusSq++;
        }
    }

    @Override
    public List<BlockPos> scanChunk(PlayerContext ctx, BlockOptionalMetaLookup filter, ChunkPos pos, int max, int yLevelThreshold) {
        if (filter.blocks().isEmpty()) {
            return Collections.emptyList();
        }

        Chunk chunk = ctx.world().getChunkAt(pos.x, pos.z);
        int playerY = ctx.playerFeet().getY();

        if (chunk == null) {
            return Collections.emptyList();
        }

        ArrayList<BlockPos> res = new ArrayList<>();
        scanChunkInto(pos.x << 4, pos.z << 4, chunk, filter, res, max, yLevelThreshold, playerY, DEFAULT_COORDINATE_ITERATION_ORDER);
        return res;
    }

    @Override
    public int repack(PlayerContext ctx) {
        return this.repack(ctx, 40);
    }

    @Override
    public int repack(PlayerContext ctx, int range) {
        ICachedWorld cachedWorld = ctx.worldData().getCachedWorld();

        BetterBlockPos playerPos = ctx.playerFeet();

        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;

        int minX = playerChunkX - range;
        int minZ = playerChunkZ - range;
        int maxX = playerChunkX + range;
        int maxZ = playerChunkZ + range;

        int queued = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Chunk chunk = ctx.world().getChunkAt(x, z);

                if (chunk != null) {
                    queued++;
                    cachedWorld.queueForPacking(chunk);
                }
            }
        }

        return queued;
    }

    private boolean scanChunkInto(int chunkX, int chunkZ, Chunk chunk, BlockOptionalMetaLookup filter, Collection<BlockPos> result, int max, int yLevelThreshold, int playerY, int[] coordinateIterationOrder) {

        boolean foundWithinY = false;
        for(BlockPos pos : ChunkPacker.getLocations(chunk)) {
        	BlockState state = BlockState.getFromBlockData(pos, chunk.getWorld());//.get(pos.getX(), pos.getY(), pos.getZ());
            if (filter.has(state)) {
                int y = pos.getY();
                if (result.size() >= max) {
                    if (Math.abs(y - playerY) < yLevelThreshold) {
                        foundWithinY = true;
                    } else {
                        if (foundWithinY) {
                            // have found within Y in this chunk, so don't need to consider outside Y
                            // TODO continue iteration to one more sorted Y coordinate block
                            return true;
                        }
                    }
                }
                result.add(pos);
            }
        }
        return foundWithinY;
    }
}
