package dev.strace.twings.commands;

import dev.strace.twings.Main;
import dev.strace.twings.wing.Wing;
import org.bukkit.entity.Player;

import java.util.List;

public final class Add extends SubCommand {

    public Add(Main plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "add";
    }

    @Override
    public String description() {
        return "Adds a wing to your equipped ones.";
    }

    @Override
    public String syntax() {
        return "/wings add [twing]";
    }

    @Override
    public void perform(Player p, String[] args) {
        if (args.length == 1) {
            WingsCommand.sendClickableList(plugin, p, "add");
            return;
        }
        Wing wing = plugin.wings().matchByInput(args[1]);
        if (wing == null) {
            p.sendMessage(plugin.messages().getWingNotFound(args[1]));
            return;
        }
        plugin.equipService().equipAdd(p, wing);
    }

    @Override
    public List<String> tabComplete(Player p, String[] args) {
        return args.length == 2 ? plugin.wings().inputSuggestions() : List.of();
    }
}
