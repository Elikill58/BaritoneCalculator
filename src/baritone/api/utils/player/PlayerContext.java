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

package baritone.api.utils.player;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import baritone.api.BaritoneAPI;
import baritone.api.nms.Vec3d;
import baritone.api.nms.block.BlockPos;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.api.utils.Rotation;
import baritone.cache.WorldData;

public class PlayerContext implements Helper {

	private final Player p;
	
    public PlayerContext(Player p) {
    	this.p = p;
    }
    
    public Player getPlayer() {
		return p;
	}

    public PlayerContext player() {
        return this;
    }
    
    public boolean onGround() {
    	return player().isOnGround();
    }

    public World world() {
        return player().getWorld();
    }

    public WorldData worldData() {
        return BaritoneAPI.getProvider().getBaritone(p).getWorldProvider().getCurrentWorld(); // TODO improve this !
    }

	public boolean isLookingAt(BlockPos blockPos) {
		return false;
	}

	public Location playerFeetLocation() {
        // TODO find a better way to deal with soul sand!!!!!
        return new Location(p.getWorld(), player().locX(), player().locY() + 0.1251, player().locZ());
    }

	public BetterBlockPos playerFeet() {
        return playerFeetBlockPos();
    }

	public BetterBlockPos playerFeetBlockPos() {
        return new BetterBlockPos(player().locX(), player().locY() + 0.1251, player().locZ());
    }

	public Vec3d playerHead() {
        return new Vec3d(player().locX(), player().locY() + player().getHeadHeight(), player().locZ());
    }

	public Rotation playerRotations() {
        return new Rotation(player().getYaw(), player().getPitch());
    }

	public double motionX() {
		return player().getPositionVector().x;
	}

	public double motionY() {
		return player().getPositionVector().y;
	}

	public double motionZ() {
		return player().getPositionVector().z;
	}
	
	public double getBlockReachDistance() {
        return player().isCreative() ? 5.0F : BaritoneAPI.getSettings().blockReachDistance.value;
    }
	
	public boolean isOnGround() {
		return ((Entity) p).isOnGround();
	}
	
	public boolean isSneaking() {
		return p.isSneaking();
	}
	
	public double locX() {
		return p.getLocation().getX();
	}
	
	public double locY() {
		return p.getLocation().getY();
	}
	
	public double locZ() {
		return p.getLocation().getZ();
	}
	
	public double getFoodLevel() {
		return p.getFoodLevel();
	}

	public Vec3d getPositionVector() {
		return new Vec3d(p.getLocation());
	}

	public World getWorld() {
		return p.getWorld();
	}
	
	public float getYaw() {
		return p.getLocation().getYaw();
	}
	
	public float getPitch() {
		return p.getLocation().getPitch();
	}
	
	public float getHeadHeight() {
		return (float) p.getEyeHeight();
	}

	public boolean isCreative() {
		return p.getGameMode().equals(GameMode.CREATIVE);
	}

	public void setSprinting(boolean b) {
		p.setSprinting(b);
	}

	public boolean hasEffect(PotionEffectType effect) {
		return p.hasPotionEffect(effect);
	}
	
	public int getEffectAmplifier(PotionEffectType effect) {
		return p.getActivePotionEffects().stream().filter((e) -> e.getType().equals(effect)).findFirst().get().getAmplifier();
	}
	
	public boolean getCollides() {
		return p.isCollidable();
	}
	
	public int getItemInHandIndex() {
		return p.getInventory().getHeldItemSlot();
	}
	
	public int getSlot(ItemStack item) {
		return p.getInventory().first(item);
	}

	public ItemStack getItem(int bestSlot) {
		ItemStack item = p.getInventory().getItem(bestSlot);
		return item == null ? new ItemStack(Material.AIR) : item;
	}
}
