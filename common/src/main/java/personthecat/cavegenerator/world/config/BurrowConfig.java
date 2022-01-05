package personthecat.cavegenerator.world.config;

import lombok.AllArgsConstructor;
import personthecat.fastnoise.FastNoise;

@AllArgsConstructor
public class BurrowConfig {
    public final ConditionConfig conditions;
    public final DecoratorConfig decorators;
    public final FastNoise map;
    public final FastNoise offset;
    public final float radius;
    public final float target;
    public final float stretch;
    public final float exponent;
    public final float shift;
    public final float wallDistance;
    public final float wallExponent;
    public final TunnelConfig branches;
}
