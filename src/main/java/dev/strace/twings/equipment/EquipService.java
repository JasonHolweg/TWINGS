package dev.strace.twings.equipment;

import dev.strace.twings.Main;
import dev.strace.twings.wing.Wing;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * User-facing equip flows: permission checks, messages and sounds —
 * matching the 2.x behavior the players are used to.
 */
public final class EquipService {

    private final Main plugin;

    public EquipService(Main plugin) {
        this.plugin = plugin;
    }

    /** Empty permission means the wing is available to everyone. */
    public boolean mayUse(Player p, Wing wing) {
        return wing.permission().isEmpty() || p.hasPermission(wing.permission());
    }

    /** Replaces all equipped wings with the given one ("left click" flow). */
    public boolean equipReplace(Player p, Wing wing) {
        if (!mayUse(p, wing)) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.4F, 0.7F);
            p.sendMessage(plugin.messages().getNoPermission());
            return false;
        }
        unequipAll(p);
        plugin.equipment().set(p.getUniqueId(), wing.id());
        if (plugin.messages().isShowMessages()) {
            p.sendMessage(plugin.messages().getEquip(wing));
        }
        p.playSound(p.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 0.4f, 10f);
        return true;
    }

    /** Adds a wing to the equipped set ("shift left click" flow). */
    public boolean equipAdd(Player p, Wing wing) {
        if (!mayUse(p, wing)) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.4F, 0.7F);
            p.sendMessage(plugin.messages().getNoPermission());
            return false;
        }
        switch (plugin.equipment().add(p.getUniqueId(), wing.id())) {
            case ALREADY_EQUIPPED -> {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 10f);
                if (plugin.messages().isShowMessages()) {
                    p.sendMessage(plugin.messages().getAlreadyEquipped());
                }
                return false;
            }
            case MAX_REACHED -> {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 10f);
                if (plugin.messages().isShowMessages()) {
                    p.sendMessage(plugin.messages().getMaxTwings());
                }
                return false;
            }
            default -> {
                if (plugin.messages().isShowMessages()) {
                    p.sendMessage(plugin.messages().getEquip(wing));
                }
                p.playSound(p.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 0.4f, 10f);
                return true;
            }
        }
    }

    public void unequip(Player p, Wing wing) {
        if (plugin.equipment().remove(p.getUniqueId(), wing.id())) {
            p.playSound(p.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 0.4f, 10f);
            if (plugin.messages().isShowMessages()) {
                p.sendMessage(plugin.messages().getUnequip(wing));
            }
        }
    }

    public void unequipAll(Player p) {
        if (plugin.messages().isShowMessages()) {
            for (String id : plugin.equipment().getEquipped(p.getUniqueId())) {
                Wing wing = plugin.wings().get(id);
                if (wing != null) p.sendMessage(plugin.messages().getUnequip(wing));
            }
        }
        plugin.equipment().clear(p.getUniqueId());
    }
}
