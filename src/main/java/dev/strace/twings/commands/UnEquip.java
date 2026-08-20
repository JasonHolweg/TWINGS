package dev.strace.twings.commands;

import dev.strace.twings.Main;
import org.bukkit.entity.Player;

public final class UnEquip extends SubCommand {

    public UnEquip(Main plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "unequip";
    }

    @Override
    public String description() {
        return "Unequips all your current wings.";
    }

    @Override
    public String syntax() {
        return "/wings unequip";
    }

    @Override
    public void perform(Player p, String[] args) {
        if (plugin.equipment().getEquipped(p.getUniqueId()).isEmpty()) return;
        plugin.equipService().unequipAll(p);
    }
}
