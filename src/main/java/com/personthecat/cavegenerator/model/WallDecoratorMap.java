package com.personthecat.cavegenerator.model;

import com.personthecat.cavegenerator.data.WallDecoratorSettings;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Singular;
import lombok.experimental.FieldDefaults;
import net.minecraft.world.World;

import java.util.List;

@Builder
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class WallDecoratorMap {

    @Singular("up") List<ConfiguredWallDecorator> up;
    @Singular("down") List<ConfiguredWallDecorator> down;
    @Singular("north") List<ConfiguredWallDecorator> north;
    @Singular("south") List<ConfiguredWallDecorator> south;
    @Singular("east") List<ConfiguredWallDecorator> east;
    @Singular("west") List<ConfiguredWallDecorator> west;
    boolean containsAny;

    public static WallDecoratorMap sort(List<WallDecoratorSettings> decorators, World world) {
        final WallDecoratorMapBuilder builder = builder().containsAny(!decorators.isEmpty());
        for (WallDecoratorSettings cfg : decorators) {
            final ConfiguredWallDecorator wall = new ConfiguredWallDecorator(cfg, world);
            for (Direction d : cfg.directions) {
                switch (d) {
                    case ALL: builder.up(wall).down(wall).north(wall).south(wall).east(wall).west(wall); break;
                    case SIDE: builder.north(wall).south(wall).east(wall).west(wall); break;
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
