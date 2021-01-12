package com.personthecat.cavegenerator.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

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
            return runExF("Error: Direction \"{}\" does not exist. The following are valid options:\n\n{}", s, o);
        });
    }

    /**
     * A DTO counterpart to using Direction arrays.
     */
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
    public static class Container {
        boolean up, down, side;

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
