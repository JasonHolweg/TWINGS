package dev.strace.twings.wing;

import dev.strace.twings.Main;
import dev.strace.twings.util.MyColors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads and caches all wings. Wings are addressed by their id (file name
 * without .yml) instead of File objects, and nothing here ever touches the
 * disk outside of {@link #loadAll()} / {@link #reloadWing(String)}.
 */
public final class WingManager {

    /** Pseudo-category meaning "all wings" (legacy value used by 2.x GUIs). */
    public static final String ALL = "XXX";

    private final Main plugin;
    private final Map<String, Wing> byId = new LinkedHashMap<>();

    public WingManager(Main plugin) {
        this.plugin = plugin;
    }

    public File wingsDir() {
        return new File(plugin.getDataFolder(), "wings");
    }

    public void loadAll() {
        File dir = wingsDir();
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().severe("Could not create " + dir);
        }
        new File(plugin.getDataFolder(), "pictures").mkdirs();
        WingFiles.ensureTemplate(plugin, dir);
        extractPresets(dir);

        byId.clear();
        int failed = 0;
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    Wing wing = Wing.load(file);
                    byId.put(wing.idLower(), wing);
                } catch (WingLoadException e) {
                    failed++;
                    plugin.getLogger().warning("Skipped wing " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("Loaded " + byId.size() + " wings" + (failed > 0 ? " (" + failed + " failed)" : "") + ".");
    }

    private static final List<String> PRESETS = List.of("angel", "fire", "fairy", "bat", "ice", "nature");

    /**
     * Unpacks the bundled preset wings once (marker file), so the plugin is
     * fun out of the box but deleted presets stay deleted.
     */
    private void extractPresets(File dir) {
        File marker = new File(dir, ".presets_installed");
        if (marker.exists()) return;
        int extracted = 0;
        for (String id : PRESETS) {
            File target = new File(dir, id + ".yml");
            if (target.exists()) continue;
            try (InputStream in = plugin.getResource("presets/" + id + ".yml")) {
                if (in == null) continue;
                Files.copy(in, target.toPath());
                extracted++;
            } catch (IOException e) {
                plugin.getLogger().warning("Could not extract preset " + id + ": " + e.getMessage());
            }
        }
        try {
            if (!marker.createNewFile()) {
                plugin.getLogger().warning("Could not create preset marker file.");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not create preset marker file: " + e.getMessage());
        }
        if (extracted > 0) {
            plugin.getLogger().info("Installed " + extracted + " preset wings.");
        }
    }

    /** Re-parses a single wing file (used by the live edit mode). */
    public boolean reloadWing(String id) {
        Wing old = get(id);
        if (old == null) return false;
        try {
            Wing fresh = Wing.load(old.file());
            byId.put(fresh.idLower(), fresh);
            return true;
        } catch (WingLoadException e) {
            plugin.getLogger().warning("Edit reload failed: " + e.getMessage());
            return false;
        }
    }

    public Wing get(String id) {
        if (id == null) return null;
        return byId.get(id.toLowerCase(Locale.ROOT));
    }

    /** Accepts "name" as well as legacy "name.yml" references. */
    public Wing byFileName(String fileName) {
        if (fileName == null) return null;
        return get(fileName.replaceAll("(?i)\\.yml$", ""));
    }

    public Collection<Wing> all() {
        return byId.values();
    }

    public List<Wing> byCategory(String category) {
        List<Wing> list = new ArrayList<>();
        for (Wing wing : byId.values()) {
            if (ALL.equalsIgnoreCase(category) || wing.category().equalsIgnoreCase(category)) {
                list.add(wing);
            }
        }
        return list;
    }

    /**
     * Matches a command argument against wing ids and display names
     * (color codes stripped, spaces as underscores) — the same inputs the
     * 2.x clickable lists produced.
     */
    public Wing matchByInput(String input) {
        if (input == null || input.isEmpty()) return null;
        Wing byIdMatch = get(input);
        if (byIdMatch != null) return byIdMatch;
        for (Wing wing : byId.values()) {
            String plain = MyColors.strip(wing.itemName()).replace(" ", "_");
            if (plain.equalsIgnoreCase(input)) return wing;
        }
        return null;
    }

    /** Plain display names (underscored) plus ids, for tab completion. */
    public List<String> inputSuggestions() {
        List<String> out = new ArrayList<>();
        for (Wing wing : byId.values()) {
            String plain = MyColors.strip(wing.itemName()).replace(" ", "_");
            if (!plain.isEmpty() && !out.contains(plain)) out.add(plain);
        }
        return out;
    }
}
