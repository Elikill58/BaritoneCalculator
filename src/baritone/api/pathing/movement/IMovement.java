package baritone.api.pathing.movement;

import baritone.api.nms.PlayerContext;
import baritone.api.nms.block.BlockPos;
import baritone.api.utils.BetterBlockPos;

public interface IMovement {

    double getCost();

    /**
     * Resets the current state status to {@link MovementStatus#PREPPING}
     */
    void reset();

    /**
     * Resets the cache for special break, place, and walk into blocks
     */
    void resetBlockCache();

    /**
     * @return Whether or not it is safe to cancel the current movement state
     */
    boolean safeToCancel(PlayerContext ctx);

    boolean calculatedWhileLoaded();

    BetterBlockPos getSrc();

    BetterBlockPos getDest();

    BlockPos getDirection();
}
