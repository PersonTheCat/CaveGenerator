package personthecat.cavegenerator.commands.arguments;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import personthecat.catlib.data.Lazy;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static personthecat.catlib.exception.Exceptions.cmdSyntax;

public class FeatureArgument implements ArgumentType<Feature<?>> {

    private static final Lazy<List<String>> FEATURES;

    public static void register() {
        ArgumentTypes.register("cavegenerator:feature_argument", FeatureArgument.class,
            new EmptyArgumentSerializer<>(FeatureArgument::new));
    }

    @Override
    public Feature<?> parse(final StringReader reader) throws CommandSyntaxException {
        final Feature<?> feature = Registry.FEATURE.get(new ResourceLocation(reader.readString()));
        if (feature == null) {
            throw cmdSyntax(reader, "Feature not found");
        }
        return feature;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> ctx, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(FEATURES.get(), builder);
    }

    static {
        FEATURES = Lazy.of(() -> {
            final ImmutableList.Builder<String> builder = ImmutableList.builder();
            Registry.FEATURE.keySet().forEach(id -> {
                if ("minecraft".equals(id.getNamespace())) builder.add(id.getPath());
                builder.add(id.toString());
            });
            return builder.build();
        });
    }
}
