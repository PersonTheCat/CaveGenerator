package com.personthecat.cavegenerator.io;

import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.util.Result;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.CheckReturnValue;
import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import static com.personthecat.cavegenerator.util.CommonMethods.*;

/** A few potentially controversial ways for handling errors in file io. */
public class SafeFileIO {

    /** The directory where all file backups will be stored. */
    private static final File BACKUP_DIR = new File(Loader.instance().getConfigDir(), Main.MODID + "/backup");

    /**
     * Ensures that the input @param file refers to a directory, creating one if nothing is found.
     */
    @CheckReturnValue
    public static Result<SecurityException> ensureDirExists(File file) {
        try { // Test for and create all directories.
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (SecurityException e) {
            // Error found. Return it.
            return Result.of(e);
        } // No error found. Return OK.
        return Result.ok();
    }

    /** Safely calls File#mkdirs without testing. */
    @CheckReturnValue
    public static Result<SecurityException> mkdirs(File file) {
        try { // Standard mkdirs() call.
            file.mkdirs();
        } catch (SecurityException e) {
            // Error found. Return it.
            return Result.of(e);
        } // No error found. Return OK.
        return Result.ok();
    }

    /** Checks whether @param file exists, neatly throwing @param error, if needed. */
    public static boolean fileExists(File file, String error) {
        boolean ret;
        try {
            ret = file.exists();
        } catch (SecurityException e) {
            throw runEx(error);
        }
        return ret;
    }

    /** Copies a file to the specified directory. May look clean more than it is actually safe. */
    @CheckReturnValue
    public static Result<IOException> copy(File file, File toDir) {
        try {
            Files.copy(file.toPath(), new File(toDir, file.getName()).toPath());
        } catch (IOException e) {
            return Result.of(e);
        }
        return Result.ok();
    }

    /** Copies a file to the backup directory. */
    public static void backup(File file) {
        final File backup = new File(BACKUP_DIR, file.getName());
        if (!fileExists(BACKUP_DIR, "Unable to handle backup directory.")) {
            mkdirs(BACKUP_DIR).throwIfPresent();
        }
        if (fileExists(backup, "Unable to handle existing backup file.")) {
            backup.delete();
        }
        copy(file, BACKUP_DIR).throwIfPresent();
    }

    /** Renames a file when given a top-level name only. */
    public static void rename(File file, String name) {
        final File path = new File(file.getParentFile(), name);
        if (!file.renameTo(path)) {
            throw runExF("Cannot rename: {}", path);
        }
    }

    /** Equivalent of calling File#listFiles. Does not return null. */
    @CheckReturnValue
    public static Optional<File[]> listFiles(File dir) {
        return Optional.ofNullable(dir.listFiles());
    }

    /** Attempts to retrieve the contents of the input file. */
    public static Optional<List<String>> safeContents(File file) {
        try {
            return full(Files.readAllLines(file.toPath()));
        } catch (IOException ignored) {
            return empty();
        }
    }

    /** Standard stream copy process. Returns an exception, instead of throwing it. */
    @CheckReturnValue
    public static Result<IOException> copyStream(InputStream input, OutputStream output, int bufferSize) {
        byte[] buffer = new byte[bufferSize];
        int length;
        try {
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
        } catch (IOException e) {
            return Result.of(e);
        }
        return Result.ok();
    }

    /** Retrieves an asset from the jar file. */
    @CheckReturnValue
    public static Optional<InputStream> getResource(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return nullable(SafeFileIO.class.getResourceAsStream(path));
    }

    /** Retrieves an asset from the jar file */
    public static InputStream getRequiredResource(String path) {
        return getResource(path)
            .orElseThrow(() -> runExF("The required file \"{}\" was not present in the jar.", path));
    }
}