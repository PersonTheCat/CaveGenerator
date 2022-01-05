package personthecat.cavegenerator.world.config;

import lombok.AllArgsConstructor;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.model.ScalableFloat;
import personthecat.fastnoise.FastNoise;

@AllArgsConstructor
public class RavineConfig {
    public final ConditionConfig conditions;
    public final DecoratorConfig decorators;
    public final Range originHeight;
    public final float noiseYFactor;
    public final ScalableFloat dYaw;
    public final ScalableFloat dPitch;
    public final ScalableFloat scale;
    public final ScalableFloat stretch;
    public final ScalableFloat yaw;
    public final ScalableFloat pitch;
    public final int distance;
    public final int chance;
    public final int resolution;
    public final double cutoffStrength;
    public final boolean useWallNoise;
    public final boolean checkWater;
    public final FastNoise walls;
}
