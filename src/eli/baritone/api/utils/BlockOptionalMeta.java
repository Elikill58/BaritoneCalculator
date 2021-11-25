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

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.block.Block;

import com.google.common.collect.ImmutableSet;

import eli.baritone.api.nms.block.BlockState;

public final class BlockOptionalMeta {

    private final Block block;
    private final int meta;
    private final boolean noMeta;
    private final Set<BlockState> blockstates;
    private final ImmutableSet<Integer> stateHashes;

    public BlockOptionalMeta(@Nonnull Block block, @Nullable Integer meta) {
        this.block = block;
        this.noMeta = meta == null;
        this.meta = noMeta ? 0 : meta;
        this.blockstates = getStates(block, meta);
        this.stateHashes = getStateHashes(blockstates);
    }
	
    @Deprecated
    private static Set<BlockState> getStates(@Nonnull Block block, @Nullable Integer meta) {
        return new HashSet<>();
    }

    private static ImmutableSet<Integer> getStateHashes(Set<BlockState> blockstates) {
        return ImmutableSet.copyOf(
                blockstates.stream()
                        .map(BlockState::hashCode)
                        .toArray(Integer[]::new)
        );
    }

    public Block getBlock() {
        return block;
    }

    public Integer getMeta() {
        return noMeta ? null : meta;
    }

    public boolean matches(@Nonnull BlockState blockstate) {
        Block block = blockstate.getBlock();
        return block == this.block && stateHashes.contains(blockstate.hashCode());
    }

    @Override
    public String toString() {
        return String.format("BlockOptionalMeta{block=%s,meta=%s}", block, getMeta());
    }

    public BlockState getAnyBlockState() {
        if (blockstates.size() > 0) {
            return blockstates.iterator().next();
        }

        return null;
    }
}
