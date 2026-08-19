package dev.strace.twings.commands;

import dev.strace.twings.Main;
import dev.strace.twings.wing.WingDownloader;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * /wings import &lt;url&gt; [name] — downloads a wing file (.yml) or an image
 * from an arbitrary URL and installs it as a wing.
 */
public final class Import extends SubCommand {

    public Import(Main plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "import";
    }

    @Override
    public String description() {
        return "Imports a wing file or image from a URL.";
    }

    @Override
    public String syntax() {
        return "/wings import <url> [name]";
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
        return args.length == 2 ? List.of("https://") : List.of();
    }

    private void reply(CommandSender sender, String msg) {
        sender.sendMessage(plugin.settings().prefix() + " " + msg);
    }

    private void run(CommandSender sender, String[] args) {
        if (args.length < 2) {
            reply(sender, "§7try: §c" + syntax());
            return;
        }
        final URI uri;
        try {
            uri = URI.create(args[1]);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                reply(sender, "§cOnly http/https links are supported.");
                return;
            }
        } catch (IllegalArgumentException e) {
            reply(sender, "§cThat is not a valid link.");
            return;
        }
        String requested = args.length > 2 ? args[2] : nameFromUrl(uri);
        final String wingId = WingDownloader.sanitizeName(requested);
        if (wingId.isEmpty()) {
            reply(sender, "§cPlease give the wing a name: §7" + syntax());
            return;
        }
        if (plugin.wings().get(wingId) != null) {
            reply(sender, "§cA wing named '" + wingId + "' already exists. Pick another name: §7/wings import <url> <name>");
            return;
        }
        reply(sender, "§7Downloading §f" + wingId + "§7 ...");
        WingDownloader.run(plugin, sender, uri, wingId, null);
    }

    private static String nameFromUrl(URI uri) {
        String path = uri.getPath() == null ? "" : uri.getPath();
        int slash = path.lastIndexOf('/');
        String file = slash >= 0 ? path.substring(slash + 1) : path;
        return file.replaceAll("(?i)\\.(yml|yaml|png|jpe?g|gif)$", "");
    }
}
