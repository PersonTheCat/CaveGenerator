package personthecat.buildtools;

import dev.architectury.transformer.input.*;
import dev.architectury.transformer.transformers.base.AssetEditTransformer;
import dev.architectury.transformer.transformers.base.edit.TransformerContext;
import dev.architectury.transformer.util.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ArchitecturyCompat implements AssetEditTransformer {

    final Method getFS;
    final Field outputInterface;
    boolean argumentsFixed = false;

    public ArchitecturyCompat() {
        try {
            this.getFS = JarInputInterface.class.getDeclaredMethod("getFS");
            this.getFS.setAccessible(true);
        } catch (final NoSuchMethodException ignored) {
            throw new MissingFSMethodException();
        }
        try {
            this.outputInterface = OpenedOutputInterface.class.getDeclaredField("outputInterface");
            this.outputInterface.setAccessible(true);
        } catch (final NoSuchFieldException ignored) {
            throw new MissingOutputInterfaceFieldException();
        }
    }

    @Override
    public void doEdit(final TransformerContext context, final OutputInterface output) throws IOException {
        if (!this.argumentsFixed && context.canAppendArgument()) {
            fixArguments(context);
            this.argumentsFixed = true;
        }
        if (output instanceof AbstractOutputInterface) {
            Logger.info("Scanning source files in platform project.");
            final File src = getSourceDirectory();
            final List<String> projectFiles = this.getPathsEndingWith(src.toPath(), ".java");

            Logger.info("Found " + projectFiles.size() + " files. Looking for redundant entries.");
            final FileSystem fs = this.getFS(this.getJarInterface(output));
            final List<String> commonFiles = this.getPathsEndingWith(fs.getPath("/"), ".class");

            for (final String project : projectFiles) {
                for (final String common : commonFiles) {
                    if (project.endsWith(common)) {
                        Logger.info("Found overlapping class file: " + common + ".class. Deleting...");
                        Files.delete(fs.getPath(common + ".class"));
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void fixArguments(final TransformerContext context) {
        try {
            final Field appendArgument = context.getClass().getDeclaredField("appendArgument");
            appendArgument.setAccessible(true);
            final Object lambda = appendArgument.get(context);
            final Field argumentsField = lambda.getClass().getDeclaredFields()[0];
            argumentsField.setAccessible(true);
            final List<String> arguments = (List<String>) argumentsField.get(lambda);
            final List<String> broken = new ArrayList<>();
            for (final String argument : arguments) {
                if (argument.startsWith("/")) {
                    broken.add(argument);
                }
            }
            for (final String argument : broken) {
                arguments.remove(argument);
                arguments.add(argument.substring(1));
            }
        } catch (final Exception e) {
            Logger.info("Error inspecting launch arguments: " + e);
        }
    }

    private static File getSourceDirectory() {
        final String path = System.getProperty("architectury.main.class");
        Objects.requireNonNull(path, "Expected Architectury");
        final int gradleIndex = path.lastIndexOf(".gradle");
        assert gradleIndex > 0 : "Invalid Architectury path";
        return new File(path.substring(0, gradleIndex), "src/main/java");
    }

    private List<String> getPathsEndingWith(final Path root, final String ext) throws IOException {
        return Files.walk(root)
            .filter(p -> p.toString().endsWith(ext))
            .map(p -> {
                final String s = p.toString();
                return s.substring(0, s.length() - ext.length()).replace("\\", "/");
            }).collect(Collectors.toList());
    }

    private JarInputInterface getJarInterface(final OutputInterface output) {
        final OpenedOutputInterface opened = this.getOpenedOutput(output);
        final OutputInterface wrapped = this.getWrappedInterface(opened);
        if (!(wrapped instanceof JarInputInterface)) {
            throw new UnexpectedOutputTargetException(wrapped);
        }
        return (JarInputInterface) wrapped;
    }

    private OpenedOutputInterface getOpenedOutput(final OutputInterface output) {
        for (final Field f : output.getClass().getDeclaredFields()) {
            if (f.getType().isAssignableFrom(OpenedOutputInterface.class)) {
                f.setAccessible(true);
                try {
                    return (OpenedOutputInterface) f.get(output);
                } catch (final IllegalAccessException e) {
                    throw new OutputNotAccessibleException(output, f);
                }
            }
        }
        throw new UnexpectedCallerException(output);
    }

    private OutputInterface getWrappedInterface(final OpenedOutputInterface output) {
        try {
            return (OutputInterface) this.outputInterface.get(output);
        } catch (final IllegalAccessException ignored) {
            throw new OutputInterfaceNotFoundException();
        }
    }

    private FileSystem getFS(final JarInputInterface input) {
        try {
            return (FileSystem) this.getFS.invoke(input);
        } catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException ignored) {
            throw new FileSystemNotFoundException();
        }
    }

    private static class UnexpectedCallerException extends RuntimeException {
        UnexpectedCallerException(final OutputInterface output) {
            super("Expected transformer runtime. Received: " + output.getClass().getName());
        }
    }

    private static class OutputNotAccessibleException extends RuntimeException {
        OutputNotAccessibleException(final Object o, final Field f) {
            super ("Error reading output from field: " + o.getClass().getName() + "#" + f.getName());
        }
    }

    private static class FileSystemNotFoundException extends RuntimeException {
        FileSystemNotFoundException() {
            super("Getting jar FS");
        }
    }

    private static class OutputInterfaceNotFoundException extends RuntimeException {
        OutputInterfaceNotFoundException() {
            super("Getting jar output interface");
        }
    }

    private static class UnexpectedOutputTargetException extends RuntimeException {
        UnexpectedOutputTargetException(final OutputInterface output) {
            super("Expected jar output target. Received: " + output.getClass().getName());
        }
    }

    private static class MissingFSMethodException extends RuntimeException {
        MissingFSMethodException() {
            super("Error locating getFS method of JarInputInterface. Update needed?");
        }
    }

    private static class MissingOutputInterfaceFieldException extends RuntimeException {
        MissingOutputInterfaceFieldException() {
            super("Error locating outputInterface field of OpenedOutputInterface");
        }
    }
}
