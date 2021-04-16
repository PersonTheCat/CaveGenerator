package com.personthecat.cavegenerator.util;

import com.personthecat.cavegenerator.model.Direction;
import com.personthecat.cavegenerator.model.FloatRange;
import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.model.ScalableFloat;
import com.personthecat.cavegenerator.data.WallDecoratorSettings.Placement;
import fastnoise.FastNoise.CellularDistanceFunction;
import fastnoise.FastNoise.CellularReturnType;
import fastnoise.FastNoise.FractalType;
import fastnoise.FastNoise.Interp;
import fastnoise.FastNoise.NoiseType;
import lombok.AllArgsConstructor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import org.hjson.JsonObject;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.personthecat.cavegenerator.util.CommonMethods.runExF;

@AllArgsConstructor
public class HjsonMapper {

    private final JsonObject json;

    public HjsonMapper mapBool(String field, Consumer<Boolean> ifPresent) {
        HjsonTools.getBool(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapInt(String field, Consumer<Integer> ifPresent) {
        HjsonTools.getInt(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapIntList(String field, Consumer<List<Integer>> ifPresent) {
        HjsonTools.getIntList(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapFloat(String field, Consumer<Float> ifPresent) {
        HjsonTools.getFloat(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapString(String field, Consumer<String> ifPresent) {
        HjsonTools.getString(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRequiredString(String field, String parent, Consumer<String> mapper) {
        mapper.accept(HjsonTools.getString(json, field).orElseThrow(() -> runExF("{}.{} is required", parent, field)));
        return this;
    }

    public HjsonMapper mapBiomes(String field, Consumer<List<Biome>> ifPresent) {
        HjsonTools.getBiomeList(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRange(String field, Consumer<Range> ifPresent) {
        HjsonTools.getRange(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRangeOrTry(String field, String otherField, Consumer<Range> ifPresent) {
        final Optional<Range> range = HjsonTools.getRange(json, field);
        range.ifPresent(ifPresent);
        if (!range.isPresent()) {
            return mapRange(otherField, ifPresent);
        }
        return this;
    }

    public HjsonMapper mapFloatRange(String field, Consumer<FloatRange> ifPresent) {
        HjsonTools.getFloatRange(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapDistFunc(String field, Consumer<CellularDistanceFunction> ifPresent) {
        HjsonTools.getEnumValue(json, field, CellularDistanceFunction.class).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapReturnType(String field, Consumer<CellularReturnType> ifPresent) {
        HjsonTools.getEnumValue(json, field, CellularReturnType.class).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapFractalType(String field, Consumer<FractalType> ifPresent) {
        HjsonTools.getEnumValue(json, field, FractalType.class).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapInterp(String field, Consumer<Interp> ifPresent) {
        HjsonTools.getEnumValue(json, field, Interp.class).ifPresent(ifPresent);
        return this;
    }

    public <E extends Enum<E>> HjsonMapper mapEnum(String field, Class<E> e, Consumer<E> ifPresent) {
        HjsonTools.getEnumValue(json, field, e).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapNoiseType(String field, Consumer<NoiseType> ifPresent) {
        HjsonTools.getEnumValue(json, field, NoiseType.class).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapDirectionList(String field, Consumer<List<Direction>> ifPresent) {
        HjsonTools.getDirectionList(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapPlacementPreference(String field, Consumer<Placement> ifPresent) {
        HjsonTools.getEnumValue(json, field, Placement.class).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapState(String field, Consumer<IBlockState> ifPresent) {
        HjsonTools.getState(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapStateList(String field, Consumer<List<IBlockState>> ifPresent) {
        HjsonTools.getStateList(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRequiredState(String field, String parent, Consumer<IBlockState> mapper) {
        mapper.accept(HjsonTools.getState(json, field).orElseThrow(() -> runExF("{}.{} is required", parent, field)));
        return this;
    }

    public HjsonMapper mapRequiredStateList(String field, String parent, Consumer<List<IBlockState>> mapper) {
        final List<IBlockState> states = HjsonTools.getStateList(json, field)
            .orElseThrow(() -> runExF("{}.{} is required", parent, field));
        mapper.accept(states);
        return this;
    }

    public HjsonMapper mapBlockPos(String field, Consumer<BlockPos> ifPresent) {
        HjsonTools.getPosition(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapBlockPosList(String field, Consumer<List<BlockPos>> ifPresent) {
        HjsonTools.getPositionList(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapScalableFloat(String field, ScalableFloat defaults, Consumer<ScalableFloat> ifPresent) {
        if (json.has(field)) {
            ifPresent.accept(HjsonTools.getScalableFloatOr(json, field, defaults));
        }
        return this;
    }

    public HjsonMapper mapPlacementSettings(Consumer<PlacementSettings> mapper) {
        mapper.accept(HjsonTools.getPlacementSettings(json));
        return this;
    }

    public HjsonMapper mapObject(String field, Consumer<JsonObject> ifPresent) {
        HjsonTools.getObject(json, field).ifPresent(ifPresent);
        return this;
    }

    public <T> HjsonMapper mapArray(String field, Function<JsonObject, T> mapper, Consumer<List<T>> ifPresent) {
        if (json.has(field)) {
            final List<T> list = HjsonTools.getObjectArray(json, field).stream()
                .map(mapper)
                .collect(Collectors.toList());
            ifPresent.accept(list);
        }
        return this;
    }

    public HjsonMapper mapSelf(Consumer<JsonObject> mapper) {
        mapper.accept(this.json);
        return this;
    }

    public <T> T release(Supplier<T> supplier) {
        return supplier.get();
    }

}
