package dev.strace.twings.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * Thin wrapper around a YAML data file. Unlike the old ConfigManager it never
 * touches the disk on construction — loading is explicit and saving can be
 * pushed off the main thread.
 */
public class YamlFile {

    private final File file;
    private final Object writeLock = new Object();
    private YamlConfiguration cfg;

    public YamlFile(File file) {
        this.file = file;
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    public YamlFile(Plugin plugin, String fileName) {
        this(new File(plugin.getDataFolder(), fileName));
    }

    public YamlConfiguration cfg() {
        return cfg;
    }

    public File file() {
        return file;
    }

    public void reload() {
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    /** Synchronous save; use only during startup/shutdown. */
    public boolean save(Plugin plugin) {
        try {
            cfg.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getName(), e);
            return false;
        }
    }

    /**
     * Serializes on the calling (main) thread, writes on an async thread.
     * Concurrent writes to the same file are serialized via a lock.
     */
    public void saveAsync(Plugin plugin) {
        final String data = cfg.saveToString();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (writeLock) {
                try {
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new IOException("could not create " + parent);
                    }
                    Files.write(file.toPath(), data.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getName(), e);
                }
            }
        });
    }
}
