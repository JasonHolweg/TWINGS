package dev.strace.twings.wing;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Immutable definition of one wing, parsed from {@code wings/<id>.yml}.
 * The file format is unchanged from 2.x (including the {@code mirrow} typo,
 * which is accepted alongside {@code mirror}).
 */
public final class Wing {

    /** One drawable cell of the pattern grid. */
    public record Cell(int col, int row, ParticleSpec spec) {
    }

    private final String id;
    private final File file;
    private final Material material;
    private final String itemName;
    private final String creator;
    private final String category;
    private final String permission;
    private final int tilt;
    private final int rotation;
    private final int sneakAddition;
    private final int degreeAddition;
    private final boolean animated;
    private final boolean mirror;
    private final boolean tiltBefore;
    private final boolean showWhenRunning;
    private final double moveback;
    private final double moveup;
    private final double spacing;
    private final List<String> patternLore;
    private final List<Cell> cells;
    /**
     * Legacy quirk: 2.x sized the pattern one column wider than the actual
     * data ({@code split.length + 1}) and derived the wing's x-origin from
     * that. Kept so existing wings render at the exact same position.
     */
    private final int geometryCols;

    private Wing(Builder b) {
        this.id = b.id;
        this.file = b.file;
        this.material = b.material;
        this.itemName = b.itemName;
        this.creator = b.creator;
        this.category = b.category;
        this.permission = b.permission;
        this.tilt = b.tilt;
        this.rotation = b.rotation;
        this.sneakAddition = b.sneakAddition;
        this.degreeAddition = b.degreeAddition;
        this.animated = b.animated;
        this.mirror = b.mirror;
        this.tiltBefore = b.tiltBefore;
        this.showWhenRunning = b.showWhenRunning;
        this.moveback = b.moveback;
        this.moveup = b.moveup;
        this.spacing = b.spacing;
        this.patternLore = List.copyOf(b.patternLore);
        this.cells = List.copyOf(b.cells);
        this.geometryCols = b.geometryCols;
    }

    public static Wing load(File file) throws WingLoadException {
        return parse(file, YamlConfiguration.loadConfiguration(file));
    }

    /** Parses a wing from an already-loaded configuration (used by /wings import). */
    public static Wing parse(File file, YamlConfiguration cfg) throws WingLoadException {
        Builder b = new Builder();
        b.file = file;
        b.id = file.getName().replaceAll("(?i)\\.yml$", "");

        String materialName = cfg.getString("Item.Material");
        b.material = materialName == null ? null : Material.matchMaterial(materialName);
        if (b.material == null) {
            throw new WingLoadException(file, "unknown Item.Material '" + materialName + "'");
        }
        b.itemName = cfg.getString("Item.Name", b.id);
        b.creator = cfg.getString("creator");
        b.category = cfg.getString("category", "wings");
        b.permission = cfg.getString("permission", "");
        b.tilt = cfg.getInt("tilt");
        b.rotation = cfg.getInt("rotation");
        b.sneakAddition = cfg.getInt("SneakingDegreeAddition");
        b.degreeAddition = cfg.getInt("DegreeAddition");
        b.animated = cfg.getBoolean("Animated");
        b.mirror = cfg.contains("mirror") ? cfg.getBoolean("mirror") : cfg.getBoolean("mirrow");
        b.tiltBefore = cfg.getBoolean("tiltbefore");
        b.showWhenRunning = cfg.getBoolean("ShowWhenRunning");
        b.moveback = cfg.getDouble("moveback");
        b.moveup = cfg.getDouble("moveup");
        b.spacing = cfg.getDouble("spacing");
        if (b.spacing == 0) b.spacing = 0.07;

        List<String> exclude = cfg.getStringList("exclude");

        // Particles section -> parsed specs, minus excluded codes
        List<ParticleSpec> specs = new ArrayList<>();
        var particleSection = cfg.getConfigurationSection("Particles");
        if (particleSection == null) {
            throw new WingLoadException(file, "missing 'Particles' section");
        }
        for (String code : particleSection.getKeys(false)) {
            if (exclude.contains(code)) continue;
            try {
                specs.add(ParticleSpec.parse(code, particleSection.getString(code)));
            } catch (IllegalArgumentException e) {
                throw new WingLoadException(file, "Particles." + code + ": " + e.getMessage());
            }
        }

        // pattern grid -> cells
        List<String> rows = cfg.getStringList("pattern");
        if (rows.isEmpty()) {
            throw new WingLoadException(file, "missing or empty 'pattern' list");
        }
        b.geometryCols = rows.get(0).split(",").length + 1;
        for (int row = 0; row < rows.size(); row++) {
            String[] tokens = rows.get(row).split(",");
            for (int col = 0; col < tokens.length; col++) {
                String token = tokens[col].trim();
                for (ParticleSpec spec : specs) {
                    if (token.equalsIgnoreCase(spec.code())) {
                        b.cells.add(new Cell(col, row, spec));
                        break;
                    }
                }
            }
        }

        // pattern lore preview lines (colored symbols), built once
        for (int row = 0; row < rows.size(); row++) {
            b.patternLore.add(rows.get(row));
        }
        return new Wing(b);
    }

    private static final class Builder {
        String id;
        File file;
        Material material;
        String itemName;
        String creator;
        String category;
        String permission;
        int tilt;
        int rotation;
        int sneakAddition;
        int degreeAddition;
        boolean animated;
        boolean mirror;
        boolean tiltBefore;
        boolean showWhenRunning;
        double moveback;
        double moveup;
        double spacing;
        int geometryCols;
        final List<String> patternLore = new ArrayList<>();
        final List<Cell> cells = new ArrayList<>();
    }

    public String id() {
        return id;
    }

    public String idLower() {
        return id.toLowerCase(Locale.ROOT);
    }

    public File file() {
        return file;
    }

    public Material material() {
        return material;
    }

    public String itemName() {
        return itemName;
    }

    public String creator() {
        return creator;
    }

    public String category() {
        return category;
    }

    public String permission() {
        return permission;
    }

    public int tilt() {
        return tilt;
    }

    public int rotation() {
        return rotation;
    }

    public int sneakAddition() {
        return sneakAddition;
    }

    public int degreeAddition() {
        return degreeAddition;
    }

    public boolean animated() {
        return animated;
    }

    public boolean mirror() {
        return mirror;
    }

    public boolean tiltBefore() {
        return tiltBefore;
    }

    public boolean showWhenRunning() {
        return showWhenRunning;
    }

    public double moveback() {
        return moveback;
    }

    public double moveup() {
        return moveup;
    }

    public double spacing() {
        return spacing;
    }

    /** Raw pattern rows as written in the file (for GUI lore). */
    public List<String> patternRows() {
        return patternLore;
    }

    public List<Cell> cells() {
        return cells;
    }

    public int geometryCols() {
        return geometryCols;
    }

    /** Finds the parsed spec for a pattern token, or null. */
    public ParticleSpec specFor(String token) {
        for (Cell c : cells) {
            if (c.spec().code().equalsIgnoreCase(token)) return c.spec();
        }
        return null;
    }
}
