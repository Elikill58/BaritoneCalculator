package eli.baritone.utils;

import eli.baritone.Baritone;
import eli.baritone.api.utils.Helper;
import eli.baritone.api.utils.player.PlayerContext;

public abstract class BaritoneProcessHelper implements Helper {

    protected final Baritone baritone;
    protected final PlayerContext ctx;

    public BaritoneProcessHelper(Baritone baritone) {
        this.baritone = baritone;
        this.ctx = baritone.getPlayerContext();
    }
    
    public boolean isTemporary() {
        return false;
    }
}