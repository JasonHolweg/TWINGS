package dev.strace.twings.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material mat) {
        this.item = new ItemStack(mat);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder addGlow() {
        if (meta != null) {
            meta.addEnchant(Enchantment.INFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder setName(String name) {
        if (meta != null) meta.setDisplayName(name);
        return this;
    }

    public ItemBuilder addLore(String line) {
        if (meta != null) {
            List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
            lore.add(line);
            meta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        if (meta != null) meta.setLore(lore);
        return this;
    }

    public ItemStack build() {
        if (meta != null) item.setItemMeta(meta);
        return item;
    }
}
