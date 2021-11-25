package eli.baritone.api.nms;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class NmsUtils {

	public static final String VERSION = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",")
			.split(",")[3];
	private static final String NMS_PREFIX;
	private static Class<?> CRAFT_PLAYER_CLASS;
	
	/**
	 * This Map is to reduce Reflection action which take more ressources than just RAM action
	 */
	private static final HashMap<String, Class<?>> ALL_CLASS = new HashMap<>();
	
	static {
		NMS_PREFIX = Version.getVersion(VERSION).isNewerOrEquals(Version.V1_17) ? "net.minecraft." : "net.minecraft.server." + VERSION + ".";
		try {
			CRAFT_PLAYER_CLASS = Class.forName("org.bukkit.craftbukkit." + VERSION + ".entity.CraftPlayer");
			//PACKET_CLASS = Class.forName("net.minecraft.server." + VERSION + ".Packet");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This method should NOT be used. It's only when develop, and you don't know what is the prefix and easier find all not-prefixed class.
	 * 
	 * @param name the class name
	 * @return a class
	 */
	public static Class<?> getNmsClass(String name){
		return getNmsClass(name, "");
	}
	
	/**
	 * Get the Class in NMS, with a processing reducer
	 * 
	 * @param name of the NMS class (in net.minecraft.server package ONLY, because it's NMS)
	 * @return clazz the searched class
	 */
	public static Class<?> getNmsClass(String name, String packagePrefix){
		if(ALL_CLASS.containsKey(name))
			return ALL_CLASS.get(name);
		try {
			Class<?> clazz = Class.forName(NMS_PREFIX + (Version.getVersion(VERSION).isNewerOrEquals(Version.V1_17) ? packagePrefix : "") + name);
			ALL_CLASS.put(name, clazz);
			return clazz;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static Object getEntityPlayer(Player p) {
		try {
			Object craftPlayer = CRAFT_PLAYER_CLASS.cast(p);
			return craftPlayer.getClass().getMethod("getHandle").invoke(craftPlayer);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
