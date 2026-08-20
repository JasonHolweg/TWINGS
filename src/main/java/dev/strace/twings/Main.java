package dev.strace.twings;

import dev.strace.twings.api.UpdateChecker;
import dev.strace.twings.commands.WingsCommand;
import dev.strace.twings.config.Messages;
import dev.strace.twings.config.Settings;
import dev.strace.twings.equipment.EquipService;
import dev.strace.twings.equipment.EquipmentManager;
import dev.strace.twings.equipment.TimedEquips;
import dev.strace.twings.gui.CategoryConfig;
import dev.strace.twings.gui.GuiListener;
import dev.strace.twings.gui.GuiMode;
import dev.strace.twings.gui.WingItems;
import dev.strace.twings.listener.ConnectionListener;
import dev.strace.twings.listener.DeathListener;
import dev.strace.twings.render.MovementTracker;
import dev.strace.twings.render.PreviewManager;
import dev.strace.twings.render.RenderEngine;
import dev.strace.twings.wing.Wing;
import dev.strace.twings.wing.WingManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TWINGS 3.0 — particle wing cosmetics.
 *
 * @author Jason Holweg [STRACE]
 */
public final class Main extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 11688;

    private static Main instance;

    private Settings settings;
    private Messages messages;
    private CategoryConfig categories;
    private WingManager wingManager;
    private EquipmentManager equipment;
    private EquipService equipService;
    private TimedEquips timedEquips;
    private PreviewManager previews;
    private MovementTracker movement;
    private RenderEngine render;

    @Override
    public void onEnable() {
        instance = this;

        settings = new Settings(this).load();
        messages = new Messages(this).load();
        categories = new CategoryConfig(this).load();

        wingManager = new WingManager(this);
        wingManager.loadAll();

        equipment = new EquipmentManager(this);
        equipment.load();
        equipService = new EquipService(this);
        timedEquips = new TimedEquips(this);

        previews = new PreviewManager(this);
        previews.load();

        movement = new MovementTracker();
        render = new RenderEngine(this);
        render.start();

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(movement, this);
        pm.registerEvents(new GuiListener(), this);
        pm.registerEvents(new ConnectionListener(this), this);
        pm.registerEvents(new DeathListener(this), this);

        PluginCommand command = getCommand("wings");
        if (command != null) {
            WingsCommand executor = new WingsCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        new Metrics(this, BSTATS_PLUGIN_ID);
        UpdateChecker.checkAsync(this);
    }

    @Override
    public void onDisable() {
        if (render != null) render.stop();
        if (equipment != null) equipment.flushSync();
        if (timedEquips != null) timedEquips.flushSync();
    }

    /**
     * /wings reload — replaces the 2.x Main.load(), which started a new set
     * of render tasks on every invocation without cancelling the old ones.
     */
    public void reloadPlugin() {
        render.stop();
        settings.reload();
        messages.reload();
        categories.reload();
        wingManager.loadAll();
        previews.reload();
        render.start();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("twings.admin")) {
                p.sendMessage(settings.prefix() + " §cparticles reloaded.");
            }
        }
    }

    /** Item as shown in the wing GUI (used by the developer API). */
    public ItemStack buildWingItem(Wing wing, Player p) {
        return WingItems.build(this, wing, p, GuiMode.WINGS);
    }

    public static Main getInstance() {
        return instance;
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public CategoryConfig categories() {
        return categories;
    }

    public WingManager wings() {
        return wingManager;
    }

    public EquipmentManager equipment() {
        return equipment;
    }

    public EquipService equipService() {
        return equipService;
    }

    public TimedEquips timedEquips() {
        return timedEquips;
    }

    public PreviewManager previews() {
        return previews;
    }

    public MovementTracker movement() {
        return movement;
    }

    public RenderEngine render() {
        return render;
    }

    /** Kept for 2.x API compatibility. */
    public String getPrefix() {
        return settings.prefix();
    }
}
