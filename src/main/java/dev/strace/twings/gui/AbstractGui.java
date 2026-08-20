package dev.strace.twings.gui;

import dev.strace.twings.Main;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Base for all plugin GUIs. Identified via {@link InventoryHolder} instead
 * of the 2.x title matching, which broke with similar titles and required
 * rebuilding GUI objects (and reading files) on every inventory click.
 */
public abstract class AbstractGui implements InventoryHolder {

    protected final Main plugin;
    protected final Player player;
    protected Inventory inventory;

    protected AbstractGui(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    protected void createInventory(int slots, String title) {
        this.inventory = Bukkit.createInventory(this, slots, title);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.3f, 1f);
    }

    /** Handles a click on a slot of this GUI (already cancelled). */
    public abstract void onClick(int slot, ClickType click);
}
