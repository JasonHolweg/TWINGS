package dev.strace.twings.equipment;

import dev.strace.twings.Main;
import dev.strace.twings.util.YamlFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Which player has which wings equipped. Pure in-memory state with async
 * persistence to wings_equiped.yml (file name kept from 2.x so existing
 * data survives the upgrade).
 *
 * <p>2.x stored multiple wings as one string joined with "(X-X)" and then
 * split it with that string as a regex — the parentheses formed a capture
 * group, so multi-wing entries broke on every restart. Old entries are
 * migrated to proper string lists on first load.</p>
 */
public final class EquipmentManager {

    public enum AddResult {
        OK, ALREADY_EQUIPPED, MAX_REACHED
    }

    private static final Pattern LEGACY_SEPARATOR = Pattern.compile(Pattern.quote("(X-X)"));

    private final Main plugin;
    private final YamlFile store;
    private final Map<UUID, List<String>> equipped = new HashMap<>();

    public EquipmentManager(Main plugin) {
        this.plugin = plugin;
        this.store = new YamlFile(plugin, "wings_equiped.yml");
    }

    public void load() {
        equipped.clear();
        boolean migrated = false;
        for (String key : store.cfg().getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("wings_equiped.yml: skipping invalid entry '" + key + "'");
                continue;
            }
            List<String> ids;
            if (store.cfg().isList(key)) {
                ids = new ArrayList<>(store.cfg().getStringList(key));
            } else {
                ids = parseLegacyValue(store.cfg().getString(key, ""));
                store.cfg().set(key, ids);
                migrated = true;
            }
            if (!ids.isEmpty()) equipped.put(uuid, ids);
        }
        if (migrated) {
            store.saveAsync(plugin);
            plugin.getLogger().info("Migrated wings_equiped.yml to the 3.0 list format.");
        }
    }

    public List<String> getEquipped(UUID uuid) {
        return equipped.getOrDefault(uuid, List.of());
    }

    public boolean hasEquipped(UUID uuid, String wingId) {
        for (String id : getEquipped(uuid)) {
            if (id.equalsIgnoreCase(wingId)) return true;
        }
        return false;
    }

    /** Replaces all equipped wings with the given one. */
    public void set(UUID uuid, String wingId) {
        List<String> list = new ArrayList<>(1);
        list.add(wingId);
        equipped.put(uuid, list);
        persist(uuid);
    }

    public AddResult add(UUID uuid, String wingId) {
        List<String> list = equipped.computeIfAbsent(uuid, k -> new ArrayList<>(2));
        if (hasEquipped(uuid, wingId)) return AddResult.ALREADY_EQUIPPED;
        if (list.size() >= plugin.settings().maxEquipped()) return AddResult.MAX_REACHED;
        list.add(wingId);
        persist(uuid);
        return AddResult.OK;
    }

    public boolean remove(UUID uuid, String wingId) {
        List<String> list = equipped.get(uuid);
        if (list == null) return false;
        boolean removed = list.removeIf(id -> id.equalsIgnoreCase(wingId));
        if (removed) {
            if (list.isEmpty()) equipped.remove(uuid);
            persist(uuid);
        }
        return removed;
    }

    public void clear(UUID uuid) {
        if (equipped.remove(uuid) != null) {
            persist(uuid);
        }
    }

    /** Parses a 2.x single-string entry, possibly "(X-X)"-joined, into wing ids. */
    public static List<String> parseLegacyValue(String raw) {
        List<String> ids = new ArrayList<>();
        if (raw == null) return ids;
        for (String part : LEGACY_SEPARATOR.split(raw)) {
            String id = part.replaceAll("(?i)\\.yml$", "").trim();
            if (!id.isEmpty()) ids.add(id);
        }
        return ids;
    }

    private void persist(UUID uuid) {
        List<String> list = equipped.get(uuid);
        store.cfg().set(uuid.toString(), list == null || list.isEmpty() ? null : list);
        store.saveAsync(plugin);
    }

    /** Called from onDisable — one last synchronous write. */
    public void flushSync() {
        store.save(plugin);
    }
}
