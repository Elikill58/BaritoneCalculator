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

package eli.baritone.cache;

import java.nio.file.Path;

import eli.baritone.api.cache.ICachedWorld;

/**
 * Data about a world, from baritone's point of view. Includes cached chunks, waypoints, and map data.
 *
 * @author leijurv
 */
public class WorldData {

    public final CachedWorld cache;

    WorldData(Path directory, int dimension) {
        //this.directory = directory;
        this.cache = new CachedWorld(directory.resolve("cache"), dimension);
        //this.dimension = dimension;
    }

    public ICachedWorld getCachedWorld() {
        return this.cache;
    }
}
