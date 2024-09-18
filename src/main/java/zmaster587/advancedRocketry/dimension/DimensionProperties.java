package zmaster587.advancedRocketry.dimension;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.TempCategory;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.BiomeManager.BiomeEntry;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.SidedProxy;
import org.apache.commons.lang3.ArrayUtils;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.*;
import zmaster587.advancedRocketry.api.atmosphere.AtmosphereRegister;
import zmaster587.advancedRocketry.api.dimension.IDimensionProperties;
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.fuckin_bs_integrated_server_and_client_variable_sharing_crap_fix_fuckit_Im_in_rage.Afuckinginterface;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.advancedRocketry.network.PacketDimInfo;
import zmaster587.advancedRocketry.network.PacketSatellite;
import zmaster587.advancedRocketry.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.util.*;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;
import zmaster587.libVulpes.util.VulpineMath;
import zmaster587.libVulpes.util.ZUtils;

import java.util.*;
import java.util.Map.Entry;

import static org.apache.commons.lang3.RandomUtils.nextInt;


public class DimensionProperties implements Cloneable, IDimensionProperties {

    /**
     * Contains default graphic {@link ResourceLocation} to display for different planet types
     */
    public static final ResourceLocation atmosphere = new ResourceLocation("advancedrocketry:textures/planets/Atmosphere2.png");
    public static final ResourceLocation atmosphereLEO = new ResourceLocation("advancedrocketry:textures/planets/AtmosphereLEO.png");
    public static final ResourceLocation atmGlow = new ResourceLocation("advancedrocketry:textures/planets/atmGlow.png");
    public static final ResourceLocation planetRings = new ResourceLocation("advancedrocketry:textures/planets/rings.png");
    public static final ResourceLocation planetRingsNew = new ResourceLocation("advancedrocketry:textures/planets/ringsnew.png");
    public static final ResourceLocation planetRingShadow = new ResourceLocation("advancedrocketry:textures/planets/ringShadow.png");
    public static final ResourceLocation shadow = new ResourceLocation("advancedrocketry:textures/planets/shadow.png");
    public static final ResourceLocation shadow3 = new ResourceLocation("advancedrocketry:textures/planets/shadow3.png");
    public static final int MAX_ATM_PRESSURE = 1600;
    public static final int MIN_ATM_PRESSURE = 0;
    public static final int MAX_DISTANCE = Integer.MAX_VALUE;
    public static final int MIN_DISTANCE = 1;
    public static final int MAX_GRAVITY = 400;
    public static final int MIN_GRAVITY = 0;
    //True if dimension is managed and created by AR (false otherwise)
    public boolean isNativeDimension;
    public boolean skyRenderOverride;
    //Gas giants DO NOT need a dimension registered to them
    public float[] skyColor;
    public float[] fogColor;
    public float[] ringColor;
    public float gravitationalMultiplier;
    public int orbitalDist;
    public boolean hasOxygen;
    public boolean colorOverride;
    //Used in solar panels
    public double peakInsolationMultiplier;
    public double peakInsolationMultiplierWithoutAtmosphere;
    //Stored in Kelvin
    public int averageTemperature;
    public int rotationalPeriod;
    //Stored in radians
    public double orbitTheta;
    public double baseOrbitTheta;
    public double prevOrbitalTheta;
    public double orbitalPhi;
    public double rotationalPhi;
    public boolean isRetrograde;
    public OreGenProperties oreProperties = null;
    public List<ItemStack> laserDrillOres;
    public List<String> geodeOres;
    public List<String> craterOres;
    // The parsing of laserOreDrills is destructive of the actual oredict entries, so we keep a copy of the raw data around for XML writing
    public String laserDrillOresRaw;
    public String customIcon;
    public float[] sunriseSunsetColors;
    public boolean hasRings;
    public int ringAngle;
    public boolean hasRivers;
    public List<ItemStack> requiredArtifacts;
    IAtmosphere atmosphereType;
    StellarBody star;
    int starId;
    private int originalAtmosphereDensity;
    private int atmosphereDensity;
    private String name;
    //public ExtendedBiomeProperties biomeProperties;
    private LinkedList<BiomeEntry> allowedBiomes;
    private LinkedList<BiomeEntry> craterBiomeWeights;
    private boolean isRegistered = false;
    //private boolean isTerraformed = false;
    //Planet Heirachy
    private HashSet<Integer> childPlanets;
    private int parentPlanet;
    private int planetId;
    private boolean isStation;
    private boolean isGasGiant;
    private boolean canGenerateCraters;
    private boolean canGenerateGeodes;
    private boolean canGenerateVolcanoes;
    private boolean canGenerateStructures;
    private boolean canGenerateCaves;
    private boolean canDecorate; //Should the button draw shadows, etc.  Clientside
    private boolean overrideDecoration;
    private float craterFrequencyMultiplier;
    private float volcanoFrequencyMultiplier;
    private float geodeFrequencyMultiplier;
    //Satellites
    private HashMap<Long, SatelliteBase> satellites;
    private HashMap<Long, SatelliteBase> tickingSatellites;
    private List<Fluid> harvestableAtmosphere;
    private List<SpawnListEntryNBT> spawnableEntities;
    private HashSet<HashedBlockPosition> beaconLocations;
    private IBlockState oceanBlock;
    private IBlockState fillerBlock;
    private int seaLevel;
    private int generatorType;
    //public int target_sea_level;




    @SidedProxy(serverSide = "zmaster587.advancedRocketry.fuckin_bs_integrated_server_and_client_variable_sharing_crap_fix_fuckit_Im_in_rage.serverlists", clientSide = "zmaster587.advancedRocketry.fuckin_bs_integrated_server_and_client_variable_sharing_crap_fix_fuckit_Im_in_rage.clientlists")
    public static Afuckinginterface proxylists;



    public List<ChunkPos> terraformingChunksAlreadyAdded;

    //class


    public List<watersourcelocked> water_source_locked_positions;

    //public boolean water_can_exist;
    public DimensionProperties(int id) {
        name = "Temp";
        resetProperties();

        planetId = id;
        parentPlanet = Constants.INVALID_PLANET;
        childPlanets = new HashSet<>();
        orbitalPhi = 0;
        isRetrograde = false;
        ringColor = new float[]{.4f, .4f, .7f};
        oceanBlock = null;
        fillerBlock = null;

        laserDrillOres = new ArrayList<>();
        geodeOres = new ArrayList<>();
        craterOres = new ArrayList<>();

        allowedBiomes = new LinkedList<>();
        craterBiomeWeights = new LinkedList<>();
        satellites = new HashMap<>();
        requiredArtifacts = new LinkedList<>();
        tickingSatellites = new HashMap<>();
        isNativeDimension = true;
        skyRenderOverride = false;
        hasOxygen = true;
        colorOverride = false;
        peakInsolationMultiplier = -1;
        peakInsolationMultiplierWithoutAtmosphere = -1;
        isGasGiant = false;
        hasRings = false;
        canGenerateCraters = true;
        canGenerateGeodes = true;
        canGenerateStructures = true;
        canGenerateVolcanoes = false;
        canGenerateCaves = true;
        hasRivers = true;
        craterFrequencyMultiplier = 1f;
        volcanoFrequencyMultiplier = 1f;
        geodeFrequencyMultiplier = 1f;
        canDecorate = true;

        customIcon = "";
        harvestableAtmosphere = new LinkedList<>();
        spawnableEntities = new LinkedList<>();
        beaconLocations = new HashSet<>();
        seaLevel = 63;
        generatorType = 0;

        //target_sea_level = seaLevel;
        //water_can_exist = true;
        water_source_locked_positions = new ArrayList<>();

        terraformingChunksAlreadyAdded = new ArrayList<>();

        ringAngle = 70;


        //dont need this here because the terraforming terminal will re-create it anyway
        //this.chunkMgrTerraformed = new ChunkManagerPlanet(net.minecraftforge.common.DimensionManager.getWorld(id), net.minecraftforge.common.DimensionManager.getWorld(getId()).getWorldInfo().getGeneratorOptions(), getTerraformedBiomes());
    }

    public void load_terraforming_helper(boolean reset) {
        if (!net.minecraftforge.common.DimensionManager.getWorld(getId()).isRemote) {

            if (!proxylists.isinitialized(getId())){
                proxylists.initdim(getId());
            }

            getAverageTemp();
            getViableBiomes(false);
            if (reset) {
                proxylists.getChunksFullyTerraformed(getId()).clear();
                proxylists.getChunksFullyBiomeChanged(getId()).clear();
                terraformingChunksAlreadyAdded.clear();
            }

            System.out.println("load helper with protecting blocks: " + proxylists.getProtectingBlocksForDimension(getId()).size() + " (" + reset + ")");

            proxylists.sethelper(getId(), new TerraformingHelper(getId(), getBiomesEntries(getViableBiomes(false)), proxylists.getChunksFullyTerraformed(getId()), proxylists.getChunksFullyBiomeChanged(getId())));

            System.out.println("num biomes: "+ getViableBiomes(false).size());

            Collection<Chunk> list = (net.minecraftforge.common.DimensionManager.getWorld(getId())).getChunkProvider().getLoadedChunks();
            System.out.println("add chunks to tf list");
            if (!list.isEmpty()) {
                for (Chunk chunk : list) {
                    add_chunk_to_terraforming_list(chunk);
                }
            }
            System.out.println("ok!");
        }

    }

    public void registerProtectingBlock(BlockPos p) {
        boolean already_registered = false;
        for (BlockPos i : proxylists.getProtectingBlocksForDimension(getId())) {
            if (i.equals(p)) {
                already_registered = true;
                break;
            }
        }
        //System.out.println("register protecting block called");
        if (!already_registered) {
            proxylists.getProtectingBlocksForDimension(getId()).add(p);
            //System.out.println("block registered");
            if (proxylists.gethelper(getId()) != null) {
                proxylists.gethelper(getId()).recalculate_chunk_status();
            }
        }
    }

    public void unregisterProtectingBlock(BlockPos p) {
        for (BlockPos i : proxylists.getProtectingBlocksForDimension(getId())) {
            if (i.equals(p)) {
                proxylists.getProtectingBlocksForDimension(getId()).remove(i);
                if (proxylists.gethelper(getId()) != null)
                    proxylists.gethelper(getId()).recalculate_chunk_status();
                break;
            }
        }
    }

    public void add_block_to_terraforming_queue(BlockPos p) {
        proxylists.gethelper(getId()).add_position_to_queue(p);
    }
    public void add_chunk_to_terraforming_list_but_this_time_real_terraforming_and_not_biomechanging(ChunkPos pos){
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                    add_block_to_terraforming_queue(new BlockPos(pos.x * 16 + x, 0, pos.z * 16 + z));
            }
        }
    }

    public void add_block_to_biomechanging_queue(BlockPos p) {
        proxylists.gethelper(getId()).add_position_to_biomechanging_queue(p);
    }

    synchronized boolean chunk_was_added_to_terraforming_list_if_not_add_it(ChunkPos pos){
        for (ChunkPos i : terraformingChunksAlreadyAdded) {
            if (pos.x == i.x && pos.z == i.z) {
                return true;
            }
        }
        terraformingChunksAlreadyAdded.add(new ChunkPos(pos.x,pos.z));
        return false;
    }

    //adds a chunk to the terraforming list
    //adds it to be biomechanged by default
    //if it already was biomechanged fully, add it directly to terraforming queue
    public void add_chunk_to_terraforming_list(Chunk chunk) {

        if (proxylists.gethelper(getId()) != null) {

            boolean chunk_was_already_done = proxylists.getChunksFullyTerraformed(getId()).contains(new ChunkPos(chunk.x,chunk.z));; // do not add a chunk if it is already fully terraformed
            if (chunk_was_already_done)
                return;

            //System.out.println("add chunk to terraforming list: "+chunk.x+":"+chunk.z);

            chunkdata current_chunk = proxylists.gethelper(getId()).getChunkFromList(chunk.x, chunk.z);
            if (current_chunk == null || !current_chunk.chunk_fully_biomechanged) {

                if(chunk_was_added_to_terraforming_list_if_not_add_it(new ChunkPos(chunk.x,chunk.z)))
                    return;



                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if (current_chunk == null || !current_chunk.fully_generated[x][z])
                            // if a position in the chunk is already fully generated, skip
                            add_block_to_biomechanging_queue(new BlockPos(chunk.x * 16 + x, 0, chunk.z * 16 + z));

                    }
                }
            }else  if (!current_chunk.chunk_fully_generated) {
                if(chunk_was_added_to_terraforming_list_if_not_add_it(new ChunkPos(chunk.x,chunk.z)))
                    return;

                add_chunk_to_terraforming_list_but_this_time_real_terraforming_and_not_biomechanging(new ChunkPos(chunk.x,chunk.z));
            }
        }
    }

    public DimensionProperties(int id, String name) {
        this(id);
        this.name = name;
    }

    public DimensionProperties(int id, boolean shouldRegister) {
        this(id);
        isStation = !shouldRegister;
    }

    /**
     * @return {@link ResourceLocation} refering to the image to render as atmospheric haze as seen from orbit
     */
    public static ResourceLocation getAtmosphereResource() {
        return atmosphere;
    }

    public static ResourceLocation getShadowResource() {
        return shadow;
    }

    public static ResourceLocation getAtmosphereLEOResource() {
        return atmosphereLEO;
    }

    public static DimensionProperties createFromNBT(int id, NBTTagCompound nbt) {
        DimensionProperties properties = new DimensionProperties(id);
        properties.readFromNBT(nbt);
        properties.planetId = id;

        return properties;
    }

    public void copyData(DimensionProperties props) {
        this.satellites = props.satellites;
        this.tickingSatellites = props.tickingSatellites;
    }


    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /**
     * @param world
     * @return null to use default world gen properties, otherwise a list of ores to generate
     */
    public OreGenProperties getOreGenProperties(World world) {
        if (oreProperties != null)
            return oreProperties;
        return OreGenProperties.getOresForPressure(AtmosphereTypes.getAtmosphereTypeFromValue(originalAtmosphereDensity), Temps.getTempFromValue(getAverageTemp()));
    }

    /**
     * Resets all properties to default
     */
    public void resetProperties() {
        fogColor = new float[]{1f, 1f, 1f};
        skyColor = new float[]{1f, 1f, 1f};
        sunriseSunsetColors = new float[]{.7f, .2f, .2f, 1};
        ringColor = new float[]{.4f, .4f, .7f};
        gravitationalMultiplier = 1;
        rotationalPeriod = 24000;
        orbitalDist = 100;
        originalAtmosphereDensity = atmosphereDensity = 100;
        childPlanets = new HashSet<>();
        requiredArtifacts = new LinkedList<>();
        parentPlanet = Constants.INVALID_PLANET;
        starId = 0;
        averageTemperature = 100;
        hasRings = false;
        harvestableAtmosphere = new LinkedList<>();
        spawnableEntities = new LinkedList<>();
        beaconLocations = new HashSet<>();
        seaLevel = 63;
        oceanBlock = null;
        fillerBlock = null;
        generatorType = 0;
        laserDrillOres = new ArrayList<>();
    }

    public List<Fluid> getHarvestableGasses() {
        return harvestableAtmosphere;
    }

    public List<ItemStack> getRequiredArtifacts() {
        return requiredArtifacts;
    }

    @Override
    public float getGravitationalMultiplier() {
        return gravitationalMultiplier;
    }

    @Override
    public void setGravitationalMultiplier(float mult) {
        gravitationalMultiplier = mult;
    }

    public List<SpawnListEntryNBT> getSpawnListEntries() {
        return spawnableEntities;
    }

    /**
     * @return the color of the sun as an array of floats represented as  {r,g,b}
     */
    public float[] getSunColor() {
        return getStar().getColor();
    }

    /**
     * @return the host star for this planet
     */
    public StellarBody getStar() {
        if (isStar()) {
            star = getStarData();
        }
        if (star == null)
            star = DimensionManager.getInstance().getStar(starId);
        return star;
    }

    public boolean hasSurface() {
        return !(isGasGiant() || isStar());
    }

    public boolean isGasGiant() {
        return isGasGiant;
    }

    public void setGasGiant(boolean gas) {
        this.isGasGiant = gas;
    }

    public boolean isStar() {
        return planetId >= Constants.STAR_ID_OFFSET;
    }

    /**
     * Sets the host star for the planet
     *
     * @param star the star to set as the host for this planet
     */
    public void setStar(StellarBody star) {
        this.starId = star.getId();
        this.star = star;
        if (!this.isMoon() && !isStation())
            this.star.addPlanet(this);
    }

    public void setStar(int id) {
        this.starId = id;
        if (DimensionManager.getInstance().getStar(id) != null)
            setStar(DimensionManager.getInstance().getStar(id));
    }

    public StellarBody getStarData() {
        return DimensionManager.getInstance().getStar(planetId - Constants.STAR_ID_OFFSET);
    }

    public boolean hasRings() {
        return this.hasRings;
    }

    public void setHasRings(boolean value) {
        this.hasRings = value;
    }

    //Adds a beacon location to the planet's surface
    public void addBeaconLocation(World world, HashedBlockPosition pos) {
        beaconLocations.add(pos);
        DimensionManager.getInstance().knownPlanets.add(getId());

        //LAAZZY
        if (!world.isRemote)
            PacketHandler.sendToAll(new PacketDimInfo(getId(), this));
    }

    public HashSet<HashedBlockPosition> getBeacons() {
        return beaconLocations;
    }

    //Removes a beacon location to the planet's surface
    public void removeBeaconLocation(World world, HashedBlockPosition pos) {
        beaconLocations.remove(pos);

        if (beaconLocations.isEmpty() && !ARConfiguration.getCurrentConfig().initiallyKnownPlanets.contains(getId()))
            DimensionManager.getInstance().knownPlanets.remove(getId());

        //LAAZZY
        if (!world.isRemote)
            PacketHandler.sendToAll(new PacketDimInfo(getId(), this));
    }

    /**
     * @return the {@link ResourceLocation} representing this planet, generated from the planet's properties
     */
    public ResourceLocation getPlanetIcon() {


        if (!customIcon.isEmpty()) {
            try {
                String resource_location = "advancedrocketry:textures/planets/" + customIcon.toLowerCase() + ".png";
                if (TextureResources.planetResources.containsKey(resource_location))
                    return TextureResources.planetResources.get(resource_location);

                ResourceLocation new_resource = new ResourceLocation(resource_location);
                TextureResources.planetResources.put(resource_location, new_resource);
                return new_resource;
            } catch (IllegalArgumentException e) {
                return PlanetIcons.UNKNOWN.resource;
            }

        }

        AtmosphereTypes atmType = AtmosphereTypes.getAtmosphereTypeFromValue(atmosphereDensity);
        Temps tempType = Temps.getTempFromValue(getAverageTemp());

        if (isStar() && getStarData().isBlackHole())
            return TextureResources.locationBlackHole_icon;

        if (isGasGiant())
            return PlanetIcons.GASGIANTBLUE.resource;

        if (isAsteroid())
            return PlanetIcons.ASTEROID.resource;

        if (tempType == Temps.TOOHOT)
            return PlanetIcons.MARSLIKE.resource;
        if (atmType != AtmosphereTypes.NONE && VulpineMath.isBetween(tempType.ordinal(), Temps.COLD.ordinal(), Temps.TOOHOT.ordinal()))
            return PlanetIcons.EARTHLIKE.resource;//TODO: humidity
        else if (tempType.compareTo(Temps.COLD) > 0)
            if (atmType.compareTo(AtmosphereTypes.LOW) > 0)
                return PlanetIcons.MOON.resource;
            else
                return PlanetIcons.ICEWORLD.resource;
        else if (atmType.compareTo(AtmosphereTypes.LOW) > 0) {

            if (tempType.compareTo(Temps.COLD) < 0)
                return PlanetIcons.MARSLIKE.resource;
            else
                return PlanetIcons.MOON.resource;
        } else
            return PlanetIcons.LAVA.resource;
    }

    /**
     * @return the {@link ResourceLocation} representing this planet, generated from the planet's properties
     */
    public ResourceLocation getPlanetIconLEO() {

        if (!customIcon.isEmpty()) {
            try {
                String resource_location = "advancedrocketry:textures/planets/" + customIcon.toLowerCase() + "leo.jpg";
                if (TextureResources.planetResources.containsKey(resource_location))
                    return TextureResources.planetResources.get(resource_location);

                ResourceLocation new_resource = new ResourceLocation(resource_location);
                TextureResources.planetResources.put(resource_location, new_resource);
                return new_resource;

            } catch (IllegalArgumentException e) {
                return PlanetIcons.UNKNOWN.resource;
            }
        }

        AtmosphereTypes atmType = AtmosphereTypes.getAtmosphereTypeFromValue(atmosphereDensity);
        Temps tempType = Temps.getTempFromValue(getAverageTemp());


        if (isGasGiant())
            return PlanetIcons.GASGIANTBLUE.resourceLEO;

        if (tempType == Temps.TOOHOT)
            return PlanetIcons.MARSLIKE.resourceLEO;
        if (atmType != AtmosphereTypes.NONE && VulpineMath.isBetween(tempType.ordinal(), Temps.COLD.ordinal(), Temps.TOOHOT.ordinal()))
            return PlanetIcons.EARTHLIKE.resourceLEO;//TODO: humidity
        else if (tempType.compareTo(Temps.COLD) > 0)
            if (atmType.compareTo(AtmosphereTypes.LOW) > 0)
                return PlanetIcons.MOON.resourceLEO;
            else
                return PlanetIcons.ICEWORLD.resourceLEO;
        else if (atmType.compareTo(AtmosphereTypes.LOW) > 0) {

            if (tempType.compareTo(Temps.COLD) < 0)
                return PlanetIcons.MARSLIKE.resourceLEO;
            else
                return PlanetIcons.MOON.resourceLEO;
        } else
            return PlanetIcons.LAVA.resourceLEO;
    }

    /**
     * @return the name of the planet
     */
    public String getName() {
        return name;
    }

    //Planet hierarchy

    /**
     * Sets the name of the planet
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the DIMID of the planet
     */
    public int getId() {
        return planetId;
    }

    /**
     * Sets the planet's id
     *
     * @param id
     */
    public void setId(int id) {
        this.planetId = id;
    }

    /**
     * @return the DimID of the parent planet
     */
    public int getParentPlanet() {
        return parentPlanet;
    }

    /**
     * Sets this planet as a moon of the supplied planet's id.
     *
     * @param parent parent planet's DimensionProperties, or null for none
     */
    public void setParentPlanet(DimensionProperties parent) {
        this.setParentPlanet(parent, true);
    }

    /**
     * @return the {@link DimensionProperties} of the parent planet
     */
    public DimensionProperties getParentProperties() {
        if (parentPlanet != Constants.INVALID_PLANET)
            return DimensionManager.getInstance().getDimensionProperties(parentPlanet);
        return null;
    }

    /**
     * Range 0 < value <= 200
     *
     * @return if the planet is a moon, then the distance from the host planet where the earth's moon is 100, higher is farther, if planet, distance from the star, 100 is earthlike, higher value is father
     */
    public int getParentOrbitalDistance() {
        return orbitalDist;
    }

    @Override
    public void setParentOrbitalDistance(int distance) {
        this.orbitalDist = distance;

    }

    /**
     * @return if a planet, the same as getParentOrbitalDistance(), if a moon, the moon's distance from the host star
     */
    public int getSolarOrbitalDistance() {
        if (this.isStar()) {
            return 1;
        }
        if (parentPlanet != Constants.INVALID_PLANET)
            return getParentProperties().getSolarOrbitalDistance();
        return orbitalDist;

    }

    public double getSolarTheta() {
        if (parentPlanet != Constants.INVALID_PLANET)
            return getParentProperties().getSolarTheta();
        return orbitTheta;
    }

    /**
     * Sets this planet as a moon of the supplied planet's ID
     *
     * @param parent DimensionProperties of the parent planet, or null for none
     * @param update true to update the parent's planet to the change
     */
    public void setParentPlanet(DimensionProperties parent, boolean update) {

        if (update) {
            if (parentPlanet != Constants.INVALID_PLANET)
                getParentProperties().childPlanets.remove(getId());

            if (parent == null) {
                parentPlanet = Constants.INVALID_PLANET;
            } else {
                parentPlanet = parent.getId();
                star = parent.getStar();
                if (parent.getId() != Constants.INVALID_PLANET)
                    parent.childPlanets.add(getId());
            }
        } else {
            if (parent == null) {
                parentPlanet = Constants.INVALID_PLANET;
            } else {
                star = parent.getStar();
                starId = star.getId();
                parentPlanet = parent.getId();
            }
        }
    }

    /**
     * @return true if the planet has moons
     */
    public boolean hasChildren() {
        return !childPlanets.isEmpty();
    }

    /**
     * @return true if this DIM orbits another
     */
    public boolean isMoon() {
        return parentPlanet != Constants.INVALID_PLANET && parentPlanet != SpaceObjectManager.WARPDIMID;
    }


    public int getAtmosphereDensity() {
        return atmosphereDensity;
    }

    //TODO: allow for more exotic atmospheres

    public void setAtmosphereDensity(int atmosphereDensity) {

        int prevAtm = this.atmosphereDensity;
        this.atmosphereDensity = atmosphereDensity;

        load_terraforming_helper(true);


        PacketHandler.sendToAll(new PacketDimInfo(getId(), this));
    }

    public void setAtmosphereDensityDirect(int atmosphereDensity) {
        originalAtmosphereDensity = this.atmosphereDensity = atmosphereDensity;
    }

    /**
     * @return true if the dimension properties refer to that of a space station or orbiting object registered in {@link SpaceObjectManager}
     */
    public boolean isStation() {
        return isStation;
    }

    /**
     * @return the default atmosphere of this dimension
     */
    public IAtmosphere getAtmosphere() {
        if (hasAtmosphere() && hasOxygen) {
            if (averageTemperature >= 900)
                return AtmosphereType.SUPERHEATED;
            if (Temps.getTempFromValue(getAverageTemp()) == Temps.TOOHOT)
                return AtmosphereType.VERYHOT;
            if (AtmosphereTypes.getAtmosphereTypeFromValue(getAtmosphereDensity()) == AtmosphereTypes.SUPERHIGHPRESSURE)
                return AtmosphereType.SUPERHIGHPRESSURE;
            if (AtmosphereTypes.getAtmosphereTypeFromValue(getAtmosphereDensity()) == AtmosphereTypes.HIGHPRESSURE)
                return AtmosphereType.HIGHPRESSURE;
            if (AtmosphereTypes.getAtmosphereTypeFromValue(getAtmosphereDensity()) == AtmosphereTypes.LOW)
                return AtmosphereType.LOWOXYGEN;
            return AtmosphereType.AIR;
        } else if (hasAtmosphere() && !hasOxygen) {
            if (averageTemperature >= 900)
                return AtmosphereType.SUPERHEATEDNOO2;
            if (Temps.getTempFromValue(averageTemperature) == Temps.TOOHOT)
                return AtmosphereType.VERYHOTNOO2;
            if (AtmosphereTypes.getAtmosphereTypeFromValue(getAtmosphereDensity()) == AtmosphereTypes.SUPERHIGHPRESSURE)
                return AtmosphereType.SUPERHIGHPRESSURENOO2;
            if (AtmosphereTypes.getAtmosphereTypeFromValue(getAtmosphereDensity()) == AtmosphereTypes.HIGHPRESSURE)
                return AtmosphereType.HIGHPRESSURENOO2;
            return AtmosphereType.NOO2;
        }
        return AtmosphereType.VACUUM;
    }

    /**
     * @return true if the planet has an atmosphere
     */
    public boolean hasAtmosphere() {
        return AtmosphereTypes.getAtmosphereTypeFromValue(atmosphereDensity).compareTo(AtmosphereTypes.NONE) < 0;
    }

    /**
     * @return the multiplier compared to Earth(1040W) for peak insolation of the body
     */
    public double getPeakInsolationMultiplier() {
        //Set peak insolation multiplier --  we do this here because I've had problems with it in the past in the XML loader, and people keep asking to change it
        //Assumes that a 16 atmosphere is 16x the partial pressure but not thicker, because I don't want to deal with that and this is fairly simple right now
        //Get what it would be relative to LEO, this gives ~0.76 for Earth at the surface
        double insolationRelativeToLEO = AstronomicalBodyHelper.getStellarBrightness(getStar(), getSolarOrbitalDistance()) * Math.pow(Math.E, -(0.0026899d * getAtmosphereDensity()));
        //Multiply by Earth LEO/Earth Surface for ratio relative to Earth surface (1360/1040)
        peakInsolationMultiplier = insolationRelativeToLEO * 1.308d;
        return peakInsolationMultiplier;
    }

    /**
     * @return the multiplier compared to Earth(1040W) for peak insolation of the body, ignoring the atmosphere
     */
    public double getPeakInsolationMultiplierWithoutAtmosphere() {
        //Set peak insolation multiplier without atmosphere --  we do this here because I've had problems with it in the past in the XML loader, and people keep asking to change it
        peakInsolationMultiplierWithoutAtmosphere = AstronomicalBodyHelper.getStellarBrightness(getStar(), getSolarOrbitalDistance()) * 1.308d;
        return peakInsolationMultiplierWithoutAtmosphere;
    }


    public boolean isAsteroid() {
        return generatorType == Constants.GENTYPE_ASTEROID;
    }

    /**
     * @return true if the planet should be rendered with shadows, atmosphere glow, clouds, etc
     */
    public boolean hasDecorators() {
        return !isAsteroid() && !isStar() || (canDecorate && overrideDecoration);
    }

    public void setDecoratoration(boolean value) {
        canDecorate = value;
        overrideDecoration = true;
    }

    public boolean isDecorationOverridden() {
        return overrideDecoration;
    }

    public void unsetDecoratoration() {
        overrideDecoration = false;
    }

    /**
     * @return set of all moons orbiting this planet
     */
    public Set<Integer> getChildPlanets() {
        return childPlanets;
    }

    /**
     * @return how many moons deep this planet is, IE: if the moon of a moon of a planet then three is returned
     */
    public int getPathLengthToStar() {
        if (isMoon())
            return 1 + getParentProperties().getPathLengthToStar();
        return 1;
    }

    /**
     * Does not check for hierarchy loops!
     *
     * @param child DimensionProperties of the new child
     * @return true if successfully added as a child planet
     */
    public boolean addChildPlanet(DimensionProperties child) {
        //TODO: check for hierarchy loops!
        if (child == this)
            return false;

        childPlanets.add(child.getId());
        child.setParentPlanet(this);
        return true;
    }

    /**
     * Removes the passed DIMID from the list of moons
     *
     * @param id DIMID of the child planet to remove
     */
    public void removeChild(int id) {
        childPlanets.remove(id);
    }

    //Satellites --------------------------------------------------------

    /**
     * Adds a satellite to this DIM
     *
     * @param satellite satellite to add
     * @param world     world to add the satellite to
     */
    public void addSatellite(SatelliteBase satellite, World world) {
        //Prevent dupes
        if (satellites.containsKey(satellite.getId())) {
            satellites.remove(satellite.getId());
            tickingSatellites.remove(satellite.getId());
        }

        satellites.put(satellite.getId(), satellite);
        satellite.setDimensionId(world);


        if (satellite.canTick())
            tickingSatellites.put(satellite.getId(), satellite);

        if (!world.isRemote)
            PacketHandler.sendToAll(new PacketSatellite(satellite));
    }

    /**
     * Adds a satellite to this DIM
     *
     * @param satellite satellite to add
     * @param world     world to add the satellite to
     */
    public void addSatellite(SatelliteBase satellite, int world, boolean isRemote) {
        //Prevent dupes
        if (satellites.containsKey(satellite.getId())) {
            satellites.remove(satellite.getId());
            tickingSatellites.remove(satellite.getId());
        }

        satellites.put(satellite.getId(), satellite);
        satellite.setDimensionId(world);


        if (satellite.canTick())
            tickingSatellites.put(satellite.getId(), satellite);

        if (!isRemote)
            PacketHandler.sendToAll(new PacketSatellite(satellite));
    }

    /**
     * Really only meant to be used on the client when receiving a packet
     *
     * @param satellite the satellite to add to orbit
     */
    public void addSatellite(SatelliteBase satellite) {
        if (satellites.containsKey(satellite.getId())) {
            satellites.remove(satellite.getId());
            tickingSatellites.remove(satellite.getId());
        }
        satellites.put(satellite.getId(), satellite);

        if (satellite.canTick()) //TODO: check for dupes
            tickingSatellites.put(satellite.getId(), satellite);
    }

    /**
     * Removes the satellite from orbit around this world
     *
     * @param satelliteId ID # for this satellite
     * @return reference to the satellite object
     */
    public SatelliteBase removeSatellite(long satelliteId) {
        SatelliteBase satellite = satellites.remove(satelliteId);

        if (satellite != null && satellite.canTick() && tickingSatellites.containsKey(satelliteId))
            tickingSatellites.get(satelliteId).setDead();

        return satellite;
    }

    /**
     * @param id ID # for this satellite
     * @return a reference to the satelliteBase object given this ID
     */
    public SatelliteBase getSatellite(long id) {
        return satellites.get(id);
    }

    /**
     * Returns all of a dimension's satellites
     *
     * @return a Collection containing all of a dimension's satellites
     */
    public Collection<SatelliteBase> getAllSatellites() {
        return this.satellites.values();
    }

    public Collection<SatelliteBase> getTickingSatellites() {
        return this.tickingSatellites.values();
    }

    //TODO: multithreading

    /**
     * Tick satellites as needed
     */
    public void tick() {

        Iterator<SatelliteBase> iterator = tickingSatellites.values().iterator();
        //System.out.println(":"+tickingSatellites.size());
        while (iterator.hasNext()) {
            SatelliteBase satellite = iterator.next();
            satellite.tickEntity();

            if (satellite.isDead()) {
                iterator.remove();
                satellites.remove(satellite.getId());
                break;//avoid  java.util.ConcurrentModificationException
            }
        }
        updateOrbit();

        //remove water source locks over time
        Iterator<watersourcelocked> iterator_2 = water_source_locked_positions.iterator();
        while (iterator_2.hasNext()) {
            watersourcelocked i = iterator_2.next();
            i.timer -= 1;
            if (i.timer <= 0) {
                BlockPos p = i.pos.getBlockPos();
                iterator_2.remove(); // Safe removal during iteration
                World world = (net.minecraftforge.common.DimensionManager.getWorld(getId()));
                world.notifyNeighborsOfStateChange(p, world.getBlockState(p).getBlock(), false);
            }
        }

        World world = (net.minecraftforge.common.DimensionManager.getWorld(getId()));
        //world has to be loaded
        if (world != null) {
            if (proxylists.gethelper(getId()) != null) {
                TerraformingHelper t = proxylists.gethelper(getId());
                if (t.has_blocks_in_dec_queue()) {
                    //if (new Random().nextInt(100) < 50) {
                    for (int i = 0; i < 5; i++) {
                        BlockPos target = t.get_next_position_decoration(true);
                        if (target != null) {
                            BiomeHandler.do_decoration(world, target, getId());
                        } else break;
                    }
                    //}
                }
            }
        }
    }
    public void add_water_locked_pos(HashedBlockPosition pos) {
        for (watersourcelocked i : water_source_locked_positions) {
            if (i.pos.equals(pos)) {
                i.reset_timer();
                return;
            }
        }
        this.water_source_locked_positions.add(new watersourcelocked(pos));
    }

    public void updateOrbit() {
        this.prevOrbitalTheta = this.orbitTheta;
        if (this.isMoon()) {
            this.orbitTheta = (AstronomicalBodyHelper.getMoonOrbitalTheta(orbitalDist, getParentProperties().gravitationalMultiplier) + baseOrbitTheta) * (isRetrograde ? -1 : 1);
        } else if (!this.isMoon()) {
            this.orbitTheta = (AstronomicalBodyHelper.getOrbitalTheta(orbitalDist, getStar().getSize()) + baseOrbitTheta) * (isRetrograde ? -1 : 1);
        }
    }

    /**
     * @return true if this dimension is allowed to have rivers
     */
    public boolean hasRivers() {
        return hasRivers || (AtmosphereTypes.getAtmosphereTypeFromValue(originalAtmosphereDensity).compareTo(AtmosphereTypes.LOW) <= 0 && Temps.getTempFromValue(getAverageTemp()).isInRange(Temps.COLD, Temps.HOT));
    }


    /**
     * Each Planet is assigned a list of biomes that are allowed to spawn there
     *
     * @return List of biomes allowed to spawn on this planet
     */
    public List<BiomeEntry> getBiomes() {
        return allowedBiomes;
    }

    /**
     * Clears the list of allowed biomes and replaces it with the provided list
     *
     * @param biomes
     */
    public void setBiomes(List<Biome> biomes) {
        allowedBiomes.clear();
        addBiomes(biomes);
    }


    /**
     * Used to determine if a biome is allowed to spawn on ANY planet
     *
     * @param biome biome to check
     * @return true if the biome is not allowed to spawn on any Dimension
     */
    public boolean isBiomeblackListed(Biome biome, boolean is_NOT_terraforming) {

        if (!is_NOT_terraforming) {
            String modId = biome.getRegistryName().getResourceDomain();
            if (!ARConfiguration.getCurrentConfig().allowNonArBiomesInTerraforming) {
                if (!modId.equals("minecraft") && !modId.equals("advancedrocketry")) {
                    return true;
                }
            }
        }
        if (biome.equals(AdvancedRocketryBiomes.spaceBiome)) return true;

        return AdvancedRocketryBiomes.instance.getBlackListedBiomes().contains(Biome.getIdForBiome(biome));
    }

    /**
     * @return a list of biomes allowed to spawn in this dimension
     */
    public List<Biome> getViableBiomes(boolean not_terraforming) {
        Random random = new Random(System.nanoTime());
        List<Biome> viableBiomes = new ArrayList<>();

        if (atmosphereDensity > AtmosphereTypes.LOW.value && random.nextInt(3) == 0 && not_terraforming) {
            List<Biome> list = new LinkedList<>(AdvancedRocketryBiomes.instance.getSingleBiome());

            while (list.size() > 1) {
                Biome biome = list.get(random.nextInt(list.size()));
                Temps temp = Temps.getTempFromValue(averageTemperature);
                if ((biome.getTempCategory() == TempCategory.COLD && temp.isInRange(Temps.FRIGID, Temps.NORMAL)) ||
                        ((biome.getTempCategory() == TempCategory.MEDIUM || biome.getTempCategory() == TempCategory.OCEAN) &&
                                temp.isInRange(Temps.COLD, Temps.HOT)) ||
                        (biome.getTempCategory() == TempCategory.WARM && temp.isInRange(Temps.NORMAL, Temps.HOT))) {
                    viableBiomes.add(biome);
                    return viableBiomes;
                }
                list.remove(biome);
            }
        }


        if (atmosphereDensity <= AtmosphereTypes.LOW.value) {
            viableBiomes.add(AdvancedRocketryBiomes.moonBiome);
            viableBiomes.add(AdvancedRocketryBiomes.moonBiomeDark);
        } else if (Temps.getTempFromValue(averageTemperature).hotterOrEquals(Temps.TOOHOT)) {
            viableBiomes.add(AdvancedRocketryBiomes.hotDryBiome);
            viableBiomes.add(AdvancedRocketryBiomes.volcanic);
            viableBiomes.add(AdvancedRocketryBiomes.volcanicBarren);
//            viableBiomes.add(Biomes.HELL);
        } else if (Temps.getTempFromValue(averageTemperature).hotterOrEquals(Temps.HOT)) {

            for (Biome biome : Biome.REGISTRY) {
                if (biome != null && (BiomeDictionary.getTypes(biome).contains(BiomeDictionary.Type.HOT) || BiomeDictionary.getTypes(biome).contains(BiomeDictionary.Type.OCEAN)) && !isBiomeblackListed(biome, not_terraforming)) {
                    viableBiomes.add(biome);
                }
            }
        } else if (Temps.getTempFromValue(averageTemperature).hotterOrEquals(Temps.NORMAL)) {
            for (Biome biome : Biome.REGISTRY) {
                if (biome != null && !BiomeDictionary.getTypes(biome).contains(BiomeDictionary.Type.COLD) && !isBiomeblackListed(biome, not_terraforming)) {
                    viableBiomes.add(biome);
                }
            }
            //if (not_terraforming)
            //viableBiomes.addAll(BiomeDictionary.getBiomes(BiomeDictionary.Type.OCEAN));
        } else if (Temps.getTempFromValue(averageTemperature).hotterOrEquals(Temps.COLD)) {
            for (Biome biome : Biome.REGISTRY) {
                if (biome != null && !BiomeDictionary.getTypes(biome).contains(BiomeDictionary.Type.HOT) && !isBiomeblackListed(biome, not_terraforming)) {
                    viableBiomes.add(biome);
                }
            }
            //if (not_terraforming)
            //viableBiomes.addAll(BiomeDictionary.getBiomes(BiomeDictionary.Type.OCEAN));
        } else if (Temps.getTempFromValue(averageTemperature).hotterOrEquals(Temps.FRIGID)) {

            for (Biome biome : Biome.REGISTRY) {
                if (biome != null && BiomeDictionary.getTypes(biome).contains(BiomeDictionary.Type.COLD) && !isBiomeblackListed(biome, not_terraforming)) {
                    viableBiomes.add(biome);
                }
            }
        } else {
            for (Biome biome : Biome.REGISTRY) {
                if (biome != null && BiomeDictionary.getTypes(biome).contains(BiomeDictionary.Type.COLD) && !isBiomeblackListed(biome, not_terraforming)) {
                    viableBiomes.add(biome);
                }
            }
        }

        int maxBiomesPerPlanet = ARConfiguration.getCurrentConfig().maxBiomesPerPlanet;
        if (viableBiomes.size() > maxBiomesPerPlanet) {
            viableBiomes = ZUtils.copyRandomElements(viableBiomes, maxBiomesPerPlanet);
        }

        if (atmosphereDensity > AtmosphereTypes.HIGHPRESSURE.value && Temps.getTempFromValue(averageTemperature).isInRange(Temps.NORMAL, Temps.HOT))
            viableBiomes.addAll(AdvancedRocketryBiomes.instance.getHighPressureBiomes());

        return viableBiomes;
    }

    /**
     * Adds a biome and weight to the list for craters
     *
     * @param biome     biome to be added as viable
     * @param frequency frequency, with 100 as max (and default), for craters to spawn in this biome
     */
    public void addCraterBiomeWeight(Biome biome, int frequency) {
        ArrayList<BiomeEntry> biomes = new ArrayList<>();
        biomes.add(new BiomeEntry(biome, Math.min(Math.max(0, frequency), 100)));
        craterBiomeWeights.addAll(biomes);
    }

    /**
     * Gets the list of crater frequency biomes
     *
     * @return list of crater biomes + frequency in BiomeEntry format (0-100 weight)
     */
    public List<BiomeEntry> getCraterBiomeWeights() {
        return craterBiomeWeights;
    }

    /**
     * Adds a biome to the list of biomes allowed to spawn on this planet
     *
     * @param biome biome to be added as viable
     */
    public void addBiomeWeighted(Biome biome, int weight) {
        ArrayList<BiomeEntry> biomes = new ArrayList<>();
        biomes.add(new BiomeEntry(biome, weight));
        allowedBiomes.addAll(biomes);
    }

    /**
     * Adds a biome to the list of biomes allowed to spawn on this planet
     *
     * @param biome biome to be added as viable
     */
    public void addBiome(Biome biome) {
        ArrayList<Biome> biomes = new ArrayList<>();
        biomes.add(biome);
        allowedBiomes.addAll(getBiomesEntries(biomes));
    }

    /**
     * Adds a biome to the list of biomes allowed to spawn on this planet
     *
     * @param biomeId biome to be added as viable
     * @return true if the biome was added successfully, false otherwise
     */
    public boolean addBiome(int biomeId) {

        Biome biome = Biome.getBiome(biomeId);
        if (biomeId == 0 || Biome.getIdForBiome(biome) != 0) {
            List<Biome> biomes = new ArrayList<>();
            biomes.add(biome);
            allowedBiomes.addAll(getBiomesEntries(biomes));
            return true;
        }
        return false;
    }

    /**
     * Adds a list of biomes to the allowed list of biomes for this planet
     *
     * @param biomes
     */
    public void addBiomes(List<Biome> biomes) {
        //TODO check for duplicates
        allowedBiomes.addAll(getBiomesEntries(biomes));
    }

    public void setBiomeEntries(List<BiomeEntry> biomes) {
        //If list is itself DO NOT CLEAR IT
        if (biomes != allowedBiomes) {
            allowedBiomes.clear();
            allowedBiomes.addAll(biomes);
        }
    }

    /**
     * Adds all biomes of this type to the list of biomes allowed to generate
     *
     * @param type
     */
    public void addBiomeType(BiomeDictionary.Type type) {

        ArrayList<Biome> entryList = new ArrayList<>(BiomeDictionary.getBiomes(type));

        //Neither are acceptable on planets
        entryList.remove(Biome.getBiome(8));
        entryList.remove(Biome.getBiome(9));

        //Make sure we don't add double entries
        Iterator<Biome> iter = entryList.iterator();
        while (iter.hasNext()) {
            Biome nextBiome = iter.next();
            for (BiomeEntry entry : allowedBiomes) {
                if (BiomeDictionary.areSimilar(entry.biome, nextBiome))
                    iter.remove();
            }

        }
        allowedBiomes.addAll(getBiomesEntries(entryList));

    }

    /**
     * Removes all biomes of this type from the list of biomes allowed to generate
     *
     * @param type
     */
    public void removeBiomeType(BiomeDictionary.Type type) {
        for (Biome biome : Biome.REGISTRY) {
            allowedBiomes.removeIf(biomeEntry -> BiomeDictionary.areSimilar(biomeEntry.biome, biome));
        }

    }

    /**
     * Gets a list of BiomeEntries allowed to spawn in this dimension
     *
     * @param biomeIds
     * @return the list of BiomeEntries
     */
    private ArrayList<BiomeEntry> getBiomesEntries(List<Biome> biomeIds) {

        ArrayList<BiomeEntry> biomeEntries = new ArrayList<>();

        for (Biome biomes : biomeIds) {
			/*if(biomes == Biome.desert) {
				biomeEntries.add(new BiomeEntry(BiomeGenBase.desert, 30));
				continue;
			}
			else if(biomes == BiomeGenBase.savanna) {
				biomeEntries.add(new BiomeEntry(BiomeGenBase.savanna, 20));
				continue;
			}
			else if(biomes == BiomeGenBase.plains) {
				biomeEntries.add(new BiomeEntry(BiomeGenBase.plains, 10));
				continue;
			}*/

            boolean notFound = true;

            label:

            for (BiomeManager.BiomeType types : BiomeManager.BiomeType.values()) {
                for (BiomeEntry entry : BiomeManager.getBiomes(types)) {
                    if (biomes == null)
                        AdvancedRocketry.logger.warn("Null biomes loaded for DIMID: " + this.getId());
                    else if (entry.biome.equals(biomes)) {
                        biomeEntries.add(entry);
                        notFound = false;

                        break label;
                    }
                }
            }

            if (notFound && biomes != null) {
                biomeEntries.add(new BiomeEntry(biomes, 30));
            }
        }

        return biomeEntries;
    }

    public void initDefaultAttributes() {
        if (Temps.getTempFromValue(averageTemperature).hotterOrEquals(Temps.TOOHOT))
            setOceanBlock(Blocks.LAVA.getDefaultState());

        //Add planet Properties
        setGenerateCraters(AtmosphereTypes.getAtmosphereTypeFromValue(getAtmosphereDensity()).lessDenseThan(AtmosphereTypes.NORMAL));
        setGenerateVolcanos(Temps.getTempFromValue(averageTemperature).hotterOrEquals(DimensionProperties.Temps.HOT));
        setGenerateStructures(isHabitable());
        setGenerateGeodes(getAtmosphereDensity() > 125);
    }


    private void readFromTechnicalNBT(NBTTagCompound nbt) {
        NBTTagList list;
        if (nbt.hasKey("beaconLocations")) {
            list = nbt.getTagList("beaconLocations", NBT.TAG_INT_ARRAY);

            for (int i = 0; i < list.tagCount(); i++) {
                int[] location = list.getIntArrayAt(i);
                beaconLocations.add(new HashedBlockPosition(location[0], location[1], location[2]));
            }
            DimensionManager.getInstance().knownPlanets.add(getId());
        } else
            beaconLocations.clear();

        //Satellites

        if (nbt.hasKey("satallites")) {
            NBTTagCompound allSatelliteNBT = nbt.getCompoundTag("satallites");

            for (String keyObject : allSatelliteNBT.getKeySet()) {
                Long longKey = Long.parseLong(keyObject);

                NBTTagCompound satelliteNBT = allSatelliteNBT.getCompoundTag(keyObject);

                if (satellites.containsKey(longKey)) {
                    satellites.get(longKey).readFromNBT(satelliteNBT);
                } else {
                    //Check for NBT errors
                    try {
                        SatelliteBase satellite = SatelliteRegistry.createFromNBT(satelliteNBT);

                        satellites.put(longKey, satellite);

                        if (satellite.canTick()) {
                            tickingSatellites.put(satellite.getId(), satellite);
                        }

                    } catch (NullPointerException e) {
                        AdvancedRocketry.logger.warn("Satellite with bad NBT detected, Removing");
                    }
                }
            }
        }
    }

    public void readFromNBT(NBTTagCompound nbt) {

        NBTTagList list;

        if (nbt.hasKey("skyColor")) {
            list = nbt.getTagList("skyColor", NBT.TAG_FLOAT);
            skyColor = new float[list.tagCount()];
            for (int f = 0; f < list.tagCount(); f++) {
                skyColor[f] = list.getFloatAt(f);
            }
        }

        if (nbt.hasKey("ringColor")) {
            list = nbt.getTagList("ringColor", NBT.TAG_FLOAT);
            ringColor = new float[list.tagCount()];
            for (int f = 0; f < list.tagCount(); f++) {
                ringColor[f] = list.getFloatAt(f);
            }
        }

        if (nbt.hasKey("sunriseSunsetColors")) {
            list = nbt.getTagList("sunriseSunsetColors", NBT.TAG_FLOAT);
            sunriseSunsetColors = new float[list.tagCount()];
            for (int f = 0; f < list.tagCount(); f++) {
                sunriseSunsetColors[f] = list.getFloatAt(f);
            }
        }

        if (nbt.hasKey("fogColor")) {
            list = nbt.getTagList("fogColor", NBT.TAG_FLOAT);
            fogColor = new float[list.tagCount()];
            for (int f = 0; f < list.tagCount(); f++) {
                fogColor[f] = list.getFloatAt(f);
            }
        }

        //Load biomes
        if (nbt.hasKey("biomes")) {

            allowedBiomes.clear();
            int[] biomeIds = nbt.getIntArray("biomes");
            int[] biomeWeights = nbt.getIntArray("weights");
            //Old handling
            if (biomeWeights.length == 0) {
                biomeWeights = new int[biomeIds.length];
                Arrays.fill(biomeWeights, 30);
            }
            List<BiomeEntry> biomesList = new ArrayList<>();


            for (int i = 0; i < biomeIds.length; i++) {
                biomesList.add(new BiomeEntry(AdvancedRocketryBiomes.instance.getBiomeById(biomeIds[i]), biomeWeights[i]));
            }

            allowedBiomes.addAll(biomesList);
        }

        if (nbt.hasKey("craterBiomes")) {

            craterBiomeWeights.clear();
            int[] biomeIds = nbt.getIntArray("craterBiomes");
            int[] biomeWeights = nbt.getIntArray("craterWeights");
            List<BiomeEntry> biomesList = new ArrayList<>();
            for (int i = 0; i < biomeIds.length; i++) {
                biomesList.add(new BiomeEntry(AdvancedRocketryBiomes.instance.getBiomeById(biomeIds[i]), biomeWeights[i]));
            }

            craterBiomeWeights.addAll(biomesList);
        }

        if (nbt.hasKey("laserDrillOres")) {
            laserDrillOres.clear();
            list = nbt.getTagList("laserDrillOres", NBT.TAG_COMPOUND);
            for (NBTBase entry : list) {
                assert entry instanceof NBTTagCompound;
                laserDrillOres.add(new ItemStack((NBTTagCompound) entry));
            }
        }

        if (nbt.hasKey("laserDrillOresRaw")) {
            laserDrillOresRaw = nbt.getString("laserDrillOresRaw");
        }

        if (nbt.hasKey("geodeOres")) {
            geodeOres.clear();
            list = nbt.getTagList("geodeOres", NBT.TAG_STRING);
            for (NBTBase entry : list) {
                assert entry instanceof NBTTagString;
                geodeOres.add(((NBTTagString) entry).getString());
            }
        }

        if (nbt.hasKey("craterOres")) {
            craterOres.clear();
            list = nbt.getTagList("craterOres", NBT.TAG_STRING);
            for (NBTBase entry : list) {
                assert entry instanceof NBTTagString;
                craterOres.add(((NBTTagString) entry).getString());
            }
        }

        if (nbt.hasKey("artifacts")) {
            requiredArtifacts.clear();
            list = nbt.getTagList("artifacts", NBT.TAG_COMPOUND);
            for (NBTBase entry : list) {
                assert entry instanceof NBTTagCompound;
                requiredArtifacts.add(new ItemStack((NBTTagCompound) entry));
            }
        }

        gravitationalMultiplier = nbt.getFloat("gravitationalMultiplier");
        orbitalDist = nbt.getInteger("orbitalDist");
        orbitTheta = nbt.getDouble("orbitTheta");
        baseOrbitTheta = nbt.getDouble("baseOrbitTheta");
        orbitalPhi = nbt.getDouble("orbitPhi");
        rotationalPhi = nbt.getDouble("rotationalPhi");
        isRetrograde = nbt.getBoolean("isRetrograde");
        hasOxygen = nbt.getBoolean("hasOxygen");
        colorOverride = nbt.getBoolean("colorOverride");
        atmosphereDensity = nbt.getInteger("atmosphereDensity");

        if (nbt.hasKey("originalAtmosphereDensity"))
            originalAtmosphereDensity = nbt.getInteger("originalAtmosphereDensity");
        else
            originalAtmosphereDensity = atmosphereDensity;

        peakInsolationMultiplier = nbt.getDouble("peakInsolationMultiplier");
        peakInsolationMultiplierWithoutAtmosphere = nbt.getDouble("peakInsolationMultiplierWithoutAtmosphere");
        averageTemperature = nbt.getInteger("avgTemperature");
        rotationalPeriod = nbt.getInteger("rotationalPeriod");
        name = nbt.getString("name");
        customIcon = nbt.getString("icon");
        isNativeDimension = !nbt.hasKey("isNative") || nbt.getBoolean("isNative"); //Prevent world breakages when loading from old version
        isGasGiant = nbt.getBoolean("isGasGiant");
        hasRings = nbt.getBoolean("hasRings");
        ringAngle = nbt.getInteger("ringAngle");
        seaLevel = nbt.getInteger("sealevel");
        //target_sea_level = nbt.getInteger("target_sea_level");
        generatorType = nbt.getInteger("genType");
        canGenerateCraters = nbt.getBoolean("canGenerateCraters");
        canGenerateGeodes = nbt.getBoolean("canGenerateGeodes");
        canGenerateStructures = nbt.getBoolean("canGenerateStructures");
        canGenerateVolcanoes = nbt.getBoolean("canGenerateVolcanos");
        canGenerateCaves = nbt.getBoolean("canGenerateCaves");
        hasRivers = nbt.getBoolean("hasRivers");
        geodeFrequencyMultiplier = nbt.getFloat("geodeFrequencyMultiplier");
        craterFrequencyMultiplier = nbt.getFloat("craterFrequencyMultiplier");
        volcanoFrequencyMultiplier = nbt.getFloat("volcanoFrequencyMultiplier");


        //Hierarchy
        if (nbt.hasKey("childrenPlanets")) {
            for (int i : nbt.getIntArray("childrenPlanets"))
                childPlanets.add(i);
        }

        //Note: parent planet must be set before setting the star otherwise it would cause duplicate planets in the StellarBody's array
        parentPlanet = nbt.getInteger("parentPlanet");
        this.setStar(DimensionManager.getInstance().getStar(nbt.getInteger("starId")));

        if (isGasGiant) {
            NBTTagList fluidList = nbt.getTagList("fluids", NBT.TAG_STRING);
            getHarvestableGasses().clear();

            for (int i = 0; i < fluidList.tagCount(); i++) {
                Fluid fluid = FluidRegistry.getFluid(fluidList.getStringTagAt(i));
                if (fluid != null)
                    getHarvestableGasses().add(fluid);
            }

            //Do not allow empty atmospheres, at least not yet
            if (getHarvestableGasses().isEmpty())
                getHarvestableGasses().addAll(AtmosphereRegister.getInstance().getHarvestableGasses());
        }

        if (nbt.hasKey("oceanBlock")) {
            Block block = Block.REGISTRY.getObject(new ResourceLocation(nbt.getString("oceanBlock")));
            if (block == Blocks.AIR) {
                oceanBlock = null;
            } else {
                int meta = nbt.getInteger("oceanBlockMeta");
                oceanBlock = block.getStateFromMeta(meta);
            }
        } else
            oceanBlock = null;

        if (nbt.hasKey("fillBlock")) {
            Block block = Block.REGISTRY.getObject(new ResourceLocation(nbt.getString("fillBlock")));
            if (block == Blocks.AIR) {
                fillerBlock = null;
            } else {
                int meta = nbt.getInteger("fillBlockMeta");
                fillerBlock = block.getStateFromMeta(meta);
            }
        } else
            fillerBlock = null;


        readFromTechnicalNBT(nbt);
    }



    private void writeTechnicalNBT(NBTTagCompound nbt) {
        NBTTagList list;
        if (!beaconLocations.isEmpty()) {
            list = new NBTTagList();

            for (HashedBlockPosition pos : beaconLocations) {
                list.appendTag(new NBTTagIntArray(new int[]{pos.x, pos.y, pos.z}));
            }
            nbt.setTag("beaconLocations", list);
        }

        //Satellites

        if (!satellites.isEmpty()) {
            NBTTagCompound allSatelliteNBT = new NBTTagCompound();
            for (Entry<Long, SatelliteBase> entry : satellites.entrySet()) {
                NBTTagCompound satelliteNBT = new NBTTagCompound();

                entry.getValue().writeToNBT(satelliteNBT);
                allSatelliteNBT.setTag(entry.getKey().toString(), satelliteNBT);
            }
            nbt.setTag("satallites", allSatelliteNBT);
        }   }

    //terraforming data
    public void read_terraforming_data(NBTTagCompound nbt){

        int dimid =getId();
        if (!proxylists.isinitialized(dimid)){
            proxylists.initdim(dimid);
        }

        if (nbt.hasKey("fullyGeneratedChunks")) {

            NBTTagList list = nbt.getTagList("fullyGeneratedChunks", NBT.TAG_COMPOUND);
            if (!list.hasNoTags())
                proxylists.setChunksFullyTerraformed(dimid, new HashSet<ChunkPos>());
            for (NBTBase entry : list) {
                assert entry instanceof NBTTagCompound;
                int x = ((NBTTagCompound) entry).getInteger("x");
                int z = ((NBTTagCompound) entry).getInteger("z");
                System.out.println("Chunk fully terraformed: " + x + ":" + z);

                boolean chunk_was_already_done = false;
                for (ChunkPos i : proxylists.getChunksFullyTerraformed(dimid)) {
                    if (x == i.x && z == i.z) {
                        chunk_was_already_done = true;
                        break;
                    }
                }
                if (!chunk_was_already_done)
                    proxylists.getChunksFullyTerraformed(dimid).add(new ChunkPos(x, z));
                else System.out.println("Chunk is already in list: " + x + ":" + z);
            }
        }

        if (nbt.hasKey("fullyBiomeChangedChunks")) {

            NBTTagList list = nbt.getTagList("fullyBiomeChangedChunks", NBT.TAG_COMPOUND);
            if (!list.hasNoTags())
                proxylists.setChunksFullyBiomeChanged(dimid, new HashSet<ChunkPos>());
            for (NBTBase entry : list) {
                assert entry instanceof NBTTagCompound;
                int x = ((NBTTagCompound) entry).getInteger("x");
                int z = ((NBTTagCompound) entry).getInteger("z");
                System.out.println("Chunk fully biome changed: " + x + ":" + z);

                boolean chunk_was_already_done = false;
                for (ChunkPos i : proxylists.getChunksFullyBiomeChanged(dimid)) {
                    if (x == i.x && z == i.z) {
                        chunk_was_already_done = true;
                        break;
                    }
                }
                if (!chunk_was_already_done)
                    proxylists.getChunksFullyBiomeChanged(dimid).add(new ChunkPos(x, z));
                else System.out.println("Chunk is already in list: " + x + ":" + z);
            }
        }

        if (nbt.hasKey("terraformingProtectedBlocks")) {

            NBTTagList list = nbt.getTagList("terraformingProtectedBlocks", NBT.TAG_COMPOUND);
            if (!list.hasNoTags())
                proxylists.setProtectingBlocksForDimension(dimid, new ArrayList<>());
            for (NBTBase entry : list) {
                assert entry instanceof NBTTagCompound;
                int x = ((NBTTagCompound) entry).getInteger("x");
                int z = ((NBTTagCompound) entry).getInteger("z");
                int y = ((NBTTagCompound) entry).getInteger("y");
                proxylists.getProtectingBlocksForDimension(dimid).add(new BlockPos(x, y, z));
                System.out.println("read protecting block at " + x + ":" + y + ":" + z + " - - " + proxylists.getProtectingBlocksForDimension(dimid).size());
            }
        }
    }
    public void write_terraforming_data(NBTTagCompound nbt) {
        // write terraforming data

        int dimid = getId();
        if (!proxylists.isinitialized(dimid)){
            return;
        }
        NBTTagList list = new NBTTagList();
        for (ChunkPos pos : proxylists.getChunksFullyTerraformed(dimid)) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("x", pos.x);
            entry.setInteger("z", pos.z);
            list.appendTag(entry);
        }
        nbt.setTag("fullyGeneratedChunks", list);

        list = new NBTTagList();
        for (ChunkPos pos : proxylists.getChunksFullyBiomeChanged(dimid)) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("x", pos.x);
            entry.setInteger("z", pos.z);
            list.appendTag(entry);
        }
        nbt.setTag("fullyBiomeChangedChunks", list);

        list = new NBTTagList();
            for (BlockPos pos : proxylists.getProtectingBlocksForDimension(dimid)) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setInteger("x", pos.getX());
                entry.setInteger("y", pos.getY());
                entry.setInteger("z", pos.getZ());
                list.appendTag(entry);
            }
            nbt.setTag("terraformingProtectedBlocks", list);


    }
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList list;

        if (skyColor != null) {
            list = new NBTTagList();
            for (float f : skyColor) {
                list.appendTag(new NBTTagFloat(f));
            }
            nbt.setTag("skyColor", list);
        }

        if (sunriseSunsetColors != null) {
            list = new NBTTagList();
            for (float f : sunriseSunsetColors) {
                list.appendTag(new NBTTagFloat(f));
            }
            nbt.setTag("sunriseSunsetColors", list);
        }

        list = new NBTTagList();
        for (float f : fogColor) {
            list.appendTag(new NBTTagFloat(f));
        }
        nbt.setTag("fogColor", list);

        if (hasRings) {
            nbt.setInteger("ringAngle", ringAngle);
            list = new NBTTagList();
            for (float f : ringColor) {
                list.appendTag(new NBTTagFloat(f));
            }
            nbt.setTag("ringColor", list);
        }


        if (!allowedBiomes.isEmpty()) {
            int[] biomeId = new int[allowedBiomes.size()];
            int[] weights = new int[allowedBiomes.size()];
            for (int i = 0; i < allowedBiomes.size(); i++) {
                biomeId[i] = Biome.getIdForBiome(allowedBiomes.get(i).biome);
                weights[i] = allowedBiomes.get(i).itemWeight;
            }
            nbt.setIntArray("biomes", biomeId);
            nbt.setIntArray("weights", weights);
        }

        if (!craterBiomeWeights.isEmpty()) {
            int[] biomeId = new int[craterBiomeWeights.size()];
            int[] weights = new int[craterBiomeWeights.size()];
            for (int i = 0; i < craterBiomeWeights.size(); i++) {
                biomeId[i] = Biome.getIdForBiome(craterBiomeWeights.get(i).biome);
                weights[i] = craterBiomeWeights.get(i).itemWeight;
            }
            nbt.setIntArray("craterBiomes", biomeId);
            nbt.setIntArray("craterWeights", weights);
        }

        if (!laserDrillOres.isEmpty()) {
            list = new NBTTagList();
            for (ItemStack ore : laserDrillOres) {
                NBTTagCompound entry = new NBTTagCompound();
                ore.writeToNBT(entry);
                list.appendTag(entry);
            }
            nbt.setTag("laserDrillOres", list);
        }

        if (laserDrillOresRaw != null) {
            nbt.setTag("laserDrillOresRaw", new NBTTagString(laserDrillOresRaw));
        }

        if (!geodeOres.isEmpty()) {
            list = new NBTTagList();
            for (String ore : geodeOres) {
                list.appendTag(new NBTTagString(ore));
            }
            nbt.setTag("geodeOres", list);
        }

        if (!craterOres.isEmpty()) {
            list = new NBTTagList();
            for (String ore : craterOres) {
                list.appendTag(new NBTTagString(ore));
            }
            nbt.setTag("craterOres", list);
        }

        if (!requiredArtifacts.isEmpty()) {
            list = new NBTTagList();
            for (ItemStack ore : requiredArtifacts) {
                NBTTagCompound entry = new NBTTagCompound();
                ore.writeToNBT(entry);
                list.appendTag(entry);
            }
            nbt.setTag("artifacts", list);
        }

        nbt.setInteger("starId", starId);
        nbt.setFloat("gravitationalMultiplier", gravitationalMultiplier);
        nbt.setInteger("orbitalDist", orbitalDist);
        nbt.setDouble("orbitTheta", orbitTheta);
        nbt.setDouble("baseOrbitTheta", baseOrbitTheta);
        nbt.setDouble("orbitPhi", orbitalPhi);
        nbt.setDouble("rotationalPhi", rotationalPhi);
        nbt.setBoolean("isRetrograde", isRetrograde);
        nbt.setBoolean("hasOxygen", hasOxygen);
        nbt.setBoolean("colorOverride", colorOverride);
        nbt.setInteger("atmosphereDensity", atmosphereDensity);
        nbt.setInteger("originalAtmosphereDensity", originalAtmosphereDensity);
        nbt.setDouble("peakInsolationMultiplier", peakInsolationMultiplier);
        nbt.setDouble("peakInsolationMultiplierWithoutAtmosphere", peakInsolationMultiplierWithoutAtmosphere);
        nbt.setInteger("avgTemperature", averageTemperature);
        nbt.setInteger("rotationalPeriod", rotationalPeriod);
        nbt.setString("name", name);
        nbt.setString("icon", customIcon);
        nbt.setBoolean("isNative", isNativeDimension);
        nbt.setBoolean("isGasGiant", isGasGiant);
        nbt.setBoolean("hasRings", hasRings);
        nbt.setInteger("sealevel", seaLevel);
        //nbt.setInteger("target_sea_level", target_sea_level);
        nbt.setInteger("genType", generatorType);
        nbt.setBoolean("canGenerateCraters", canGenerateCraters);
        nbt.setBoolean("canGenerateGeodes", canGenerateGeodes);
        nbt.setBoolean("canGenerateStructures", canGenerateStructures);
        nbt.setBoolean("canGenerateVolcanos", canGenerateVolcanoes);
        nbt.setBoolean("canGenerateCaves", canGenerateCaves);
        nbt.setBoolean("hasRivers", hasRivers);
        nbt.setFloat("geodeFrequencyMultiplier", geodeFrequencyMultiplier);
        nbt.setFloat("craterFrequencyMultiplier", craterFrequencyMultiplier);
        nbt.setFloat("volcanoFrequencyMultiplier", volcanoFrequencyMultiplier);

        //Hierarchy
        if (!childPlanets.isEmpty()) {
            Integer[] intList = new Integer[childPlanets.size()];

            NBTTagIntArray childArray = new NBTTagIntArray(ArrayUtils.toPrimitive(childPlanets.toArray(intList)));
            nbt.setTag("childrenPlanets", childArray);
        }

        nbt.setInteger("parentPlanet", parentPlanet);

        if (isGasGiant) {
            NBTTagList fluidList = new NBTTagList();

            for (Fluid f : getHarvestableGasses()) {
                fluidList.appendTag(new NBTTagString(f.getName()));
            }

            nbt.setTag("fluids", fluidList);
        }

        if (oceanBlock != null) {
            nbt.setString("oceanBlock", Block.REGISTRY.getNameForObject(oceanBlock.getBlock()).toString());
            nbt.setInteger("oceanBlockMeta", oceanBlock.getBlock().getMetaFromState(oceanBlock));
        }

        if (fillerBlock != null) {
            nbt.setString("fillBlock", Block.REGISTRY.getNameForObject(fillerBlock.getBlock()).toString());
            nbt.setInteger("fillBlockMeta", fillerBlock.getBlock().getMetaFromState(fillerBlock));
        }



        writeTechnicalNBT(nbt);
    }

    /**
     * @return temperature of the planet in Kelvin
     */
    @Override
    public int getAverageTemp() {
        averageTemperature = AstronomicalBodyHelper.getAverageTemperature(this.getStar(), this.getSolarOrbitalDistance(), this.getAtmosphereDensity());

        /*
        int temp = averageTemperature;
        float pressure = (float) (atmosphereDensity + 1) / (float) 100;
        pressure = (float) Math.max(0.01, pressure);
        float water_can_exist_value = 400;
        float planetvalue = temp / pressure;

        if (planetvalue < water_can_exist_value) {
            water_can_exist = true;
        } else water_can_exist = false;
        */

        return averageTemperature;
    }

    public IBlockState getOceanBlock() {
        return oceanBlock;
    }

    public void setOceanBlock(IBlockState block) {
        oceanBlock = block;
    }

    public IBlockState getStoneBlock() {
        return fillerBlock;
    }

    public void setStoneBlock(IBlockState block) {
        fillerBlock = block;
    }

    /**
     * Function for calculating atmosphere thinning with respect to height, normalized
     *
     * @param y
     * @return the density of the atmosphere at the given height
     */
    public float getAtmosphereDensityAtHeight(double y) {
        return atmosphereDensity * MathHelper.clamp((float) (1 + (256 - y) / 200f), 0f, 1f) / 100f;
    }

    /**
     * Gets the fog color at a given altitude, used to assist the illusion of thinning atmosphere
     *
     * @param y        y-height
     * @param fogColor current fog color at this location
     * @return
     */
    public float[] getFogColorAtHeight(double y, Vec3d fogColor) {
        float atmDensity = getAtmosphereDensityAtHeight(y);
        return new float[]{(float) (atmDensity * fogColor.x), (float) (atmDensity * fogColor.y), (float) (atmDensity * fogColor.z)};
    }

    public boolean isHabitable() {
        return this.getAtmosphere().isBreathable()
                && Temps.getTempFromValue(this.averageTemperature).isInRange(Temps.COLD, Temps.HOT);
    }

    public double[] getPlanetPosition() {
        double orbitalDistance = this.orbitalDist;
        double theta = this.orbitTheta;
        double phi = this.orbitalPhi;

        return new double[]{orbitalDistance * Math.cos(theta), orbitalDistance * Math.sin(phi), orbitalDistance * Math.sin(theta)};
    }

    public int getStarId() {
        return starId;
    }

    @Override
    public String toString() {
        return String.format("Dimension ID: %d.  Dimension Name: %s.  Parent Star %d ", getId(), getName(), getStarId());
    }

    @Override
    public double getOrbitTheta() {
        return orbitTheta;
    }

    @Override
    public int getOrbitalDist() {
        return orbitalDist;
    }

    public int getSeaLevel() {
        return seaLevel;
    }

    public void setSeaLevel(int sealevel) {
        this.seaLevel = MathHelper.clamp(sealevel, 0, 255);
    }
/*
    public int getTargetSeaLevel() {
        //check if at least one dimension changing satellite is in orbit
        boolean weathercontrollerfound = false;

        for (SatelliteBase satellite : tickingSatellites.values()) {
            if (satellite instanceof SatelliteWeatherController) {
                weathercontrollerfound = true;
                break;
            }
        }
        if (!weathercontrollerfound) {
            target_sea_level = seaLevel;
        }

        return this.target_sea_level;
    }

    public void setTargetSeaLevel(int sealevel) {
        this.target_sea_level = MathHelper.clamp(sealevel, 0, 255);
    }

 */

    public int getGenType() {
        return generatorType;
    }

    public void setGenType(int genType) {
        this.generatorType = genType;
    }

    public void setGenerateCraters(boolean canGenerateCraters) {
        this.canGenerateCraters = canGenerateCraters;
    }

    public boolean canGenerateCraters() {
        return this.canGenerateCraters;
    }

    public float getCraterMultiplier() {
        return craterFrequencyMultiplier;
    }

    public void setCraterMultiplier(float craterFrequencyMultiplier) {
        this.craterFrequencyMultiplier = craterFrequencyMultiplier;
    }

    public void setGenerateGeodes(boolean canGenerateGeodes) {
        this.canGenerateGeodes = canGenerateGeodes;
    }

    public boolean canGenerateGeodes() {
        return this.canGenerateGeodes;
    }

    public float getGeodeMultiplier() {
        return volcanoFrequencyMultiplier;
    }

    public void setGeodeMultiplier(float geodeFrequencyMultiplier) {
        this.geodeFrequencyMultiplier = geodeFrequencyMultiplier;
    }

    public void setGenerateVolcanos(boolean canGenerateVolcanos) {
        this.canGenerateVolcanoes = canGenerateVolcanos;
    }

    public boolean canGenerateVolcanos() {
        return this.canGenerateVolcanoes;
    }

    public float getVolcanoMultiplier() {
        return volcanoFrequencyMultiplier;
    }

    public void setVolcanoMultiplier(float volcanoFrequencyMultiplier) {
        this.volcanoFrequencyMultiplier = volcanoFrequencyMultiplier;
    }

    public void setGenerateStructures(boolean canGenerateStructures) {
        this.canGenerateStructures = canGenerateStructures;
    }

    public boolean canGenerateStructures() {
        return canGenerateStructures;
    }

    public void setGenerateCaves(boolean canGenerateCaves) {
        this.canGenerateCaves = canGenerateCaves;
    }

    public boolean canGenerateCaves() {
        return this.canGenerateCaves;
    }

    public float getRenderSizePlanetView() {
        return (isMoon() ? 8f : 10f) * Math.max(this.getGravitationalMultiplier() * this.getGravitationalMultiplier(), .5f) * 100;
    }

    public float getRenderSizeSolarView() {
        return (isMoon() ? 0.2f : 1f) * Math.max(this.getGravitationalMultiplier() * this.getGravitationalMultiplier(), .5f) * 100;
    }

    // Relative to parent
    @Override
    public SpacePosition getSpacePosition() {
        float distanceMultiplier = isMoon() ? 75f : 100f;

        SpacePosition spacePosition = new SpacePosition();
        spacePosition.star = getStar();
        spacePosition.world = this;
        spacePosition.isInInterplanetarySpace = this.isMoon();
        spacePosition.pitch = 0;
        spacePosition.roll = 0;
        spacePosition.yaw = 0;

        spacePosition = spacePosition.getFromSpherical(distanceMultiplier * orbitalDist + (isMoon() ? 100 : 0), orbitTheta);

        return spacePosition;
    }

    @Override
    public float[] getRingColor() {
        return ringColor;
    }

    @Override
    public float[] getSkyColor() {
        return skyColor;
    }

    /**
     * Temperatures are stored in Kelvin
     * This facilitates precise temperature calculations and specifications
     * 286 is Earthlike (13 C), Hot is 52 C, Cold is -23 C. Snowball is absolute zero
     */
    public enum Temps {
        TOOHOT(450),
        HOT(325),
        NORMAL(275),
        COLD(250),
        FRIGID(175),
        SNOWBALL(0);

        private final int temp;

        Temps(int i) {
            temp = i;
        }

        /**
         * @return a temperature that refers to the supplied value
         */

        public static Temps getTempFromValue(int value) {
            for (Temps type : Temps.values()) {
                if (value >= type.temp)
                    return type;
            }
            return SNOWBALL;
        }

        @Deprecated
        public int getTemp() {
            return temp;
        }

        public boolean hotterThan(Temps type) {
            return this.compareTo(type) < 0;
        }

        public boolean hotterOrEquals(Temps type) {
            return this.compareTo(type) <= 0;
        }

        public boolean colderThan(Temps type) {
            return this.compareTo(type) > 0;
        }

        /**
         * @param lowerBound lower Bound (inclusive)
         * @param upperBound upper Bound (inclusive)
         * @return true if this resides between the to bounds
         */
        public boolean isInRange(Temps lowerBound, Temps upperBound) {
            return this.compareTo(lowerBound) <= 0 && this.compareTo(upperBound) >= 0;
        }
    }

    /**
     * Contains standardized pressure ranges for planets
     * where 100 is earthlike, largers values are higher pressure
     */
    public enum AtmosphereTypes {
        SUPERHIGHPRESSURE(800),
        HIGHPRESSURE(200),
        NORMAL(75),
        LOW(25),
        NONE(0);

        private final int value;

        AtmosphereTypes(int value) {
            this.value = value;
        }

        public static AtmosphereTypes getAtmosphereTypeFromValue(int value) {
            for (AtmosphereTypes type : AtmosphereTypes.values()) {
                if (value > type.value)
                    return type;
            }
            return NONE;
        }

        public int getAtmosphereValue() {
            return value;
        }

        public boolean denserThan(AtmosphereTypes type) {
            return this.compareTo(type) < 0;
        }

        public boolean lessDenseThan(AtmosphereTypes type) {
            return this.compareTo(type) > 0;
        }
    }

    public enum PlanetIcons {
        EARTHLIKE(new ResourceLocation("advancedrocketry:textures/planets/Earthlike.png")),
        LAVA(new ResourceLocation("advancedrocketry:textures/planets/Lava.png")),
        MARSLIKE(new ResourceLocation("advancedrocketry:textures/planets/marslike.png")),
        MOON(new ResourceLocation("advancedrocketry:textures/planets/moon.png")),
        WATERWORLD(new ResourceLocation("advancedrocketry:textures/planets/WaterWorld.png")),
        ICEWORLD(new ResourceLocation("advancedrocketry:textures/planets/IceWorld.png")),
        DESERT(new ResourceLocation("advancedrocketry:textures/planets/desertworld.png")),
        CARBON(new ResourceLocation("advancedrocketry:textures/planets/carbonworld.png")),
        VENUSIAN(new ResourceLocation("advancedrocketry:textures/planets/venusian.png")),
        GASGIANTBLUE(new ResourceLocation("advancedrocketry:textures/planets/GasGiantBlue.png")),
        GASGIANTRED(new ResourceLocation("advancedrocketry:textures/planets/GasGiantred.png")),
        GASGIANTBROWN(new ResourceLocation("advancedrocketry:textures/planets/gasgiantbrown.png")),
        ASTEROID(new ResourceLocation("advancedrocketry:textures/planets/asteroid.png")),
        UNKNOWN(new ResourceLocation("advancedrocketry:textures/planets/Unknown.png"));


        private ResourceLocation resource;
        private ResourceLocation resourceLEO;

        PlanetIcons(ResourceLocation resource) {
            this.resource = resource;

            this.resourceLEO = new ResourceLocation(resource.toString().substring(0, resource.toString().length() - 4) + "LEO.jpg");
        }

        PlanetIcons(ResourceLocation resource, ResourceLocation leo) {
            this.resource = resource;

            this.resourceLEO = leo;
        }

        public ResourceLocation getResource() {
            return resource;
        }

        public ResourceLocation getResourceLEO() {
            return resourceLEO;
        }
    }
}
