package com.personthecat.cavegenerator.world.generator;

import com.personthecat.cavegenerator.data.RavineSettings;
import com.personthecat.cavegenerator.data.TunnelSettings;
import com.personthecat.cavegenerator.model.ScalableFloat;
import com.personthecat.cavegenerator.model.PrimerData;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

/**
 * Holds all of the information related to the tunnel generator's current position along
 * the path of a tunnel. Used for generating a series of center coordinates around which
 * to generate spheres. Based on Mojang's original algorithm.
 */
public class TunnelPathInfo {

    /** The angles in radians for this tunnel. */
    private float yaw, pitch;

    /** The amount to alter angle(XZ/Y) per-segment. */
    private float dYaw, dPitch;
    private float scale, scaleY;

    /** Coordinates of this tunnel's current destination. */
    private float x, y, z;
    private final ScalableFloat sfYaw, sfPitch;
    private final ScalableFloat sfdYaw, sfdPitch;
    private final ScalableFloat sfScale, sfScaleY;

    private static final float PI_TIMES_2 = (float) (Math.PI * 2);

    /** Neatly constructs a new object based on values from tunnel settings. */
    public TunnelPathInfo(TunnelSettings cfg, Random rand, int destChunkX, int destChunkZ) {
        this(cfg.yaw, cfg.pitch, cfg.dYaw, cfg.dPitch, cfg.scale, cfg.skew, rand, destChunkX, destChunkZ, cfg.conditions.height.min, cfg.conditions.height.max);
    }

    /** Neatly constructs a new object based on values from ravine settings. */
    public TunnelPathInfo(RavineSettings cfg, Random rand, int destChunkX, int destChunkZ) {
        this(cfg.yaw, cfg.pitch, cfg.dYaw, cfg.dPitch, cfg.scale, cfg.skew, rand, destChunkX, destChunkZ, cfg.conditions.height.min, cfg.conditions.height.max);
    }

    /** Used for handling initial encapsulation of inner values. */
    private TunnelPathInfo(
        ScalableFloat yaw,
        ScalableFloat pitch,
        ScalableFloat dYaw,
        ScalableFloat dPitch,
        ScalableFloat scale,
        ScalableFloat scaleY,
        Random rand,
        int destChunkX,
        int destChunkZ,
        int minHeight,
        int maxHeight
    ) {
        this.yaw = yaw.startVal + yaw.startValRandFactor * (rand.nextFloat() * PI_TIMES_2);
        this.pitch = pitch.startVal + pitch.startValRandFactor * (rand.nextFloat() - 0.50F);
        this.scale = scale.startVal + scale.startValRandFactor * (rand.nextFloat() * 2.00F + rand.nextFloat());
        this.dYaw = dYaw.startVal;
        this.dPitch = dPitch.startVal;
        this.scaleY = scaleY.startVal;
        this.sfYaw = yaw;
        this.sfPitch = pitch;
        this.sfdYaw = dYaw;
        this.sfdPitch = dPitch;
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
        float yaw,
        float pitch,
        float dYaw,
        float dPitch,
        float scale,
        float scaleY,
        float x,
        float y,
        float z
    ) {
        this.sfYaw = from.sfYaw;
        this.sfPitch = from.sfPitch;
        this.sfdYaw = from.sfdYaw;
        this.sfdPitch = from.sfdPitch;
        this.sfScale = from.sfScale;
        this.sfScaleY = from.sfScaleY;
        this.yaw = yaw;
        this.pitch = pitch;
        this.scale = scale;
        this.dYaw = dYaw;
        this.dPitch = dPitch;
        this.scaleY = scaleY;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** Returns a new instance, resetting all primary fields to the input values. */
    public TunnelPathInfo reset(float angleXZ, float angleY, float scale, float scaleY) {
        return new TunnelPathInfo(this, angleXZ, angleY, sfdYaw.startVal, sfdPitch.startVal, scale, scaleY, x, y, z);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
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
            pitch *= angleYFactor;
        }
        // Recalculate the size / angle data for the next segment.
        updateVals(rand, twistPotential);
    }

    void nextPos() {
        final float cos = MathHelper.cos(pitch);
        final float sin = MathHelper.sin(pitch);
        x += MathHelper.cos(yaw) * cos;
        y += sin;
        z += MathHelper.sin(yaw) * cos;
    }

    void updateVals(Random rand, float twistPotential) {
        // Adjust the angle based on current twist(XZ/Y). twist
        // will have been recalculated on subsequent iterations.
        // The potency of twist is reduced immediately.
        yaw += dYaw * twistPotential;
        pitch += dPitch * twistPotential;
        // Rotates the beginning of the chain around the end.
        dPitch = adjustTwist(dPitch, rand, sfdPitch);
        // Positive is counterclockwise, negative is clockwise.
        dYaw = adjustTwist(dYaw, rand, sfdYaw);
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