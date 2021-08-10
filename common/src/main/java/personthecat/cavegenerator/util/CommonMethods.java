package personthecat.cavegenerator.util;

public class CommonMethods {
    // Todo: move to CatLib
    public static int invert(double value) {
        return value == 0 ? Integer.MAX_VALUE : (int) (1 / value);
    }
}
