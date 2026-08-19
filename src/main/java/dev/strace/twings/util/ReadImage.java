package dev.strace.twings.util;

import dev.strace.twings.Main;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts an image into a grid of dust colors for picture wings.
 * A wing pattern larger than {@link #MAX_PIXELS} pixels is rejected.
 */
public class ReadImage {

    public static final int MAX_PIXELS = 5000;

    private final Map<Color, String> colorlist = new LinkedHashMap<>();

    /**
     * @return per-pixel color codes [row][col], or null if the image is too large.
     */
    public String[][] getColorCodes(BufferedImage img) {
        if (img == null) return null;
        if (img.getWidth() * img.getHeight() > MAX_PIXELS) return null;

        String[][] codes = new String[img.getHeight()][img.getWidth()];
        int count = 1;
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                Color col = new Color(img.getRGB(x, y));
                String code = colorlist.get(col);
                if (code == null) {
                    code = "C" + count++;
                    colorlist.put(col, code);
                }
                codes[y][x] = code;
            }
        }
        return codes;
    }

    public BufferedImage getImage(String name) {
        File dir = new File(Main.getInstance().getDataFolder(), "pictures");
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (file.getName().equalsIgnoreCase(name)) {
                try {
                    return ImageIO.read(file);
                } catch (Exception e) {
                    Main.getInstance().getLogger().warning("Could not read image " + file.getName() + ": " + e.getMessage());
                    return null;
                }
            }
        }
        return null;
    }

    public Map<Color, String> getColorlist() {
        return colorlist;
    }
}
