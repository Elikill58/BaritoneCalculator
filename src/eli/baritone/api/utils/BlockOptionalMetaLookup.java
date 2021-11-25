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

import java.util.Arrays;
import java.util.List;

import eli.baritone.api.nms.block.BlockState;

public class BlockOptionalMetaLookup {

    private final BlockOptionalMeta[] boms;

    public BlockOptionalMetaLookup(BlockOptionalMeta... boms) {
        this.boms = boms;
    }

    public boolean has(BlockState state) {
        for (BlockOptionalMeta bom : boms) {
            if (bom.matches(state)) {
                return true;
            }
        }

        return false;
    }

    public List<BlockOptionalMeta> blocks() {
        return Arrays.asList(boms);
    }

    @Override
    public String toString() {
        return String.format(
                "BlockOptionalMetaLookup{%s}",
                Arrays.toString(boms)
        );
    }
}
