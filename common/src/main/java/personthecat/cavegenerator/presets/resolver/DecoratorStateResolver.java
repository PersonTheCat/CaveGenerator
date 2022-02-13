package personthecat.cavegenerator.presets.resolver;

import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ArrayUtils;
import org.hjson.JsonObject;
import personthecat.catlib.util.HjsonUtils;
import personthecat.catlib.util.JsonTransformer;
import personthecat.cavegenerator.presets.data.*;

import java.util.ArrayList;
import java.util.List;

public class DecoratorStateResolver {

    public static List<BlockState> resolveBlockStates(final JsonObject preset) {
        final List<BlockState> decorators = new ArrayList<>();
        addAllDecorators(decorators, preset); // Top-level overrides
        addAllDecorators(decorators, preset, OverrideSettings.Fields.branches);
        addAllDecorators(decorators, preset, OverrideSettings.Fields.rooms);
        addAllDecorators(decorators, preset, CaveSettings.Fields.tunnels);
        addAllDecorators(decorators, preset, CaveSettings.Fields.tunnels, TunnelSettings.Fields.branches);
        addAllDecorators(decorators, preset, CaveSettings.Fields.ravines);
        addAllDecorators(decorators, preset, CaveSettings.Fields.caverns);
        addAllDecorators(decorators, preset, CaveSettings.Fields.caverns, CavernSettings.Fields.branches);
        addAllDecorators(decorators, preset, CaveSettings.Fields.burrows, BurrowSettings.Fields.branches);
        addAll(decorators, preset, ClusterSettings.Fields.states, CaveSettings.Fields.clusters);
        addAll(decorators, preset, LayerSettings.Fields.state, CaveSettings.Fields.layers);
        return decorators;
    }

    private static void addAllDecorators(final List<BlockState> decorators, final JsonObject json, final String... path) {
        JsonTransformer.withPath(ArrayUtils.add(path, DecoratorSettings.Fields.wallDecorators))
            .forEach(json, j -> addAll(decorators, j, WallDecoratorSettings.Fields.states));
        JsonTransformer.withPath(ArrayUtils.add(path, DecoratorSettings.Fields.caveBlocks))
            .forEach(json, j -> addAll(decorators, j, CaveBlockSettings.Fields.states));
        JsonTransformer.withPath(ArrayUtils.add(path, DecoratorSettings.Fields.ponds))
            .forEach(json, j -> addAll(decorators, j, PondSettings.Fields.states));
        JsonTransformer.withPath(ArrayUtils.addAll(path, DecoratorSettings.Fields.shell, ShellSettings.Fields.decorators))
            .forEach(json, j -> addAll(decorators, j, ShellSettings.Decorator.Fields.states));
    }

    private static void addAll(final List<BlockState> decorators, final JsonObject json, final String field, final String... path) {
        JsonTransformer.withPath(path).forEach(json, j ->
            HjsonUtils.getStateList(j, field).ifPresent(decorators::addAll));
    }
}
