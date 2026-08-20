package dev.strace.twings.commands;

import dev.strace.twings.Main;
import dev.strace.twings.gui.GuiMode;
import org.bukkit.entity.Player;

public final class Edit extends SubCommand {

    public Edit(Main plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "edit";
    }

    @Override
    public String description() {
        return "Opens a GUI to put a wing into live edit mode.";
    }

    @Override
    public String syntax() {
        return "/wings edit";
    }

    @Override
    public boolean adminOnly() {
        return true;
    }

    @Override
    public void perform(Player p, String[] args) {
        WingsCommand.openMenu(plugin, p, GuiMode.EDIT);
    }
}
