package dev.strace.twings.commands;

import dev.strace.twings.Main;
import dev.strace.twings.equipment.TimedEquips;
import dev.strace.twings.wing.Wing;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class Give extends SubCommand {

    public Give(Main plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "give";
    }

    @Override
    public String description() {
        return "Gives a player wings for a limited time or until death.";
    }

    @Override
    public String syntax() {
        return "/wings give [player] [twing] [time|untildeath]";
    }

    @Override
    public boolean adminOnly() {
        return true;
    }

    @Override
    public void perform(Player p, String[] args) {
        if (args.length != 4) {
            p.sendMessage(plugin.settings().prefix() + " §7try: §c" + syntax());
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            p.sendMessage(plugin.messages().getPlayerNotFound());
            return;
        }
        Wing wing = plugin.wings().matchByInput(args[2]);
        if (wing == null) {
            p.sendMessage(plugin.messages().getWingNotFound(args[2]));
            return;
        }
        long seconds = TimedEquips.parseSeconds(args[3]);
        if (seconds < 0) {
            p.sendMessage(plugin.settings().prefix() + " §7try: §c" + syntax());
            return;
        }
        if (seconds == 0) {
            plugin.timedEquips().giveUntilDeath(target, wing);
        } else {
            plugin.timedEquips().giveTimed(target, wing, seconds, TimedEquips.describe(args[3]));
        }
    }

    @Override
    public List<String> tabComplete(Player p, String[] args) {
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) names.add(online.getName());
            return names;
        }
        if (args.length == 3) return plugin.wings().inputSuggestions();
        if (args.length == 4) return List.of("60s", "30m", "5h", "1d", "untildeath");
        return List.of();
    }
}
