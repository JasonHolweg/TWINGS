package dev.strace.twings.render;

import dev.strace.twings.Main;
import dev.strace.twings.wing.Wing;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * The single repeating task that animates and draws everything. Replaces
 * three independent 2.x timers (PlayWings, Animation, WingPreview) that ran
 * every tick due to a config-key typo, were re-registered on every reload
 * without cancelling the old ones, and did file I/O per tick.
 *
 * <p>Runs synchronously: the per-tick math is trivial after precomputation,
 * and it keeps every Bukkit API call on the main thread.</p>
 */
public final class RenderEngine {

    private static final class AnimState {
        int flap;
        boolean plus = true;
        int rotCounter;
        int rotStep;
    }

    private final Main plugin;
    private final Random random = new Random();
    private final Map<String, AnimState> anim = new HashMap<>();
    private BukkitTask task;
    private String editWingId;
    private long editStartedMs;
    private long lastEditReloadMs;

    public RenderEngine(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        int rate = plugin.settings().updateRate();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(rate), 1, rate);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        anim.clear();
    }

    /** Puts a wing into live-edit mode: its file is re-read once a second. */
    public void setEditWing(Wing wing) {
        this.editWingId = wing == null ? null : wing.idLower();
        this.editStartedMs = System.currentTimeMillis();
    }

    public void clearEditWing() {
        this.editWingId = null;
    }

    private void tick(int rate) {
        long now = System.currentTimeMillis();
        advanceAnimations(rate);
        handleEditMode(now);
        plugin.timedEquips().tick(now);

        for (Player p : Bukkit.getOnlinePlayers()) {
            var ids = plugin.equipment().getEquipped(p.getUniqueId());
            if (!ids.isEmpty()) {
                for (String id : ids) {
                    Wing wing = plugin.wings().get(id);
                    if (wing != null) {
                        WingRenderer.drawOnPlayer(wing, p, snapshot(wing), plugin.settings(), plugin.movement());
                    }
                }
            } else if (plugin.settings().showWithPerms()) {
                Wing auto = autoWing(p);
                if (auto != null) {
                    WingRenderer.drawOnPlayer(auto, p, snapshot(auto), plugin.settings(), plugin.movement());
                }
            }
        }

        for (var entry : plugin.previews().all().entrySet()) {
            Wing wing = plugin.wings().get(entry.getKey());
            if (wing == null) continue;
            Location loc = entry.getValue().resolve();
            if (loc != null) {
                WingRenderer.drawAtLocation(wing, loc, snapshot(wing), plugin.settings());
            }
        }
    }

    /**
     * Animation steps are advanced once per server tick worth of updates,
     * so the visible flap/rotation speed is independent of the update rate.
     */
    private void advanceAnimations(int rate) {
        for (Wing wing : plugin.wings().all()) {
            if (!wing.animated()) continue;
            AnimState st = anim.computeIfAbsent(wing.idLower(), k -> {
                AnimState s = new AnimState();
                if (!wing.mirror()) s.rotStep = random.nextInt(20);
                return s;
            });
            for (int i = 0; i < rate; i++) {
                if (wing.mirror()) {
                    // legacy ping-pong 0..32 in steps of 2
                    if (st.flap <= 30 && st.plus) st.flap += 2;
                    else st.plus = false;
                    if (st.flap > 0 && !st.plus) st.flap -= 2;
                    else st.plus = true;
                } else {
                    st.rotCounter++;
                    if (st.rotStep >= 300) {
                        st.rotStep = 0;
                        st.rotCounter = 0;
                    }
                    if (st.rotCounter % 5 == 0) st.rotStep++;
                }
            }
        }
    }

    private WingRenderer.AnimSnapshot snapshot(Wing wing) {
        AnimState st = anim.get(wing.idLower());
        if (st == null) return null;
        return new WingRenderer.AnimSnapshot(st.flap, st.rotStep);
    }

    private void handleEditMode(long now) {
        if (editWingId == null) return;
        // auto-expire the edit session after 10 minutes
        if (now - editStartedMs > 10 * 60 * 1000) {
            editWingId = null;
            return;
        }
        if (now - lastEditReloadMs >= 1000) {
            lastEditReloadMs = now;
            plugin.wings().reloadWing(editWingId);
        }
    }

    /**
     * "showwithperms" fallback: players without equipped wings display the
     * first wing whose permission they have. (The 2.x implementation of this
     * option never ran — it read a config key that was never written.)
     */
    private Wing autoWing(Player p) {
        for (Wing wing : plugin.wings().all()) {
            if (!wing.permission().isEmpty() && p.hasPermission(wing.permission())) {
                return wing;
            }
        }
        return null;
    }
}
