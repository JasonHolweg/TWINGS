package dev.strace.twings.wing;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;

/**
 * One particle definition from a wing file's {@code Particles} section,
 * e.g. {@code REDSTONE:(255,0,0):2} or {@code FLAME:0.1}. Parsed once at
 * load time — including the DustOptions, which the old code re-allocated
 * for every single particle every tick.
 */
public final class ParticleSpec {

    private final String code;
    private final Particle particle;
    private final double speed;
    private final Particle.DustOptions dust;
    private final String hex;

    private ParticleSpec(String code, Particle particle, double speed, Particle.DustOptions dust, String hex) {
        this.code = code;
        this.particle = particle;
        this.speed = speed;
        this.dust = dust;
        this.hex = hex;
    }

    /**
     * @throws IllegalArgumentException with a human-readable reason if the
     *                                  definition cannot be parsed.
     */
    public static ParticleSpec parse(String code, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("empty particle definition");
        }
        String[] split = value.split(":");
        Particle particle = ParticleAliases.resolve(split[0]);
        if (particle == null) {
            throw new IllegalArgumentException("unknown particle '" + split[0] + "'");
        }
        try {
            if (particle == Particle.DUST) {
                // legacy format: REDSTONE:(r,g,b) or REDSTONE:r,g,b, optional :size
                if (split.length < 2) {
                    throw new IllegalArgumentException("dust particle needs a color, e.g. DUST:(255,0,0)");
                }
                String[] rgb = split[1].replace("(", "").replace(")", "").replace(" ", "").split(",");
                Color color = Color.fromRGB(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
                // dust "speed" is its size; 0 would be invisible, the old default was 0.8
                double size = split.length > 2 ? Double.parseDouble(split[2]) : 0.8;
                String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                return new ParticleSpec(code, particle, size,
                        new Particle.DustOptions(color, (float) size), hex);
            }
            double speed = split.length > 1 ? Double.parseDouble(split[1]) : 0;
            return new ParticleSpec(code, particle, speed, null, null);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("invalid particle definition '" + value + "'");
        }
    }

    public void spawn(World world, double x, double y, double z) {
        if (dust != null) {
            world.spawnParticle(particle, x, y, z, 0, dust);
        } else {
            world.spawnParticle(particle, x, y, z, 1, 0, 0, 0, speed);
        }
    }

    public String code() {
        return code;
    }

    public boolean isDust() {
        return dust != null;
    }

    /** e.g. {@code #ff0000}; null for non-dust particles. */
    public String hex() {
        return hex;
    }
}
