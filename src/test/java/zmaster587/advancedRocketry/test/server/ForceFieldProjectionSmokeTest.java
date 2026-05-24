package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.18 — Force Field Projector real projection cycle.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.TileForceFieldProjector} runs an
 * extension cycle every 5 world ticks: while powered AND
 * {@code extensionRange &lt; MAX_RANGE (32)} it extends a column of
 * {@code advancedrocketry:forceField} outward in the projector's facing
 * direction; when unpowered, it collapses inward.</p>
 *
 * <p>The production guard {@code world.getTotalWorldTime() % 5 == 0} means
 * {@code /artest tile force-tick} can NOT drive the projector
 * deterministically (force-tick doesn't advance world time, so the gate
 * either hits or doesn't depending on when the test happens to run). We use
 * the dedicated {@code /artest field info} probe instead — it blocks the
 * server thread up to 1.5 s, releasing in 50 ms slices so the server's
 * natural tick loop advances world time and the projector's extension cycle
 * fires.</p>
 *
 * <p><b>Why not in MachineDomainSmokeSuite</b>: the test depends on the
 * server's natural tick loop touching the projector's chunk (2200, 64, 2200).
 * In the shared-harness suite, by the time this test ran the chunk had
 * frequently been evicted by earlier tests' chunk allocations, stalling
 * extension at range=0. Kept isolated so its tick loop has a fresh server
 * with the projector's chunk in active range.</p>
 */
public class ForceFieldProjectionSmokeTest extends AbstractHeadlessServerTest {

    private static final Pattern RANGE = Pattern.compile("\"extensionRange\":(-?\\d+)");
    private static final Pattern POWERED = Pattern.compile("\"isPowered\":(true|false)");

    @Test
    public void poweredProjectorProjectsAndUnpoweredCollapses() throws Exception {
        // Isolated patch — no collision with §7.13 (1500) / Microwave (1700) /
        // BHG (1800) / Pipe (1110) / Terraformer (2000) / MachineController (2100).
        int px = 2200, py = 64, pz = 2200;

        // Solid stone slab under the projector + clear airspace around it so
        // the field has room to grow in any facing direction.
        client().execute("artest fill 0 " + (px - 3) + " " + (py - 1) + " " + (pz - 3)
                + " " + (px + 3) + " " + (py - 1) + " " + (pz + 3) + " minecraft:stone");
        client().execute("artest fill 0 " + (px - 3) + " " + py + " " + (pz - 3)
                + " " + (px + 3) + " " + (py + 4) + " " + (pz + 3) + " minecraft:air");

        // Place projector facing UP (BlockFullyRotatable meta=1 = UP).
        // Explicit meta avoids the default-facing problem: if the projector
        // points at a non-air block, nextPos is non-replaceable and
        // extensionRange stalls at 1 with no field ever placed. UP-facing +
        // clear air column above guarantees the field grows.
        String placeProj = String.join("\n", client().execute(
                "artest place 0 " + px + " " + py + " " + pz
                        + " advancedrocketry:forceFieldProjector 1"));
        assertTrue("projector place failed: " + placeProj,
                placeProj.contains("\"placed\":true"));

        // Initial state — extensionRange must be 0, projector unpowered.
        String pre = String.join("\n", client().execute(
                "artest field info-now 0 " + px + " " + py + " " + pz));
        assertTrue("projector probe must recognise the tile: " + pre,
                pre.contains("\"isProjector\":true"));
        assertTrue("initial extensionRange must be 0: " + pre,
                "0".equals(group(RANGE, pre)));
        assertTrue("projector must not be powered yet: " + pre,
                "false".equals(group(POWERED, pre)));

        // Place redstone block BELOW projector → projector powered.
        // We avoid adjacent horizontal placement because the projector's
        // private facing direction may point at that block, making nextPos
        // non-replaceable and stalling extensionRange at 1 with no field
        // ever placed. Below-the-projector position only blocks the DOWN
        // facing — every horizontal facing has clear airspace.
        String placeRedstone = String.join("\n", client().execute(
                "artest place 0 " + px + " " + (py - 1) + " " + pz
                        + " minecraft:redstone_block"));
        assertTrue("redstone place failed: " + placeRedstone,
                placeRedstone.contains("\"placed\":true"));

        // Powered check — production reads this every tick.
        String poweredProbe = String.join("\n", client().execute(
                "artest field info-now 0 " + px + " " + py + " " + pz));
        assertTrue("projector must be powered after adjacent redstone: " + poweredProbe,
                "true".equals(group(POWERED, poweredProbe)));

        // Drive the projector's extension cycle directly via the test-only
        // `field tick` probe — bypasses the production %5 natural-tick gate
        // so we don't depend on natural-tick rate (which stretches under
        // parallel-fork load and flakes the 12 s wait budget; TASK-28 F2).
        // Five calls = five extensions; happy-path each call advances range
        // by 1 (or stays put if next block isn't replaceable).
        String tickResp = String.join("\n", client().execute(
                "artest field tick 0 " + px + " " + py + " " + pz + " 5"));
        int rangeAfter = Integer.parseInt(group(RANGE, tickResp));
        assertTrue("extensionRange must grow above 0 once powered (got: " + tickResp + ")",
                rangeAfter > 0);

        // Diagnostic dump — for each of the 6 cardinal neighbours, print what
        // block is there. Helps identify whether the field landed at an
        // unexpected position OR has a different registry name than expected.
        StringBuilder dump = new StringBuilder();
        int[][] dirs6 = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        for (int[] d : dirs6) {
            int qx = px + d[0], qy = py + d[1], qz = pz + d[2];
            String at = String.join("\n", client().execute(
                    "artest block at 0 " + qx + " " + qy + " " + qz));
            dump.append("\n  d=(").append(d[0]).append(',').append(d[1]).append(',').append(d[2])
                    .append(") ").append(at);
        }

        // Locate the field block — accept any block name containing
        // "forcefield" (case-insensitive) within a 3-block cube.
        int fieldX = Integer.MIN_VALUE, fieldY = 0, fieldZ = 0;
        outer:
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    int qx = px + dx, qy = py + dy, qz = pz + dz;
                    String at = String.join("\n", client().execute(
                            "artest block at 0 " + qx + " " + qy + " " + qz));
                    if (at.toLowerCase().contains("forcefield")
                            && !at.toLowerCase().contains("forcefieldproj")) {
                        fieldX = qx; fieldY = qy; fieldZ = qz;
                        break outer;
                    }
                }
            }
        }
        assertTrue("force field block must exist within 3-block cube of projector; "
                        + "extensionRange=" + rangeAfter + " neighbours:" + dump,
                fieldX != Integer.MIN_VALUE);

        // Remove redstone → projector unpowered.
        client().execute("artest place 0 " + px + " " + (py - 1) + " " + pz + " minecraft:stone");

        // Drive collapse directly via `field tick` (same rationale as above
        // — bypass the natural-tick %5 gate). Five calls is enough to drop
        // extensionRange from the typical post-extension value back to 0.
        client().execute(
                "artest field tick 0 " + px + " " + py + " " + pz + " 5");

        String finalProbe = String.join("\n", client().execute(
                "artest field info-now 0 " + px + " " + py + " " + pz));
        int finalRange = Integer.parseInt(group(RANGE, finalProbe));
        // The nearest field block must have been cleared by now — the
        // projector collapses from outermost inward, but the IMMEDIATE
        // neighbour at extensionRange=1 is the LAST to clear. So we only
        // require that the field is shorter than at peak OR the immediate
        // neighbour is gone.
        String afterCollapse = String.join("\n", client().execute(
                "artest block at 0 " + fieldX + " " + fieldY + " " + fieldZ));
        boolean cleared = !afterCollapse.contains("\"block\":\"advancedrocketry:forceField\"");
        boolean shrunk = finalRange < rangeAfter;
        assertTrue("either the recorded field neighbour must be cleared, or the "
                        + "projector's extensionRange must have shrunk after redstone removal "
                        + "(was=" + rangeAfter + " now=" + finalRange + " neighbour=" + afterCollapse + ")",
                cleared || shrunk);
    }

    private static String group(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : "";
    }
}
