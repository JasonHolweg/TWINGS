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
        double flap;
        boolean plus = true;
        double rotStep;
    }

    private final Main plugin;
    private final Random random = new Random();
    private final Map<String, AnimState> anim = new HashMap<>();
    private BukkitTask task;
    private long engineTick;
    private String editWingId;
    private long editStartedMs;
    private long lastEditReloadMs;

    public RenderEngine(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        // Runs every tick; each wing is drawn only every N ticks (its own
        // redraw rate, defaulting to the global update rate), and its
        // animation advances every tick scaled by its animation speed.
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
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

    private void tick() {
        long now = System.currentTimeMillis();
        engineTick++;
        advanceAnimations();
        handleEditMode(now);
        plugin.timedEquips().tick(now);

        for (Player p : Bukkit.getOnlinePlayers()) {
            var ids = plugin.equipment().getEquipped(p.getUniqueId());
            if (!ids.isEmpty()) {
                for (String id : ids) {
                    Wing wing = plugin.wings().get(id);
                    if (wing != null && dueThisTick(wing)) {
                        WingRenderer.drawOnPlayer(wing, p, snapshot(wing), plugin.settings(), plugin.movement());
                    }
                }
            } else if (plugin.settings().showWithPerms()) {
                Wing auto = autoWing(p);
                if (auto != null && dueThisTick(auto)) {
                    WingRenderer.drawOnPlayer(auto, p, snapshot(auto), plugin.settings(), plugin.movement());
                }
            }
        }

        for (var entry : plugin.previews().all().entrySet()) {
            Wing wing = plugin.wings().get(entry.getKey());
            if (wing == null || !dueThisTick(wing)) continue;
            Location loc = entry.getValue().resolve();
            if (loc != null) {
                WingRenderer.drawAtLocation(wing, loc, snapshot(wing), plugin.settings());
            }
        }
    }

    /** Whether this wing should be drawn on the current engine tick. */
    private boolean dueThisTick(Wing wing) {
        int every = wing.redrawTicks() > 0 ? wing.redrawTicks() : plugin.settings().updateRate();
        if (every < 1) every = 1;
        return engineTick % every == 0;
    }

    /**
     * Advances every animated wing's flap/rotation once per tick, scaled by
     * the wing's animation speed. Runs every tick regardless of redraw rate,
     * so the flap position is smooth whenever the wing is actually drawn.
     */
    private void advanceAnimations() {
        for (Wing wing : plugin.wings().all()) {
            if (!wing.animated()) continue;
            AnimState st = anim.computeIfAbsent(wing.idLower(), k -> {
                AnimState s = new AnimState();
                if (!wing.mirror()) s.rotStep = random.nextInt(20);
                return s;
            });
            double speed = wing.animationSpeed();
            if (wing.mirror()) {
                // ping-pong 0..32; +2 per tick at speed 1
                double delta = 2 * speed;
                if (st.plus) {
                    st.flap += delta;
                    if (st.flap >= 32) { st.flap = 32; st.plus = false; }
                } else {
                    st.flap -= delta;
                    if (st.flap <= 0) { st.flap = 0; st.plus = true; }
                }
            } else {
                // +1 per 5 ticks at speed 1; wraps at 300
                st.rotStep += speed / 5.0;
                if (st.rotStep >= 300) st.rotStep -= 300;
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
