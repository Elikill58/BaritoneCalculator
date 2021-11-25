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

import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;

/**
 * @author Brady
 * @since 11/5/2019
 */
public final class BlockStateInterfaceAccessWrapper {

    private final BlockStateInterface bsi;

    BlockStateInterfaceAccessWrapper(BlockStateInterface bsi) {
        this.bsi = bsi;
    }

	public Fluid getFluid(BlockPosition arg0) {
		return ((CraftWorld) bsi.world).getHandle().getFluid(arg0);
	}

	public TileEntity getTileEntity(BlockPosition arg0) {
		return ((CraftWorld) bsi.world).getHandle().getTileEntity(arg0);
	}

	public IBlockData getType(BlockPosition pos) {
		return ((CraftWorld) bsi.world).getHandle().getType(pos);
	}
}
