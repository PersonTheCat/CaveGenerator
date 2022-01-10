package personthecat.cavegenerator.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldNameConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.serialization.EasyStateCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static personthecat.catlib.serialization.CodecUtils.codecOf;
import static personthecat.catlib.serialization.CodecUtils.easyList;
import static personthecat.catlib.serialization.CodecUtils.easySet;
import static personthecat.catlib.serialization.CodecUtils.simpleEither;
import static personthecat.catlib.serialization.FieldDescriptor.field;

@AllArgsConstructor
@FieldNameConstants
public class BlockCheck {
    public final Set<BlockState> matchers;
    public final List<BlockPos> positions;

    public static final Codec<BlockCheck> OBJECT_CODEC = codecOf(
        field(easySet(EasyStateCodec.INSTANCE), Fields.matchers, s -> s.matchers),
        field(easyList(BlockPos.CODEC), Fields.positions, s -> s.positions),
        BlockCheck::new
    );

    public static final Codec<BlockCheck> ARRAY_CODEC = Codec.either(EasyStateCodec.INSTANCE, BlockPos.CODEC).listOf()
        .xmap(BlockCheck::fromEntries, BlockCheck::toEntries);

    public static final Codec<BlockCheck> CODEC = simpleEither(OBJECT_CODEC, ARRAY_CODEC);

    private static BlockCheck fromEntries(List<Either<BlockState, BlockPos>> entries) {
        final ImmutableSet.Builder<BlockState> matchers = ImmutableSet.builder();
        final ImmutableList.Builder<BlockPos> positions = ImmutableList.builder();
        for (final Either<BlockState, BlockPos> either : entries) {
            either.ifLeft(matchers::add).ifRight(positions::add);
        }
        return new BlockCheck(matchers.build(), positions.build());
    }

    private List<Either<BlockState, BlockPos>> toEntries() {
        final List<Either<BlockState, BlockPos>> entries = new ArrayList<>();
        this.matchers.forEach(m -> entries.add(Either.left(m)));
        this.positions.forEach(p -> entries.add(Either.right(p)));
        return entries;
    }
}
