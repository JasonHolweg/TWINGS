package dev.strace.twings.commands;

import dev.strace.twings.Main;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class SubCommand {

    protected final Main plugin;

    protected SubCommand(Main plugin) {
        this.plugin = plugin;
    }

    public abstract String name();

    public abstract String description();

    public abstract String syntax();

    /** Admin-only commands are gated on twings.admin centrally. */
    public boolean adminOnly() {
        return false;
    }

    /** Whether the command may also be run from the server console. */
    public boolean consoleCapable() {
        return false;
    }

    public abstract void perform(Player p, String[] args);

    /** Console entry point for {@link #consoleCapable()} commands. */
    public void performConsole(org.bukkit.command.CommandSender sender, String[] args) {
    }

    /** Completions for args[1..]; args includes the subcommand name. */
    public List<String> tabComplete(Player p, String[] args) {
        return List.of();
    }
}
