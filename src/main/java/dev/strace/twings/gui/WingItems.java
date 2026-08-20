package dev.strace.twings.gui;

import dev.strace.twings.Main;
import dev.strace.twings.util.ItemBuilder;
import dev.strace.twings.util.MyColors;
import dev.strace.twings.wing.ParticleSpec;
import dev.strace.twings.wing.Wing;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Builds the GUI item for a wing — same look and lore as 2.x. */
public final class WingItems {

    private WingItems() {
    }

    public static ItemStack build(Main plugin, Wing wing, Player p, GuiMode mode) {
        var settings = plugin.settings();
        var messages = plugin.messages();

        ItemBuilder item = new ItemBuilder(wing.material()).setName(MyColors.format(wing.itemName()));
        List<String> lore = new ArrayList<>();

        if (settings.lorePattern()) {
            String symbol = settings.menuSymbol();
            for (String row : wing.patternRows()) {
                StringBuilder line = new StringBuilder();
                for (String token : row.split(",")) {
                    ParticleSpec spec = wing.specFor(token.trim());
                    if (spec == null) {
                        line.append("§r§f").append(symbol);
                    } else if (spec.isDust()) {
                        line.append(MyColors.format(spec.hex() + symbol + "&r"));
                    } else {
                        line.append(symbol);
                    }
                }
                lore.add(line.toString());
            }
        }
        if (wing.creator() != null && settings.loreCreator()) {
            lore.add(MyColors.format(settings.creatorFormat().replace("%prefix%", settings.prefix()))
                    .replace("%creator%", wing.creator()));
        }

        switch (mode) {
            case WINGS -> {
                if (settings.lorePermissions()) {
                    String perms = plugin.equipService().mayUse(p, wing)
                            ? settings.hasPermissionText() : settings.noPermissionText();
                    lore.add(MyColors.format(settings.permissionsFormat().replace("%prefix%", settings.prefix()))
                            .replace("%perms%", perms));
                }
                if (plugin.equipment().hasEquipped(p.getUniqueId(), wing.id())) {
                    item.addGlow();
                    if (settings.loreAction1()) lore.add(messages.getRightClick());
                    if (settings.loreAction2()) lore.add(messages.getShiftRightClick());
                } else {
                    if (settings.loreAction1()) lore.add(messages.getLeftClick());
                    if (settings.loreAction2()) lore.add(messages.getShiftLeftClick());
                }
            }
            case PREVIEW -> {
                lore.add(MyColors.format("&cLeft click to set the Preview Location"));
                lore.add(MyColors.format("&cRight click to remove the Preview"));
            }
            case EDIT -> {
                lore.add(messages.getEditLeft());
                lore.add(messages.getEditRight());
            }
        }

        return item.setLore(lore).build();
    }
}
