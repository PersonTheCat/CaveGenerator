package com.personthecat.cavegenerator.world;

import com.personthecat.cavegenerator.util.ScalableFloat;
import com.personthecat.cavegenerator.world.GeneratorSettings.TunnelSettings;
import com.personthecat.cavegenerator.world.GeneratorSettings.RavineSettings;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

/**
 * Holds all of the information related to the tunnel generator's
 * current position along the path of a tunnel. Used for generating
 * a series of center coordinates around which to generate spheres.
 * Based on Mojang's original algorithm.
 */
public class TunnelPathInfo {
    /** The angles in radians for this tunnel. */
    private float angleXZ, angleY;
    /** The amount to alter angle(XZ/Y) per-segment. */
    private float twistXZ, twistY;
    private float scale, scaleY;
    /** Coordinates of this tunnel's current destination. */
    private float x, y, z;
    private final ScalableFloat sfAngleXZ, sfAngleY;
    private final ScalableFloat sfTwistXZ, sfTwistY;
    private final ScalableFloat sfScale, sfScaleY;

    private static final float PI_TIMES_2 = (float) (Math.PI * 2);

    /** Neatly constructs a new object based on values from tunnel settings. */
    public TunnelPathInfo(TunnelSettings cfg, Random rand, int destChunkX, int destChunkZ) {
        this(cfg.angleXZ, cfg.angleY, cfg.twistXZ, cfg.twistY, cfg.scale, cfg.scaleY, rand, destChunkX, destChunkZ, cfg.minHeight, cfg.maxHeight);
    }

    /** Neatly constructs a new object based on values from ravine settings. */
    public TunnelPathInfo(RavineSettings cfg, Random rand, int destChunkX, int destChunkZ) {
        this(cfg.angleXZ, cfg.angleY, cfg.twistXZ, cfg.twistY, cfg.scale, cfg.scaleY, rand, destChunkX, destChunkZ, cfg.minHeight, cfg.maxHeight);
    }

    /** Used for handling initial encapsulation of inner values. */
    private TunnelPathInfo(
        ScalableFloat angleXZ,
        ScalableFloat angleY,
        ScalableFloat twistXZ,
        ScalableFloat twistY,
        ScalableFloat scale,
        ScalableFloat scaleY,
        Random rand,
        int destChunkX,
        int destChunkZ,
        int minHeight,
        int maxHeight
    ) {
        this.angleXZ = angleXZ.startVal + angleXZ.startValRandFactor * (rand.nextFloat() * PI_TIMES_2);
        this.angleY = angleY.startVal + angleY.startValRandFactor * (rand.nextFloat() - 0.50F);
        this.scale = scale.startVal + scale.startValRandFactor * (rand.nextFloat() * 2.00F + rand.nextFloat());
        this.twistXZ = twistXZ.startVal;
        this.twistY = twistY.startVal;
        this.scaleY = scaleY.startVal;
        this.sfAngleXZ = angleXZ;
        this.sfAngleY = angleY;
        this.sfTwistXZ = twistXZ;
        this.sfTwistY = twistY;
        this.sfScale = scale;
        this.sfScaleY = scaleY;
        // Random coordinates in the destination chunk.
        final int heightDiff = maxHeight - minHeight;
        this.x = (destChunkX * 16) + rand.nextInt(16);
        this.y = rand.nextInt(rand.nextInt(heightDiff) + 2) + minHeight;
        this.z = (destChunkZ * 16) + rand.nextInt(16);
    }

    /** Used for constructing updated or cloned instances. */
    private TunnelPathInfo(
        TunnelPathInfo from,
        float angleXZ,
        float angleY,
        float twistXZ,
        float twistY,
        float scale,
        float scaleY,
        float x,
        float y,
        float z
    ) {
        this.sfAngleXZ = from.sfAngleXZ;
        this.sfAngleY = from.sfAngleY;
        this.sfTwistXZ = from.sfTwistXZ;
        this.sfTwistY = from.sfTwistY;
        this.sfScale = from.sfScale;
        this.sfScaleY = from.sfScaleY;
        this.angleXZ = angleXZ;
        this.angleY = angleY;
        this.scale = scale;
        this.twistXZ = twistXZ;
        this.twistY = twistY;
        this.scaleY = scaleY;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** Returns a new instance, resetting all primary fields to the input values. */
    public TunnelPathInfo reset(float angleXZ, float angleY, float scale, float scaleY) {
        return new TunnelPathInfo(this, angleXZ, angleY, sfTwistXZ.startVal, sfTwistY.startVal, scale, scaleY, x, y, z);
    }

    public float getAngleXZ() {
        return angleXZ;
    }

    public float getAngleY() {
        return angleY;
    }

    public float getScale() {
        return scale;
    }

    public void multiplyScale(float by) {
        scale *= by;
    }

    public float getScaleY() {
        return scaleY;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public void update(Random rand, boolean noiseYReduction, float angleYFactor, float twistPotential) {
        // Find the next position on a curvilinear path.
        nextPos();
        // Vertical noise control.
        if (noiseYReduction) {
            angleY *= angleYFactor;
        }
        // Recalculate the size / angle data for the next segment.
        updateVals(rand, twistPotential);
    }

    public void nextPos() {
        final float cos = MathHelper.cos(angleY);
        final float sin = MathHelper.sin(angleY);
        x += MathHelper.cos(angleXZ) * cos;
        y += sin;
        z += MathHelper.sin(angleXZ) * cos;
    }

    public void updateVals(Random rand, float twistPotential) {
        // Adjust the angle based on current twist(XZ/Y). twist
        // will have been recalculated on subsequent iterations.
        // The potency of twist is reduced immediately.
        angleXZ += twistXZ * twistPotential;
        angleY += twistY * twistPotential;
        // Rotates the beginning of the chain around the end.
        twistY = adjustTwist(twistY, rand, sfTwistY);
        // Positive is counterclockwise, negative is clockwise.
        twistXZ = adjustTwist(twistXZ, rand, sfTwistXZ);
        scale = adjustScale(scale, rand, sfScale);
        scaleY = adjustScale(scaleY, rand, sfScaleY);
    }

    /** Updates the value of `original` based on the input settings. */
    private float adjustTwist(float original, Random rand, ScalableFloat f) {
        original = (float) Math.pow(original, f.exponent);
        original *= f.factor;
        original += f.randFactor * (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat();
        return original;
    }

    /** Updates the value of `original` based on the input settings. */
    private float adjustScale(float original, Random rand, ScalableFloat f) {
        original = (float) Math.pow(original, f.exponent);
        original *= f.factor;
        //original += f.randFactor * (rand.nextFloat() - 0.5F);
        if (original < 0) original = 0;
        return original;
    }

    public boolean travelledTooFar(PrimerData data, int currentPos, int distance) {
        final double fromCenterX = x - data.centerX;
        final double fromCenterZ = z - data.centerZ;
        // Name? Is this related to Y?
        final double distanceRemaining = distance - currentPos;
        final double adjustedScale = scale + 18.00;

        final double fromCenterX2 = fromCenterX * fromCenterX;
        final double fromCenterZ2 = fromCenterZ * fromCenterZ;
        final double distanceRemaining2 = distanceRemaining * distanceRemaining;
        final double adjustedScale2 = adjustedScale * adjustedScale;

        return (fromCenterX2 + fromCenterZ2 - distanceRemaining2) > adjustedScale2;
    }

    public boolean touchesChunk(PrimerData data, double diameterXZ) {
        return x >= data.centerX - 16.0 - diameterXZ &&
            z >= data.centerZ - 16.0 - diameterXZ &&
            x <= data.centerX + 16.0 + diameterXZ &&
            z <= data.centerZ + 16.0 + diameterXZ;
    }
}