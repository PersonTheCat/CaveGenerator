package com.personthecat.cavegenerator.util;

@FunctionalInterface
public interface PositionPredicate {
    boolean test(int x, int y, int z);
}
