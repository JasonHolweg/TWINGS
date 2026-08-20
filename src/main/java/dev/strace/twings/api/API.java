package dev.strace.twings.api;

import dev.strace.twings.Main;
import dev.strace.twings.util.ItemBuilder;
import dev.strace.twings.util.MyColors;
import dev.strace.twings.wing.Wing;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Public developer API, kept source-compatible with 2.x where possible.
 * {@link #getTwing()} now returns the immutable {@link Wing} model.
 */
public class API {

    private final Wing wing;

    /** Accepts the wing file name with or without the .yml extension. */
    public API(String fileName) {
        this.wing = Main.getInstance().wings().byFileName(fileName);
    }

    public Wing getTwing() {
        return wing;
    }

    public boolean exists() {
        return wing != null;
    }

    public String getWingName() {
        return wing == null ? null : MyColors.format(wing.itemName());
    }

    public ItemStack getWingItem(Player p, boolean lore) {
        if (wing == null) return null;
        if (!lore) {
            return new ItemBuilder(wing.material()).setName(getWingName()).build();
        }
        return Main.getInstance().buildWingItem(wing, p);
    }

    /** Replaces the player's equipped wings with this one (no messages). */
    public void setPlayerCurrentWing(Player p) {
        if (wing != null) {
            Main.getInstance().equipment().set(p.getUniqueId(), wing.id());
        }
    }

    /** Adds this wing to the player's equipped wings (with feedback messages). */
    public void addPlayerCurrentWings(Player p) {
        if (wing != null) {
            Main.getInstance().equipService().equipAdd(p, wing);
        }
    }
}
