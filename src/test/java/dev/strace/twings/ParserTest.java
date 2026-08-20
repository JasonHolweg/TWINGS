package dev.strace.twings;

import dev.strace.twings.equipment.EquipmentManager;
import dev.strace.twings.equipment.TimedEquips;
import dev.strace.twings.util.MyColors;
import dev.strace.twings.wing.ParticleAliases;
import dev.strace.twings.wing.ParticleSpec;
import dev.strace.twings.wing.Wing;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure parsing logic — everything here runs without a server.
 */
class ParserTest {

    // --- particle aliases (2.x wing files must keep working) ---

    @Test
    void resolvesLegacyParticleNames() {
        assertEquals(Particle.DUST, ParticleAliases.resolve("REDSTONE"));
        assertEquals(Particle.HAPPY_VILLAGER, ParticleAliases.resolve("VILLAGER_HAPPY"));
        assertEquals(Particle.ENCHANT, ParticleAliases.resolve("ENCHANTMENT_TABLE"));
        assertEquals(Particle.SWEEP_ATTACK, ParticleAliases.resolve("SWEEP_ATTACK"));
        assertEquals(Particle.DUST, ParticleAliases.resolve(" dust "));
        assertNull(ParticleAliases.resolve("NOT_A_PARTICLE"));
    }

    @Test
    void parsesLegacyDustFormats() {
        ParticleSpec withParens = ParticleSpec.parse("R", "REDSTONE:(255,0,0):2");
        assertTrue(withParens.isDust());
        assertEquals("#ff0000", withParens.hex());

        ParticleSpec withoutParens = ParticleSpec.parse("B", "REDSTONE:0,0,255");
        assertTrue(withoutParens.isDust());
        assertEquals("#0000ff", withoutParens.hex());

        ParticleSpec modern = ParticleSpec.parse("G", "DUST:(0,255,0):1.5");
        assertEquals("#00ff00", modern.hex());
    }

    @Test
    void parsesPlainParticles() {
        assertFalse(ParticleSpec.parse("S", "SWEEP_ATTACK").isDust());
        assertFalse(ParticleSpec.parse("E", "VILLAGER_HAPPY:10").isDust());
    }

    @Test
    void rejectsInvalidParticleDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> ParticleSpec.parse("X", "NOT_A_PARTICLE"));
        assertThrows(IllegalArgumentException.class, () -> ParticleSpec.parse("X", "DUST"));
        assertThrows(IllegalArgumentException.class, () -> ParticleSpec.parse("X", "DUST:abc"));
        assertThrows(IllegalArgumentException.class, () -> ParticleSpec.parse("X", ""));
    }

    // --- equipped-wings migration (the broken "(X-X)" regex split of 2.x) ---

    @Test
    void migratesLegacyEquippedEntries() {
        assertEquals(List.of("vampire", "angel"),
                EquipmentManager.parseLegacyValue("vampire.yml(X-X)angel.yml"));
        assertEquals(List.of("vampire"), EquipmentManager.parseLegacyValue("vampire.yml"));
        assertEquals(List.of("vampire"), EquipmentManager.parseLegacyValue("vampire"));
        assertEquals(List.of(), EquipmentManager.parseLegacyValue(""));
        assertEquals(List.of(), EquipmentManager.parseLegacyValue(null));
    }

    // --- /wings give time parsing ---

    @Test
    void parsesGiveDurations() {
        assertEquals(18000, TimedEquips.parseSeconds("300M"));
        assertEquals(18000, TimedEquips.parseSeconds("5h"));
        assertEquals(18000, TimedEquips.parseSeconds("18000S"));
        assertEquals(172800, TimedEquips.parseSeconds("2d"));
        assertEquals(90, TimedEquips.parseSeconds("90"));
        assertEquals(0, TimedEquips.parseSeconds("UntilDeath"));
        assertEquals(-1, TimedEquips.parseSeconds("abc"));
        assertEquals(-1, TimedEquips.parseSeconds("-5m"));
        assertEquals(-1, TimedEquips.parseSeconds("0"));
        assertEquals(-1, TimedEquips.parseSeconds(null));
    }

    @Test
    void describesGiveDurations() {
        assertEquals("300 minutes", TimedEquips.describe("300M"));
        assertEquals("untildeath", TimedEquips.describe("untildeath"));
        assertEquals("45 seconds", TimedEquips.describe("45"));
        assertEquals("2 days", TimedEquips.describe("2d"));
    }

    // --- color formatting (hex must work without the removed version sniff) ---

    @Test
    void formatsLegacyAndHexColors() {
        assertEquals("§aHi", MyColors.format("&aHi"));
        assertEquals("§x§f§f§0§0§0§0X", MyColors.format("#ff0000X"));
        assertEquals("Test Wing", MyColors.strip("&a&lTest #ff0000Wing"));
        assertEquals("", MyColors.format(null));
    }

    // --- full wing file parsing ---

    @Test
    void loadsWingFileWithLegacyKeysAndAliases(@TempDir Path dir) throws Exception {
        File file = dir.resolve("testwing.yml").toFile();
        Files.writeString(file.toPath(), """
                Particles:
                  R: 'REDSTONE:(255,0,0):2'
                  E: 'VILLAGER_HAPPY:10'
                  SWEEP: 'SWEEP_ATTACK'
                Item:
                  Material: ELYTRA
                  Name: '&dTest Wings'
                permission: twings.test
                creator: Tester
                mirrow: true
                Animated: true
                tilt: 10
                spacing: 0.07
                exclude:
                - x
                - SWEEP
                pattern:
                - x,R,x
                - R,E,R
                """);

        Wing wing = Wing.load(file);
        assertEquals("testwing", wing.id());
        assertTrue(wing.mirror(), "mirrow: must be accepted as mirror");
        assertTrue(wing.animated());
        // 4 drawable cells: R / R,E,R — SWEEP is excluded, x never draws
        assertEquals(4, wing.cells().size());
        // legacy geometry: row-0 columns + phantom column
        assertEquals(4, wing.geometryCols());
        assertEquals("wings", wing.category(), "default category");
        assertEquals(0.07, wing.spacing());
    }

    @Test
    void wingLoadFailsWithHelpfulMessages(@TempDir Path dir) throws Exception {
        File file = dir.resolve("broken.yml").toFile();
        Files.writeString(file.toPath(), """
                Particles:
                  R: 'NOT_A_PARTICLE'
                Item:
                  Material: ELYTRA
                pattern:
                - R
                """);
        Exception e = assertThrows(Exception.class, () -> Wing.load(file));
        assertTrue(e.getMessage().contains("NOT_A_PARTICLE"));
    }
}
