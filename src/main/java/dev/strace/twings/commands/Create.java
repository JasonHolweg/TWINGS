package dev.strace.twings.commands;

import dev.strace.twings.Main;
import dev.strace.twings.gui.PictureGui;
import org.bukkit.entity.Player;

public final class Create extends SubCommand {

    public Create(Main plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "create";
    }

    @Override
    public String description() {
        return "Creates a wing from a picture in the pictures folder.";
    }

    @Override
    public String syntax() {
        return "/wings create";
    }

    @Override
    public boolean adminOnly() {
        return true;
    }

    @Override
    public void perform(Player p, String[] args) {
        new PictureGui(plugin, p).open();
    }
}
