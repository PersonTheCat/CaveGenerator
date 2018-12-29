package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.util.Direction;
import com.personthecat.cavegenerator.util.NoiseSettings3D;
import com.personthecat.cavegenerator.util.SimplexNoiseGenerator3D;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.hjson.JsonObject;

import java.util.Arrays;
import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.util.HjsonTools.*;

public class WallDecorators {
    /** Mandatory fields to be filled by the constructor. */
    private final double chance;
    private final IBlockState fillBlock;
    private final int minHeight, maxHeight;
    private final Direction[] directions;
    private final IBlockState[] matchers;
    private final Preference preference;

    /** Null-safe, optional noise settings. I'm not dealing with NPEs. */
    private final Optional<SimplexNoiseGenerator3D> noise;
    private final Optional<NoiseSettings3D> settings;

    /** The default noise values for WallDecorators with noise. */
    public static final NoiseSettings3D DEFAULT_NOISE =
        new NoiseSettings3D(0.10f, 50.00f, 1.00f, 1);

    /** From Json. */
    public WallDecorators(IBlockState fillBlock, JsonObject wall) {
        this(
            fillBlock,
            getFloatOr(wall, "chance", 100.0f),
            getIntOr(wall, "minHeight", 10),
            getIntOr(wall, "maxHeight", 50),
            getDirectionsOr(wall, "directions", Direction.ALL),
            getBlocksOr(wall, "matchers", Blocks.STONE.getDefaultState()),
            getPreferenceOr(wall, "preference", Preference.REPLACE_MATCH),
            getObject(wall, "noise3D").map(o -> toNoiseSettings(o, DEFAULT_NOISE))
        );
    }

    public WallDecorators(
        IBlockState fillBlock,
        double chance,
        int minHeight,
        int maxHeight,
        Direction[] directions,
        IBlockState[] matchers,
        Preference preference,
        Optional<NoiseSettings3D> settings
    ) {
        this.fillBlock = fillBlock;
        this.chance = chance;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.directions = directions;
        this.matchers = matchers;
        this.preference = preference;
        this.settings = settings;
        this.noise = setupNoise();
    }

    /** Sets up a noise generator to use for placement. */
    private Optional<SimplexNoiseGenerator3D> setupNoise() {
        if (settings.isPresent()) {
            // The noise for this generator will be unique to the block ID.
            return full(new SimplexNoiseGenerator3D(Block.getStateId(fillBlock)));
        }
        return empty();
    }

    public boolean spawnInPatches() {
        return noise.isPresent();
    }

    public boolean hasDirections() {
        return directions.length > 0;
    }

    public Direction[] getDirections() {
        return directions;
    }

    public boolean hasDirection(Direction direction) {
        return find(directions, (dir) -> dir == Direction.ALL || dir == direction)
            .isPresent();
    }

    public IBlockState[] getMatchers() {
        return matchers;
    }

    public double getChance() {
        return chance;
    }

    public IBlockState getFillBlock() {
        return fillBlock;
    }

    public boolean canGenerateAtHeight(final int y) {
        return y >= minHeight && y <= maxHeight;
    }

    public Preference getPreference() {
        return preference;
    }

    public enum Preference {
        REPLACE_ORIGINAL,
        REPLACE_MATCH;

        public static Preference from(final String s) {
            Optional<Preference> pref = find(values(), (v) -> v.toString().equalsIgnoreCase(s));
            return pref.orElseThrow(() -> {
                final String o = Arrays.toString(values());
                return runExF("Error: Preference \"%s\" does not exist. The following are valid options:\n\n", s, o);
            });
        }
    }
}