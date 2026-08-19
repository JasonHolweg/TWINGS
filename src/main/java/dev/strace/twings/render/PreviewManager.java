package dev.strace.twings.render;

import dev.strace.twings.Main;
import dev.strace.twings.util.YamlFile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Preview locations (locations.yml, same schema as 2.x). Loaded once —
 * the old code re-read and re-saved the file every few ticks, forever.
 */
public final class PreviewManager {

    public record StoredLocation(String world, double x, double y, double z, float yaw, float pitch) {

        public Location resolve() {
            World w = Bukkit.getWorld(world);
            if (w == null) return null;
            return new Location(w, x, y, z, yaw, pitch);
        }
    }

    private final Main plugin;
    private final YamlFile file;
    private final Map<String, StoredLocation> previews = new HashMap<>();

    public PreviewManager(Main plugin) {
        this.plugin = plugin;
        this.file = new YamlFile(plugin, "locations.yml");
    }

    public void load() {
        previews.clear();
        for (String key : file.cfg().getKeys(false)) {
            String world = file.cfg().getString(key + ".World");
            if (world == null) continue;
            previews.put(key, new StoredLocation(world,
                    file.cfg().getDouble(key + ".X"),
                    file.cfg().getDouble(key + ".Y"),
                    file.cfg().getDouble(key + ".Z"),
                    (float) file.cfg().getDouble(key + ".Yaw"),
                    (float) file.cfg().getDouble(key + ".Pitch")));
        }
    }

    public void reload() {
        file.reload();
        load();
    }

    public void set(String wingId, Location loc) {
        String key = wingId.replace(".", "");
        file.cfg().set(key + ".name", wingId);
        if (loc.getWorld() != null) file.cfg().set(key + ".World", loc.getWorld().getName());
        file.cfg().set(key + ".X", loc.getX());
        file.cfg().set(key + ".Y", loc.getY());
        file.cfg().set(key + ".Z", loc.getZ());
        file.cfg().set(key + ".Yaw", loc.getYaw());
        file.cfg().set(key + ".Pitch", loc.getPitch());
        file.saveAsync(plugin);
        load();
    }

    public void remove(String wingId) {
        file.cfg().set(wingId.replace(".", ""), null);
        file.saveAsync(plugin);
        load();
    }

    /** wingId (map key) -> stored location. */
    public Map<String, StoredLocation> all() {
        return previews;
    }
}
