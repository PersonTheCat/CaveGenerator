package com.personthecat.cavegenerator.model;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;

public class EmptyRange extends Range {

    private static final EmptyRange INSTANCE = new EmptyRange();

    private EmptyRange() {
        super(0);
    }

    public static EmptyRange getInstance() {
        return INSTANCE;
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
}
