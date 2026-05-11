package zmaster587.advancedRocketry.test.unit;

import org.junit.Test;
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;
import zmaster587.advancedRocketry.util.AstronomicalBodyHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * §6.7 Pure-math astronomy helpers.
 *
 * Excluded from these tests: {@code getOrbitalTheta} / {@code getMoonOrbitalTheta} —
 * they call {@code AdvancedRocketry.proxy.getWorldTimeUniversal} which requires the
 * proxy to be initialized (only available inside a running game). Those are covered
 * by scenario tests in §7.
 */
public class AstronomicalBodyHelperTest {

    private static StellarBody sunLikeStar() {
        StellarBody star = new StellarBody();
        // Defaults: size=1.0, blackHole=false, subStars=[]. Set temperature to a Sol-like value.
        star.setTemperature(100); // normalizedStarTemperature = 1.0 in getStellarBrightness math
        return star;
    }

    @Test
    public void bodySizeMultiplierIsInverselyProportionalToDistance() {
        // At 100 distance (1 AU equivalent) the multiplier is 1.
        assertEquals(1.0f, AstronomicalBodyHelper.getBodySizeMultiplier(100f), 1e-6);
        // Doubling the orbital distance halves the apparent size.
        assertEquals(0.5f, AstronomicalBodyHelper.getBodySizeMultiplier(200f), 1e-6);
        // Halving the distance doubles the apparent size.
        assertEquals(2.0f, AstronomicalBodyHelper.getBodySizeMultiplier(50f), 1e-6);
    }

    @Test
    public void orbitalPeriodAtEarthDistanceIsBaseline() {
        // At 100 distance and solarSize=1.0, the formula reduces to 48 days (one MC year).
        assertEquals(48.0, AstronomicalBodyHelper.getOrbitalPeriod(100, 1.0f), 1e-9);
    }

    @Test
    public void orbitalPeriodGrowsWithDistance() {
        double inner = AstronomicalBodyHelper.getOrbitalPeriod(50, 1.0f);
        double earth = AstronomicalBodyHelper.getOrbitalPeriod(100, 1.0f);
        double outer = AstronomicalBodyHelper.getOrbitalPeriod(200, 1.0f);

        assertTrue("inner planet must orbit faster than Earth", inner < earth);
        assertTrue("outer planet must orbit slower than Earth", outer > earth);
    }

    @Test
    public void moonOrbitalPeriodAtBaselineDistanceMatchesShortMonth() {
        // At distance 100 and planetary mass 1.0, the formula collapses to 8 MC days.
        assertEquals(8.0, AstronomicalBodyHelper.getMoonOrbitalPeriod(100f, 1.0f), 1e-9);
    }

    @Test
    public void stellarBrightnessMonotonicWithDistance() {
        StellarBody star = sunLikeStar();
        double atOneAu = AstronomicalBodyHelper.getStellarBrightness(star, 100);
        double atTwoAu = AstronomicalBodyHelper.getStellarBrightness(star, 200);
        double atHalfAu = AstronomicalBodyHelper.getStellarBrightness(star, 50);

        assertTrue("brightness must drop with distance", atTwoAu < atOneAu);
        assertTrue("brightness must rise as we approach the star", atHalfAu > atOneAu);
    }

    @Test
    public void stellarBrightnessAtEarthBaselineEqualsOne() {
        // sunLike: size=1.0, temperature=100 → normalized=1.0, distance=100 → AU=1.
        // Formula reduces to (1.0 * (1 * 1) / 1) = 1.0.
        assertEquals(1.0, AstronomicalBodyHelper.getStellarBrightness(sunLikeStar(), 100), 1e-9);
    }

    @Test
    public void blackHoleStarReducesBrightness() {
        StellarBody star = sunLikeStar();
        double normal = AstronomicalBodyHelper.getStellarBrightness(star, 100);

        StellarBody blackHole = sunLikeStar();
        blackHole.setBlackHole(true);
        double dimmed = AstronomicalBodyHelper.getStellarBrightness(blackHole, 100);

        // Implementation multiplies by 0.25 when the primary (and all sub-stars) are black holes.
        assertEquals(normal * 0.25, dimmed, 1e-9);
    }

    @Test
    public void planetaryLightLevelMultiplierBaselineIsOne() {
        assertEquals(1.0, AstronomicalBodyHelper.getPlanetaryLightLevelMultiplier(1.0), 1e-9);
    }

    @Test
    public void planetaryLightLevelGrowsSlowerThanInsolation() {
        // Eye-perceived brightness ~ 1.5x per 2x flux; the function is the natural log model.
        // Doubling flux must increase perceived brightness by ~1.5x.
        double doubleFlux = AstronomicalBodyHelper.getPlanetaryLightLevelMultiplier(2.0);
        assertEquals(1.5, doubleFlux, 1e-9);

        // Halving flux must drop perceived brightness to 1/1.5.
        double halfFlux = AstronomicalBodyHelper.getPlanetaryLightLevelMultiplier(0.5);
        assertEquals(1.0 / 1.5, halfFlux, 1e-9);
    }

    @Test
    public void averageTemperatureIsThicknessSensitive() {
        StellarBody star = sunLikeStar();
        int thinAtmosphereTemp = AstronomicalBodyHelper.getAverageTemperature(star, 100, 100);
        int thickAtmosphereTemp = AstronomicalBodyHelper.getAverageTemperature(star, 100, 1600);

        // A thick atmosphere heats the planet via the greenhouse multiplier in the formula.
        assertTrue("thicker atmosphere must imply higher surface temperature",
                thickAtmosphereTemp > thinAtmosphereTemp);
    }

    @Test
    public void averageTemperatureIsDistanceSensitive() {
        StellarBody star = sunLikeStar();
        int innerPlanet = AstronomicalBodyHelper.getAverageTemperature(star, 50, 100);
        int outerPlanet = AstronomicalBodyHelper.getAverageTemperature(star, 200, 100);

        assertTrue("planet farther from the star must be cooler", outerPlanet < innerPlanet);
    }
}
