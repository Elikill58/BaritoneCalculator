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

package baritone.api.utils.pathing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import baritone.Baritone;
import baritone.api.nms.block.BlockPos;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.player.PlayerContext;

public class Avoidance {

    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final double coefficient;
    private final int radius;

    public Avoidance(BlockPos center, double coefficient, int radius) {
        this(center.getX(), center.getY(), center.getZ(), coefficient, radius);
    }

    public Avoidance(int centerX, int centerY, int centerZ, double coefficient, int radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.coefficient = coefficient;
        this.radius = radius;
    }

    public static List<Avoidance> create(PlayerContext ctx) {
        if (!Baritone.settings().avoidance.value) {
            return Collections.emptyList();
        }
        List<Avoidance> res = new ArrayList<>();
        double mobSpawnerCoeff = Baritone.settings().mobSpawnerAvoidanceCoefficient.value;
        double mobCoeff = Baritone.settings().mobAvoidanceCoefficient.value;
        if (mobSpawnerCoeff != 1.0D) {
            ctx.worldData().getCachedWorld().getLocationsOf("mob_spawner", 1, ctx.playerFeet().x, ctx.playerFeet().z, 2)
                    .forEach(mobspawner -> res.add(new Avoidance(mobspawner, mobSpawnerCoeff, Baritone.settings().mobSpawnerAvoidanceRadius.value)));
        }
        if (mobCoeff != 1.0D) {
        	/*ctx.world().loadedEntityList.stream()
                    .filter(entity -> entity instanceof EntityMob)
                    .filter(entity -> (!(entity instanceof EntitySpider)) || ctx.player().getBrightness() < 0.5)
                    .filter(entity -> !(entity instanceof EntityPigZombie) || ((EntityPigZombie) entity).isAngry())
                    .filter(entity -> !(entity instanceof EntityEnderman) || ((EntityEnderman) entity).isScreaming())
                    .forEach(entity -> res.add(new Avoidance(new BlockPos(entity), mobCoeff, Baritone.settings().mobAvoidanceRadius.value)));*/
        }
        return res;
    }

    public void applySpherical(HashMap<Long, Double> map) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z <= radius * radius) {
                        long hash = BetterBlockPos.longHash(centerX + x, centerY + y, centerZ + z);
                        map.put(hash, (map.get(hash) * coefficient));
                    }
                }
            }
        }
    }
}
