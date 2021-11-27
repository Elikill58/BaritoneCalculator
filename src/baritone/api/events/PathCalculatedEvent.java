package baritone.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import baritone.api.nms.block.BlockPos;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.player.PlayerContext;
import baritone.pathing.path.PathExecutor;

public class PathCalculatedEvent extends Event {
	
	private final PlayerContext ctx;
	private final BlockPos start;
	private final Goal goal;
	private final PathExecutor current;
	
	public PathCalculatedEvent(PlayerContext ctx, BlockPos start, Goal goal, PathExecutor current) {
		this.ctx = ctx;
		this.start = start;
		this.goal = goal;
		this.current = current;
	}
	
	public PlayerContext getContext() {
		return ctx;
	}
	
	public BlockPos getStart() {
		return start;
	}
	
	public Goal getGoal() {
		return goal;
	}
	
	public PathExecutor getCurrent() {
		return current;
	}
	
	public IPath getPath() {
		return current.getPath();
	}

	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	private static final HandlerList handlers = new HandlerList();
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
