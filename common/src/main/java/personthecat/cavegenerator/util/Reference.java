package personthecat.cavegenerator.util;

import personthecat.catlib.data.ModDescriptor;

import java.util.Arrays;
import java.util.List;

public class Reference {
    public static final String MOD_NAME = "Cave Generator";
    public static final String MOD_ID = "cavegenerator";
    public static final String COMMAND_PREFIX = "cave";

    public static final ModDescriptor MOD =
        ModDescriptor.builder()
            .name(MOD_NAME)
            .modId(MOD_ID)
            .commandPrefix(COMMAND_PREFIX)
            .defaultLinter(new CaveLinter())
            .build();

    public static final List<String> VALID_EXTENSIONS = Arrays.asList("hjson", "json", "cave");
}
