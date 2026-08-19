package dev.strace.twings.gui;

import dev.strace.twings.Main;
import dev.strace.twings.util.ItemBuilder;
import dev.strace.twings.util.MyColors;
import dev.strace.twings.wing.Wing;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Paged list of wings for equipping, preview placement or edit mode.
 * Sizing and pagination mirror the 2.x layout (45 wings per page, nav row
 * only when needed).
 */
public final class WingListGui extends AbstractGui {

    private static final int PAGE_SIZE = 45;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_UNEQUIP = 49;
    private static final int SLOT_NEXT = 53;

    private final GuiMode mode;
    private final String category;
    private final int page;
    private final Map<Integer, Wing> slotWing = new HashMap<>();
    private boolean hasNext;

    public WingListGui(Main plugin, Player player, GuiMode mode, String category, int page) {
        super(plugin, player);
        this.mode = mode;
        this.category = category;
        this.page = page;

        List<Wing> wings = plugin.wings().byCategory(category);
        int size = wings.size();
        int rows = size <= 8 ? 1 : size <= 17 ? 2 : size <= 26 ? 3 : size <= 35 ? 4 : size <= 44 ? 5 : 6;

        String title = plugin.settings().menuTitle();
        if (mode == GuiMode.PREVIEW) title += MyColors.format(" &c&lPREVIEW");
        if (mode == GuiMode.EDIT) title += MyColors.format(" &c&lEDIT");
        createInventory(rows * 9, title);

        List<Wing> visible;
        if (rows == 6) {
            int start = Math.min(page * PAGE_SIZE, size);
            int end = Math.min(size, (page + 1) * PAGE_SIZE);
            visible = wings.subList(start, end);
            hasNext = size > end;
            if (hasNext) {
                inventory.setItem(SLOT_NEXT, new ItemBuilder(Material.SPECTRAL_ARROW)
                        .setName(plugin.settings().arrowNextName()).build());
            }
            if (page > 0) {
                inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                        .setName(plugin.settings().arrowBackName()).build());
            }
            inventory.setItem(SLOT_UNEQUIP, new ItemBuilder(Material.LAVA_BUCKET)
                    .setName(plugin.settings().unequipName()).build());
        } else {
            visible = wings;
        }

        int slot = 0;
        for (Wing wing : visible) {
            inventory.setItem(slot, WingItems.build(plugin, wing, player, mode));
            slotWing.put(slot, wing);
            slot++;
        }
    }

    @Override
    public void onClick(int slot, ClickType click) {
        if (slot == SLOT_NEXT && hasNext) {
            new WingListGui(plugin, player, mode, category, page + 1).open();
            return;
        }
        if (slot == SLOT_BACK && page > 0) {
            new WingListGui(plugin, player, mode, category, page - 1).open();
            return;
        }
        if (slot == SLOT_UNEQUIP && inventory.getItem(SLOT_UNEQUIP) != null && mode == GuiMode.WINGS) {
            player.closeInventory();
            plugin.equipService().unequipAll(player);
            return;
        }
        Wing wing = slotWing.get(slot);
        if (wing == null) return;
        switch (mode) {
            case WINGS -> handleEquipClick(wing, click);
            case PREVIEW -> handlePreviewClick(wing, click);
            case EDIT -> handleEditClick(wing, click);
        }
    }

    private void handleEquipClick(Wing wing, ClickType click) {
        switch (click) {
            case LEFT -> {
                if (plugin.equipService().equipReplace(player, wing)) {
                    player.closeInventory();
                }
            }
            case SHIFT_LEFT -> {
                if (plugin.equipService().equipAdd(player, wing)) {
                    player.closeInventory();
                }
            }
            case RIGHT -> {
                if (plugin.equipment().hasEquipped(player.getUniqueId(), wing.id())) {
                    plugin.equipService().unequip(player, wing);
                    player.closeInventory();
                }
            }
            case SHIFT_RIGHT -> {
                if (!plugin.equipment().getEquipped(player.getUniqueId()).isEmpty()) {
                    plugin.equipService().unequipAll(player);
                    player.closeInventory();
                }
            }
            default -> {
            }
        }
    }

    private boolean mayManage(String extraPermission) {
        return player.hasPermission(extraPermission) || player.hasPermission("twings.admin");
    }

    private void handlePreviewClick(Wing wing, ClickType click) {
        if (click == ClickType.LEFT && mayManage("twings.setpreview")) {
            plugin.previews().set(wing.id(), player.getLocation());
            player.sendMessage(plugin.messages().getPreviewSet(wing));
            player.closeInventory();
        } else if (click == ClickType.RIGHT && mayManage("twings.setpreview")) {
            plugin.previews().remove(wing.id());
            player.sendMessage(plugin.messages().getPreviewRemoved(wing));
            player.closeInventory();
        }
    }

    private void handleEditClick(Wing wing, ClickType click) {
        if (click == ClickType.LEFT && mayManage("twings.setedit")) {
            plugin.render().setEditWing(wing);
            player.sendMessage(plugin.messages().getEditModeSet(wing));
            player.closeInventory();
        } else if (click == ClickType.RIGHT && mayManage("twings.setedit")) {
            plugin.render().clearEditWing();
            player.closeInventory();
        }
    }
}
