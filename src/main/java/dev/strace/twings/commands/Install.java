package dev.strace.twings.commands;

import dev.strace.twings.Main;
import dev.strace.twings.wing.WingDownloader;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * /wings install &lt;code&gt; [name] — installs a wing from the TWINGS library
 * (designed and published at jasonholweg.de/twings). The wing name defaults
 * to the published name; pass one explicitly on conflicts.
 */
public final class Install extends SubCommand {

    public Install(Main plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "install";
    }

    @Override
    public String description() {
        return "Installs a wing from the TWINGS library by its code.";
    }

    @Override
    public String syntax() {
        return "/wings install <code> [name]";
    }

    @Override
    public boolean adminOnly() {
        return true;
    }

    @Override
    public boolean consoleCapable() {
        return true;
    }

    @Override
    public void performConsole(CommandSender sender, String[] args) {
        run(sender, args);
    }

    @Override
    public void perform(Player p, String[] args) {
        run(p, args);
    }

    @Override
    public List<String> tabComplete(Player p, String[] args) {
        return List.of();
    }

    private void reply(CommandSender sender, String msg) {
        sender.sendMessage(plugin.settings().prefix() + " " + msg);
    }

    private void run(CommandSender sender, String[] args) {
        if (args.length < 2) {
            reply(sender, "§7try: §c" + syntax());
            return;
        }
        String code = args[1].toLowerCase(Locale.ROOT);
        if (!code.matches("[a-z0-9]{4,20}")) {
            reply(sender, "§cThat does not look like a library code. §7Example: /twings install ab12cd34ef");
            return;
        }
        String explicitName = null;
        if (args.length > 2) {
            explicitName = WingDownloader.sanitizeName(args[2]);
            if (explicitName.isEmpty()) {
                reply(sender, "§cThat name cannot be used.");
                return;
            }
            if (plugin.wings().get(explicitName) != null) {
                reply(sender, "§cA wing named '" + explicitName + "' already exists.");
                return;
            }
        }
        final URI uri;
        try {
            uri = new URI(plugin.settings().libraryUrl() + "/api.php?action=get&id=" + code);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                reply(sender, "§cThe configured library URL is invalid. §7Check 'library.url' in config.yml.");
                return;
            }
        } catch (java.net.URISyntaxException e) {
            reply(sender, "§cThe configured library URL is invalid. §7Check 'library.url' in config.yml.");
            return;
        }
        reply(sender, "§7Fetching wing §f" + code + "§7 from the library ...");
        WingDownloader.run(plugin, sender, uri, explicitName, code);
    }
}
