package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.18 — Hovercraft entity smoke (lifecycle only).
 *
 * <p>{@code EntityHoverCraft} is registered via
 * {@code EntityRegistry.registerModEntity(new ResourceLocation(modId, "ARHoverCraft"), ...)}
 * with the runtime registry name {@code advancedrocketry:ARHoverCraft}. SMART
 * §7.18 lists it as "hovercraft if feasible" — we cover the server-side
 * lifecycle: spawn → entity alive → tick → still alive (no NPE during the
 * physics update path).</p>
 *
 * <p>Real player-riding gameplay (mount, throttle, fuel burn, fan
 * orientation) requires a client harness with a real player — covered by the
 * deferred @Ignore client E2E tests, not here.</p>
 */
public class HovercraftEntitySmokeTest extends AbstractHeadlessServerTest {

    private static final Pattern ENTITY_ID = Pattern.compile("\"entityId\":(\\d+)");
    private static final Pattern POS_Y = Pattern.compile("\"posY\":(-?[\\d.]+)");

    @Test
    public void hovercraftSpawnsAndTicksWithoutCrash() throws Exception {
        int px = 2300, py = 80, pz = 2300;

        // Solid floor so the hovercraft falls onto stone, not into a cave.
        client().execute("artest fill 0 " + (px - 1) + " " + (py - 1) + " " + (pz - 1)
                + " " + (px + 1) + " " + (py - 1) + " " + (pz + 1) + " minecraft:stone");

        String spawn = String.join("\n", client().execute(
                "artest entity spawn 0 " + px + ".5 " + py + " " + pz + ".5"
                        + " advancedrocketry:ARHoverCraft"));
        assertTrue("hovercraft spawn failed: " + spawn,
                spawn.contains("\"ok\":true") && spawn.contains("\"spawned\":true"));

        Matcher m = ENTITY_ID.matcher(spawn);
        assertTrue("spawn response must carry entityId: " + spawn, m.find());
        int entityId = Integer.parseInt(m.group(1));

        // Verify entity registered and alive.
        String info1 = String.join("\n", client().execute(
                "artest entity info 0 " + entityId));
        assertTrue("entity must be alive immediately after spawn: " + info1,
                info1.contains("\"isAlive\":true"));
        assertTrue("entity class must be EntityHoverCraft: " + info1,
                info1.contains("EntityHoverCraft"));
        assertTrue("entity must NOT be dead-flagged after spawn: " + info1,
                info1.contains("\"isDead\":false"));

        // The hovercraft uses ITickable-equivalent World.tick path, not a tile
        // entity tick — we exercise stability by querying state across server
        // ticks. We can't force entity.onUpdate() directly via /artest tile
        // force-tick, but the server's own tick loop runs the entity update on
        // each /artest invocation indirectly (each command runs on the server
        // thread between game ticks; subsequent calls observe the post-tick
        // state). Spam a series of state queries to give the server's update
        // loop room to fire.
        for (int i = 0; i < 10; i++) {
            String poll = String.join("\n", client().execute(
                    "artest entity info 0 " + entityId));
            assertTrue("entity must stay alive across poll " + i + ": " + poll,
                    poll.contains("\"isAlive\":true"));
            assertTrue("entity must not crash with isDead=true: " + poll,
                    poll.contains("\"isDead\":false"));
        }

        // Confirm posY is within sane bounds (gravity / hover physics applied
        // without NaN / underflow).
        String finalInfo = String.join("\n", client().execute(
                "artest entity info 0 " + entityId));
        Matcher py2 = POS_Y.matcher(finalInfo);
        assertTrue("final posY must be readable: " + finalInfo, py2.find());
        double finalY = Double.parseDouble(py2.group(1));
        assertTrue("hovercraft must not fall below world floor (got " + finalY + ")",
                finalY > 0 && finalY < 256);
    }
}
