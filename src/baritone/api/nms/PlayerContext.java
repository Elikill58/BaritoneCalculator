package baritone.api.nms;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import baritone.api.utils.BetterBlockPos;

public class PlayerContext {

	private final Player p;
	private final Object obj;
	
	public PlayerContext(Player p) {
		this(p, NmsUtils.getEntityPlayer(p));
	}
	
	public PlayerContext(Player p, Object ep) {
		this.obj = ep;
		this.p = p;
	}
	
	/**
	 * Get bukkit player
	 * 
	 * @return bukkit player
	 */
	public Player getPlayer() {
		return p;
	}
	
	/**
	 * Know if this player is on ground
	 * 
	 * @return true if on ground
	 */
	public boolean isOnGround() {
		return ((Entity) p).isOnGround(); // cast to prevent deprecated
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
		return new Vec3d(ReflectionUtils.invoke(obj, "getPositionVector"));
	}
	
	public World world() {
		return getWorld();
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
		return ReflectionUtils.field(obj, "collides");
	}

	@SuppressWarnings("deprecation")
	public ItemStack getItemInHand() {
		return p.getItemInHand() == null ? new ItemStack(Material.AIR) : p.getItemInHand();
	}

	public BetterBlockPos playerFeet() {
        // TODO find a better way to deal with soul sand!!!!!
        return new BetterBlockPos(locX(), locY() + 0.1251, locZ());
    }

	public double motionY() {
		return getPositionVector().y;
	}
	
	public double getBlockReachDistance() {
        return isCreative() ? 5.0F : 4.5;
    }

	public ItemStack getItem(int bestSlot) {
		return p.getInventory().getItem(bestSlot);
	}
	
	public boolean isAllowedFly() {
		return isCreative() || p.getAllowFlight();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof PlayerContext) {
			PlayerContext o = (PlayerContext) obj;
			return o.p.getUniqueId().equals(p.getUniqueId());
		}
		return super.equals(obj);
	}
}
