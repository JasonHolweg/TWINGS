package dev.strace.twings.config;

import dev.strace.twings.Main;
import dev.strace.twings.util.MyColors;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed view of config.yml. All values are read once per (re)load — nothing
 * in the render or click paths touches the configuration anymore.
 * Keys are kept identical to 2.x so existing config files stay valid.
 */
public final class Settings {

    private final Main plugin;

    private String prefix;
    private String menuTitle;
    private String menuSymbol;
    private String creatorFormat;
    private String permissionsFormat;
    private String hasPermissionText;
    private String noPermissionText;
    private String arrowBackName;
    private String arrowNextName;
    private String unequipName;
    private boolean lorePattern;
    private boolean loreCreator;
    private boolean lorePermissions;
    private boolean loreAction1;
    private boolean loreAction2;
    private int maxEquipped;
    private boolean showWithPerms;
    private int updateRate;
    private boolean hideOnSpectator;
    private boolean hideOnInvis;
    private double rotationSpeed;
    private String libraryUrl;

    public Settings(Main plugin) {
        this.plugin = plugin;
    }

    public Settings load() {
        FileConfiguration cfg = plugin.getConfig();
        cfg.addDefault("Prefix", "&e&lTWings");
        cfg.addDefault("max equipped twings", 2);
        cfg.addDefault("Wings.showwithperms", false);
        cfg.addDefault("Wings.updaterate", 3);
        cfg.addDefault("Menu.title", "%prefix% &9Choose your Wings!");
        cfg.addDefault("Menu.symbol", "⏹");
        cfg.addDefault("Menu.creator", "&c&lCreator:&f %creator%");
        cfg.addDefault("Menu.permissions", "&7unlocked: [%perms%&7]");
        cfg.addDefault("Menu.lore.pattern", true);
        cfg.addDefault("Menu.lore.creator", true);
        cfg.addDefault("Menu.lore.permissions", true);
        cfg.addDefault("Menu.lore.action1", true);
        cfg.addDefault("Menu.lore.action2", true);
        cfg.addDefault("haspermission", "&aYES");
        cfg.addDefault("nopermission", "&cNO");
        cfg.addDefault("gui.arrowback", "&c<- back");
        cfg.addDefault("gui.arrownext", "&anext ->");
        cfg.addDefault("gui.unequip", "&4unequip particles");
        cfg.addDefault("hide on spectator", true);
        cfg.addDefault("hide on invis", true);
        cfg.addDefault("rotation speed", 0.2);
        cfg.addDefault("library.url", "https://jasonholweg.de/twings");
        cfg.options().copyDefaults(true);
        plugin.saveConfig();

        prefix = MyColors.format(cfg.getString("Prefix", "&e&lTWings"));
        menuTitle = MyColors.format(cfg.getString("Menu.title", "")).replace("%prefix%", prefix);
        menuSymbol = cfg.getString("Menu.symbol", "⏹");
        creatorFormat = cfg.getString("Menu.creator", "");
        permissionsFormat = cfg.getString("Menu.permissions", "");
        hasPermissionText = MyColors.format(cfg.getString("haspermission", "&aYES"));
        noPermissionText = MyColors.format(cfg.getString("nopermission", "&cNO"));
        arrowBackName = MyColors.format(cfg.getString("gui.arrowback", "&c<- back"));
        arrowNextName = MyColors.format(cfg.getString("gui.arrownext", "&anext ->"));
        unequipName = MyColors.format(cfg.getString("gui.unequip", "&4unequip particles"));
        lorePattern = cfg.getBoolean("Menu.lore.pattern", true);
        loreCreator = cfg.getBoolean("Menu.lore.creator", true);
        lorePermissions = cfg.getBoolean("Menu.lore.permissions", true);
        loreAction1 = cfg.getBoolean("Menu.lore.action1", true);
        loreAction2 = cfg.getBoolean("Menu.lore.action2", true);
        maxEquipped = cfg.getInt("max equipped twings", 2);
        showWithPerms = cfg.getBoolean("Wings.showwithperms", false);
        updateRate = Math.max(1, cfg.getInt("Wings.updaterate", 3));
        hideOnSpectator = cfg.getBoolean("hide on spectator", true);
        hideOnInvis = cfg.getBoolean("hide on invis", true);
        rotationSpeed = cfg.getDouble("rotation speed", 0.2);
        if (rotationSpeed == 0) rotationSpeed = 0.2;
        libraryUrl = cfg.getString("library.url", "https://jasonholweg.de/twings")
                .trim().replaceAll("/+$", "");
        return this;
    }

    public Settings reload() {
        plugin.reloadConfig();
        return load();
    }

    public String prefix() {
        return prefix;
    }

    public String menuTitle() {
        return menuTitle;
    }

    public String menuSymbol() {
        return menuSymbol;
    }

    public String creatorFormat() {
        return creatorFormat;
    }

    public String permissionsFormat() {
        return permissionsFormat;
    }

    public String hasPermissionText() {
        return hasPermissionText;
    }

    public String noPermissionText() {
        return noPermissionText;
    }

    public String arrowBackName() {
        return arrowBackName;
    }

    public String arrowNextName() {
        return arrowNextName;
    }

    public String unequipName() {
        return unequipName;
    }

    public boolean lorePattern() {
        return lorePattern;
    }

    public boolean loreCreator() {
        return loreCreator;
    }

    public boolean lorePermissions() {
        return lorePermissions;
    }

    public boolean loreAction1() {
        return loreAction1;
    }

    public boolean loreAction2() {
        return loreAction2;
    }

    public int maxEquipped() {
        return maxEquipped;
    }

    public boolean showWithPerms() {
        return showWithPerms;
    }

    public int updateRate() {
        return updateRate;
    }

    public boolean hideOnSpectator() {
        return hideOnSpectator;
    }

    public boolean hideOnInvis() {
        return hideOnInvis;
    }

    public double rotationSpeed() {
        return rotationSpeed;
    }

    public String libraryUrl() {
        return libraryUrl;
    }
}
