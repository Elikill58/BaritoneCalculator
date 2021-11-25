package eli.com.elikill58.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import eli.baritone.api.BaritoneAPI;
import eli.baritone.api.utils.BetterBlockPos;
import eli.baritone.api.utils.player.PlayerContext;
import eli.baritone.pathing.calc.AbstractNodeCostSearch;
import eli.baritone.pathing.path.PathExecutor;

public class TickEvent extends Event {
	
	private final Player player;
	private BetterBlockPos expectedPosition;
	private AbstractNodeCostSearch inProgress;
	private PathExecutor pathExecutor;
	private boolean far = false;
	
	public TickEvent(Player p) {
		this.player = p;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public PlayerContext getEntityPlayer() {
		return BaritoneAPI.getProvider().getBaritone(player).getPlayerContext();
	}

	public BetterBlockPos getExpectedPosition() {
		return expectedPosition;
	}

	public void setExpectedPosition(BetterBlockPos expectedPosition) {
		this.expectedPosition = expectedPosition;
	}
	
	public void setInProgress(AbstractNodeCostSearch inProgress) {
		this.inProgress = inProgress;
	}
	
	public AbstractNodeCostSearch getInProgress() {
		return inProgress;
	}
	
	public PathExecutor getPathExecutor() {
		return pathExecutor;
	}
	
	public void setPathExecutor(PathExecutor pathExecutor) {
		this.pathExecutor = pathExecutor;
	}
	
	public boolean isFar() {
		return far;
	}
	
	public void setFar(boolean far) {
		this.far = far;
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
