package personthecat.cavegenerator.model;

import com.mojang.serialization.Codec;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

import static personthecat.catlib.serialization.CodecUtils.ofEnum;

public enum Direction {
    UP,
    DOWN,
    NORTH,
    SOUTH,
    EAST,
    WEST,
    SIDE,
    ALL;

    public static final Codec<Direction> CODEC = ofEnum(Direction.class);

    @NoArgsConstructor(force = true)
    @AllArgsConstructor
    public static class Container {

        public final boolean up, down, side, north, south, east, west;

        private static final Container ALL_DIRECTIONS =
            new Container(true, true, true, true, true, true, true);

        public static Container from(final List<Direction> directions) {
            // Empty array -> all == true
            if (directions.isEmpty()) {
                return ALL_DIRECTIONS;
            }
            boolean up = false, down = false, side = false, north = false, south = false, east = false, west = false;
            // Directions are assumed to be non-redundant from preset tester.
            for (final Direction direction : directions) {
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
