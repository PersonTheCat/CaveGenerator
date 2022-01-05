package personthecat.cavegenerator.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Singular;
import lombok.experimental.FieldDefaults;
import personthecat.cavegenerator.world.config.WallDecoratorConfig;

import java.util.List;

@Builder
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class WallDecoratorMap {

    @Singular("all") List<WallDecoratorConfig> all;
    @Singular("side") List<WallDecoratorConfig> side;
    @Singular("up") List<WallDecoratorConfig> up;
    @Singular("down") List<WallDecoratorConfig> down;
    @Singular("north") List<WallDecoratorConfig> north;
    @Singular("south") List<WallDecoratorConfig> south;
    @Singular("east") List<WallDecoratorConfig> east;
    @Singular("west") List<WallDecoratorConfig> west;
    boolean containsAny;

    public static WallDecoratorMap sort(final List<WallDecoratorConfig> decorators) {
        final WallDecoratorMapBuilder builder = builder().containsAny(!decorators.isEmpty());
        for (final WallDecoratorConfig cfg : decorators) {
            for (final Direction d : cfg.directions) {
                switch (d) {
                    case ALL: builder.all(cfg); break;
                    case SIDE: builder.side(cfg); break;
                    case UP: builder.up(cfg); break;
                    case DOWN: builder.down(cfg); break;
                    case NORTH: builder.north(cfg); break;
                    case SOUTH: builder.south(cfg); break;
                    case EAST: builder.east(cfg); break;
                    case WEST: builder.west(cfg); break;
                }
            }
        }
        return builder.build();
    }
}
