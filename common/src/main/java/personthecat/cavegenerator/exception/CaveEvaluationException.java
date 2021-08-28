package personthecat.cavegenerator.exception;

import org.hjson.JsonObject;
import personthecat.catlib.util.HjsonUtils;

import java.io.File;

public class CaveEvaluationException extends RuntimeException {
    private static final int MAX_LENGTH = 25;

    public CaveEvaluationException(final File f) {
        super("Error evaluating Cave file: " + f.getName());
    }

    public CaveEvaluationException(final JsonObject f) {
        super("Error evaluating Cave expression: " + readCropping(f));
    }

    private static String readCropping(final JsonObject f) {
        final String data = f.toString(HjsonUtils.NO_CR);
        if (data.length() > MAX_LENGTH) {
            return data.substring(0, MAX_LENGTH) + "...";
        }
        return data;
    }
}
