package com.personthecat.cavegenerator.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static com.personthecat.cavegenerator.util.CommonMethods.*;

public enum Direction {
    UP,
    DOWN,
    NORTH,
    SOUTH,
    EAST,
    WEST,
    SIDE,
    ALL;

    public static Direction from(final String s) {
        Optional<Direction> dir = find(values(), (v) -> v.toString().equalsIgnoreCase(s));
        return dir.orElseThrow(() -> {
            final String o = Arrays.toString(values());
            return runExF("Error: Direction \"{}\" does not exist. The following are valid options:\n\n{}", s, o);
        });
    }

    /** A DTO counterpart to using Direction arrays.*/
    @NoArgsConstructor(force = true)
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
    public static class Container {

        private static final Container ALL_DIRECTIONS =
            new Container(true, true, true, true, true, true, true);

        boolean up, down, side, north, south, east, west;

        /** Converts a direction array into a Direction.Container */
        public static Container from(List<Direction> directions) {
            // Empty array -> all == true
            if (directions.isEmpty()) {
                return ALL_DIRECTIONS;
            }
            boolean up = false, down = false, side = false, north = false, south = false, east = false, west = false;
            // Directions are assumed to be non-redundant from preset tester.
            for (Direction direction : directions) {
                switch (direction) {
                    case UP : up = true; break;
                    case DOWN : down = true; break;
                    case SIDE : side = true; break;
                    case NORTH : north = true; break;
                    case SOUTH : south = true; break;
                    case EAST : east = true; break;
                    case WEST : west = true; break;
                    case ALL : return ALL_DIRECTIONS;
                }
            }
            return new Container(up, down, side, north, south, east, west);
        }
    }
}
