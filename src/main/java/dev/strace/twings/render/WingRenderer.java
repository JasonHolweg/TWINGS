package dev.strace.twings.render;

import dev.strace.twings.config.Settings;
import dev.strace.twings.wing.Wing;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Draws a wing with pure double math — no Location/Vector allocations and
 * no config lookups in the hot path.
 *
 * <p><b>Visual compatibility:</b> the formulas replicate the 2.x renderer
 * exactly, including its quirks, so wings that server owners tuned for
 * years look identical:</p>
 * <ul>
 *   <li>the yaw is converted to "radians" by dividing by 60 plus a fudge
 *       constant (2.985), not by {@code Math.toRadians}</li>
 *   <li>the rotated offset was accidentally applied twice (the tilt vector
 *       and the rotated vector were the same mutated object), doubling the
 *       wing scale — kept as a factor of 2</li>
 *   <li>the configured {@code rotation} (degrees) was added to an angle in
 *       radians — kept</li>
 *   <li>{@code SneakingDegreeAddition} always applies, not only while
 *       sneaking — kept</li>
 *   <li>the pattern's x-origin is derived from the row-0 column count plus
 *       one phantom column — kept (see {@link Wing#geometryCols()})</li>
 * </ul>
 */
public final class WingRenderer {

    private static final double COS_45 = 0.7071067811865476;

    private WingRenderer() {
    }

    /** Flap/rotation state supplied by the engine; may be null (previews of non-animated wings). */
    public record AnimSnapshot(double flap, double rotationStep) {
    }

    public static void drawOnPlayer(Wing w, Player p, AnimSnapshot anim, Settings settings, MovementTracker tracker) {
        if (settings.hideOnInvis() && p.hasPotionEffect(PotionEffectType.INVISIBILITY)) return;
        if (settings.hideOnSpectator() && p.getGameMode() == GameMode.SPECTATOR) return;
        if (tracker.isMoving(p) && !w.showWhenRunning()) return;

        Location l = p.getLocation();
        World world = l.getWorld();
        if (world == null) return;

        double yaw = l.getYaw();
        double space = w.spacing();
        double baseDY = 1.4 + (w.tiltBefore() ? 0 : w.moveup());
        if (p.isSneaking()) {
            baseDY -= 0.3;
            space -= 0.01;
        }

        // 2.x added the flap value to both angle terms -> effectively 2x
        double flap2 = (w.animated() && w.mirror() && anim != null) ? 2 * anim.flap() : 0;
        double totalAdd = w.degreeAddition() + w.sneakAddition() + flap2;

        draw(w, world, l.getX(), l.getY(), l.getZ(), yaw, space, baseDY, totalAdd, anim, settings);
    }

    /** Preview rendering (no sneak/flap additions — matches 2.x). */
    public static void drawAtLocation(Wing w, Location loc, AnimSnapshot anim, Settings settings) {
        World world = loc.getWorld();
        if (world == null) return;
        double baseDY = 1.4 + (w.tiltBefore() ? 0 : w.moveup());
        draw(w, world, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), w.spacing(), baseDY,
                w.degreeAddition(), anim, settings);
    }

    private static void draw(Wing w, World world, double px, double py, double pz, double yaw,
                             double space, double baseDY, double totalAdd, AnimSnapshot anim, Settings settings) {
        // wing origin, moved behind the player (horizontal facing * cos 45°, legacy pitch trick)
        double yawRad = Math.toRadians(yaw);
        double ox = px - (-Math.sin(yawRad) * COS_45) * w.moveback();
        double oz = pz - (Math.cos(yawRad) * COS_45) * w.moveback();

        // continuous rotation animation replaces the yaw-based angle entirely
        Double rotAngle = null;
        if (w.animated() && !w.mirror() && anim != null) {
            rotAngle = anim.rotationStep() / (settings.rotationSpeed() * 90.0);
        }

        double base = (yaw < -180 ? Math.PI : 2.985);
        double angleRight = rotAngle != null ? rotAngle : -((yaw + 180 + totalAdd) / 60.0) + base + w.rotation();
        drawHalf(w, world, ox, py, oz, yaw, space, baseDY, false, angleRight);
        if (w.mirror()) {
            double angleLeft = rotAngle != null ? rotAngle : -((yaw + 180 - totalAdd) / 60.0) + base + w.rotation();
            drawHalf(w, world, ox, py, oz, yaw, space, baseDY, true, angleLeft);
        }
    }

    private static void drawHalf(Wing w, World world, double ox, double oy, double oz, double yaw,
                                 double space, double baseDY, boolean left, double angle) {
        int len = w.geometryCols();
        // mirror halves start offset to one side; single (non-mirror) wings are centered
        double defXrel;
        if (left) {
            defXrel = space * len;
        } else {
            defXrel = w.mirror() ? -space * len + space : -space * len / 2 + space;
        }

        double tiltRad = Math.toRadians(w.tilt());
        double ct = Math.cos(tiltRad);
        double st = Math.sin(tiltRad);
        double ca = Math.cos(angle);
        double sa = Math.sin(angle);

        // Sideways "back vector" that pushes the wing behind the body.
        // In 2.x this came from getBackVector(loc, left), which negated the
        // vector for the left half, and a multiplier that ALSO flipped sign
        // (-0.25 vs 0.25) — the two negations cancel, so both mirror halves end
        // up with the SAME offset (-0.25). Applying only the multiplier sign
        // here would push the mirrored half in front of the body instead.
        double sideRad = Math.toRadians(yaw + 90);
        double sideScale = w.mirror() ? -0.25 : -0.5;
        double v2x = Math.cos(sideRad) * sideScale;
        double v2z = Math.sin(sideRad) * sideScale;

        double yBump = w.tiltBefore() ? w.moveup() : 0;

        for (Wing.Cell cell : w.cells()) {
            double dx = left ? defXrel - cell.col() * space : defXrel + cell.col() * space;
            double dy = baseDY - cell.row() * space;
            // rotate around X by tilt (dz is 0), then around Y by angle
            double ry = dy * ct;
            double rz = dy * st;
            double x2 = dx * ca + rz * sa;
            double z2 = -dx * sa + rz * ca;
            // factor 2 = legacy double-apply, see class javadoc
            cell.spec().spawn(world,
                    ox + 2 * x2 + v2x,
                    oy + 2 * ry + yBump,
                    oz + 2 * z2 + v2z);
        }
    }
}
