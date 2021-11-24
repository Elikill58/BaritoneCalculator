package baritone;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;

import baritone.api.nms.PlayerContext;
import baritone.behavior.PathingBehavior;

public class Baritone {

	public static ThreadPoolExecutor threadPool;

    static {
        threadPool = new ThreadPoolExecutor(4, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
    }

    private final PathingBehavior pathingBehavior;
    private final PlayerContext ctx;

    Baritone(Player p) {
    	this.ctx = new PlayerContext(p);
        this.pathingBehavior = new PathingBehavior(this);
    }

    /**
     * Get the player context
     * 
     * @return the player context
     */
    public PlayerContext getPlayerContext() {
        return ctx;
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
    
    /**
     * Get pathing behavior for the current baritone instance
     * 
     * @return the pathing behavior
     */
    public PathingBehavior getPathingBehavior() {
        return this.pathingBehavior;
    }
}
