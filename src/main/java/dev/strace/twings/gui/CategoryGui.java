package dev.strace.twings.gui;

import dev.strace.twings.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.HashMap;
import java.util.Map;

/** Category selection screen; opens the wing list for the clicked category. */
public final class CategoryGui extends AbstractGui {

    private final GuiMode mode;
    private final Map<Integer, String> slotCategory = new HashMap<>();

    public CategoryGui(Main plugin, Player player, GuiMode mode) {
        super(plugin, player);
        this.mode = mode;
        var config = plugin.categories();
        createInventory(config.rows() * 9, config.title());
        for (var category : config.categories()) {
            if (category.slot() >= 0 && category.slot() < inventory.getSize()) {
                inventory.setItem(category.slot(), category.item());
                slotCategory.put(category.slot(), category.name());
            }
        }
    }

    @Override
    public void onClick(int slot, ClickType click) {
        if (!click.isLeftClick() && !click.isRightClick()) return;
        String category = slotCategory.get(slot);
        if (category == null) return;
        new WingListGui(plugin, player, mode, category, 0).open();
    }
}
