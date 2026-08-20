package dev.strace.twings.equipment;

import dev.strace.twings.Main;
import dev.strace.twings.util.YamlFile;
import dev.strace.twings.wing.Wing;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

/**
 * Wings granted for a limited time (/wings give ... 5m) or until death.
 * 2.x kept timers only in memory, so a restart made timed wings permanent
 * and long durations could overflow the tick delay. 3.0 persists the expiry
 * timestamp (timed.yml) and checks it periodically.
 */
public final class TimedEquips {

    private final Main plugin;
    private final YamlFile untilDeath;
    private final YamlFile timed;
    private long lastCheckMs;

    public TimedEquips(Main plugin) {
        this.plugin = plugin;
        this.untilDeath = new YamlFile(plugin, "untildeath.yml");
        this.timed = new YamlFile(plugin, "timed.yml");
    }

    /** Parses "300M", "5h", "18000S", "2d", "untildeath" or plain seconds; -1 if invalid, 0 = until death. */
    public static long parseSeconds(String input) {
        if (input == null || input.isEmpty()) return -1;
        String s = input.trim().toLowerCase(Locale.ROOT);
        if (s.equals("untildeath")) return 0;
        long factor = 1;
        char unit = s.charAt(s.length() - 1);
        String number = s;
        if (Character.isLetter(unit)) {
            number = s.substring(0, s.length() - 1);
            switch (unit) {
                case 's' -> factor = 1;
                case 'm' -> factor = 60;
                case 'h' -> factor = 3600;
                case 'd' -> factor = 86400;
                default -> {
                    return -1;
                }
            }
        }
        try {
            long value = Long.parseLong(number);
            return value <= 0 ? -1 : value * factor;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Human-readable form of what was granted, for the "wings given" message. */
    public static String describe(String input) {
        long seconds = parseSeconds(input);
        if (seconds == 0) return "untildeath";
        char unit = Character.toLowerCase(input.charAt(input.length() - 1));
        String number = Character.isLetter(unit) ? input.substring(0, input.length() - 1) : input;
        return switch (unit) {
            case 'm' -> number + " minutes";
            case 'h' -> number + " hours";
            case 'd' -> number + " days";
            default -> number + " seconds";
        };
    }

    public void giveUntilDeath(Player target, Wing wing) {
        plugin.equipment().set(target.getUniqueId(), wing.id());
        untilDeath.cfg().set(target.getUniqueId().toString(), wing.id());
        untilDeath.saveAsync(plugin);
        target.sendMessage(plugin.messages().getWingsReceived("untildeath"));
    }

    public void giveTimed(Player target, Wing wing, long seconds, String describedTime) {
        plugin.equipment().set(target.getUniqueId(), wing.id());
        timed.cfg().set(target.getUniqueId() + "." + wing.idLower(),
                System.currentTimeMillis() + seconds * 1000L);
        timed.saveAsync(plugin);
        target.sendMessage(plugin.messages().getWingsReceived(describedTime));
    }

    public void onDeath(Player p) {
        String key = p.getUniqueId().toString();
        String wingId = untilDeath.cfg().getString(key);
        if (wingId == null) return;
        plugin.equipment().remove(p.getUniqueId(), wingId.replaceAll("(?i)\\.yml$", ""));
        untilDeath.cfg().set(key, null);
        untilDeath.saveAsync(plugin);
        if (plugin.messages().isShowMessages()) {
            p.sendMessage(plugin.messages().getWingsGone());
        }
    }

    public void onJoin(Player p) {
        expireFor(p, System.currentTimeMillis());
    }

    /** Called from the render tick; does real work at most once per second. */
    public void tick(long nowMs) {
        if (nowMs - lastCheckMs < 1000) return;
        lastCheckMs = nowMs;
        for (Player p : Bukkit.getOnlinePlayers()) {
            expireFor(p, nowMs);
        }
    }

    private void expireFor(Player p, long nowMs) {
        var section = timed.cfg().getConfigurationSection(p.getUniqueId().toString());
        if (section == null) return;
        boolean changed = false;
        for (String wingId : section.getKeys(false)) {
            if (section.getLong(wingId) <= nowMs) {
                section.set(wingId, null);
                changed = true;
                plugin.equipment().remove(p.getUniqueId(), wingId);
                if (plugin.messages().isShowMessages()) {
                    p.sendMessage(plugin.messages().getWingsGone());
                }
            }
        }
        if (changed) {
            if (section.getKeys(false).isEmpty()) {
                timed.cfg().set(p.getUniqueId().toString(), null);
            }
            timed.saveAsync(plugin);
        }
    }

    public void flushSync() {
        untilDeath.save(plugin);
        timed.save(plugin);
    }
}
