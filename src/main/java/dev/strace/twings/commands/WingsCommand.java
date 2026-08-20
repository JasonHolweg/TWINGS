package dev.strace.twings.commands;

import dev.strace.twings.Main;
import dev.strace.twings.gui.CategoryGui;
import dev.strace.twings.gui.GuiMode;
import dev.strace.twings.gui.WingListGui;
import dev.strace.twings.wing.Wing;
import dev.strace.twings.wing.WingManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WingsCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final List<SubCommand> subCommands = new ArrayList<>();

    public WingsCommand(Main plugin) {
        this.plugin = plugin;
        subCommands.add(new Equip(plugin));
        subCommands.add(new Add(plugin));
        subCommands.add(new UnEquip(plugin));
        subCommands.add(new ListCmd(plugin));
        subCommands.add(new Preview(plugin));
        subCommands.add(new Edit(plugin));
        subCommands.add(new Create(plugin));
        subCommands.add(new Give(plugin));
        subCommands.add(new Install(plugin));
        subCommands.add(new Import(plugin));
        subCommands.add(new Reload(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            if (args.length > 0) {
                for (SubCommand sub : subCommands) {
                    if (args[0].equalsIgnoreCase(sub.name()) && sub.consoleCapable()) {
                        sub.performConsole(sender, args);
                        return true;
                    }
                }
            }
            sender.sendMessage("This TWINGS command is player-only.");
            return true;
        }
        if (args.length == 0) {
            openMenu(plugin, p, GuiMode.WINGS);
            return true;
        }
        for (SubCommand sub : subCommands) {
            if (args[0].equalsIgnoreCase(sub.name())) {
                if (sub.adminOnly() && !p.hasPermission("twings.admin")) {
                    p.sendMessage(plugin.messages().getNoPermission());
                    return true;
                }
                sub.perform(p, args);
                return true;
            }
        }
        p.sendMessage(plugin.messages().getNoSuchCommand());
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.4F, 3);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return List.of();
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (SubCommand sub : subCommands) {
                if (sub.adminOnly() && !p.hasPermission("twings.admin")) continue;
                if (sub.name().startsWith(args[0].toLowerCase(Locale.ROOT))) names.add(sub.name());
            }
            return names;
        }
        for (SubCommand sub : subCommands) {
            if (args[0].equalsIgnoreCase(sub.name())) {
                if (sub.adminOnly() && !p.hasPermission("twings.admin")) return List.of();
                List<String> options = sub.tabComplete(p, args);
                String current = args[args.length - 1].toLowerCase(Locale.ROOT);
                return options.stream()
                        .filter(o -> o.toLowerCase(Locale.ROOT).startsWith(current))
                        .toList();
            }
        }
        return List.of();
    }

    static void openMenu(Main plugin, Player p, GuiMode mode) {
        if (plugin.categories().enabled()) {
            new CategoryGui(plugin, p, mode).open();
        } else {
            new WingListGui(plugin, p, mode, WingManager.ALL, 0).open();
        }
    }

    /** Sends the clickable wing list used by /wings equip and /wings add. */
    static void sendClickableList(Main plugin, Player p, String subCommand) {
        p.sendMessage(plugin.messages().getList());
        for (Wing wing : plugin.wings().all()) {
            if (!plugin.equipService().mayUse(p, wing)) continue;
            String argument = dev.strace.twings.util.MyColors.strip(wing.itemName()).replace(" ", "_");
            TextComponent line = new TextComponent(plugin.messages().getListPoint(wing));
            line.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/wings " + subCommand + " " + argument));
            line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7Click to equip.")));
            p.spigot().sendMessage(line);
        }
    }
}
