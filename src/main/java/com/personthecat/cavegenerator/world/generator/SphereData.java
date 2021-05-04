package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.util.PositionFlags;

public class SphereData {

    /** The inner sphere being carved out by the generator. */
    public final PositionFlags inner = new PositionFlags(256);

    // Todo: this is redundant now
    /** A ring around the inner sphere being used as a shell. */
    public final PositionFlags shell = new PositionFlags(128);

    /**
     * Checks both of the flag holders to make sure they have enough capacity for an upcoming
     * series of write operations. These radii represent the size of a sphere in the current
     * chunk. They do not represent the entire area of a sphere and, as a result, we are not
     * calculating the volume of a sphere, but something more like a rectangular prism.
     *
     * In order to ensure that no data are written out of bounds, this should be called before
     * each sphere is generated.
     *
     * @param radX The width of this segment on the x-axis, max 15.
     * @param radY The height of this segment, max 255.
     * @param radZ The width of this segment on the z-axis, max 15.
     */
    public void grow(int radX, int radY, int radZ) {
        final int areaInner = radX * radY * radZ;
        this.inner.grow(areaInner);
        this.shell.grow(areaInner); // Todo: more specific math
    }

    /** Clears all data from both of the flag holders and resets their cursors. */
    public void reset() {
        this.inner.reset();
        this.shell.reset();
    }
}
