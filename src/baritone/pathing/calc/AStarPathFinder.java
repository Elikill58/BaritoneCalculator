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

package baritone.pathing.calc;

import java.util.Optional;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.pathing.BetterWorldBorder;
import baritone.api.utils.pathing.Favoring;
import baritone.api.utils.pathing.MutableMoveResult;
import baritone.pathing.calc.openset.BinaryHeapOpenSet;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Moves;

/**
 * The actual A* pathfinding
 *
 * @author leijurv
 */
public final class AStarPathFinder extends AbstractNodeCostSearch {

    private final Favoring favoring;
    private final CalculationContext calcContext;

    public AStarPathFinder(int startX, int startY, int startZ, Goal goal, Favoring favoring, CalculationContext context) {
        super(startX, startY, startZ, goal, context);
        this.favoring = favoring;
        this.calcContext = context;
    }

    @Override
    protected Optional<IPath> calculate0(long primaryTimeout, long failureTimeout) {
    	try {
	    	//logDebug("[AStarPathFinder] Begin of calculation");
	        startNode = getNodeAtPosition(startX, startY, startZ, BetterBlockPos.longHash(startX, startY, startZ));
	        startNode.cost = 0;
	        startNode.combinedCost = startNode.estimatedCostToGoal;
	        BinaryHeapOpenSet openSet = new BinaryHeapOpenSet();
	        openSet.insert(startNode);
	        double[] bestHeuristicSoFar = new double[COEFFICIENTS.length];//keep track of the best node by the metric of (estimatedCostToGoal + cost / COEFFICIENTS[i])
	        for (int i = 0; i < bestHeuristicSoFar.length; i++) {
	            bestHeuristicSoFar[i] = startNode.estimatedCostToGoal;
	            bestSoFar[i] = startNode;
	            //logDebug("Set " + startNode.toString() + " on slot " + i);
	        }
	        MutableMoveResult res = new MutableMoveResult();
	        BetterWorldBorder worldBorder = new BetterWorldBorder(calcContext.world.getWorldBorder(), calcContext.world);
	        long startTime = System.currentTimeMillis();
	        long primaryTimeoutTime = startTime + primaryTimeout;
	        long failureTimeoutTime = startTime + failureTimeout;
	        boolean failing = true;
	        int numNodes = 0;
	        int numMovementsConsidered = 0;
	        int numEmptyChunk = 0;
	        boolean isFavoring = !favoring.isEmpty();
	        int timeCheckInterval = 1 << 6;
	        int pathingMaxChunkBorderFetch = 50; // grab all settings beforehand so that changing settings during pathing doesn't cause a crash or unpredictable behavior
	        double minimumImprovement = MIN_IMPROVEMENT;
	        Moves[] allMoves = Moves.values();
	        while (!openSet.isEmpty() && numEmptyChunk < pathingMaxChunkBorderFetch && !cancelRequested) {
	            if ((numNodes & (timeCheckInterval - 1)) == 0) { // only call this once every 64 nodes (about half a millisecond)
	                long now = System.currentTimeMillis(); // since nanoTime is slow on windows (takes many microseconds)
	                if (now - failureTimeoutTime >= 0 || (!failing && now - primaryTimeoutTime >= 0)) {
	                    break;
	                }
	            }
	            PathNode currentNode = openSet.removeLowest();
	            mostRecentConsidered = currentNode;
	            numNodes++;
	            if (goal.isInGoal(currentNode.x, currentNode.y, currentNode.z)) {
	                logDebug("Took " + (System.currentTimeMillis() - startTime) + "ms, " + numMovementsConsidered + " movements considered");
	                return Optional.of(new Path(startNode, currentNode, numNodes, goal, calcContext));
	            }
	            //logDebug("Checked goal. Trying moves ...");
	            for (Moves moves : allMoves) {
	            	//logDebug("Checking for move " + moves.name());
	                int newX = currentNode.x + moves.xOffset;
	                int newZ = currentNode.z + moves.zOffset;
	                if ((newX >> 4 != currentNode.x >> 4 || newZ >> 4 != currentNode.z >> 4) && !calcContext.isLoaded(newX, newZ)) {
	                    // only need to check if the destination is a loaded chunk if it's in a different chunk than the start of the movement
	                    if (!moves.dynamicXZ) { // only increment the counter if the movement would have gone out of bounds guaranteed
	                        numEmptyChunk++;
	                    }
	                    logDebug("Calc not loaded. " + !moves.dynamicXZ + " > " + (newX >> 4 != currentNode.x >> 4) + " / " + (newZ >> 4 != currentNode.z >> 4));
	                    continue;
	                }
	                if (!moves.dynamicXZ && !worldBorder.entirelyContains(newX, newZ)) {
	                	logDebug("Not dynamic X/Z");
	                    continue;
	                }
	                if (currentNode.y + moves.yOffset > 256 || currentNode.y + moves.yOffset < 0) {
	                	logDebug("Invalid y. " + (currentNode.y + moves.yOffset > 256) + "/" +  (currentNode.y + moves.yOffset < 0));
	                    continue;
	                }
	                res.reset();
	                moves.apply(calcContext, currentNode.x, currentNode.y, currentNode.z, res);
	                numMovementsConsidered++;
	                double actionCost = res.cost;
	                if (actionCost >= ActionCosts.COST_INF) {
	                	//logDebug("Too high cost " + actionCost);
	                    continue;
	                }
	                if (actionCost <= 0 || Double.isNaN(actionCost)) {
	                    throw new IllegalStateException(moves + " calculated implausible cost " + actionCost);
	                }
	                // check destination after verifying it's not COST_INF -- some movements return a static IMPOSSIBLE object with COST_INF and destination being 0,0,0 to avoid allocating a new result for every failed calculation
	                if (moves.dynamicXZ && !worldBorder.entirelyContains(res.x, res.z)) { // see issue #218
	                	logDebug("Not second dynamic X/Z");
	                    continue;
	                }
	                if (!moves.dynamicXZ && (res.x != newX || res.z != newZ)) {
	                    throw new IllegalStateException(moves + " " + res.x + " " + newX + " " + res.z + " " + newZ);
	                }
	                if (!moves.dynamicY && res.y != currentNode.y + moves.yOffset) {
	                    throw new IllegalStateException(moves + " " + res.y + " " + (currentNode.y + moves.yOffset));
	                }
	                //logDebug("PathNode checking for more.");
	                long hashCode = BetterBlockPos.longHash(res.x, res.y, res.z);
	                if (isFavoring) {
	                    // see issue #18
	                    actionCost *= favoring.calculate(hashCode);
	                }
	                PathNode neighbor = getNodeAtPosition(res.x, res.y, res.z, hashCode);
	                double tentativeCost = currentNode.cost + actionCost;
	                if (neighbor.cost - tentativeCost > minimumImprovement) {
	                    neighbor.previous = currentNode;
	                    neighbor.cost = tentativeCost;
	                    neighbor.combinedCost = tentativeCost + neighbor.estimatedCostToGoal;
	                    if (neighbor.isOpen()) {
	                        openSet.update(neighbor);
	                        //logDebug("Update OpenSet " + openSet.toString());
	                    } else {
	                        openSet.insert(neighbor);//dont double count, dont insert into open set if it's already there
	                        //logDebug("Insert OpenSet " + openSet.toString());
	                    }
	                    for (int i = 0; i < COEFFICIENTS.length; i++) {
	                        double heuristic = neighbor.estimatedCostToGoal + neighbor.cost / COEFFICIENTS[i];
	                        if (bestHeuristicSoFar[i] - heuristic > minimumImprovement) {
	                            bestHeuristicSoFar[i] = heuristic;
	                            //logDebug("Changed best far from " + bestSoFar[i] + " to " + neighbor.toString() + ", heurs: " + heuristic);
	                            bestSoFar[i] = neighbor;
	                            if (failing && getDistFromStartSq(neighbor) > MIN_DIST_PATH * MIN_DIST_PATH) {
	                                failing = false;
	                            }
	                        }/* else
	                            logDebug("Uninteresing far " + neighbor.toString() + ", heurs: " + heuristic);*/
	                    }
	                }
	            }
	        }
	        if (cancelRequested) {
	            return Optional.empty();
	        }
	        logDebug("[AStarPathFinder] End of calculation. " + numMovementsConsidered + " movements considered, open set size: " + openSet.size() + ", pathNode map size: " + mapSize() + ", " + (int) (numNodes * 1.0 / ((System.currentTimeMillis() - startTime) / 1000F)) + " nodes per second");
	        Optional<IPath> result = bestSoFar(true, numNodes);
	        if (result.isPresent()) {
	            logDebug("Took " + (System.currentTimeMillis() - startTime) + "ms, " + numMovementsConsidered + " movements considered");
	        } else
	        	logDebug("[AStarPathFinder] No best result found.");
	        return result;
    	} catch (Exception e) {
    		e.printStackTrace();
    		return Optional.empty();
		}
    }
}
