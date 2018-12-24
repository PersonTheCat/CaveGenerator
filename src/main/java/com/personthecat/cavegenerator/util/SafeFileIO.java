package com.personthecat.cavegenerator.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import static com.personthecat.cavegenerator.util.CommonMethods.*;

/** A few potentially controversial ways for handling errors in file io. */
public class SafeFileIO {
    /**
     * Ensures that the input @param file refers to a directory,
     * creating one if nothing is found.
     */
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
    public static Result<SecurityException> safeMkdirs(File file) {
        try { // Standard mkdirs() call.
            file.mkdirs();
        } catch (SecurityException e) {
            // Error found. Return it.
            return Result.of(e);
        } // No error found. Return OK.
        return Result.ok();
    }

    /** Checks whether @param file exists, neatly throwing @param error, if needed. */
    public static boolean safeFileExists(File file, String error) {
        boolean ret;
        try {
            ret = file.exists();
        } catch (SecurityException e) {
            throw runEx(error);
        }
        return ret;
    }

    /** Equivalent of calling File#listFiles. Does not return null. */
    public static Optional<File[]> safeListFiles(File dir) {
        return Optional.ofNullable(dir.listFiles());
    }

    /** Standard stream copy process. Returns an exception, instead of throwing it. */
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
}