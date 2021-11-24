package baritone.api;

import org.bukkit.plugin.java.JavaPlugin;

import baritone.BaritoneProvider;

public final class BaritoneAPI {

    private static final BaritoneProvider PROVIDER = new BaritoneProvider();

    private static JavaPlugin plugin;
    private static boolean debug = false;
    
    public static BaritoneProvider getProvider() {
        return BaritoneAPI.PROVIDER;
    }
    
    public static void setPlugin(JavaPlugin pl) {
    	plugin = pl;
    }
    
    public static void setDebug(boolean b) {
    	if(b && plugin == null) {
    		System.out.println("[ERR - BaritoneAPI] Failed to start debugging. No Plugin set.");
    	} else
    		debug = b;
    }
    
    public static boolean isDebug() {
		return debug;
	}
    
    public static void debug(String msg) {
    	if(plugin != null && debug)
    		plugin.getLogger().info("[Debug] " + msg);
    }
}
