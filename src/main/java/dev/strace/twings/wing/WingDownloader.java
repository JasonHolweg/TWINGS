package dev.strace.twings.wing;

import dev.strace.twings.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Locale;

/**
 * Shared download-and-install pipeline for /wings import (arbitrary URLs)
 * and /wings install (library codes). Downloads and validates off the main
 * thread; only the final duplicate check, file write and registry reload
 * run synchronously.
 */
public final class WingDownloader {

    private static final int MAX_BYTES = 1024 * 1024;

    private WingDownloader() {
    }

    /**
     * @param explicitName wing id chosen by the sender, or null to derive it
     *                     from the response's X-Wing-Slug header
     * @param fallbackName used when no explicit name and no slug header exist
     */
    public static void run(Main plugin, CommandSender sender, URI uri, String explicitName, String fallbackName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> download(plugin, sender, uri, explicitName, fallbackName));
    }

    public static String sanitizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase(Locale.ROOT)
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_-]", "")
                .replaceAll("^([_-])+|([_-])+$", "");
    }

    private static void reply(Main plugin, CommandSender sender, String msg) {
        if (sender instanceof Player p && !p.isOnline()) return;
        sender.sendMessage(plugin.settings().prefix() + " " + msg);
    }

    private static void sync(Main plugin, Runnable r) {
        Bukkit.getScheduler().runTask(plugin, r);
    }

    private static void download(Main plugin, CommandSender sender, URI uri, String explicitName, String fallbackName) {
        byte[] data;
        String slugHeader = null;
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "TWINGS/" + plugin.getDescription().getVersion())
                    .GET().build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            // Always drain+close the body stream, even on error responses, so the
            // connection is released and HttpClient.close() does not block.
            try (InputStream in = response.body()) {
                if (response.statusCode() == 404) {
                    sync(plugin, () -> reply(plugin, sender, "§cNothing found under that link/code."));
                    return;
                }
                if (response.statusCode() != 200) {
                    final int code = response.statusCode();
                    sync(plugin, () -> reply(plugin, sender, "§cDownload failed (HTTP " + code + ")."));
                    return;
                }
                slugHeader = response.headers().firstValue("X-Wing-Slug").orElse(null);
                data = in.readNBytes(MAX_BYTES + 1);
            }
            if (data.length > MAX_BYTES) {
                sync(plugin, () -> reply(plugin, sender, "§cFile is too large (max 1 MB)."));
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (Exception e) {
            sync(plugin, () -> reply(plugin, sender, "§cDownload failed: " + e.getMessage()));
            return;
        }

        String wingId = explicitName != null ? explicitName : sanitizeName(slugHeader);
        if (wingId.isEmpty()) wingId = sanitizeName(fallbackName);
        if (wingId.isEmpty()) {
            sync(plugin, () -> reply(plugin, sender, "§cPlease provide a name for this wing."));
            return;
        }

        if (isImage(data)) {
            installImage(plugin, sender, data, wingId);
        } else {
            installYaml(plugin, sender, data, wingId);
        }
    }

    private static void installYaml(Main plugin, CommandSender sender, byte[] data, String wingId) {
        final String text = new String(data, StandardCharsets.UTF_8);
        final File target = new File(plugin.wings().wingsDir(), wingId + ".yml");
        // validate off-thread before anything is written
        try {
            YamlConfiguration cfg = new YamlConfiguration();
            cfg.loadFromString(text);
            Wing.parse(target, cfg);
        } catch (InvalidConfigurationException | WingLoadException e) {
            sync(plugin, () -> reply(plugin, sender, "§cThat file is not a valid wing: §7" + e.getMessage()));
            return;
        }
        final String id = wingId;
        sync(plugin, () -> {
            // Guard on disk too: a file that failed to parse is absent from the
            // registry but must not be silently overwritten.
            if (plugin.wings().get(id) != null || target.exists()) {
                reply(plugin, sender, "§cA wing named '" + id + "' already exists. Pick another name: §7add a name to the command.");
                return;
            }
            try {
                Files.write(target.toPath(), data);
            } catch (Exception e) {
                reply(plugin, sender, "§cCould not save the wing: " + e.getMessage());
                return;
            }
            plugin.wings().loadAll();
            reply(plugin, sender, "§aWing '" + id + "' installed! Open /wings to equip it.");
        });
    }

    private static void installImage(Main plugin, CommandSender sender, byte[] data, String wingId) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(data));
        } catch (Exception e) {
            image = null;
        }
        if (image == null) {
            sync(plugin, () -> reply(plugin, sender, "§cCould not read that image."));
            return;
        }
        final BufferedImage img = image;
        final String id = wingId;
        sync(plugin, () -> {
            if (plugin.wings().get(id) != null || new File(plugin.wings().wingsDir(), id + ".yml").exists()) {
                reply(plugin, sender, "§cA wing named '" + id + "' already exists. Pick another name: §7add a name to the command.");
                return;
            }
            if (WingFiles.createFromPicture(plugin, id, img)) {
                plugin.wings().loadAll();
                reply(plugin, sender, "§aWing '" + id + "' created from the image! Open /wings to equip it.");
            } else {
                reply(plugin, sender, "§cThe image is too large (max 5000 pixels, e.g. 64x64).");
            }
        });
    }

    private static boolean isImage(byte[] data) {
        if (data.length < 4) return false;
        return (data[0] == (byte) 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G')
                || (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8)
                || (data[0] == 'G' && data[1] == 'I' && data[2] == 'F');
    }
}
