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

package baritone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Player;

/**
 * @author Brady
 * @since 9/29/2018
 */
public final class BaritoneProvider {

	private final HashMap<Player, Baritone> players = new HashMap<>();

	/**
	 * Get baritone instance or create a new one if any founded
	 * 
	 * @param p the player that will have instance
	 * @return the new instance or saved
	 */
    public Baritone getBaritone(Player p) {
        return players.computeIfAbsent(p, Baritone::new);
    }

    /**
     * Get all registered instance, even for offline player (should be removed manually).
     * 
     * @return all baritones instance
     */
    public List<Baritone> getAllBaritones() {
        return new ArrayList<>(players.values());
    }
    
    /**
     * Remove actual baritone instance.
     * 
     * @param p the player that can have instance
     * @return the old instance or null
     */
    public Baritone removeBaritone(Player p) {
    	return players.remove(p);
    }
}
