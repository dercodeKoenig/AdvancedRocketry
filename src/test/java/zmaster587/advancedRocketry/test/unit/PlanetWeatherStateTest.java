package zmaster587.advancedRocketry.test.unit;

import org.junit.Ignore;
import org.junit.Test;

/**
 * §6.10 Future weather B1 model.
 *
 * SMART task explicitly says these classes may not exist yet (the task is a spec
 * for the upcoming weather refactor). Once {@code PlanetWeatherState},
 * {@code PlanetWeatherSavedData}, {@code PlanetWeatherManager} and
 * {@code ARWeatherWorldInfo} land, remove the {@code @Ignore} on each method and
 * implement against the new API.
 *
 * Pre-B1 these are intentionally TODO so the suite compiles and runs without
 * scaffolding fictitious classes.
 */
public class PlanetWeatherStateTest {

    @Test @Ignore("Class PlanetWeatherState does not yet exist — implement after weather B1 refactor")
    public void planetWeatherStateDefaultsStable() {}

    @Test @Ignore("Class PlanetWeatherState does not yet exist — implement after weather B1 refactor")
    public void planetWeatherStateNbtRoundTrip() {}

    @Test @Ignore("Class PlanetWeatherSavedData does not yet exist — implement after weather B1 refactor")
    public void planetWeatherSavedDataStoresByDimensionId() {}

    @Test @Ignore("Class ARWeatherWorldInfo does not yet exist — implement after weather B1 refactor")
    public void arWeatherWorldInfoDelegatesNonWeatherFields() {}

    @Test @Ignore("Class ARWeatherWorldInfo does not yet exist — implement after weather B1 refactor")
    public void arWeatherWorldInfoOverridesOnlyWeatherFields() {}

    @Test @Ignore("Class ARWeatherWorldInfo does not yet exist — implement after weather B1 refactor")
    public void arWeatherWorldInfoDoesNotOverrideWorldTime() {}

    @Test @Ignore("Class ARWeatherWorldInfo does not yet exist — implement after weather B1 refactor")
    public void arWeatherWorldInfoMarksDirtyOnWeatherMutation() {}
}
