package dev.strace.twings.commands;

import dev.strace.twings.Main;
import dev.strace.twings.gui.GuiMode;
import org.bukkit.entity.Player;

public final class Preview extends SubCommand {

    public Preview(Main plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "preview";
    }

    @Override
    public String description() {
        return "Opens a GUI to place wing previews in the world.";
    }

    @Override
    public String syntax() {
        return "/wings preview";
    }

    @Override
    public boolean adminOnly() {
        return true;
    }

    @Override
    public void perform(Player p, String[] args) {
        WingsCommand.openMenu(plugin, p, GuiMode.PREVIEW);
    }
}
