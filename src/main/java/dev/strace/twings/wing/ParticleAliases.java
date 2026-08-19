package dev.strace.twings.wing;

import org.bukkit.Particle;

import java.util.Locale;
import java.util.Map;

/**
 * Resolves particle names from wing files, including the pre-1.20.5 Bukkit
 * names still present in wing files created with TWINGS 2.x. Keeping this
 * table means nobody has to touch their wing collection to upgrade.
 */
public final class ParticleAliases {

    private static final Map<String, String> LEGACY = Map.ofEntries(
            Map.entry("REDSTONE", "DUST"),
            Map.entry("VILLAGER_HAPPY", "HAPPY_VILLAGER"),
            Map.entry("VILLAGER_ANGRY", "ANGRY_VILLAGER"),
            Map.entry("EXPLOSION_NORMAL", "POOF"),
            Map.entry("EXPLOSION_LARGE", "EXPLOSION"),
            Map.entry("EXPLOSION_HUGE", "EXPLOSION_EMITTER"),
            Map.entry("FIREWORKS_SPARK", "FIREWORK"),
            Map.entry("WATER_BUBBLE", "BUBBLE"),
            Map.entry("WATER_SPLASH", "SPLASH"),
            Map.entry("WATER_WAKE", "FISHING"),
            Map.entry("WATER_DROP", "RAIN"),
            Map.entry("SUSPENDED", "UNDERWATER"),
            Map.entry("SUSPENDED_DEPTH", "UNDERWATER"),
            Map.entry("CRIT_MAGIC", "ENCHANTED_HIT"),
            Map.entry("SMOKE_NORMAL", "SMOKE"),
            Map.entry("SMOKE_LARGE", "LARGE_SMOKE"),
            Map.entry("SPELL", "EFFECT"),
            Map.entry("SPELL_INSTANT", "INSTANT_EFFECT"),
            Map.entry("SPELL_MOB", "ENTITY_EFFECT"),
            Map.entry("SPELL_MOB_AMBIENT", "ENTITY_EFFECT"),
            Map.entry("SPELL_WITCH", "WITCH"),
            Map.entry("DRIP_WATER", "DRIPPING_WATER"),
            Map.entry("DRIP_LAVA", "DRIPPING_LAVA"),
            Map.entry("TOWN_AURA", "MYCELIUM"),
            Map.entry("ENCHANTMENT_TABLE", "ENCHANT"),
            Map.entry("SNOWBALL", "ITEM_SNOWBALL"),
            Map.entry("SNOW_SHOVEL", "ITEM_SNOWBALL"),
            Map.entry("SLIME", "ITEM_SLIME"),
            Map.entry("MOB_APPEARANCE", "ELDER_GUARDIAN"),
            Map.entry("TOTEM", "TOTEM_OF_UNDYING"),
            Map.entry("BARRIER", "BLOCK_MARKER"));

    private ParticleAliases() {
    }

    /**
     * @return the resolved particle, or null if the name is unknown even
     *         after alias translation.
     */
    public static Particle resolve(String name) {
        if (name == null) return null;
        String key = name.trim().toUpperCase(Locale.ROOT);
        try {
            return Particle.valueOf(key);
        } catch (IllegalArgumentException ignored) {
        }
        String alias = LEGACY.get(key);
        if (alias != null) {
            try {
                return Particle.valueOf(alias);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
