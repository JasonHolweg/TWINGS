package dev.strace.twings.wing;

import java.io.File;

/** A wing file could not be parsed; carries a human-readable reason. */
public class WingLoadException extends Exception {

    public WingLoadException(File file, String reason) {
        super(file.getName() + ": " + reason);
    }
}
