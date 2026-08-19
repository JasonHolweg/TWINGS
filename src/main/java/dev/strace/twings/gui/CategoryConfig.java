package dev.strace.twings.gui;

import dev.strace.twings.Main;
import dev.strace.twings.util.ItemBuilder;
import dev.strace.twings.util.MyColors;
import dev.strace.twings.util.YamlFile;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * CategoryGUI.yml — loaded once per (re)load instead of on every click.
 * File schema unchanged from 2.x.
 */
public final class CategoryConfig {

    public record Category(String name, String displayName, Material material,
                           List<String> lore, boolean glow, int slot, ItemStack item) {
    }

    private final Main plugin;
    private final YamlFile file;
    private boolean enabled;
    private String title;
    private int rows;
    private final List<Category> categories = new ArrayList<>();

    public CategoryConfig(Main plugin) {
        this.plugin = plugin;
        this.file = new YamlFile(plugin, "CategoryGUI.yml");
    }

    public CategoryConfig load() {
        var cfg = file.cfg();
        cfg.addDefault("enabled", true);
        cfg.addDefault("title", "&bChoose the category!");
        cfg.addDefault("rows", 1);
        cfg.addDefault("category.wings.displayname", "&d&lWINGS");
        cfg.addDefault("category.wings.lore", List.of(
                "&8Here you can find all wing particles!",
                "&7&oLeftclick to choose your wings!"));
        cfg.addDefault("category.wings.material", Material.ELYTRA.toString());
        cfg.addDefault("category.wings.glow", true);
        cfg.addDefault("category.wings.slot", 4);
        cfg.options().copyDefaults(true);
        file.save(plugin);

        enabled = cfg.getBoolean("enabled");
        title = MyColors.format(cfg.getString("title", ""));
        rows = Math.max(1, Math.min(6, cfg.getInt("rows", 1)));

        categories.clear();
        var section = cfg.getConfigurationSection("category");
        if (section != null) {
            for (String name : section.getKeys(false)) {
                String prefix = "category." + name + ".";
                Material material = Material.matchMaterial(cfg.getString(prefix + "material", ""));
                if (material == null) material = Material.DIRT;
                List<String> lore = cfg.getStringList(prefix + "lore");
                boolean glow = cfg.getBoolean(prefix + "glow");
                int slot = cfg.getInt(prefix + "slot");
                String displayName = MyColors.format(cfg.getString(prefix + "displayname", name));

                ItemBuilder item = new ItemBuilder(material).setName(displayName);
                if (glow) item.addGlow();
                for (String line : lore) item.addLore(MyColors.format(line));
                categories.add(new Category(name, displayName, material, lore, glow, slot, item.build()));
            }
        }
        return this;
    }

    public CategoryConfig reload() {
        file.reload();
        return load();
    }

    public boolean enabled() {
        return enabled;
    }

    public String title() {
        return title;
    }

    public int rows() {
        return rows;
    }

    public List<Category> categories() {
        return categories;
    }
}
