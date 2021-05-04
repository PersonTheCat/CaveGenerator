package com.personthecat.cavegenerator.model;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

public class EmptyRange extends Range {

    private static final EmptyRange INSTANCE = new EmptyRange();

    private EmptyRange() {
        super(0);
    }

    public static EmptyRange get() {
        return INSTANCE;
    }

    @Override
    public boolean contains(int num) {
        return false;
    }

    @Override
    public int rand(Random rand) {
        return 0;
    }

    @Override
    public int diff() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @NotNull
    @Override
    public Iterator<Integer> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public String toString() {
        return "Range[empty]";
    }
}
