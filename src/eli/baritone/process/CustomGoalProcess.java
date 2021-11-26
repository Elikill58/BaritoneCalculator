package eli.baritone.process;

import eli.baritone.Baritone;
import eli.baritone.api.BaritoneAPI;
import eli.baritone.api.pathing.goals.Goal;
import eli.baritone.api.pathing.goals.GoalBlock;
import eli.baritone.api.process.PathingCommand;
import eli.baritone.api.process.PathingCommandType;
import eli.baritone.utils.BaritoneProcessHelper;

public final class CustomGoalProcess extends BaritoneProcessHelper {

    /**
     * Default priority. Most normal processes should have this value.
     * <p>
     * Some examples of processes that should have different values might include some kind of automated mob avoidance
     * that would be temporary and would forcefully take control. Same for something that pauses pathing for auto eat, etc.
     * <p>
     * The value is -1 beacuse that's what Impact 4.5's beta auto walk returns and I want to tie with it.
     */
    double DEFAULT_PRIORITY = -1;
    
    /**
     * The current goal
     */
    private Goal goal;

    /**
     * The current process state.
     *
     * @see State
     */
    private State state;

    public CustomGoalProcess(Baritone baritone) {
        super(baritone);
    }

    public void setGoal(Goal goal) {
        this.goal = goal;
        if (this.state == State.NONE) {
            this.state = State.GOAL_SET;
        }
        if (this.state == State.EXECUTING) {
            this.state = State.PATH_REQUESTED;
        }
    }

    public void path() {
        this.state = State.PATH_REQUESTED;
    }

    public Goal getGoal() {
        return this.goal;
    }

    public boolean isActive() {
        return this.state != State.NONE;
    }

    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        switch (this.state) {
            case GOAL_SET:
                return new PathingCommand(this.goal, PathingCommandType.CANCEL_AND_SET_GOAL);
            case PATH_REQUESTED:
                // return FORCE_REVALIDATE_GOAL_AND_PATH just once
                PathingCommand ret = new PathingCommand(this.goal, PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH);
                this.state = State.EXECUTING;
                return ret;
            case EXECUTING:
                if (calcFailed) {
                    onLostControl();
                    return new PathingCommand(this.goal, PathingCommandType.CANCEL_AND_SET_GOAL);
                }
                if (this.goal == null || (this.goal.isInGoal(ctx.playerFeetBlockPos()) && this.goal.isInGoal(baritone.getPathingBehavior().pathStart()))) {
                    onLostControl(); // we're there xd
                    if (Baritone.settings().desktopNotifications.value && Baritone.settings().notificationOnPathComplete.value) {
                    	BaritoneAPI.debug("Done");
                    }
                    return new PathingCommand(this.goal, PathingCommandType.CANCEL_AND_SET_GOAL);
                }
                return new PathingCommand(this.goal, PathingCommandType.SET_GOAL_AND_PATH);
            default:
                throw new IllegalStateException();
        }
    }

    public void onLostControl() {
        this.state = State.NONE;
        this.goal = null;
    }

    public String displayName() {
        if (!isActive()) {
            // i love it when impcat's scuffed HUD calls displayName for inactive processes for 1 tick too long
            // causing NPEs when the displayname relies on fields that become null when inactive
            return "INACTIVE";
        }
        return displayName0();
    }

    public String displayName0() {
        return "Custom Goal " + this.goal;
    }
    
    public double priority() {
        return DEFAULT_PRIORITY;
    }

    protected enum State {
        NONE,
        GOAL_SET,
        PATH_REQUESTED,
        EXECUTING
    }

	public void setGoalAndPath(GoalBlock goal) {
		this.setGoal(goal);
		this.path();
	}
}
