package baritone.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import baritone.api.nms.PlayerContext;
import baritone.api.utils.BetterBlockPos;
import baritone.pathing.calc.AbstractNodeCostSearch;

public class TickEvent extends Event {
	
	private final Player player;
	private BetterBlockPos expectedPosition;
	private AbstractNodeCostSearch inProgress;
	private boolean far = false;
	
	public TickEvent(Player p) {
		this.player = p;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public PlayerContext getEntityPlayer() {
		return new PlayerContext(getPlayer());
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
