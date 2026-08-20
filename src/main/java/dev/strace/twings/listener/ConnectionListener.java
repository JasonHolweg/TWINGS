package dev.strace.twings.listener;

import dev.strace.twings.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class ConnectionListener implements Listener {

    private final Main plugin;

    public ConnectionListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // drop timed wings that expired while the player was offline
        plugin.timedEquips().onJoin(e.getPlayer());
    }
}
