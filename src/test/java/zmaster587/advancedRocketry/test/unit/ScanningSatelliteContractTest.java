package zmaster587.advancedRocketry.test.unit;

import org.junit.Test;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.satellite.SatelliteComposition;
import zmaster587.advancedRocketry.satellite.SatelliteDensity;
import zmaster587.advancedRocketry.satellite.SatelliteMassScanner;
import zmaster587.advancedRocketry.satellite.SatelliteOptical;
import zmaster587.advancedRocketry.satellite.SatelliteOreMapping;
import zmaster587.advancedRocketry.satellite.SatelliteSpyTelescope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Coverage-audit gap (Tier 2 #8) — scanning satellites' unit-tier
 * contracts.
 *
 * <p>Six scanning satellite types (Optical, Density, Composition,
 * MassScanner, OreMapping, SpyTelescope) had ZERO unit/integration
 * coverage pre-this-test. {@code SatelliteTypeBehaviourTest} only
 * covered the three action-driven types (biomeChanger, solarEnergy,
 * weatherController).</p>
 *
 * <p>Pinning the unit-tier contract for the scanners:</p>
 *
 * <ul>
 *   <li><b>Constructor doesn't throw</b> — fresh instance OK.</li>
 *   <li><b>Display name</b> — what the player sees on the satellite
 *       chip ItemStack tooltip and satellite-builder GUI. A regression
 *       that swaps names between types is a player-visible UX bug.</li>
 *   <li><b>Failure chance ≥ 0</b> — sanity invariant; a negative
 *       failure chance would break {@code SatelliteWeatherController}'s
 *       random-failure logic.</li>
 *   <li><b>OreMapping ore-filter gate</b> — {@code canFilterOre()}
 *       requires {@code maxDataStorage == 3000}. Default-constructed
 *       satellite has 0 → can't filter. {@code setSelectedSlot} is
 *       silently ignored when can't filter.</li>
 * </ul>
 *
 * <p>The deeper scan-output contracts (canBeginScan with seeded battery,
 * scanChunk returning a populated grid) need a World context — those
 * belong at server tier where the harness has a real world. The
 * unit-tier pins here protect against regressions in the registry +
 * basic invariants without server-harness cost.</p>
 */
public class ScanningSatelliteContractTest {

    @Test
    public void allScanningSatellitesProduceNonEmptyDistinctNames() {
        // Six classes, six distinct human-readable names. The names go
        // straight into ItemStack tooltips and the satellite-builder GUI.
        SatelliteBase[] scanners = new SatelliteBase[] {
                new SatelliteOreMapping(),
                new SatelliteDensity(),
                new SatelliteComposition(),
                new SatelliteMassScanner(),
                new SatelliteOptical(),
                new SatelliteSpyTelescope(),
        };
        java.util.Set<String> seenNames = new java.util.HashSet<>();
        for (SatelliteBase sat : scanners) {
            String name = sat.getName();
            assertNotNull(sat.getClass().getSimpleName() + ".getName() must be non-null",
                    name);
            assertFalse(sat.getClass().getSimpleName() + ".getName() must be non-empty",
                    name.isEmpty());
            assertTrue(sat.getClass().getSimpleName() + ".getName() must be unique "
                            + "across scanners (collision with another scanner = "
                            + "GUI ambiguity); duplicate: " + name,
                    seenNames.add(name));
        }
    }

    @Test
    public void allScanningSatellitesHaveNonNegativeFailureChance() {
        SatelliteBase[] scanners = new SatelliteBase[] {
                new SatelliteOreMapping(),
                new SatelliteDensity(),
                new SatelliteComposition(),
                new SatelliteMassScanner(),
                new SatelliteOptical(),
                new SatelliteSpyTelescope(),
        };
        for (SatelliteBase sat : scanners) {
            double fc = sat.failureChance();
            assertTrue(sat.getClass().getSimpleName() + ".failureChance() must be "
                            + ">= 0 (got " + fc + ")",
                    fc >= 0);
            // Failure chance is a probability — must be sane.
            assertTrue(sat.getClass().getSimpleName() + ".failureChance() must be "
                            + "<= 1 (got " + fc + ")",
                    fc <= 1);
        }
    }

    @Test
    public void oreMappingPinsExpectedDisplayName() {
        // Belt-and-braces pin on the literal — the suite-wide
        // uniqueness check above catches collisions but not silent
        // text mutations. "Ore Mapper" is what the player has been
        // seeing since the satellite was added.
        assertEquals("OreMapping display name", "Ore Mapper",
                new SatelliteOreMapping().getName());
    }

    @Test
    public void oreMappingCanFilterOreRequires3000MaxDataStorage() {
        // canFilterOre gate per SatelliteOreMapping:220-222 — only the
        // top-tier chip (maxDataStorage == 3000) gets ore-filter ability.
        // Default-constructed satellite has 0 maxDataStorage → can't filter.
        SatelliteOreMapping sat = new SatelliteOreMapping();
        assertFalse("default-constructed OreMapping cannot filter ore — "
                        + "satelliteProperties.getMaxDataStorage() == 0 != 3000",
                sat.canFilterOre());
    }

    @Test
    public void oreMappingSetSelectedSlotIsIgnoredWhenCannotFilter() {
        // Default OreMapping has selectedSlot=-1, canFilterOre=false.
        // setSelectedSlot guards on canFilterOre — so calling it should
        // be a no-op when the chip can't filter.
        SatelliteOreMapping sat = new SatelliteOreMapping();
        assertEquals("default selectedSlot is -1", -1, sat.getSelectedSlot());
        sat.setSelectedSlot(5);
        assertEquals("setSelectedSlot(5) on can't-filter chip must be ignored — "
                        + "preserves the GUI invariant that low-tier chips don't "
                        + "respond to filter-slot clicks",
                -1, sat.getSelectedSlot());
    }

    @Test
    public void allScanningSatellitesGetInfoOnNullWorldDoesNotThrow() {
        // getInfo() is called by the satellite-builder GUI on each chip
        // to render the status string. Some types (OreMapping, Density,
        // etc.) return a literal string and ignore the world arg —
        // SpyTelescope might read world state but should null-guard.
        // Smoke pin: calling getInfo(null) must not throw on ANY type.
        for (SatelliteBase sat : new SatelliteBase[] {
                new SatelliteOreMapping(),
                new SatelliteDensity(),
                new SatelliteComposition(),
                new SatelliteMassScanner(),
                new SatelliteOptical(),
                // SpyTelescope DOES dereference world in getInfo, so it's
                // not included in the null-tolerance set. Future test
                // can pin its non-null-world behaviour.
        }) {
            String info = sat.getInfo(null);
            assertNotNull(sat.getClass().getSimpleName()
                    + ".getInfo(null) must not return null", info);
        }
    }
}
