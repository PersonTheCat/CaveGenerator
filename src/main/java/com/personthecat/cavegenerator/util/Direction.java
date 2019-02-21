package com.personthecat.cavegenerator.util;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Optional;
import static com.personthecat.cavegenerator.util.CommonMethods.*;

public enum Direction {
    UP,
    DOWN,
    SIDE,
    ALL;

    public static Direction from(final String s) {
        Optional<Direction> dir = find(values(), (v) -> v.toString().equalsIgnoreCase(s));
        return dir.orElseThrow(() -> {
            final String o = Arrays.toString(values());
            return runExF("Error: Direction \"%s\" does not exist. The following are valid options:\n\n", s, o);
        });
    }

    /** Returns whether the input direction corresponds with `this`. */
    public boolean matches(Direction dir) {
        return this.equals(dir) || dir.equals(ALL) || this.equals(ALL);
    }

    public boolean matches(Direction[] dirs) {
        for (Direction d : dirs) {
            if (!this.matches(d)) {
                return false;
            }
        }
        return true;
    }

    public static boolean matchesVertical(Direction[] dirs) {
        return (ArrayUtils.contains(dirs, UP) && ArrayUtils.contains(dirs, DOWN)) ||
            ArrayUtils.contains(dirs, ALL);
    }

    /**
     * A DTO counter part to using Direction arrays.
     */
    public static class Container {
        public final boolean up, down, side;

        /** Primary constructor */
        public Container(boolean up, boolean down, boolean side) {
            this.up = up;
            this.down = down;
            this.side = side;
        }

        /** Converts a direction array into a Direction.Container */
        public static Container from(Direction[] directions) {
            // Empty array -> all == true
            if (directions.length == 0) {
                return new Container(true, true, true);
            }
            boolean up = false, down = false, side = false;
            for (Direction direction : directions) {
                switch (direction) {
                    case UP : up = true; break;
                    case DOWN : down = true; break;
                    case SIDE : side = true; break;
                    case ALL : return new Container(true, true, true);
                }
            }
            return new Container(up, down, side);
        }
    }
}
