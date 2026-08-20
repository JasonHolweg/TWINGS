package dev.strace.twings.commands;

import dev.strace.twings.Main;
import dev.strace.twings.wing.Wing;
import org.bukkit.entity.Player;

public final class ListCmd extends SubCommand {

    public ListCmd(Main plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String description() {
        return "Lists all wings you may use.";
    }

    @Override
    public String syntax() {
        return "/wings list";
    }

    @Override
    public void perform(Player p, String[] args) {
        p.sendMessage(plugin.messages().getList());
        for (Wing wing : plugin.wings().all()) {
            if (plugin.equipService().mayUse(p, wing)) {
                p.sendMessage(plugin.messages().getListPoint(wing));
            }
        }
    }
}
