package dev.strace.twings.render;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks "recently moved a full block" per player. Replaces the 2.x
 * ArrayList of Player references that was mutated from async tasks, leaked
 * on quit and scheduled a removal task for every single move.
 */
public final class MovementTracker implements Listener {

    /** How long after the last block move a player counts as moving (was 30 ticks). */
    private static final long MOVING_WINDOW_MS = 1500;

    private final Map<UUID, Long> lastMove = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() != e.getTo().getBlockX()
                || e.getFrom().getBlockY() != e.getTo().getBlockY()
                || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            lastMove.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastMove.remove(e.getPlayer().getUniqueId());
    }

    public boolean isMoving(Player p) {
        Long last = lastMove.get(p.getUniqueId());
        return last != null && System.currentTimeMillis() - last < MOVING_WINDOW_MS;
    }
}
