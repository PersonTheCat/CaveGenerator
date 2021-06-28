package com.personthecat.cavegenerator.io;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.util.Result;

import javax.annotation.CheckReturnValue;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.personthecat.cavegenerator.util.CommonMethods.empty;
import static com.personthecat.cavegenerator.util.CommonMethods.full;
import static com.personthecat.cavegenerator.util.CommonMethods.nullable;
import static com.personthecat.cavegenerator.util.CommonMethods.runEx;
import static com.personthecat.cavegenerator.util.CommonMethods.runExF;
import static com.personthecat.cavegenerator.CaveInit.BACKUP_DIR;

/** A few potentially controversial ways for handling errors in file io. */
public class SafeFileIO {

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
        backup(file, false);
    }

    /** Copies (or moves) a file to the backup directory. */
    public static void backup(File file, boolean move) {
        if (!fileExists(BACKUP_DIR, "Unable to handle backup directory.")) {
            mkdirs(BACKUP_DIR).throwIfPresent();
        }
        final File backup = new File(BACKUP_DIR, file.getName());
        final BackupHelper helper = new BackupHelper(file);
        helper.cycle();
        if (fileExists(backup, "Unable to handle existing backup file.")) {
            throw runExF("Could not rename backups: {}", file.getName());
        }
        if (move) {
            if (!file.renameTo(backup)) {
                throw runExF("Error moving {} to backups", file.getName());
            }
        } else {
            copy(file, BACKUP_DIR).throwIfPresent();
        }
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

    @CheckReturnValue
    public static Optional<File[]> listFiles(File dir, FileFilter filter) {
        return Optional.ofNullable(dir.listFiles(filter));
    }

    @CheckReturnValue
    public static Optional<File> getFileRecursive(File dir, FileFilter filter) {
        final File[] inDir = dir.listFiles();
        if (inDir != null) {
            for (File f : inDir) {
                if (f.isDirectory()) {
                    final Optional<File> found = getFileRecursive(f, filter);
                    if (found.isPresent()) {
                        return found;
                    }
                } else if (filter.accept(f)) {
                    return full(f);
                }
            }
        }
        return empty();
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

    private static class BackupHelper {
        final String base;
        final String ext;
        final Pattern pattern;

        BackupHelper(File file) {
            final String name = file.getName();
            final int dotIndex = name.indexOf(".");
            if (dotIndex > 0) {
                base = name.substring(0, dotIndex);
                ext = name.substring(dotIndex);
            } else {
                base = name;
                ext = "";
            }
            pattern = Pattern.compile(base + "(\\s\\((\\d+)\\))?" + ext);
        }

        void cycle() {
            final File[] arr = BACKUP_DIR.listFiles(this::matches);
            if (arr == null) return;
            final List<File> matching = Arrays.asList(arr);
            matching.sort(this::compare);
            final int end = this.getFirstGap(matching);
            for (int i = end - 1; i >= 0; i--) {
                final File f = matching.get(i);
                final int number = i + 1;
                final File newFile = new File(f.getParentFile(), base + " (" + number + ")" + ext);
                if (!f.renameTo(newFile)) {
                    throw runExF("Could not increment backup: {}", f.getName());
                }
            }
        }

        boolean matches(File file) {
            return pattern.matcher(file.getName()).matches();
        }

        private int compare(File f1, File f2) {
            return Integer.compare(this.getNumber(f1), this.getNumber(f2));
        }

        int getNumber(File file) {
            final Matcher matcher = pattern.matcher(file.getName());
            if (!matcher.find()) throw runExF("Backup deleted externally: {}", file.getName());
            final String g2 = matcher.group(2);
            return g2 == null ? 0 : Integer.parseInt(g2);
        }

        int getFirstGap(List<File> files) {
            int lastNum = 0;
            for (File f : files) {
                final int num = this.getNumber(f) + 1;
                if (num - lastNum > 1) {
                    return lastNum;
                }
                lastNum = num;
            }
            return files.size();
        }
    }
}