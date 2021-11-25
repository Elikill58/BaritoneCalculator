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
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import com.google.common.collect.ImmutableSet;

import eli.baritone.api.nms.block.BlockPos;
import eli.baritone.api.nms.block.BlockState;
import eli.baritone.api.utils.pathing.PathingBlockType;

public final class CachedChunk {

    public static final ImmutableSet<Material> BLOCKS_TO_KEEP_TRACK_OF = ImmutableSet.of(
    		Material.DIAMOND_BLOCK,
            //Blocks.COAL_ORE,
    		Material.COAL_BLOCK,
            //Blocks.IRON_ORE,
    		Material.IRON_BLOCK,
            //Blocks.GOLD_ORE,
    		Material.GOLD_BLOCK,
    		Material.EMERALD_ORE,
    		Material.EMERALD_BLOCK,

    		Material.ENDER_CHEST,
            Material.FURNACE,
            Material.CHEST,
            Material.TRAPPED_CHEST,
            Material.END_PORTAL,
            Material.END_PORTAL_FRAME,
            Material.BARRIER,
            Material.OBSERVER,
            Material.WHITE_SHULKER_BOX,
            Material.ORANGE_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX,
            Material.LIGHT_BLUE_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX,
            Material.LIME_SHULKER_BOX,
            Material.PINK_SHULKER_BOX,
            Material.GRAY_SHULKER_BOX,
            Material.CYAN_SHULKER_BOX,
            Material.PURPLE_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX,
            Material.BROWN_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX,
            Material.RED_SHULKER_BOX,
            Material.BLACK_SHULKER_BOX,
            Material.HOPPER,
            Material.BEACON,
            Material.BREWING_STAND,
            Material.ENCHANTING_TABLE,
            Material.ANVIL,
            Material.DRAGON_EGG,
            Material.JUKEBOX,
            Material.END_GATEWAY,
            Material.COBWEB,
            Material.NETHER_WART,
            Material.LADDER,
            Material.VINE
    );


    /**
     * The size of the chunk data in bits. Equal to 16 KiB.
     * <p>
     * Chunks are 16x16x256, each block requires 2 bits.
     */
    public static final int SIZE = 2 * 16 * 16 * 256;

    /**
     * The size of the chunk data in bytes. Equal to 16 KiB.
     */
    public static final int SIZE_IN_BYTES = SIZE / 8;

    /**
     * The chunk x coordinate
     */
    public final int x;

    /**
     * The chunk z coordinate
     */
    public final int z;

    /**
     * The actual raw data of this packed chunk.
     * <p>
     * Each block is expressed as 2 bits giving a total of 16 KiB
     */
    private final BitSet data;

    private final Int2ObjectOpenHashMap<String> special;

    /**
     * The block names of each surface level block for generating an overview
     */
    private final BlockState[] overview;

    private final int[] heightMap;

    private final Map<String, List<BlockPos>> specialBlockLocations;

    public final long cacheTimestamp;

    CachedChunk(int x, int z, BitSet data, BlockState[] overview, Map<String, List<BlockPos>> specialBlockLocations, long cacheTimestamp) {
        validateSize(data);

        this.x = x;
        this.z = z;
        this.data = data;
        this.overview = overview;
        this.heightMap = new int[256];
        this.specialBlockLocations = specialBlockLocations;
        this.cacheTimestamp = cacheTimestamp;
        if (specialBlockLocations.isEmpty()) {
            this.special = null;
        } else {
            this.special = new Int2ObjectOpenHashMap<>();
            setSpecial();
        }
        calculateHeightMap();
    }

    private final void setSpecial() {
        for (Map.Entry<String, List<BlockPos>> entry : specialBlockLocations.entrySet()) {
            for (BlockPos pos : entry.getValue()) {
                special.put(getPositionIndex(pos.getX(), pos.getY(), pos.getZ()), entry.getKey());
            }
        }
    }

    public final BlockState getBlock(int x, int y, int z, int dimension) {
        int index = getPositionIndex(x, y, z);
        PathingBlockType type = getType(index);
        int internalPos = z << 4 | x;
        if (heightMap[internalPos] == y && type != PathingBlockType.AVOID) {
            // if the top block in a column is water, we cache it as AVOID but we don't want to just return default state water (which is not flowing) beacuse then it would try to path through it

            // we have this exact block, it's a surface block
            /*System.out.println("Saying that " + x + "," + y + "," + z + " is " + state);
            if (!Minecraft.getMinecraft().world.getBlockState(new BlockPos(x + this.x * 16, y, z + this.z * 16)).getBlock().equals(state.getBlock())) {
                throw new IllegalStateException("failed " + Minecraft.getMinecraft().world.getBlockState(new BlockPos(x + this.x * 16, y, z + this.z * 16)).getBlock() + " " + state.getBlock() + " " + (x + this.x * 16) + " " + y + " " + (z + this.z * 16));
            }*/
            return overview[internalPos];
        }

        if (type == PathingBlockType.SOLID) {
            if (y == 127 && dimension == -1) {
                // nether roof is always unbreakable
                return BlockState.getFromType(Material.BEDROCK, x, y, z);
            }
            if (y < 5 && dimension == 0) {
                // solid blocks below 5 are commonly bedrock
                // however, returning bedrock always would be a little yikes
                // discourage paths that include breaking blocks below 5 a little more heavily just so that it takes paths breaking what's known to be stone (at 5 or above) instead of what could maybe be bedrock (below 5)
                return BlockState.getFromType(Material.OBSIDIAN, x, y, z);
            }
        }
        return ChunkPacker.pathingTypeToBlock(type, dimension);
    }

    private PathingBlockType getType(int index) {
        return PathingBlockType.fromBits(data.get(index), data.get(index + 1));
    }

    private void calculateHeightMap() {
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int index = z << 4 | x;
                heightMap[index] = 0;
                for (int y = 256; y >= 0; y--) {
                    int i = getPositionIndex(x, y, z);
                    if (data.get(i) || data.get(i + 1)) {
                        heightMap[index] = y;
                        break;
                    }
                }
            }
        }
    }

    public final BlockState[] getOverview() {
        return overview;
    }

    public final Map<String, List<BlockPos>> getRelativeBlocks() {
        return specialBlockLocations;
    }

    public final ArrayList<BlockPos> getAbsoluteBlocks(String blockType) {
        if (specialBlockLocations.get(blockType) == null) {
            return null;
        }
        ArrayList<BlockPos> res = new ArrayList<>();
        for (BlockPos pos : specialBlockLocations.get(blockType)) {
            res.add(new BlockPos(pos.getX() + x * 16, pos.getY(), pos.getZ() + z * 16));
        }
        return res;
    }

    /**
     * @return Returns the raw packed chunk data as a byte array
     */
    public final byte[] toByteArray() {
        return this.data.toByteArray();
    }

    /**
     * Returns the raw bit index of the specified position
     *
     * @param x The x position
     * @param y The y position
     * @param z The z position
     * @return The bit index
     */
    public static int getPositionIndex(int x, int y, int z) {
        return (x << 1) | (z << 5) | (y << 9);
    }

    /**
     * Validates the size of an input {@link BitSet} containing the raw
     * packed chunk data. Sizes that exceed {@link CachedChunk#SIZE} are
     * considered invalid, and thus, an exception will be thrown.
     *
     * @param data The raw data
     * @throws IllegalArgumentException if the bitset size exceeds the maximum size
     */
    private static void validateSize(BitSet data) {
        if (data.size() > SIZE) {
            throw new IllegalArgumentException("BitSet of invalid length provided");
        }
    }
}
