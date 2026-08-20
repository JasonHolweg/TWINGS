package dev.strace.twings.api;

import dev.strace.twings.Main;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Version check against the SpigotMC legacy update API — asynchronous.
 * 2.x downloaded the whole resource page on the main thread at startup and
 * on every reload.
 */
public final class UpdateChecker {

    private static final String RESOURCE_ID = "82088";

    private UpdateChecker() {
    }

    public static void checkAsync(Main plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.spigotmc.org/legacy/update.php?resource=" + RESOURCE_ID))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                String latest = client.send(request, HttpResponse.BodyHandlers.ofString()).body().trim();
                String current = plugin.getDescription().getVersion();
                if (latest.isEmpty()) return;
                if (current.equalsIgnoreCase(latest)) {
                    plugin.getLogger().info("Plugin is up to date. Thanks for using TWINGS! ~ Strace");
                } else {
                    plugin.getLogger().info("A different version is published on SpigotMC (" + latest
                            + ", you run " + current + "): https://www.spigotmc.org/resources/" + RESOURCE_ID + "/");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                plugin.getLogger().fine("Update check failed: " + e.getMessage());
            }
        });
    }
}
