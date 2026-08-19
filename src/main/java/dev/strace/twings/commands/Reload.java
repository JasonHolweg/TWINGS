package dev.strace.twings.commands;

import dev.strace.twings.Main;
import org.bukkit.entity.Player;

public final class Reload extends SubCommand {

    public Reload(Main plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String description() {
        return "Reloads configs, language and all wing files.";
    }

    @Override
    public String syntax() {
        return "/wings reload";
    }

    @Override
    public boolean adminOnly() {
        return true;
    }

    @Override
    public boolean consoleCapable() {
        return true;
    }

    @Override
    public void performConsole(org.bukkit.command.CommandSender sender, String[] args) {
        plugin.reloadPlugin();
        sender.sendMessage("TWINGS reloaded.");
    }

    @Override
    public void perform(Player p, String[] args) {
        plugin.reloadPlugin();
    }
}
