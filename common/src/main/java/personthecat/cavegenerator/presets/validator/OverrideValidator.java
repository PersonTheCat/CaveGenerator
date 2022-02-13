package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;
import personthecat.cavegenerator.presets.data.ConditionSettings;
import personthecat.cavegenerator.presets.data.DecoratorSettings;
import personthecat.cavegenerator.presets.data.OverrideSettings;

public class OverrideValidator {

    private OverrideValidator() {}

    public static void apply(final ValidationContext ctx, final OverrideSettings s) {
        final ConditionSettings conditions =
            ConditionSettings.builder()
                .biomes(s.biomes)
                .ceiling(s.ceiling)
                .dimensions(s.dimensions)
                .floor(s.floor)
                .height(s.height)
                .noise(s.noise)
                .region(s.region)
                .build();
        final DecoratorSettings decorators =
            DecoratorSettings.builder()
                .caveBlocks(s.caveBlocks)
                .ponds(s.ponds)
                .replaceableBlocks(s.replaceableBlocks)
                .replaceDecorators(s.replaceDecorators)
                .replaceSolidBlocks(s.replaceSolidBlocks)
                .shell(s.shell)
                .wallDecorators(s.wallDecorators)
                .build();
        ConditionValidator.apply(ctx, conditions, JsonPath.stub());
        DecoratorValidator.apply(ctx, decorators, JsonPath.stub());
    }
}
