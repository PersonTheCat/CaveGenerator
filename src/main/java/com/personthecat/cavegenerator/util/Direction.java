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
}
