package com.personthecat.cavegenerator.util;

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
        return this.equals(dir) || dir.equals(ALL);
    }
}
