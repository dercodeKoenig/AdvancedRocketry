package zmaster587.advancedRocketry.world.weather;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketChangeGameState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zmaster587.advancedRocketry.api.ARConfiguration;

import java.util.HashSet;
import java.util.Set;

/**
 * Central service that:
 * <ul>
 *   <li>holds the singleton {@link PlanetWeatherSavedData} (lazy-loaded from
 *       the overworld's {@link MapStorage}),</li>
 *   <li>decides which dimensions are eligible for the wrapper,</li>
 *   <li>installs / removes {@link ARWeatherWorldInfo} on a {@link WorldServer}
 *       via direct assignment to {@link World#worldInfo} (widened to public by
 *       AR's access transformer — see {@code META-INF/accessTransformer.cfg}),</li>
 *   <li>syncs weather to clients via vanilla {@link SPacketChangeGameState}
 *       packets.</li>
 * </ul>
 *
 * <p>Stateless across server restarts beyond the on-disk saved-data. The only
 * in-memory caches are {@code legacyMigrationDone} (to avoid scanning legacy
 * saved-data more than once per dim) and {@code unwrappedWarnedDims} (so the
 * weather-update warning fires at most once per dimension per run).</p>
 */
public final class PlanetWeatherManager {

    private static final Logger LOGGER = LogManager.getLogger("ARWeather");

    // Codes for SPacketChangeGameState. CAREFUL: the vanilla protocol
    // numbers don't match the human-readable names in the wiki. Decompile
    // NetHandlerPlayClient.handleChangeGameState to verify:
    //   code 1 → world.getWorldInfo().setRaining(true)  + world.setRainStrength(0)
    //   code 2 → world.getWorldInfo().setRaining(false) + world.setRainStrength(1)
    // So "send code 1 when rain is on" is correct, and "send code 2 when
    // rain is off". Earlier revisions of this manager had these constants
    // swapped (named per the wiki, not per actual client behaviour), which
    // produced inverted weather on every sync.
    private static final int STATE_BEGIN_RAINING = 1;
    private static final int STATE_END_RAINING = 2;
    private static final int STATE_RAIN_STRENGTH = 7;
    private static final int STATE_THUNDER_STRENGTH = 8;

    private static final Set<Integer> legacyMigrationDone = new HashSet<>();
    private static final Set<Integer> unwrappedWarnedDims = new HashSet<>();

    private PlanetWeatherManager() {
    }

    // ─── Saved-data lookup ────────────────────────────────────────────────

    /**
     * Look up (or create) the shared saved-data on the overworld's MapStorage.
     * Returns {@code null} if the overworld isn't loaded yet — callers must
     * treat that as "weather not available, try again later".
     */
    public static PlanetWeatherSavedData getSavedData(MinecraftServer server) {
        if (server == null) return null;
        WorldServer overworld = DimensionManager.getWorld(0);
        if (overworld == null) {
            overworld = server.getWorld(0);
        }
        if (overworld == null) return null;
        return getSavedData(overworld);
    }

    public static PlanetWeatherSavedData getSavedData(World world) {
        if (world == null) return null;
        MapStorage storage = world.getMapStorage();
        if (storage == null) return null;
        WorldSavedData existing = storage.getOrLoadData(PlanetWeatherSavedData.class,
                PlanetWeatherSavedData.STORAGE_KEY);
        if (existing instanceof PlanetWeatherSavedData) {
            return (PlanetWeatherSavedData) existing;
        }
        PlanetWeatherSavedData fresh = new PlanetWeatherSavedData();
        storage.setData(PlanetWeatherSavedData.STORAGE_KEY, fresh);
        return fresh;
    }

    public static PlanetWeatherState getOrCreate(WorldServer world) {
        PlanetWeatherSavedData saved = getSavedData(world);
        if (saved == null) return null;
        return saved.getOrCreate(world.provider.getDimension());
    }

    public static PlanetWeatherState getOrCreate(MinecraftServer server, int dimId) {
        PlanetWeatherSavedData saved = getSavedData(server);
        if (saved == null) return null;
        return saved.getOrCreate(dimId);
    }

    /** Mark the saved-data dirty so vanilla flushes it next save. */
    public static void markDirty(WorldServer world) {
        PlanetWeatherSavedData saved = getSavedData(world);
        if (saved != null) {
            saved.markDirty();
        }
    }

    // ─── Wrap policy ──────────────────────────────────────────────────────

    /**
     * Single source of truth for "should this world have per-dim weather".
     *
     * <p>Called from both the Mixin (constructor RETURN) and the
     * {@code WorldEvent.Load} fallback; safe to call either path first.</p>
     */
    public static boolean shouldWrap(WorldServer world) {
        ARConfiguration cfg = ARConfiguration.getCurrentConfig();
        if (cfg == null || !cfg.enableCustomPlanetWeather) return false;
        if (world == null || world.isRemote) return false;
        if (world.provider == null) return false;
        int dim = world.provider.getDimension();
        if (dim == 0) return false; // overworld: never touch
        if (dim == cfg.spaceDimId) return false; // space: not a planet
        if (world.getWorldInfo() instanceof ARWeatherWorldInfo) return false; // already wrapped

        if (cfg.forcePlanetWeatherWorldInfoWrapper) return true;

        // Primary AR-planet check via the AR DimensionManager (the in-Forge
        // DimensionManager only knows the dim is registered, not that it's a
        // planet). Use the type-system signal first (WorldProviderPlanet) and
        // fall back to the registry when the provider isn't installed yet.
        if (world.provider instanceof zmaster587.advancedRocketry.world.provider.WorldProviderPlanet) {
            return true;
        }
        return zmaster587.advancedRocketry.dimension.DimensionManager
                .getInstance()
                .isDimensionCreated(dim)
                && dim != cfg.spaceDimId;
    }

    /**
     * Idempotent + safe. Installs (or refreshes) {@link ARWeatherWorldInfo} on
     * the given world.
     *
     * <p>"Refresh" — if the world somehow gets a fresh {@link WorldInfo} after
     * we wrapped it once (some mods do that on world reload), we re-wrap and
     * the saved-data continues to back the same {@link PlanetWeatherState}.</p>
     */
    public static void wrapWorldInfoIfNeeded(WorldServer world) {
        if (!shouldWrap(world)) return;

        int dim = world.provider != null ? world.provider.getDimension() : Integer.MIN_VALUE;
        PlanetWeatherSavedData saved = getSavedData(world);
        if (saved == null) {
            // Overworld MapStorage not ready yet — fallback path will retry.
            return;
        }

        migrateLegacyIfNeeded(world, saved, dim);

        PlanetWeatherState state = saved.getOrCreate(dim);
        WorldInfo current = world.getWorldInfo();
        ARWeatherWorldInfo wrapped = new ARWeatherWorldInfo(current, state,
                () -> markDirty(world));

        world.worldInfo = wrapped;

        if (ARConfiguration.getCurrentConfig().logPlanetWeatherWrapping) {
            LOGGER.info("Wrapped WorldInfo for AR planet dim={} provider={}",
                    dim,
                    world.provider != null ? world.provider.getClass().getSimpleName() : "<null>");
        }
    }

    /** Reverse of {@link #wrapWorldInfoIfNeeded}. Used by tests / debug. */
    public static void unwrap(WorldServer world) {
        WorldInfo current = world.getWorldInfo();
        if (current instanceof ARWeatherWorldInfo) {
            ARWeatherWorldInfo wrapped = (ARWeatherWorldInfo) current;
            world.worldInfo = wrapped.getDelegate();
        }
    }

    // ─── Legacy migration ─────────────────────────────────────────────────

    /**
     * Pulls weather state from a pre-refactor {@code WorldInfoSavedData} (the
     * old per-world saved-data the deleted {@code CustomDerivedWorldInfo} used)
     * into our centralised per-dim store. Runs at most once per dimension per
     * server start; silently no-ops if the legacy file is absent.
     */
    private static void migrateLegacyIfNeeded(WorldServer world, PlanetWeatherSavedData target, int dim) {
        if (!legacyMigrationDone.add(dim)) return;

        try {
            // The old saved-data lived on the secondary world's perWorldStorage
            // (key "WorldInfoSavedData"). We don't depend on its class still
            // existing — read the NBT directly through MapStorage.
            MapStorage perWorld = world.getPerWorldStorage();
            if (perWorld == null) return;

            WorldSavedData legacy = perWorld.getOrLoadData(MigrationProbe.class, "WorldInfoSavedData");
            if (!(legacy instanceof MigrationProbe)) return;
            MigrationProbe probe = (MigrationProbe) legacy;
            if (probe.captured == null) return; // file existed but was empty

            PlanetWeatherState state = target.getOrCreate(dim);
            state.setCleanWeatherTime(probe.captured.getInteger("clearWeatherTime"));
            state.setRainTime(probe.captured.getInteger("rainTime"));
            state.setThunderTime(probe.captured.getInteger("thunderTime"));
            state.setRaining(probe.captured.getBoolean("raining"));
            state.setThundering(probe.captured.getBoolean("thundering"));
            target.markDirty();

            LOGGER.info("Migrated legacy WorldInfoSavedData -> PlanetWeatherSavedData for dim={}", dim);
        } catch (Throwable t) {
            // Never let migration crash world load — silently warn and move on.
            LOGGER.warn("Failed to migrate legacy weather for dim={}: {}", dim, t.toString());
        }
    }

    /**
     * Tiny WorldSavedData subclass used only to read a legacy NBT compound out
     * of perWorldStorage. We can't instantiate the deleted
     * {@code WorldInfoSavedData} class, so this is the substitute.
     */
    public static final class MigrationProbe extends WorldSavedData {
        public NBTTagCompound captured;

        public MigrationProbe(String name) {
            super(name);
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            this.captured = nbt;
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound compound) {
            // Never write through — we only consume the legacy file.
            return compound;
        }
    }

    // ─── Client sync ──────────────────────────────────────────────────────

    /**
     * Send the current weather state of the player's world to that player via
     * three vanilla {@link SPacketChangeGameState} packets. Safe to call
     * whenever the client may have stale state — login, dim change, respawn.
     */
    public static void syncToPlayer(EntityPlayerMP player) {
        if (player == null || player.world == null || player.world.isRemote) return;
        if (!(player.world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) player.world;
        WorldInfo info = ws.getWorldInfo();

        float rainStrength = ws.rainingStrength;
        float thunderStrength = ws.thunderingStrength;

        player.connection.sendPacket(new SPacketChangeGameState(
                info.isRaining() ? STATE_BEGIN_RAINING : STATE_END_RAINING, 0.0F));
        player.connection.sendPacket(new SPacketChangeGameState(
                STATE_RAIN_STRENGTH, rainStrength));
        player.connection.sendPacket(new SPacketChangeGameState(
                STATE_THUNDER_STRENGTH, thunderStrength));
    }

    public static void syncToPlayersInWorld(WorldServer world) {
        if (world == null || world.isRemote) return;
        for (Object p : world.playerEntities) {
            if (p instanceof EntityPlayerMP) {
                syncToPlayer((EntityPlayerMP) p);
            }
        }
    }

    // ─── Misc helpers ─────────────────────────────────────────────────────

    /**
     * Logs a warning at most once per dim if {@link WorldProviderPlanet#updateWeather}
     * is running against an unwrapped WorldInfo (i.e. our wrapper failed to
     * install). Distinct from the wrap-success log so it can be filtered.
     */
    public static void warnUnwrappedOnce(int dim) {
        if (unwrappedWarnedDims.add(dim)) {
            LOGGER.warn("Custom planet weather is enabled, but WorldInfo is not wrapped for "
                    + "dimension {}. Falling back to vanilla shared weather.", dim);
        }
    }
}
