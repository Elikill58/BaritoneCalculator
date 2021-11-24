package baritone.api.nms;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class NmsUtils {

	public static final String VERSION = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",")
			.split(",")[3];
	
	public static Object getEntityPlayer(Player p) {
		try {
			Object craftPlayer = Class.forName("org.bukkit.craftbukkit." + VERSION + ".entity.CraftPlayer").cast(p);
			return craftPlayer.getClass().getMethod("getHandle").invoke(craftPlayer);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
