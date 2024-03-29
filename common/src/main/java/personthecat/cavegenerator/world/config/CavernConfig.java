package personthecat.cavegenerator.world.config;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.Range;
import personthecat.fastnoise.FastNoise;

import java.util.List;

@AllArgsConstructor
public class CavernConfig {
    public final ConditionConfig conditions;
    public final DecoratorConfig decorators;
    public final Range bounds;
    public final int resolution;
    public final FastNoise offset;
    public final @Nullable FastNoise walls;
    public final FastNoise wallOffset;
    public final float wallCurveRatio;
    public final boolean wallInterpolation;
    public final List<FastNoise> generators;
    public final @Nullable TunnelConfig branches;
}
