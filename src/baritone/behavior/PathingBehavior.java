package baritone.behavior;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import baritone.Baritone;
import baritone.api.events.PathEvent;
import baritone.api.events.TickEvent;
import baritone.api.nms.PlayerContext;
import baritone.api.nms.block.BlockPos;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.api.utils.PathCalculationResult;
import baritone.api.utils.pathing.Favoring;
import baritone.pathing.calc.AStarPathFinder;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.path.PathExecutor;

public final class PathingBehavior implements Helper, Listener {

	private PathExecutor current;
	private PathExecutor next;
	private Goal goal;
	private CalculationContext context;
	private boolean safeToCancel;
	private boolean pauseRequestedLastTick;
	private boolean pausedThisTick;
	private boolean cancelRequested;
	private volatile AbstractNodeCostSearch inProgress;
	private final Object pathCalcLock = new Object();
	private final Object pathPlanLock = new Object();
	private BetterBlockPos expectedSegmentStart;
	private final Baritone baritone;
	private final PlayerContext ctx;

	private final LinkedBlockingQueue<PathEvent> toDispatch = new LinkedBlockingQueue<>();

	public PathingBehavior(Baritone baritone) {
		this.ctx = baritone.getPlayerContext();
		this.baritone = baritone;
	}
	
	public Baritone getBaritone() {
		return baritone;
	}

	private void queuePathEvent(PathEvent event) {
		toDispatch.add(event);
	}

	private void dispatchEvents() {
		ArrayList<PathEvent> curr = new ArrayList<>();
		toDispatch.drainTo(curr);
	}

	public void onTick(TickEvent e) {
		dispatchEvents();
		expectedSegmentStart = pathStart();
		tickPath(e);
		dispatchEvents();
		e.setExpectedPosition(expectedSegmentStart);
		e.setInProgress(inProgress);
		logDebug("Tick: " + (expectedSegmentStart == null ? "no" : expectedSegmentStart.toString())
				+ ", current: " + (current == null ? "no current" : current.toString()));
	}

	private void tickPath(TickEvent e) {
		PlayerContext ctx = e.getEntityPlayer();
		pausedThisTick = false;
		if (pauseRequestedLastTick && safeToCancel) {
			pauseRequestedLastTick = false;
			pausedThisTick = true;
			return;
		}
		if (cancelRequested) {
			cancelRequested = false;
		}
		// logDebug("Checking tick path");
		synchronized (pathPlanLock) {
			synchronized (pathCalcLock) {
				if (inProgress != null) {
					// we are calculating
					// are we calculating the right thing though? 🤔
					BetterBlockPos calcFrom = inProgress.getStart();
					Optional<IPath> currentBest = inProgress.bestPathSoFar();
					if ((current == null || !current.getPath().getDest().equals(calcFrom))
							&& !calcFrom.equals(ctx.playerFeet()) && !calcFrom.equals(expectedSegmentStart)
							&& (!currentBest.isPresent() || (!currentBest.get().positions().contains(ctx.playerFeet())
									&& !currentBest.get().positions().contains(expectedSegmentStart))) // if
					) {
						// when it was *just* started, currentBest will be empty so we need to also
						// check calcFrom since that's always present
						inProgress.cancel(); // cancellation doesn't dispatch any events
					}
				}
			}
			if (current == null) {
				logDebug("No current");
				return;
			}
			safeToCancel = current.onTick(e);
			// logDebug("Current: failed/finished " + current.failed() + "/" +
			// current.finished());
			if (current.failed() || current.finished()) {
				current = null;
				if (goal == null || goal.isInGoal(ctx.playerFeet())) {
					logDebug("All done. At " + goal);
					queuePathEvent(PathEvent.AT_GOAL);
					next = null;
					return;
				}
				if (next != null && !next.getPath().positions().contains(ctx.playerFeet())
						&& !next.getPath().positions().contains(expectedSegmentStart)) { // can contain either one
					// if the current path failed, we may not actually be on the next one, so make
					// sure
					logDebug("Discarding next path as it does not contain current position");
					// for example if we had a nicely planned ahead path that starts where current
					// ends
					// that's all fine and good
					// but if we fail in the middle of current
					// we're nowhere close to our planned ahead path
					// so need to discard it sadly.
					queuePathEvent(PathEvent.DISCARD_NEXT);
					next = null;
				}
				if (next != null) {
					logDebug("Continuing on to planned next path");
					queuePathEvent(PathEvent.CONTINUING_ONTO_PLANNED_NEXT);
					current = next;
					next = null;
					current.onTick(e); // don't waste a tick doing nothing, get started right away
					return;
				}
				// at this point, current just ended, but we aren't in the goal and have no plan
				// for the future
				synchronized (pathCalcLock) {
					if (inProgress != null) {
						queuePathEvent(PathEvent.PATH_FINISHED_NEXT_STILL_CALCULATING);
						return;
					}
					// we aren't calculating
					queuePathEvent(PathEvent.CALC_STARTED);
					findPathInNewThread(expectedSegmentStart, true, context);
				}
				return;
			}
			// at this point, we know current is in progress
			if (safeToCancel && next != null && next.snipsnapifpossible()) {
				// a movement just ended; jump directly onto the next path
				logDebug("Splicing into planned next path early...");
				queuePathEvent(PathEvent.SPLICING_ONTO_NEXT_EARLY);
				current = next;
				next = null;
				current.onTick(e);
				return;
			}
			current = current.trySplice(next);
			if (next != null && current.getPath().getDest().equals(next.getPath().getDest())) {
				next = null;
			}
			synchronized (pathCalcLock) {
				if (inProgress != null) {
					// if we aren't calculating right now
					return;
				}
				if (next != null) {
					// and we have no plan for what to do next
					return;
				}
				if (goal == null || goal.isInGoal(current.getPath().getDest())) {
					// and this path doesn't get us all the way there
					return;
				}
				if (ticksRemainingInSegment(false).get() < 150) {
					// and this path has 7.5 seconds or less left
					// don't include the current movement so a very long last movement (e.g.
					// descend) doesn't trip it up
					// if we actually included current, it wouldn't start planning ahead until the
					// last movement was done, if the last movement took more than 7.5 seconds on
					// its own
					logDebug("Path almost over. Planning ahead...");
					queuePathEvent(PathEvent.NEXT_SEGMENT_CALC_STARTED);
					findPathInNewThread(current.getPath().getDest(), false, context);
				}
			}
		}
	}

	public Goal getGoal() {
		return goal;
	}

	public boolean isPathing() {
		return hasPath() && !pausedThisTick;
	}

	public PathExecutor getCurrent() {
		return current;
	}

	public PathExecutor getNext() {
		return next;
	}

	public Optional<AbstractNodeCostSearch> getInProgress() {
		return Optional.ofNullable(inProgress);
	}

	public boolean isSafeToCancel() {
		return current == null || safeToCancel;
	}

	public CalculationContext secretInternalGetCalculationContext() {
		return context;
	}

	/**
	 * See issue #209
	 *
	 * @return The starting {@link BlockPos} for a new path
	 */
	public BetterBlockPos pathStart() { // TODO move to a helper or util class
		BetterBlockPos feet = ctx.playerFeet();
		if (!MovementHelper.canWalkOn(ctx, feet.down())) {
			if (ctx.isOnGround()) {
				double playerX = ctx.locX();
				double playerZ = ctx.locZ();
				ArrayList<BetterBlockPos> closest = new ArrayList<>();
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						closest.add(new BetterBlockPos(feet.x + dx, feet.y, feet.z + dz));
					}
				}
				closest.sort(Comparator.comparingDouble(pos -> ((pos.x + 0.5D) - playerX) * ((pos.x + 0.5D) - playerX)
						+ ((pos.z + 0.5D) - playerZ) * ((pos.z + 0.5D) - playerZ)));
				for (int i = 0; i < 4; i++) {
					BetterBlockPos possibleSupport = closest.get(i);
					double xDist = Math.abs((possibleSupport.x + 0.5D) - playerX);
					double zDist = Math.abs((possibleSupport.z + 0.5D) - playerZ);
					if (xDist > 0.8 && zDist > 0.8) {
						// can't possibly be sneaking off of this one, we're too far away
						continue;
					}
					if (MovementHelper.canWalkOn(ctx, possibleSupport.down())
							&& MovementHelper.canWalkThrough(ctx, possibleSupport)
							&& MovementHelper.canWalkThrough(ctx, possibleSupport.up())) {
						// this is plausible
						// logDebug("Faking path start assuming player is standing off the edge of a
						// block");
						return possibleSupport;
					}
				}

			} else {
				// !onGround
				// we're in the middle of a jump
				if (MovementHelper.canWalkOn(ctx, feet.down().down())) {
					// logDebug("Faking path start assuming player is midair and falling");
					return feet.down();
				}
			}
		}
		return feet;
	}

	/**
	 * In a new thread, pathfind to target blockpos
	 *
	 * @param start
	 * @param talkAboutIt
	 */
	private void findPathInNewThread(final BlockPos start, final boolean talkAboutIt, CalculationContext context) {
		// this must be called with synchronization on pathCalcLock!
		// actually, we can check this, muahaha
		logDebug("Pos: " + start.toString());
		if (!Thread.holdsLock(pathCalcLock)) {
			throw new IllegalStateException("Must be called with synchronization on pathCalcLock");
			// why do it this way? it's already indented so much that putting the whole
			// thing in a synchronized(pathCalcLock) was just too much lol
		}
		if (inProgress != null) {
			throw new IllegalStateException("Already doing it"); // should have been checked by caller
		}
		if (!context.safeForThreadedUse) {
			throw new IllegalStateException("Improper context thread safety level");
		}
		Goal goal = this.goal;
		if (goal == null) {
			logDebug("no goal"); // TODO should this be an exception too? definitely should be checked by caller
			return;
		}
		long primaryTimeout;
		long failureTimeout;
		if (current == null) {
			primaryTimeout = 500;
			failureTimeout = 2000;
		} else {
			primaryTimeout = 4000;
			failureTimeout = 5000;
		}
		AbstractNodeCostSearch pathfinder = createPathfinder(start, goal, current == null ? null : current.getPath(),
				context);
		if (!Objects.equals(pathfinder.getGoal(), goal)) { // will return the exact same object if simplification didn't
															// happen
			logDebug("Simplifying " + goal.getClass() + " to GoalXZ due to distance");
		}
		inProgress = pathfinder;
		Baritone.execute(() -> {
			if (talkAboutIt) {
				logDebug("Starting to search for path from " + start + " to " + goal);
			}

			PathCalculationResult calcResult = pathfinder.calculate(primaryTimeout, failureTimeout);
			synchronized (pathPlanLock) {
				Optional<PathExecutor> executor = calcResult.getPath()
						.map(p -> new PathExecutor(context.player, PathingBehavior.this, p));
				if (current == null) {
					if (executor.isPresent()) {
						if (executor.get().getPath().positions().contains(expectedSegmentStart)) {
							queuePathEvent(PathEvent.CALC_FINISHED_NOW_EXECUTING);
							current = executor.get();
						} else {
							logDebug("Warning: discarding orphan path segment with incorrect start");
						}
					} else {
						if (calcResult.getType() != PathCalculationResult.Type.CANCELLATION
								&& calcResult.getType() != PathCalculationResult.Type.EXCEPTION) {
							// don't dispatch CALC_FAILED on cancellation
							queuePathEvent(PathEvent.CALC_FAILED);
						}
					}
				} else {
					if (next == null) {
						if (executor.isPresent()) {
							if (executor.get().getPath().getSrc().equals(current.getPath().getDest())) {
								queuePathEvent(PathEvent.NEXT_SEGMENT_CALC_FINISHED);
								next = executor.get();
							} else {
								logDebug("Warning: discarding orphan next segment with incorrect start");
							}
						} else {
							queuePathEvent(PathEvent.NEXT_CALC_FAILED);
						}
					} else {
						// throw new IllegalStateException("I have no idea what to do with this path");
						// no point in throwing an exception here, and it gets it stuck with inProgress
						// being not null
						logDirect("Warning: PathingBehaivor illegal state! Discarding invalid path!");
					}
				}
				if (talkAboutIt && current != null && current.getPath() != null) {
					if (goal.isInGoal(current.getPath().getDest())) {
						logDebug("Finished finding a path from " + start + " to " + goal + ". "
								+ current.getPath().getNumNodesConsidered() + " nodes considered");
					} else {
						logDebug("Found path segment from " + start + " towards " + goal + ". "
								+ current.getPath().getNumNodesConsidered() + " nodes considered");
					}
				}
				synchronized (pathCalcLock) {
					inProgress = null;
				}
			}
		});
	}

	private static AbstractNodeCostSearch createPathfinder(BlockPos start, Goal goal, IPath previous,
			CalculationContext context) {
		Goal transformed = goal;
		Favoring favoring = new Favoring(context.player, previous, context);
		return new AStarPathFinder(start.getX(), start.getY(), start.getZ(), transformed, favoring, context);
	}

	public Optional<Double> ticksRemainingInSegment(boolean includeCurrentMovement) {
		IPathExecutor current = getCurrent();
		if (current == null) {
			return Optional.empty();
		}
		int start = includeCurrentMovement ? current.getPosition() : current.getPosition() + 1;
		return Optional.of(current.getPath().ticksRemainingFrom(start));
	}

	public boolean hasPath() {
		return getCurrent() != null;
	}

	public Optional<IPath> getPath() {
		return Optional.ofNullable(getCurrent()).map(IPathExecutor::getPath);
	}

	public void startGoal(Player p, Goal goal) {
		this.goal = goal;
		TickEvent e = new TickEvent(p);
		onTick(e); // call event
        context = new CalculationContext(baritone, true);
        if (goal == null) {
        	logDebug("No goal");
            return;
        }
        if (goal.isInGoal(ctx.playerFeet()) || goal.isInGoal(expectedSegmentStart)) {
            return;
        }
        synchronized (pathPlanLock) {
            if (current != null) {
                return;
            }
            synchronized (pathCalcLock) {
                if (inProgress != null) {
                    return;
                }
                queuePathEvent(PathEvent.CALC_STARTED);
                findPathInNewThread(expectedSegmentStart, true, context);
                return;
            }
        }
	}
	
	@Deprecated
    public boolean secretInternalSetGoalAndPath(Goal goal, Location begin) {
    	//this.goal = goal;
    	//this.expectedSegmentStart = new BetterBlockPos(begin.getX(), begin.getY(), begin.getZ());
        context = new CalculationContext(baritone, true);
        if (goal == null) {
        	logDebug("No goal");
            return false;
        }
        if (goal.isInGoal(ctx.playerFeet()) || goal.isInGoal(expectedSegmentStart)) {
            return false;
        }
        synchronized (pathPlanLock) {
            if (current != null) {
                return false;
            }
            synchronized (pathCalcLock) {
                if (inProgress != null) {
                    return false;
                }
                queuePathEvent(PathEvent.CALC_STARTED);
                findPathInNewThread(expectedSegmentStart, true, context);
                return true;
            }
        }
    }
}
