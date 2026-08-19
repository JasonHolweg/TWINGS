package dev.strace.twings.gui;

import dev.strace.twings.Main;
import dev.strace.twings.util.ItemBuilder;
import dev.strace.twings.util.MyColors;
import dev.strace.twings.util.ReadImage;
import dev.strace.twings.wing.WingFiles;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Lists the files in the pictures folder; clicking one generates a wing.
 * The file name travels in the slot map — 2.x parsed it back out of the
 * colored item display name, which never matched and broke the feature.
 */
public final class PictureGui extends AbstractGui {

    private final Map<Integer, String> slotFile = new HashMap<>();

    public PictureGui(Main plugin, Player player) {
        super(plugin, player);
        createInventory(6 * 9, plugin.settings().menuTitle() + MyColors.format(" &c&lCREATE"));
        File[] files = new File(plugin.getDataFolder(), "pictures").listFiles();
        int slot = 0;
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() || slot >= inventory.getSize()) continue;
                inventory.setItem(slot, new ItemBuilder(Material.PAPER)
                        .setName(MyColors.format("&a" + file.getName())).build());
                slotFile.put(slot, file.getName());
                slot++;
            }
        }
    }

    @Override
    public void onClick(int slot, ClickType click) {
        if (click != ClickType.LEFT) return;
        if (!player.hasPermission("twings.admin")) return;
        String fileName = slotFile.get(slot);
        if (fileName == null) return;

        BufferedImage image = new ReadImage().getImage(fileName);
        if (image == null) {
            player.sendMessage(plugin.settings().prefix() + MyColors.format(" &c" + fileName + " is not a readable image."));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1, 100f);
            player.closeInventory();
            return;
        }
        if (WingFiles.createFromPicture(plugin, fileName, image)) {
            player.sendMessage(MyColors.format("&a" + fileName + " created! To activate do &2/twings reload"));
            player.playSound(player.getLocation(), Sound.BLOCK_COMPOSTER_FILL_SUCCESS, 1, 10f);
        } else {
            player.sendMessage(MyColors.format("&c" + fileName + ": the image is too large! Maximum is "
                    + ReadImage.MAX_PIXELS + " pixels (e.g. 64x64)."));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1, 100f);
        }
        player.closeInventory();
    }
}
