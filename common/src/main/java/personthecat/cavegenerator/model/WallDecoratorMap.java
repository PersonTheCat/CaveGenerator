package personthecat.cavegenerator.model;

import personthecat.cavegenerator.data.WallDecoratorSettings;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Singular;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Random;

@Builder
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class WallDecoratorMap {

    @Singular("all") List<ConfiguredWallDecorator> all;
    @Singular("side") List<ConfiguredWallDecorator> side;
    @Singular("up") List<ConfiguredWallDecorator> up;
    @Singular("down") List<ConfiguredWallDecorator> down;
    @Singular("north") List<ConfiguredWallDecorator> north;
    @Singular("south") List<ConfiguredWallDecorator> south;
    @Singular("east") List<ConfiguredWallDecorator> east;
    @Singular("west") List<ConfiguredWallDecorator> west;
    boolean containsAny;

    public static WallDecoratorMap sort(final List<WallDecoratorSettings> decorators, final Random rand, final long seed) {
        final WallDecoratorMapBuilder builder = builder().containsAny(!decorators.isEmpty());
        for (final WallDecoratorSettings cfg : decorators) {
            final ConfiguredWallDecorator wall = new ConfiguredWallDecorator(cfg, rand, seed);
            for (final Direction d : cfg.directions) {
                switch (d) {
                    case ALL: builder.all(wall); break;
                    case SIDE: builder.side(wall); break;
                    case UP: builder.up(wall); break;
                    case DOWN: builder.down(wall); break;
                    case NORTH: builder.north(wall); break;
                    case SOUTH: builder.south(wall); break;
                    case EAST: builder.east(wall); break;
                    case WEST: builder.west(wall); break;
                }
            }
        }
        return builder.build();
    }
}
