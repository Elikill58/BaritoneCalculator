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

package eli.baritone;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;

import eli.baritone.api.BaritoneAPI;
import eli.baritone.api.Settings;
import eli.baritone.api.utils.PathingControlManager;
import eli.baritone.api.utils.player.PlayerContext;
import eli.baritone.behavior.PathingBehavior;
import eli.baritone.cache.WorldProvider;
import eli.baritone.process.CustomGoalProcess;
import eli.com.elikill58.events.InputHandler;
import eli.com.elikill58.plugin.EliPlugin;

/**
 * @author Brady
 * @since 7/31/2018
 */
public class Baritone {

    private static ThreadPoolExecutor threadPool;
    private static File dir;

    static {
        threadPool = new ThreadPoolExecutor(4, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());

        dir = new File(EliPlugin.getInstance().getDataFolder(), "baritone");
        if (!Files.exists(dir.toPath())) {
            try {
                Files.createDirectories(dir.toPath());
            } catch (IOException ignored) {}
        }
    }
    
    private CustomGoalProcess customGoalProcess;

    private PlayerContext playerContext;
    private WorldProvider worldProvider;
    private PathingBehavior pathingBehavior;
    private PathingControlManager pathingControlManager;

    Baritone(Player p) {
    	this.playerContext = new PlayerContext(p);
        this.pathingBehavior = new PathingBehavior(this);
        this.worldProvider = new WorldProvider();

        this.pathingControlManager = new PathingControlManager(this);
        this.pathingControlManager.registerProcess(customGoalProcess = new CustomGoalProcess(this)); // very high iq
    }

    public PathingControlManager getPathingControlManager() {
        return this.pathingControlManager;
    }

    public CustomGoalProcess getCustomGoalProcess() {
        return this.customGoalProcess;
    }

    public PlayerContext getPlayerContext() {
        return this.playerContext;
    }

    public WorldProvider getWorldProvider() {
        return this.worldProvider;
    }

    public static Settings settings() {
        return BaritoneAPI.getSettings();
    }
    
    /**
     * Run this runnable right now, in current thread
     * 
     * @param run what should be runned
     */
    public static void execute(Runnable run) {
    	execute(run, false);
    }
    
    /**
     * Run this runnable async (and so, with few tick of late) or right now
     * 
     * @param run what should be runned
     * @param runAsync true if should be runned async
     */
    public static void execute(Runnable run, boolean runAsync) {
    	if(runAsync)
    		threadPool.execute(run);
    	else
    		run.run(); // don't use thread now
    }
    
    public PathingBehavior getPathingBehavior() {
        return this.pathingBehavior;
    }

	public InputHandler getInputOverrideHandler() {
		return new InputHandler();
	}
}