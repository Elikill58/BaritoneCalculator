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

package baritone.api;

import org.bukkit.plugin.java.JavaPlugin;

import baritone.BaritoneProvider;

/**
 * Exposes the {@link IBaritoneProvider} instance and the {@link Settings} instance for API usage.
 *
 * @author Brady
 * @since 9/23/2018
 */
public final class BaritoneAPI {

    private static final BaritoneProvider PROVIDER = new BaritoneProvider();
    private static final Settings SETTINGS = new Settings();
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
    		System.out.println("[ERR - BaritoneAPI] Failed to start debugging: no Plugin set.");
    	} else
    		debug = b;
    }
    
    public static boolean isDebug() {
		return debug;
	}
    
    public static void debug(String msg) {
    	if(plugin != null && debug)
    		plugin.getLogger().info("[Debug-Baritone] " + msg);
    }

    public static Settings getSettings() {
        return BaritoneAPI.SETTINGS;
    }
    
    public static JavaPlugin getPlugin() {
		return plugin;
	}
}
