package personthecat.cavegenerator.world.config;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.Range;
import personthecat.cavegenerator.model.ScalableFloat;

@AllArgsConstructor
public class TunnelConfig {
    public final ConditionConfig conditions;
    public final DecoratorConfig decorators;
    public final Range originHeight;
    public final boolean noiseYReduction;
    public final boolean resizeBranches;
    public final @Nullable TunnelConfig branches;
    public final @Nullable RoomConfig rooms;
    public final ScalableFloat dYaw;
    public final ScalableFloat dPitch;
    public final ScalableFloat scale;
    public final ScalableFloat stretch;
    public final ScalableFloat yaw;
    public final ScalableFloat pitch;
    public final int systemChance;
    public final int chance;
    public final int systemDensity;
    public final int distance;
    public final int count;
    public final int resolution;
    public final @Nullable Long seed;
    public final boolean reseedBranches;
    public final boolean hasBranches;
    public final boolean checkWater;
}
