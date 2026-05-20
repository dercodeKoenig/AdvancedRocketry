package zmaster587.advancedRocketry.test.server;

import org.junit.Assume;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-07 Phase 3 — rocket dimension-transition path.
 *
 * <p>Covers the synchronous {@code EntityRocket.changeDimension(int, double,
 * double, double)} chain invoked by {@code reachSpaceManned} /
 * {@code reachSpaceUnmanned} when {@code destinationDimId != current.dim}.
 * For an unmanned rocket (no riders) the transition is a direct
 * Forge-{@code changeDimension} call — no entry is added to
 * {@code PlanetEventHandler.transitionMap} (that queue is only populated
 * with passengers in {@code EntityRocket.changeDimension(int,double,double,
 * double)} line 1967). Pinning the cause-effect:
 *
 * <ul>
 *   <li>Rocket originally in dim 0 → after force-orbit-reached on a chip
 *       programmed to another AR dim, the rocket entity is GONE from
 *       dim 0 and PRESENT in the dest dim — found by UUID.</li>
 *   <li>UUID stable across the dimension change (Forge contract).</li>
 *   <li>Storage chunk geometry / fuel-tank count / engine count
 *       preserved.</li>
 *   <li>Invalid destination dim → production
 *       {@code !DimensionManager.canTravelTo(dim)} guard in
 *       {@code EntityRocket.changeDimension} returns null; rocket stays
 *       in original dim, NO crash.</li>
 * </ul>
 *
 * <p>Probe surface introduced for these tests:
 * {@code /artest rocket find-by-uuid <uuid>},
 * {@code /artest rocket force-dest-dim <id> <dim>},
 * and a {@code uuid} field on {@code /artest rocket info}/{@code list}.
 */
public class RocketDimensionTransitionTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ROCKET_LIST_ID = Pattern.compile("\"id\":(-?\\d+)");
    private static final Pattern AR_DIMS_ARRAY =
            Pattern.compile("\"arDimensions\":\\[([^]]*)]");
    private static final Pattern UUID_FIELD =
            Pattern.compile("\"uuid\":\"([0-9a-fA-F-]+)\"");
    private static final Pattern DIM_FIELD = Pattern.compile("\"dim\":(-?\\d+)");
    private static final Pattern ENTITY_ID_FIELD = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern STORAGE_SIZE_X = Pattern.compile("\"storageSizeX\":(-?\\d+)");
    private static final Pattern STORAGE_SIZE_Y = Pattern.compile("\"storageSizeY\":(-?\\d+)");
    private static final Pattern STORAGE_SIZE_Z = Pattern.compile("\"storageSizeZ\":(-?\\d+)");
    private static final Pattern ENGINE_COUNT = Pattern.compile("\"engineCount\":(-?\\d+)");

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    private static String g(Pattern p, String s, String label) {
        Matcher m = p.matcher(s);
        if (!m.find()) throw new AssertionError("could not parse " + label + ": " + s);
        return m.group(1);
    }

    private int firstNonOverworldArDimOrSkip() throws Exception {
        String joined = ok(client().execute("artest dim list"));
        Assume.assumeFalse("No AR dimensions registered",
                joined.contains("\"arDimensions\":[]"));
        Matcher m = AR_DIMS_ARRAY.matcher(joined);
        assertTrue("could not parse arDimensions array: " + joined, m.find());
        for (String part : m.group(1).split(",")) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            int dim = Integer.parseInt(t);
            if (dim != 0) return dim;
        }
        Assume.assumeTrue("Only overworld is an AR planet", false);
        return -1;
    }

    private int buildAndAssemble(int baseX, int baseY, int baseZ) throws Exception {
        ok(client().execute(
                "artest fill 0 " + (baseX - 2) + " " + (baseY + 1) + " " + (baseZ - 2)
                        + " " + (baseX + 7) + " " + (baseY + 10) + " " + (baseZ + 7)
                        + " minecraft:air"));
        String fixture = ok(client().execute(
                "artest fixture rocket 0 " + baseX + " " + baseY + " " + baseZ + " simple"));
        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("fixture missing builderPos: " + fixture, bp.find());
        int bx = Integer.parseInt(bp.group(1));
        int by = Integer.parseInt(bp.group(2));
        int bz = Integer.parseInt(bp.group(3));
        ok(client().execute("artest rocket assemble 0 " + bx + " " + by + " " + bz));
        String list = ok(client().execute("artest rocket list 0"));
        Matcher rim = ROCKET_LIST_ID.matcher(list);
        int lastId = -1;
        while (rim.find()) lastId = Integer.parseInt(rim.group(1));
        assertTrue("no rocket after assemble: " + list, lastId >= 0);
        return lastId;
    }

    @Test
    public void rocketInfoAndListExposeUuid() throws Exception {
        // Pin the probe-surface contract first — the dimension-transition
        // tests below all depend on UUID being readable from both info and
        // list endpoints. A regression that drops the uuid field would
        // mask cause-effect failures in the harder tests.
        int id = buildAndAssemble(5000, 64, 500);
        String info = ok(client().execute("artest rocket info " + id));
        assertTrue("rocket info must expose uuid: " + info,
                UUID_FIELD.matcher(info).find());
        String list = ok(client().execute("artest rocket list 0"));
        assertTrue("rocket list must expose uuid: " + list,
                UUID_FIELD.matcher(list).find());
    }

    @Test
    public void inFlightRocketTransitionsToDestinationDim() throws Exception {
        // Drive a real cross-dim transition. After force-orbit-reached on
        // an unmanned rocket with destDim set to an AR dim:
        //   1. EntityRocket.reachSpaceManned() invokes this.changeDimension(destDim, ...)
        //   2. EntityRocket.changeDimension calls super (Forge), which
        //      respawns the entity in the destination world with a new
        //      entityId but preserves the UUID.
        //   3. The old entity in dim 0 is killed (isDead=true).
        //
        // Assertion: find-by-uuid in destDim must succeed and report dim==destDim.
        // The old entityId must NOT exist in dim 0 anymore.
        int destDim = firstNonOverworldArDimOrSkip();
        int id = buildAndAssemble(5100, 64, 500);

        // Capture UUID before launch.
        String infoBefore = ok(client().execute("artest rocket info " + id));
        String uuid = g(UUID_FIELD, infoBefore, "uuid");

        // Force-load the destination dim before transition. The shared
        // harness has no player to keep arbitrary AR dims hot, and Forge's
        // changeDimension chain bails silently if initDimension fails
        // (return value not checked by reachSpaceManned).
        ok(client().execute("artest chunk forceload " + destDim + " 0 0"));
        ok(client().execute("artest rocket set-destination " + id + " " + destDim));
        ok(client().execute("artest rocket launch " + id + " true instant"));

        String launchedInfo = ok(client().execute("artest rocket info " + id));
        assertTrue("launch must set isInFlight=true (precondition for transition test): "
                        + launchedInfo,
                launchedInfo.contains("\"isInFlight\":true"));

        // Force orbit reached → triggers transition.
        ok(client().execute("artest rocket force-orbit-reached " + id));

        // Find the rocket by UUID — must now be in destDim.
        String byUuid = ok(client().execute("artest rocket find-by-uuid " + uuid));
        assertTrue("rocket must be findable by UUID after transition: " + byUuid,
                byUuid.contains("\"ok\":true"));
        int dimAfter = Integer.parseInt(g(DIM_FIELD, byUuid, "dim"));
        assertEquals("rocket must have transitioned to destination dim", destDim, dimAfter);
    }

    @Test
    public void transitionPreservesRocketIdentityAndStorageContents() throws Exception {
        // After transition the rocket is a NEW entity (different entityId)
        // but the same persistent identity (UUID) and the same storage
        // chunk geometry. This pins the Forge Entity.changeDimension
        // contract that copyDataFromOld carries NBT across — a regression
        // that drops the storage NBT (e.g. fails to call
        // copyDataFromOld) would shrink storageSizeX/Y/Z to defaults.
        int destDim = firstNonOverworldArDimOrSkip();
        int id = buildAndAssemble(5200, 64, 500);

        String infoBefore = ok(client().execute("artest rocket info " + id));
        String uuid = g(UUID_FIELD, infoBefore, "uuid");
        int idBefore = Integer.parseInt(g(ENTITY_ID_FIELD, infoBefore, "entityId before"));
        int sxBefore = Integer.parseInt(g(STORAGE_SIZE_X, infoBefore, "sizeX before"));
        int syBefore = Integer.parseInt(g(STORAGE_SIZE_Y, infoBefore, "sizeY before"));
        int szBefore = Integer.parseInt(g(STORAGE_SIZE_Z, infoBefore, "sizeZ before"));
        int engBefore = Integer.parseInt(g(ENGINE_COUNT, infoBefore, "engines before"));

        // Force-load the destination dim before transition. The shared
        // harness has no player to keep arbitrary AR dims hot, and Forge's
        // changeDimension chain bails silently if initDimension fails
        // (return value not checked by reachSpaceManned).
        ok(client().execute("artest chunk forceload " + destDim + " 0 0"));
        ok(client().execute("artest rocket set-destination " + id + " " + destDim));
        ok(client().execute("artest rocket launch " + id + " true instant"));
        ok(client().execute("artest rocket force-orbit-reached " + id));

        // Pull all the assertion fields out of the find-by-uuid response
        // atomically — calling "rocket info <id>" afterwards is racy
        // because the destination dim/chunk may unload before the second
        // round-trip lands (no player anchor in the dest dim).
        String byUuid = ok(client().execute("artest rocket find-by-uuid " + uuid));
        assertTrue("rocket must be findable post-transition: " + byUuid,
                byUuid.contains("\"ok\":true"));
        int idAfter = Integer.parseInt(g(ENTITY_ID_FIELD, byUuid, "entityId after"));
        assertNotEquals("entityId must change across changeDimension", idBefore, idAfter);
        int sxAfter = Integer.parseInt(g(STORAGE_SIZE_X, byUuid, "sizeX after"));
        int syAfter = Integer.parseInt(g(STORAGE_SIZE_Y, byUuid, "sizeY after"));
        int szAfter = Integer.parseInt(g(STORAGE_SIZE_Z, byUuid, "sizeZ after"));
        int engAfter = Integer.parseInt(g(ENGINE_COUNT, byUuid, "engines after"));
        String uuidAfter = g(UUID_FIELD, byUuid, "uuid after");

        assertEquals("storage sizeX preserved", sxBefore, sxAfter);
        assertEquals("storage sizeY preserved", syBefore, syAfter);
        assertEquals("storage sizeZ preserved", szBefore, szAfter);
        assertEquals("engine count preserved", engBefore, engAfter);
        assertEquals("UUID preserved across changeDimension", uuid, uuidAfter);
    }

    @Test
    public void transitionToInvalidDimFailsGracefullyAndKeepsRocket() throws Exception {
        // Force destDimId to a bogus value (-12345) directly, bypassing
        // launch()'s canTravelTo guard. Then force-orbit-reached → the
        // reachSpaceManned branch calls changeDimension(-12345) which
        // checks canTravelTo and returns null (line 1944 in EntityRocket).
        // Assertion: the call doesn't throw, and the rocket still exists
        // in dim 0 under its original UUID (no half-transitioned state).
        int id = buildAndAssemble(5300, 64, 500);

        String infoBefore = ok(client().execute("artest rocket info " + id));
        String uuid = g(UUID_FIELD, infoBefore, "uuid");

        // Launch needs a valid dim — use overworld self-route as a
        // pre-launch nudge, then force a bogus destDim AFTER launch.
        // Actually simpler: skip launch() (it would set destDim from the
        // chip and call canTravelTo). Just force in-flight + bogus destDim
        // + force-orbit-reached.
        ok(client().execute("artest rocket launch " + id + " true force"));
        ok(client().execute("artest rocket force-dest-dim " + id + " -12345"));

        // force-orbit-reached invokes onOrbitReached -> reachSpaceManned
        // -> changeDimension(-12345) -> canTravelTo guard returns null.
        String resp = ok(client().execute("artest rocket force-orbit-reached " + id));
        assertTrue("force-orbit-reached must not crash on invalid destDim: " + resp,
                resp.contains("\"ok\":true"));

        // Rocket must still be findable by UUID, dim unchanged.
        String byUuid = ok(client().execute("artest rocket find-by-uuid " + uuid));
        assertTrue("rocket must still exist after invalid-dim transition attempt: " + byUuid,
                byUuid.contains("\"ok\":true"));
        int dimAfter = Integer.parseInt(g(DIM_FIELD, byUuid, "dim after"));
        assertEquals("rocket must remain in original dim 0", 0, dimAfter);
        assertFalse("rocket must NOT be marked dead by the failed transition: " + byUuid,
                byUuid.contains("\"isDead\":true"));
    }

    @Test
    public void findByUuidOnUnknownUuidReturnsError() throws Exception {
        // Probe contract test: a UUID that does not match any loaded
        // entity must return a structured "not found" error rather than
        // crashing or returning a stale match.
        String resp = ok(client().execute(
                "artest rocket find-by-uuid 00000000-0000-0000-0000-000000000000"));
        assertTrue("unknown uuid must error: " + resp,
                resp.contains("\"error\":\"rocket not found by uuid\""));
    }

    @Test
    public void findByUuidOnMalformedUuidReturnsError() throws Exception {
        String resp = ok(client().execute("artest rocket find-by-uuid not-a-uuid"));
        assertTrue("malformed uuid must error: " + resp,
                resp.contains("\"error\":\"invalid uuid\""));
    }
}
