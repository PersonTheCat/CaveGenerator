package personthecat.cavegenerator.presets;

import org.hjson.JsonObject;

public class CaveOutput {
    public final JsonObject complete;
    public final JsonObject generated;
    public final JsonObject user;

    public CaveOutput(final JsonObject complete, final JsonObject generated, final JsonObject user) {
        this.complete = complete;
        this.generated = generated;
        this.user = user;
    }
}
