package dev.strace.twings.commands;

import dev.strace.twings.Main;
import dev.strace.twings.wing.Wing;
import org.bukkit.entity.Player;

import java.util.List;

public final class Equip extends SubCommand {

    public Equip(Main plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "equip";
    }

    @Override
    public String description() {
        return "Equips a specific wing (replaces the current ones).";
    }

    @Override
    public String syntax() {
        return "/wings equip [twing]";
    }

    @Override
    public void perform(Player p, String[] args) {
        if (args.length == 1) {
            WingsCommand.sendClickableList(plugin, p, "equip");
            return;
        }
        Wing wing = plugin.wings().matchByInput(args[1]);
        if (wing == null) {
            p.sendMessage(plugin.messages().getWingNotFound(args[1]));
            return;
        }
        plugin.equipService().equipReplace(p, wing);
    }

    @Override
    public List<String> tabComplete(Player p, String[] args) {
        return args.length == 2 ? plugin.wings().inputSuggestions() : List.of();
    }
}
