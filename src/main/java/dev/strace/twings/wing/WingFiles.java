package dev.strace.twings.wing;

import dev.strace.twings.Main;
import dev.strace.twings.util.ReadImage;
import dev.strace.twings.util.YamlFile;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates wing files: the bundled template on first run, and wings generated
 * from pictures. File format matches 2.x; new files use the current particle
 * names (DUST instead of REDSTONE etc.).
 */
public final class WingFiles {

    private WingFiles() {
    }

    public static void ensureTemplate(Main plugin, File wingsDir) {
        File file = new File(wingsDir, "template.yml");
        if (file.exists()) return;

        YamlFile yaml = new YamlFile(file);
        YamlConfiguration cfg = yaml.cfg();
        cfg.set("Particles.R", "DUST:(255,0,0):2");
        cfg.set("Particles.G", "DUST:(0,255,0)");
        cfg.set("Particles.B", "DUST:(0,0,255)");
        cfg.set("Particles.SWEEP", "SWEEP_ATTACK");
        cfg.set("Particles.E", "HAPPY_VILLAGER:10");
        cfg.set("Item.Material", Material.ELYTRA.toString());
        cfg.set("Item.Name", "&dUgly Template Wings");
        cfg.set("permission", "twings.template");
        cfg.set("creator", "PixelStrace");
        cfg.set("DegreeAddition", 10);
        cfg.set("SneakingDegreeAddition", 20);
        cfg.set("ShowWhenRunning", false);
        cfg.set("tilt", 10);
        cfg.set("tiltbefore", false);
        cfg.set("runtilt", 0);
        cfg.set("mirrow", true);
        cfg.set("moveup", 0);
        cfg.set("moveback", 0);
        cfg.set("rotation", 0);
        cfg.set("spacing", 0.07);
        cfg.set("category", "wings");
        cfg.set("exclude", List.of("x", "SWEEP"));
        cfg.set("Animated", true);
        cfg.set("pattern", List.of(
                "x,E,x,x,x,x,x,x,x,x,x,x,x",
                "x,x,R,x,x,x,x,x,x,x,x,x,x",
                "x,G,R,R,x,x,x,x,x,x,x,x,x",
                "x,x,G,B,B,B,x,x,x,x,x,x,x",
                "x,x,x,x,B,B,B,R,x,x,x,x,x",
                "x,SWEEP,x,x,B,B,B,B,R,x,x,x,x",
                "x,x,x,R,B,B,B,B,R,R,x,x,x",
                "x,x,x,R,x,G,G,B,B,R,G,G,x",
                "x,x,x,x,x,x,x,x,B,B,B,x,B",
                "x,x,x,x,x,x,x,x,x,x,B,B,B",
                "x,x,x,x,x,x,x,x,B,B,B,B,B",
                "x,x,x,x,x,x,B,B,B,B,x,x,x",
                "x,x,x,x,B,B,x,B,x,x,x,x,x",
                "x,x,x,x,R,x,x,x,x,x,x,x,x",
                "x,x,x,x,x,x,x,x,x,x,x,x,x"));
        yaml.save(plugin);
    }

    /**
     * Creates a wing from an image (one dust color per pixel). The code of
     * the top-left pixel ("C1", usually the background) is excluded so the
     * background does not render.
     *
     * @return false if the image is missing or larger than
     *         {@link ReadImage#MAX_PIXELS} pixels.
     */
    public static boolean createFromPicture(Main plugin, String pictureFileName, BufferedImage image) {
        ReadImage reader = new ReadImage();
        String[][] codes = reader.getColorCodes(image);
        if (codes == null) return false;

        String wingId = pictureFileName.replaceAll("(?i)\\.(png|jpe?g|gif|bmp)$", "");
        File file = new File(plugin.wings().wingsDir(), wingId + ".yml");
        YamlFile yaml = new YamlFile(file);
        YamlConfiguration cfg = yaml.cfg();

        List<String> pattern = new ArrayList<>();
        for (String[] row : codes) {
            pattern.add(String.join(",", row));
        }
        for (var entry : reader.getColorlist().entrySet()) {
            Color color = entry.getKey();
            cfg.set("Particles." + entry.getValue(),
                    "DUST:" + color.getRed() + "," + color.getGreen() + "," + color.getBlue());
        }
        cfg.set("Item.Material", Material.COOKIE.toString());
        cfg.set("Item.Name", "&c" + wingId);
        cfg.set("permission", "twings.picture");
        cfg.set("creator", "STRACE");
        cfg.set("DegreeAddition", 0);
        cfg.set("SneakingDegreeAddition", 0);
        cfg.set("ShowWhenRunning", false);
        cfg.set("tilt", 0);
        cfg.set("tiltbefore", false);
        cfg.set("runtilt", 0);
        cfg.set("mirrow", false);
        cfg.set("rotation", 0);
        cfg.set("moveup", 1);
        cfg.set("moveback", 0);
        cfg.set("Animated", false);
        cfg.set("spacing", 0.08);
        cfg.set("category", "picture");
        cfg.set("exclude", List.of("C1"));
        cfg.set("pattern", pattern);
        return yaml.save(plugin);
    }
}
