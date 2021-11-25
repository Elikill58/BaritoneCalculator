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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.bukkit.entity.Player;

import eli.baritone.api.utils.player.PlayerContext;
import eli.baritone.cache.WorldScanner;

/**
 * @author Brady
 * @since 9/29/2018
 */
public final class BaritoneProvider {

    private final List<Baritone> all = new ArrayList<>();
    private final HashMap<Player, Baritone> players = new HashMap<>();

    @Deprecated
    public Baritone getPrimaryBaritone() {
        return all.get(0);
    }

    public List<Baritone> getAllBaritones() {
        return all;
    }

    public WorldScanner getWorldScanner() {
        return WorldScanner.INSTANCE;
    }
    
    public Baritone getBaritone(Player p) {
    	return players.computeIfAbsent(p, Baritone::new);
    }
    
    public Baritone getBaritoneForPlayer(PlayerContext player) {
        for (Baritone baritone : getAllBaritones()) {
            if (Objects.equals(player, baritone.getPlayerContext().player())) {
                return baritone;
            }
        }
        return null;
    }
}
