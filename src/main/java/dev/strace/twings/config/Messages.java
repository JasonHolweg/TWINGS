package dev.strace.twings.config;

import dev.strace.twings.Main;
import dev.strace.twings.util.MyColors;
import dev.strace.twings.util.YamlFile;
import dev.strace.twings.wing.Wing;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * lang.yml — keys are identical to 2.x so existing translations keep working.
 * All strings are resolved once per (re)load.
 */
public final class Messages {

    private final Main plugin;
    private final YamlFile file;

    private boolean showMessages;
    private String equip;
    private String unequip;
    private String previewSet;
    private String previewRemoved;
    private String editModeSet;
    private String wingNotFound;
    private String editRight;
    private String editLeft;
    private String list;
    private String listPoint;
    private String noPermission;
    private String noSuchCommand;
    private String wingsReceived;
    private String wingsGone;
    private String playerNotFound;
    private String rightClick;
    private String leftClick;
    private String shiftLeftClick;
    private String shiftRightClick;
    private String maxTwings;
    private String alreadyEquipped;

    public Messages(Main plugin) {
        this.plugin = plugin;
        this.file = new YamlFile(plugin, "lang.yml");
    }

    public Messages load() {
        YamlConfiguration cfg = file.cfg();
        cfg.addDefault("show messages", true);
        cfg.addDefault("equip", "%prefix% &9you have equipped %WingName%!");
        cfg.addDefault("unequip", "%prefix% &9you have unequipped %WingName%!");
        cfg.addDefault("preview set", "%prefix% &aPreview Location set.");
        cfg.addDefault("preview removed", "%prefix% &cPreview removed.");
        cfg.addDefault("editmode", "%prefix% &c%WingName% is now in edit mode.");
        cfg.addDefault("wing not found", "%prefix% &c%WingName% not found. All Particles: &d/twings list");
        cfg.addDefault("menu.editleft", "&cLeft click to set the edit status");
        cfg.addDefault("menu.editright", "&cRight click to remove the edit status");
        cfg.addDefault("list", "%prefix% &7List of all Particles:");
        cfg.addDefault("bulletpoint", " &7- &f%WingName%");
        cfg.addDefault("noperms", "%prefix% &cSorry, you don't have the permission to use that.");
        cfg.addDefault("no such command", "%prefix% &cSorry, there is no such command.");
        cfg.addDefault("wings given", "%prefix% &7You now have wings for &c%time%&7.");
        cfg.addDefault("wings gone", "%prefix% &cYour wings are now gone.");
        cfg.addDefault("player not found", "%prefix% &cthe player wasn't found.");
        cfg.addDefault("rightclick to unequip", "%prefix% &cRightclick to unequip!");
        cfg.addDefault("leftclick to equip", "%prefix% &bLeftclick to equip!");
        cfg.addDefault("shiftleft to add", "%prefix% &bLeftclick to add to other equipped.");
        cfg.addDefault("shiftright to unequip all", "%prefix% &cRightclick to unequip all.");
        cfg.addDefault("already equipped", "%prefix% &cYou already equipped these Particles.");
        cfg.addDefault("max twings", "%prefix% &cYou can't equip more then %max% Particles!");
        cfg.options().copyDefaults(true);
        file.save(plugin);

        showMessages = cfg.getBoolean("show messages");
        equip = cfg.getString("equip", "");
        unequip = cfg.getString("unequip", "");
        previewSet = cfg.getString("preview set", "");
        previewRemoved = cfg.getString("preview removed", "");
        editModeSet = cfg.getString("editmode", "");
        wingNotFound = cfg.getString("wing not found", "");
        editRight = cfg.getString("menu.editright", "");
        editLeft = cfg.getString("menu.editleft", "");
        list = cfg.getString("list", "");
        listPoint = cfg.getString("bulletpoint", "");
        noPermission = cfg.getString("noperms", "");
        noSuchCommand = cfg.getString("no such command", "");
        wingsReceived = cfg.getString("wings given", "");
        wingsGone = cfg.getString("wings gone", "");
        playerNotFound = cfg.getString("player not found", "");
        rightClick = cfg.getString("rightclick to unequip", "");
        leftClick = cfg.getString("leftclick to equip", "");
        shiftLeftClick = cfg.getString("shiftleft to add", "");
        shiftRightClick = cfg.getString("shiftright to unequip all", "");
        alreadyEquipped = cfg.getString("already equipped", "");
        maxTwings = cfg.getString("max twings", "");
        return this;
    }

    public Messages reload() {
        file.reload();
        return load();
    }

    private String base(String raw) {
        return MyColors.format(raw.replace("%prefix%", plugin.settings().prefix()));
    }

    private String withWing(String raw, Wing wing) {
        String msg = raw.replace("%prefix%", plugin.settings().prefix());
        if (wing != null) msg = msg.replace("%WingName%", wing.itemName());
        return MyColors.format(msg);
    }

    public boolean isShowMessages() {
        return showMessages;
    }

    public String getEquip(Wing wing) {
        return withWing(equip, wing);
    }

    public String getUnequip(Wing wing) {
        return wing == null ? "" : withWing(unequip, wing);
    }

    public String getPreviewSet(Wing wing) {
        return withWing(previewSet, wing);
    }

    public String getPreviewRemoved(Wing wing) {
        return withWing(previewRemoved, wing);
    }

    public String getEditModeSet(Wing wing) {
        return withWing(editModeSet, wing);
    }

    public String getWingNotFound(String name) {
        return base(wingNotFound.replace("%WingName%", name));
    }

    public String getEditRight() {
        return base(editRight);
    }

    public String getEditLeft() {
        return base(editLeft);
    }

    public String getList() {
        return base(list);
    }

    public String getListPoint(Wing wing) {
        return base(listPoint.replace("%WingName%", wing.itemName().replace(" ", "_")));
    }

    public String getNoPermission() {
        return base(noPermission);
    }

    public String getNoSuchCommand() {
        return base(noSuchCommand);
    }

    public String getWingsReceived(String time) {
        return base(wingsReceived.replace("%time%", time));
    }

    public String getWingsGone() {
        return base(wingsGone);
    }

    public String getPlayerNotFound() {
        return base(playerNotFound);
    }

    public String getRightClick() {
        return base(rightClick);
    }

    public String getLeftClick() {
        return base(leftClick);
    }

    public String getShiftLeftClick() {
        return base(shiftLeftClick);
    }

    public String getShiftRightClick() {
        return base(shiftRightClick);
    }

    public String getAlreadyEquipped() {
        return base(alreadyEquipped);
    }

    public String getMaxTwings() {
        return base(maxTwings.replace("%max%", String.valueOf(plugin.settings().maxEquipped())));
    }
}
