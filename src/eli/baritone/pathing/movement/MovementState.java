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

package eli.baritone.pathing.movement;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import eli.baritone.api.pathing.movement.MovementStatus;
import eli.baritone.api.utils.Rotation;
import eli.baritone.api.utils.input.Input;

public class MovementState {

    private MovementStatus status;
    private MovementTarget target = new MovementTarget();
    private final Map<Input, Boolean> inputState = new HashMap<>();

    public MovementState setStatus(MovementStatus status) {
        this.status = status;
        return this;
    }

    public MovementStatus getStatus() {
        return status;
    }

    public MovementTarget getTarget() {
        return this.target;
    }

    public MovementState setTarget(MovementTarget target) {
        this.target = target;
        return this;
    }

    public MovementState setInput(Input input, boolean forced) {
        this.inputState.put(input, forced);
        return this;
    }

    public Map<Input, Boolean> getInputStates() {
        return this.inputState;
    }

    public static class MovementTarget {

        /**
         * Yaw and pitch angles that must be matched
         */
        public Rotation rotation;

        public MovementTarget() {
            this(null);
        }

        public MovementTarget(Rotation rotation) {
            this.rotation = rotation;
        }

        public final Optional<Rotation> getRotation() {
            return Optional.ofNullable(this.rotation);
        }
    }
}
