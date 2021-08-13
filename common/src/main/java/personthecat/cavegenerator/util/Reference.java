package personthecat.cavegenerator.util;

import personthecat.catlib.data.ModDescriptor;

public class Reference {
    public static final String MOD_NAME = "Cave Generator";
    public static final String MOD_ID = "cavegenerator";
    public static final String COMMAND_PREFIX = "cave";

    public static final ModDescriptor MOD_DESCRIPTOR = ModDescriptor.builder()
        .name(MOD_NAME)
        .modId(MOD_ID)
        .commandPrefix(COMMAND_PREFIX)
        .build();
}
