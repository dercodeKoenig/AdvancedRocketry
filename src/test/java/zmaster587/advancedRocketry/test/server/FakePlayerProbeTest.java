package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-10 Phase 1 — FakePlayer probe surface smoke.
 *
 * <p>Pins the {@code /artest fakeplayer} probe contract before any
 * higher-level player-event test depends on it. A regression in
 * Forge's FakePlayerFactory API or in our probe's lifecycle map would
 * surface here rather than as a confusing failure in the consumer
 * tests.</p>
 */
public class FakePlayerProbeTest extends AbstractSharedServerTest {

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    @Test
    public void createPersistsInListAndExposesInfo() throws Exception {
        String created = ok(client().execute("artest fakeplayer create test_alpha"));
        assertTrue("create must succeed: " + created, created.contains("\"ok\":true"));
        assertTrue("create must echo the name: " + created,
                created.contains("\"name\":\"test_alpha\""));

        String list = ok(client().execute("artest fakeplayer list"));
        assertTrue("list must contain the created name: " + list,
                list.contains("\"test_alpha\""));

        String info = ok(client().execute("artest fakeplayer info test_alpha"));
        assertTrue("info must expose dim: " + info, info.contains("\"dim\":"));
        assertTrue("info must expose uuid: " + info, info.contains("\"uuid\":"));
        assertTrue("info must expose health: " + info, info.contains("\"health\":"));
        assertTrue("info must expose air: " + info, info.contains("\"air\":"));
        // FakePlayer is alive at creation.
        assertTrue("freshly created fake player must be alive: " + info,
                info.contains("\"isAlive\":true"));
    }

    @Test
    public void teleportMovesPlayerToTargetDimAndCoords() throws Exception {
        ok(client().execute("artest fakeplayer create test_traveller"));
        String tp = ok(client().execute(
                "artest fakeplayer teleport test_traveller 0 100 80 200"));
        assertTrue("teleport must succeed: " + tp, tp.contains("\"ok\":true"));
        assertTrue("teleport must echo target dim: " + tp,
                tp.contains("\"toDim\":0"));

        String info = ok(client().execute("artest fakeplayer info test_traveller"));
        assertTrue("post-teleport dim must be 0: " + info,
                info.contains("\"dim\":0"));
        // Positions are emitted as doubles in JSON ("posX":100.0).
        assertTrue("post-teleport posX must reflect: " + info,
                info.contains("\"posX\":100"));
        assertTrue("post-teleport posZ must reflect: " + info,
                info.contains("\"posZ\":200"));
    }

    @Test
    public void tickIsSafeOnFakePlayer() throws Exception {
        // FakePlayer.onUpdate() does NOT increment ticksExisted on its
        // own (Entity.ticksExisted advances via World.updateEntities;
        // direct onUpdate calls bypass it). Pin the weaker but valid
        // contract: 5 ticks via the probe complete without exception
        // and the probe reports ticked=5.
        ok(client().execute("artest fakeplayer create test_ticker"));
        String tickResp = ok(client().execute("artest fakeplayer tick test_ticker 5"));
        assertTrue("tick must succeed: " + tickResp,
                tickResp.contains("\"ok\":true") && tickResp.contains("\"ticked\":5"));
    }

    @Test
    public void destroyRemovesPlayerFromList() throws Exception {
        ok(client().execute("artest fakeplayer create test_doomed"));
        String list1 = ok(client().execute("artest fakeplayer list"));
        assertTrue("pre-destroy list contains test_doomed: " + list1,
                list1.contains("\"test_doomed\""));

        ok(client().execute("artest fakeplayer destroy test_doomed"));

        String list2 = ok(client().execute("artest fakeplayer list"));
        assertTrue("post-destroy list must NOT contain test_doomed: " + list2,
                !list2.contains("\"test_doomed\""));

        String info = ok(client().execute("artest fakeplayer info test_doomed"));
        assertTrue("info on destroyed player must error: " + info,
                info.contains("\"error\":\"fake player not found\""));
    }

    @Test
    public void teleportToUnknownPlayerReturnsError() throws Exception {
        String resp = ok(client().execute(
                "artest fakeplayer teleport ghost_player 0 0 64 0"));
        assertTrue("teleport on unknown name must error: " + resp,
                resp.contains("\"error\":\"fake player not found"));
    }

    @Test
    public void createIsIdempotentAcrossRepeatedCalls() throws Exception {
        // FakePlayerFactory caches per (world, GameProfile). Repeated
        // create calls with the same name must NOT spawn duplicate
        // entities — they return the cached instance with the same UUID.
        ok(client().execute("artest fakeplayer create test_idempotent"));
        String uuidFirst = ok(client().execute("artest fakeplayer info test_idempotent"));
        ok(client().execute("artest fakeplayer create test_idempotent"));
        String uuidSecond = ok(client().execute("artest fakeplayer info test_idempotent"));

        // Extract the uuid field; expect equal across the two calls.
        String u1 = extractField(uuidFirst, "uuid");
        String u2 = extractField(uuidSecond, "uuid");
        assertTrue("repeated create must yield the same UUID (was " + u1
                + ", now " + u2 + ")", u1.equals(u2));
    }

    private static String extractField(String json, String key) {
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("\"" + key + "\":\"([^\"]+)\"").matcher(json);
        if (!m.find()) throw new AssertionError(key + " field not found in: " + json);
        return m.group(1);
    }
}
