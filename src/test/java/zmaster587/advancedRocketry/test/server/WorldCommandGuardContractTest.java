package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * TASK-11 Phase 4 — non-player-sender guard contracts.
 *
 * <p>Several {@code /ar} subcommands operate on a player entity
 * (held-item mutations, per-player gravity, teleport). Their guards
 * for non-Entity senders are part of the surfaced contract — without
 * the guards, console invocation would crash. Each test fires the
 * subcommand from the harness console (which has
 * {@code getCommandSenderEntity() == null}) and pins the
 * production guard's chat envelope.</p>
 *
 * <p>Positive (player-equipped) variants of these subcommands belong
 * in testClient e2e — they need a real EntityPlayerMP on a connected
 * client. This class only pins the negative side, which is itself a
 * non-trivial contract: <em>"command does not crash when invoked
 * without a player"</em>.</p>
 */
public class WorldCommandGuardContractTest extends AbstractSharedServerTest {

    @Test
    public void addTorchRefusesConsoleSenderWithNotAPlayerEntityMessage() throws Exception {
        String resp = exec("ar addTorch");
        assertTrue("addTorch must refuse console with 'Not a player entity' — got: "
                + resp, resp.contains("Not a player entity"));
    }

    @Test
    public void addSolidBlockOverrideRefusesConsoleSenderWithNotAPlayerEntityMessage()
            throws Exception {
        String resp = exec("ar addSolidBlockOverride");
        assertTrue("addSolidBlockOverride must refuse console — got: " + resp,
                resp.contains("Not a player entity"));
    }

    @Test
    public void setGravityRefusesConsoleSenderWithNotAValidPlayerMessage()
            throws Exception {
        String resp = exec("ar setGravity 0.5");
        assertTrue("setGravity must refuse console — got: " + resp,
                resp.contains("Not a valid player"));
    }

    @Test
    public void fillDataRefusesConsoleSenderWithGhostsDontHaveItemsMessage()
            throws Exception {
        // fillData reaches the entity-null branch via the args.length >= 2
        // path; the help/length guards short-circuit otherwise.
        String resp = exec("ar fillData distance");
        assertTrue("fillData must refuse console — got: " + resp,
                resp.contains("Ghosts don't have items"));
    }

    @Test
    public void gotoRefusesConsoleSenderWithMustBeAPlayerMessage() throws Exception {
        String resp = exec("ar goto 0");
        assertTrue("goto must refuse console — got: " + resp,
                resp.contains("Must be a player to use this command"));
    }

    @Test
    public void fetchUnknownPlayerEmitsInvalidPlayerNameMessage() throws Exception {
        // fetch's null-target branch (line 359-361) runs before the
        // `me.world.provider` access that would NPE on console — pins
        // that we get the "Invalid player name" guard rather than a crash.
        String resp = exec("ar fetch nonExistentPlayerName123");
        assertTrue("fetch must report invalid player name — got: " + resp,
                resp.contains("Invalid player name"));
    }

    @Test
    public void giveStationWithUnknownPlayerNameEmitsNotFoundMessage() throws Exception {
        String resp = exec("ar giveStation 7 nonExistentPlayerName123");
        assertTrue("giveStation must report player not found — got: " + resp,
                resp.contains("not found"));
    }

    /** Top-level switch in {@code execute} has no default branch — an
     *  unknown subcommand silently no-ops. Pin that we do NOT fall
     *  through to the help envelope. Guards against a future refactor
     *  that adds a default case which prints help (and thereby spams
     *  console for every typo). */
    @Test
    public void unknownTopLevelSubcommandDoesNotEmitHelpEnvelope() throws Exception {
        String resp = exec("ar definitelyNotARealSubcommand");
        assertFalse("unknown sub must NOT echo the help header — got: " + resp,
                resp.contains("Subcommands:"));
    }
}
