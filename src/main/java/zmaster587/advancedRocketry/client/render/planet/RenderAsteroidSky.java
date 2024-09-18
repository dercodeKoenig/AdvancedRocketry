package zmaster587.advancedRocketry.client.render.planet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.IRenderHandler;
import org.lwjgl.opengl.GL11;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.IPlanetaryProvider;
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.event.RocketEventHandler;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.advancedRocketry.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.stations.SpaceStationObject;
import zmaster587.advancedRocketry.util.AstronomicalBodyHelper;
import zmaster587.libVulpes.util.Vector3F;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class RenderAsteroidSky extends IRenderHandler {


    public static final ResourceLocation asteroid1 = new ResourceLocation("advancedRocketry:textures/planets/asteroid_a.png");
    public static final ResourceLocation asteroid2 = new ResourceLocation("advancedRocketry:textures/planets/asteroid_b.png");
    public static final ResourceLocation asteroid3 = new ResourceLocation("advancedRocketry:textures/planets/asteroid_c.png");
    ResourceLocation currentlyBoundTex = null;
    float celestialAngle;
    Vector3F<Float> axis;
    Minecraft mc = Minecraft.getMinecraft();
    private int starGLCallList;
    private int glSkyList;
    private int glSkyList2;
    private int glSkyList3;

    //Mostly vanilla code
    //TODO: make usable on other planets
    public RenderAsteroidSky() {
        axis = new Vector3F<>(1f, 0f, 0f);

        this.starGLCallList = GLAllocation.generateDisplayLists(4);
        GL11.glPushMatrix();
        GL11.glNewList(this.starGLCallList, GL11.GL_COMPILE);
        this.renderStars();
        GL11.glEndList();
        GL11.glPopMatrix();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        this.glSkyList = this.starGLCallList + 1;
        GL11.glNewList(this.glSkyList, GL11.GL_COMPILE);
        byte b2 = 64;
        int i = 256 / b2 + 2;
        float f = 16.0F;
        int j;
        int k;

        for (j = -b2 * i; j <= b2 * i; j += b2) {
            for (k = -b2 * i; k <= b2 * i; k += b2) {
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
                buffer.pos(j, f, k).endVertex();
                buffer.pos(j + b2, f, k).endVertex();
                buffer.pos(j + b2, f, k + b2).endVertex();
                buffer.pos(j, f, k + b2).endVertex();
                Tessellator.getInstance().draw();
            }
        }

        GL11.glEndList();
        this.glSkyList2 = this.starGLCallList + 2;
        GL11.glNewList(this.glSkyList2, GL11.GL_COMPILE);
        f = -16.0F;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        for (j = -b2 * i; j <= b2 * i; j += b2) {
            for (k = -b2 * i; k <= b2 * i; k += b2) {
                buffer.pos(j, f, k).endVertex();
                buffer.pos(j + b2, f, k).endVertex();
                buffer.pos(j + b2, f, k + b2).endVertex();
                buffer.pos(j, f, k + b2).endVertex();
            }
        }

        Tessellator.getInstance().draw();
        GL11.glEndList();

        this.glSkyList3 = this.starGLCallList + 3;
        GL11.glPushMatrix();
        GL11.glNewList(this.glSkyList3, GL11.GL_COMPILE);
        renderAsteroids();
        GL11.glEndList();
        GL11.glPopMatrix();
    }

    private void renderAsteroids() {
        Random random = new Random(10843L);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        for (int i = 0; i < 200; ++i) {
            double d0 = random.nextFloat() * 2F - 1F;
            double d1 = random.nextFloat() - .5F;
            double d2 = random.nextFloat() * 2F - 1F;
            double size = 0.15F + random.nextFloat();
            double d4 = d0 * d0 + d1 * d1 + d2 * d2;

            if (d4 < 1.0D && d4 > 0.01D) {
                d4 = 0.5D / Math.sqrt(d4);
                d0 *= d4;
                d1 *= d4;
                d2 *= d4;
                double d5 = d0 * 100.0D;
                double d6 = d1 * 100.0D;
                double d7 = d2 * 100.0D;
                double d8 = Math.atan2(d0, d2);
                double d9 = Math.sin(d8);
                double d10 = Math.cos(d8);
                double d11 = Math.atan2(Math.sqrt(d0 * d0 + d2 * d2), d1);
                double d12 = Math.sin(d11);
                double d13 = Math.cos(d11);
                double d14 = random.nextDouble() * Math.PI * 2.0D;
                double d15 = Math.sin(d14);
                double d16 = Math.cos(d14);

                float r, g, b;
                r = random.nextFloat() * 0.05f + .95f;
                g = random.nextFloat() * 0.1f + .9f;
                b = random.nextFloat() * 0.1f + .9f;

                for (int j = 0; j < 4; ++j) {
                    double d17 = 0.0D;
                    double d18 = (double) ((j & 2) - 1) * size;
                    double d19 = (double) ((j + 1 & 2) - 1) * size;
                    double d20 = d18 * d16 - d19 * d15;
                    double d21 = d19 * d16 + d18 * d15;
                    double d22 = d20 * d12 + d17 * d13;
                    double d23 = d17 * d12 - d20 * d13;
                    double d24 = d23 * d9 - d21 * d10;
                    double d25 = d21 * d9 + d23 * d10;
                    buffer.pos(d5 + d24, d6 + d22, d7 + d25).tex(d18 / (size * 2) + .5, d19 / (size * 2) + .5).color(r, g, b, 1f).endVertex();
                }
            }
        }

        Tessellator.getInstance().draw();
        //buffer.finishDrawing();
    }

    private void renderStars() {
        Random random = new Random(10842L);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        for (int i = 0; i < 2000; ++i) {
            double d0 = random.nextFloat() * 2.0F - 1.0F;
            double d1 = random.nextFloat() * 2.0F - 1.0F;
            double d2 = random.nextFloat() * 2.0F - 1.0F;
            double d3 = 0.15F + random.nextFloat() * 0.1F;
            double d4 = d0 * d0 + d1 * d1 + d2 * d2;

            if (d4 < 1.0D && d4 > 0.01D) {
                d4 = 1.0D / Math.sqrt(d4);
                d0 *= d4;
                d1 *= d4;
                d2 *= d4;
                double d5 = d0 * 100.0D;
                double d6 = d1 * 100.0D;
                double d7 = d2 * 100.0D;
                double d8 = Math.atan2(d0, d2);
                double d9 = Math.sin(d8);
                double d10 = Math.cos(d8);
                double d11 = Math.atan2(Math.sqrt(d0 * d0 + d2 * d2), d1);
                double d12 = Math.sin(d11);
                double d13 = Math.cos(d11);
                double d14 = random.nextDouble() * Math.PI * 2.0D;
                double d15 = Math.sin(d14);
                double d16 = Math.cos(d14);

                for (int j = 0; j < 4; ++j) {
                    double d17 = 0.0D;
                    double d18 = (double) ((j & 2) - 1) * d3;
                    double d19 = (double) ((j + 1 & 2) - 1) * d3;
                    double d20 = d18 * d16 - d19 * d15;
                    double d21 = d19 * d16 + d18 * d15;
                    double d22 = d20 * d12 + d17 * d13;
                    double d23 = d17 * d12 - d20 * d13;
                    double d24 = d23 * d9 - d21 * d10;
                    double d25 = d21 * d9 + d23 * d10;
                    buffer.pos(d5 + d24, d6 + d22, d7 + d25).endVertex();
                }
            }
        }

        Tessellator.getInstance().draw();
        //buffer.finishDrawing();
    }

    private static float xrotangle = 0; // used for ring rotation because I don't want to bother changing the definitions of methods.
    private static float[] skycolor = {0,0,0}; // used for black hole rendering - same reason as above
    private static double currentplanetphi = 0; // used for calculating ring/disk angle

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {


        //TODO: properly handle this
        float atmosphere;
        int solarOrbitalDistance, planetOrbitalDistance = 0;
        double myPhi = 0, myTheta = 0, myPrevOrbitalTheta = 0, myRotationalPhi = 0;
        boolean hasAtmosphere = false, isMoon;
        float[] shadowColorMultiplier = {0f, 0f, 0f};
        float[] parentAtmColor = new float[]{1f, 1f, 1f};
        float[] parentRingColor = new float[]{1f, 1f, 1f};
        float[] ringColor = new float[]{1f, 1f, 1f};
        float sunSize = 1.0f;
        float starSeparation = 0f;
        boolean isWarp = false;
        boolean isGasGiant = false;
        boolean hasRings = false;
        boolean parentPlanetHasDecorator = true;
        boolean parentHasRings = false;
        boolean parentHasATM = false;
        DimensionProperties parentProperties = null;
        DimensionProperties properties;
        EnumFacing travelDirection = null;
        ResourceLocation parentPlanetIcon = null;
        List<DimensionProperties> children;
        StellarBody primaryStar;
        celestialAngle = mc.world.getCelestialAngle(partialTicks);

        Vec3d sunColor;


        if (mc.world.provider instanceof IPlanetaryProvider) {
            IPlanetaryProvider planetaryProvider = (IPlanetaryProvider) mc.world.provider;

            properties = (DimensionProperties) planetaryProvider.getDimensionProperties(mc.player.getPosition());

            atmosphere = planetaryProvider.getAtmosphereDensityFromHeight(mc.getRenderViewEntity().posY, mc.player.getPosition());
            EnumFacing dir = getRotationAxis(properties, mc.player.getPosition());
            axis.x = (float) dir.getFrontOffsetX();
            axis.y = (float) dir.getFrontOffsetY();
            axis.z = (float) dir.getFrontOffsetZ();

            myPhi = properties.orbitalPhi;
            myTheta = properties.orbitTheta;
            myRotationalPhi = properties.rotationalPhi;
            myPrevOrbitalTheta = properties.prevOrbitalTheta;
            hasRings = properties.hasRings();
            ringColor = properties.ringColor;

            children = new LinkedList<>();
            for (Integer i : properties.getChildPlanets()) {
                children.add(DimensionManager.getInstance().getDimensionProperties(i));
            }

            solarOrbitalDistance = properties.getSolarOrbitalDistance();


            if (isMoon = properties.isMoon()) {
                parentProperties = properties.getParentProperties();
                planetOrbitalDistance = properties.getParentOrbitalDistance();
                parentHasRings = parentProperties.hasRings;
                parentRingColor = parentProperties.ringColor;
            }

            sunColor = planetaryProvider.getSunColor(mc.player.getPosition());
            primaryStar = properties.getStar();
            if (primaryStar != null) {
                sunSize = properties.getStar().getSize();
            } else
                primaryStar = DimensionManager.getInstance().getStar(0);
            if (world.provider.getDimension() == ARConfiguration.getCurrentConfig().spaceDimId) {
                isWarp = properties.getParentPlanet() == SpaceObjectManager.WARPDIMID;
                if (isWarp) {
                    SpaceStationObject station = (SpaceStationObject) SpaceObjectManager.getSpaceManager().getSpaceStationFromBlockCoords(mc.player.getPosition());
                    travelDirection = station.getForwardDirection();
                }
            }
        }
        else if (DimensionManager.getInstance().isDimensionCreated(mc.world.provider.getDimension())) {

            properties = DimensionManager.getInstance().getDimensionProperties(mc.world.provider.getDimension());

            atmosphere = properties.getAtmosphereDensityAtHeight(mc.getRenderViewEntity().posY);//planetaryProvider.getAtmosphereDensityFromHeight(mc.getRenderViewEntity().posY, mc.player.getPosition());
            EnumFacing dir = getRotationAxis(properties, mc.player.getPosition());
            axis.x = (float) dir.getFrontOffsetX();
            axis.y = (float) dir.getFrontOffsetY();
            axis.z = (float) dir.getFrontOffsetZ();

            myPhi = properties.orbitalPhi;
            myTheta = properties.orbitTheta;
            myRotationalPhi = properties.rotationalPhi;
            myPrevOrbitalTheta = properties.prevOrbitalTheta;
            hasRings = properties.hasRings();
            ringColor = properties.ringColor;

            children = new LinkedList<>();
            for (Integer i : properties.getChildPlanets()) {
                children.add(DimensionManager.getInstance().getDimensionProperties(i));
            }

            solarOrbitalDistance = properties.getSolarOrbitalDistance();


            if (isMoon = properties.isMoon()) {
                parentProperties = properties.getParentProperties();
                planetOrbitalDistance = properties.getParentOrbitalDistance();
                parentHasRings = parentProperties.hasRings;
                parentHasATM = parentProperties.hasAtmosphere();
                parentRingColor = parentProperties.ringColor;
            }

            float[] sunColorFloat = properties.getSunColor();
            sunColor = new Vec3d(sunColorFloat[0], sunColorFloat[1], sunColorFloat[2]);//planetaryProvider.getSunColor(mc.player.getPosition());
            primaryStar = properties.getStar();
            if (primaryStar != null) {
                sunSize = properties.getStar().getSize();
            } else
                primaryStar = DimensionManager.getInstance().getStar(0);
            if (world.provider.getDimension() == ARConfiguration.getCurrentConfig().spaceDimId) {
                isWarp = properties.getParentPlanet() == SpaceObjectManager.WARPDIMID;
                if (isWarp) {
                    SpaceStationObject station = (SpaceStationObject) SpaceObjectManager.getSpaceManager().getSpaceStationFromBlockCoords(mc.player.getPosition());
                    travelDirection = station.getForwardDirection();
                }
            }
        }
        else {
            children = new LinkedList<>();
            isMoon = false;
            atmosphere = DimensionManager.overworldProperties.getAtmosphereDensityAtHeight(mc.getRenderViewEntity().posY);
            solarOrbitalDistance = DimensionManager.overworldProperties.orbitalDist;
            sunColor = new Vec3d(1, 1, 1);
            primaryStar = DimensionManager.overworldProperties.getStar();
            properties = DimensionManager.overworldProperties;
        }

        currentplanetphi = myPhi;

        GlStateManager.disableTexture2D();
        Vec3d vec3 = Minecraft.getMinecraft().world.getSkyColor(this.mc.getRenderViewEntity(), partialTicks);
        float f1 = (float) vec3.x;
        float f2 = (float) vec3.y;
        float f3 = (float) vec3.z;
        float f6;

        if (this.mc.gameSettings.anaglyph) {
            float f4 = (f1 * 30.0F + f2 * 59.0F + f3 * 11.0F) / 100.0F;
            float f5 = (f1 * 30.0F + f2 * 70.0F) / 100.0F;
            f6 = (f1 * 30.0F + f3 * 70.0F) / 100.0F;
            f1 = f4;
            f2 = f5;
            f3 = f6;
        }

        //Simulate atmospheric thickness, vaugely
        //This is done like this to prevent problems with superbright atmospheres on low-atmosphere planets
        //Plus you couldn't see stars during the day anyway
        int atmosphereInt = properties.getAtmosphereDensity();
//        System.out.println("before:"+f1+":"+f2+":"+f3);
        f1 = atmosphereInt < 1 ? 0 : (float) Math.pow(f1, Math.sqrt(Math.max(atmosphere, 0.0001)));
        f2 = atmosphereInt < 1 ? 0 : (float) Math.pow(f2, Math.sqrt(Math.max(atmosphere, 0.0001)));
        f3 = atmosphereInt < 1 ? 0 : (float) Math.pow(f3, Math.sqrt(Math.max(atmosphere, 0.0001)));

        f1*=Math.min(1,atmosphere);
        f2*=Math.min(1,atmosphere);
        f3*=Math.min(1,atmosphere);

        skycolor[0] = f1;
        skycolor[1] = f2;
        skycolor[2] = f3;

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GlStateManager.enableFog();
        GlStateManager.color(f1, f2, f3);
        GL11.glCallList(this.glSkyList);
        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        RenderHelper.disableStandardItemLighting();
        float[] afloat = mc.world.provider.calcSunriseSunsetColors(celestialAngle, partialTicks);
        float f7;
        float f8;
        float f9;
        float f10;

        if (afloat != null) {
            GlStateManager.disableTexture2D();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            GL11.glPushMatrix();
            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(MathHelper.sin(mc.world.getCelestialAngleRadians(partialTicks)) < 0.0F ? 180.0F : 0.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotated(90.0F - myRotationalPhi, 0.0F, 0.0F, 1.0F);

            //Sim atmospheric thickness
            f6 = afloat[0];
            f7 = afloat[1];
            f8 = afloat[2];
            float f11;

            if (this.mc.gameSettings.anaglyph) {
                f9 = (f6 * 30.0F + f7 * 59.0F + f8 * 11.0F) / 100.0F;
                f10 = (f6 * 30.0F + f7 * 70.0F) / 100.0F;
                f11 = (f6 * 30.0F + f8 * 70.0F) / 100.0F;
                f6 = f9;
                f7 = f10;
                f8 = f11;
            }

            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(0.0D, 100.0D, 0.0D).color(f6, f7, f8, afloat[3] * atmosphere).endVertex();
            byte b0 = 16;

            for (int j = 0; j <= b0; ++j) {
                f11 = (float) j * (float) Math.PI * 2.0F / (float) b0;
                float f12 = MathHelper.sin(f11);
                float f13 = MathHelper.cos(f11);
                buffer.pos(f12 * 120.0F, f13 * 120.0F, -f13 * 40.0F * afloat[3]).color(afloat[0], afloat[1], afloat[2], 0.0F).endVertex();
            }

            Tessellator.getInstance().draw();
            GL11.glPopMatrix();
            GlStateManager.shadeModel(GL11.GL_FLAT);
        }
        shadowColorMultiplier = new float[]{f1, f2, f3};

        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);

        GL11.glPushMatrix();


        if (atmosphere > 0)
            f6 = 1.0F - (mc.world.getRainStrength(partialTicks) * (atmosphere / 100f));
        else
            f6 = 1f;

        f7 = 0.0F;
        f8 = 0.0F;
        f9 = 0.0F;
        GlStateManager.color(1.0F, 1.0F, 1.0F, f6);
        GL11.glTranslatef(f7, f8, f9);
        GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);

        float multiplier = (2 - atmosphere) / 2f;//atmosphere > 1 ? (2-atmosphere) : 1f;
        if (mc.world.isRainingAt(mc.player.getPosition().add(0, 199, 0)))
            multiplier *= 1 - mc.world.getRainStrength(partialTicks);

        GL11.glRotatef((float) myRotationalPhi, 0f, 1f, 0f);

        //Draw Rings
        if (hasRings) {
            GL11.glPushMatrix();
            GL11.glRotatef(90f, 0f, 1f, 0f);

            f10 = 100;
            double ringDist = 0;
            //mc.renderEngine.bindTexture(DimensionProperties.planetRings);
            mc.renderEngine.bindTexture(DimensionProperties.planetRingsNew);

            GL11.glRotated(70, 1, 0, 0);
            GL11.glTranslated(0, -10, 0);

            GlStateManager.color(ringColor[0], ringColor[1], ringColor[2], multiplier);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(f10, ringDist, -f10).tex(1.0D, 0.0D).endVertex();
            buffer.pos(-f10, ringDist, -f10).tex(0.0D, 0.0D).endVertex();
            buffer.pos(-f10, ringDist, f10).tex(0.0D, 1.0D).endVertex();
            buffer.pos(f10, ringDist, f10).tex(1.0D, 1.0D).endVertex();
            Tessellator.getInstance().draw();
            GL11.glPopMatrix();

            /*
            GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
            GL11.glPushMatrix();

            GL11.glRotatef(90f, 0f, 1f, 0f);
            GL11.glRotated(70, 1, 0, 0);
            GL11.glRotatef(isWarp ? 0 : celestialAngle * 360.0F, 0, 1, 0);
            GL11.glTranslated(0, -10, 0);

            mc.renderEngine.bindTexture(DimensionProperties.planetRingShadow);
            GlStateManager.color(0f, 0f, 0f, multiplier);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(f10, ringDist, -f10).tex(1.0D, 0.0D).endVertex();
            buffer.pos(-f10, ringDist, -f10).tex(0.0D, 0.0D).endVertex();
            buffer.pos(-f10, ringDist, f10).tex(0.0D, 1.0D).endVertex();
            buffer.pos(f10, ringDist, f10).tex(1.0D, 1.0D).endVertex();
            Tessellator.getInstance().draw();
            GL11.glPopMatrix();
             */

            GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
        }

        GlStateManager.disableTexture2D();
        //This determines whether stars should come out regardless of thickness of atmosphere, as that is factored in later
        // - it checks if the colors of the sky are so close to black that you'd see stars, or if the atmosphere is zero and so no one gives a damn
        float f18 = mc.world.getStarBrightness(partialTicks) * f6;//((atmosphere == 0 || (f1 < 0.09 && f2 < 0.09 && f3 < 0.09)) ? 1 : 0);// - (atmosphere > 1 ? atmosphere - 1 : 0);


        float starAlpha = 1-((1-f18)*atmosphere);
        //System.out.println(starAlpha+":"+f18+":"+atmosphere);

        //if (f18 > 0.0F) {
        if (true){
            GlStateManager.color(1, 1, 1, 1);
            GL11.glPushMatrix();
            if (isWarp) {
                for (int i = -3; i < 5; i++) {
                    GL11.glPushMatrix();
                    double magnitude = i * -100 + (((System.currentTimeMillis()) + 50) % 2000) / 20f;
                    GL11.glTranslated(-travelDirection.getFrontOffsetZ() * magnitude, 0, travelDirection.getFrontOffsetX() * magnitude);
                    GL11.glCallList(this.starGLCallList);
                    GL11.glPopMatrix();
                }
                //GL11.glTranslated(((System.currentTimeMillis()/10) + 50) % 100, 0, 0);

            } else {
                GL11.glColor4f(1,1,1,starAlpha);
                GL11.glCallList(this.starGLCallList);
                //Extra stars for low ATM
                if (atmosphere < 0.5) {
                    GL11.glColor4f(1,1,1,starAlpha/2);
                    GL11.glPushMatrix();
                    GL11.glRotatef(-90, 0, 1, 0);
                    GL11.glCallList(this.starGLCallList);
                    GL11.glPopMatrix();
                }
                if (atmosphere < 0.25) {
                    GL11.glColor4f(1,1,1,starAlpha/4);
                    GL11.glPushMatrix();
                    GL11.glRotatef(90, 0, 1, 0);
                    GL11.glCallList(this.starGLCallList);
                    GL11.glPopMatrix();
                }
                GlStateManager.color(1, 1, 1, 1);

            }
            GL11.glPopMatrix();
        }
        GlStateManager.enableTexture2D();


        mc.renderEngine.bindTexture(TextureResources.locationSunPng);

        //--------------------------- Draw the suns --------------------
        if (!isWarp) {
            if (parentProperties == null || !parentProperties.isStar()) {
                xrotangle = ((float) (properties.getSolarTheta() * 180f / Math.PI) % 360f); // for black hole disk
                //System.out.println(xrotangle+":"+properties.getSolarTheta());
                drawStarAndSubStars(buffer, primaryStar, properties, solarOrbitalDistance, sunSize, sunColor, multiplier);
                xrotangle = 0;
            }
        }


        //For these parts only render if the atmosphere is below a certain threshold (SHP atmosphere)
        if (DimensionProperties.AtmosphereTypes.SUPERHIGHPRESSURE.denserThan(DimensionProperties.AtmosphereTypes.getAtmosphereTypeFromValue((int) (100 * atmosphere)))) {
            //Render the parent planet
            if (isMoon) {
                GL11.glPushMatrix();

                //Do a whole lotta math to figure out where the parent planet is supposed to be
                //That 0.3054325f is there because we need to do adjustments for some ^$%^$% reason and it's consistently off by 17.5 degrees
                float planetPositionTheta = AstronomicalBodyHelper.getParentPlanetThetaFromMoon(properties.rotationalPeriod, properties.orbitalDist, parentProperties.gravitationalMultiplier, myTheta, properties.baseOrbitTheta);

                GL11.glRotatef((float) myPhi, 0f, 0f, 1f);
                GL11.glRotatef(planetPositionTheta, 1f, 0f, 0f);

                float phiAngle = (float) ((myPhi) * Math.PI / 180f);

                //Close enough approximation, I missed something but seems to off by no more than 30*
                //Nobody will look
                double x = MathHelper.sin(phiAngle) * MathHelper.cos((float) myTheta);
                double y = -MathHelper.sin((float) myTheta);
                double rotation = -Math.PI / 2f + Math.atan2(x, y) - (myTheta - Math.PI) * MathHelper.sin(phiAngle);

                if (parentHasRings) {
                    //Semihacky rotation stuff to keep rings synced to a different rotation than planet in the sky
                    xrotangle = -planetPositionTheta + ((float) (myTheta * 180f / Math.PI) % 360f);
                    //System.out.println("r:"+xrotangle);
                }

                shadowColorMultiplier = new float[]{f1, f2, f3};

                //System.out.println("draw moon (renderplanet");
                renderPlanet(buffer, parentProperties, planetOrbitalDistance, multiplier, rotation, false, parentHasRings, (float) Math.pow(parentProperties.getGravitationalMultiplier(), 0.4), shadowColorMultiplier, 1);
                xrotangle = 0;
                GL11.glPopMatrix();
            }

            //This needs to exist specifically for init purposes
            //The overworld literally breaks without it
            shadowColorMultiplier[0] = 1.000001f * shadowColorMultiplier[0];

            for (DimensionProperties moons : children) {
                GL11.glPushMatrix();

                float planetPositionTheta = (float) ((partialTicks * moons.orbitTheta + ((1 - partialTicks) * moons.prevOrbitalTheta)) * 180F / Math.PI);
                float flippedPlanetPositionTheta = 360 - planetPositionTheta;

                GL11.glRotatef((float) moons.orbitalPhi, 0f, 0f, 1f);
                GL11.glRotated(planetPositionTheta, 1f, 0f, 0f);

                //Close enough approximation, I missed something but seems to off by no more than 30*
                //Nobody will look
                float phiAngle = (float) ((moons.orbitalPhi) * Math.PI / 180f);
                double x = -MathHelper.sin(phiAngle) * MathHelper.cos((float) moons.orbitTheta);
                double y = MathHelper.sin((float) moons.orbitTheta);
                double rotation = (-Math.PI / 2f + Math.atan2(x, y) - (moons.orbitTheta - Math.PI) * MathHelper.sin(phiAngle)) + Math.PI;

                shadowColorMultiplier = new float[]{f1, f2, f3};
                renderPlanet(buffer, moons, moons.getParentOrbitalDistance(), multiplier, rotation, moons.hasAtmosphere(), moons.hasRings, (float) Math.pow(moons.gravitationalMultiplier, 0.4), shadowColorMultiplier, 1);
                GL11.glPopMatrix();
            }
        }

        GlStateManager.enableFog();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();

        GL11.glPopMatrix();

        GlStateManager.enableTexture2D();

        mc.renderEngine.bindTexture(asteroid1);
        GlStateManager.color(1, 1, 1);
        GL11.glCallList(this.glSkyList3);

        GL11.glPushMatrix();
        GL11.glRotatef(90, 0.2f, 0.8f, 0);
        mc.renderEngine.bindTexture(asteroid2);
        GL11.glCallList(this.glSkyList3);
        GL11.glRotatef(90, 0.2f, 0.8f, 0);
        mc.renderEngine.bindTexture(asteroid3);
        GL11.glCallList(this.glSkyList3);
        GL11.glPopMatrix();

        GL11.glDepthMask(true);


        //RocketEventHandler.onPostWorldRender(partialTicks);
        //Fix player/items going transparent
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 0, 0);
    }
    protected void drawStarAndSubStars(BufferBuilder buffer, StellarBody sun, DimensionProperties properties, int solarOrbitalDistance, float sunSize, Vec3d sunColor, float multiplier) {
        drawStar(buffer, sun, properties, solarOrbitalDistance, sunSize, sunColor, multiplier);

        List<StellarBody> subStars = sun.getSubStars();

        if (subStars != null && !subStars.isEmpty()) {
            GL11.glPushMatrix();
            float phaseInc = 360f / subStars.size();

            for (StellarBody subStar : subStars) {
                GL11.glRotatef(phaseInc, 0, 1, 0);
                GL11.glPushMatrix();

                GL11.glRotatef(subStar.getStarSeparation() * AstronomicalBodyHelper.getBodySizeMultiplier(solarOrbitalDistance), 1, 0, 0);
                float[] color = subStar.getColor();
                drawStar(buffer, subStar, properties, solarOrbitalDistance, subStar.getSize(), new Vec3d(color[0], color[1], color[2]), multiplier);
                GL11.glPopMatrix();
            }
            GL11.glPopMatrix();
        }
    }
    protected ResourceLocation getTextureForPlanet(DimensionProperties properties) {
        return properties.getPlanetIcon();
    }

    protected ResourceLocation getTextureForPlanetLEO(DimensionProperties properties) {
        return properties.getPlanetIcon();
    }

    protected EnumFacing getRotationAxis(DimensionProperties properties, BlockPos pos) {
        return EnumFacing.EAST;
    }


    protected void renderPlanet(BufferBuilder buffer, DimensionProperties properties, float planetOrbitalDistance, float alphaMultiplier, double shadowAngle, boolean hasAtmosphere, boolean hasRing, float gravitationalMultiplier, float[] shadowColorMultiplier, float alphaMultiplier2) {
        renderPlanet2(buffer, properties, 20f * AstronomicalBodyHelper.getBodySizeMultiplier(planetOrbitalDistance) * gravitationalMultiplier, alphaMultiplier, shadowAngle, hasRing, shadowColorMultiplier, alphaMultiplier2);
    }

    protected void renderPlanet2(BufferBuilder buffer, DimensionProperties properties, float size, float alphaMultiplier, double shadowAngle, boolean hasRing, float[] shadowColorMultiplier, float alphaMultiplier2) {
        ResourceLocation icon = getTextureForPlanet(properties);
        boolean hasAtmosphere = properties.hasAtmosphere();
        boolean hasDecorators = properties.hasDecorators();
        boolean gasGiant = properties.isGasGiant();
        float[] skyColor = properties.skyColor;
        float[] ringColor = properties.ringColor;
     RenderPlanetarySky.   renderPlanetPubHelper(buffer, icon, 0, 0, -20, size * 0.2f, alphaMultiplier, shadowAngle, hasAtmosphere, skyColor, ringColor, gasGiant, hasRing, properties.ringAngle, hasDecorators, shadowColorMultiplier, alphaMultiplier2);
    }


    protected Vector3F<Float> getRotateAxis() {
        return axis;
    }

    public void renderSphere(double x, double y, double z, float radius, int slices, int stacks) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        for(int i = 0; i < slices; i++) {
            for(int j = 0; j < stacks; j++) {
                double firstLong = 2 * Math.PI * (i / (double)slices);
                double secondLong = 2 * Math.PI * ((i + 1) / (double)slices);
                double firstLat = Math.PI * (j / (double)stacks) - Math.PI / 2;
                double secondLat = Math.PI * ((j + 1) / (double)stacks) - Math.PI / 2;

                bufferBuilder.pos(x + radius * Math.cos(firstLat) * Math.cos(firstLong), y + radius * Math.sin(firstLat), z + radius * Math.cos(firstLat) * Math.sin(firstLong)).tex(0.0D, 0.0D).endVertex();
                bufferBuilder.pos(x + radius * Math.cos(secondLat) * Math.cos(firstLong), y + radius * Math.sin(secondLat), z + radius * Math.cos(secondLat) * Math.sin(firstLong)).tex(1.0D, 0.0D).endVertex();
                bufferBuilder.pos(x + radius * Math.cos(secondLat) * Math.cos(secondLong), y + radius * Math.sin(secondLat), z + radius * Math.cos(secondLat) * Math.sin(secondLong)).tex(1.0D, 1.0D).endVertex();
                bufferBuilder.pos(x + radius * Math.cos(firstLat) * Math.cos(secondLong), y + radius * Math.sin(firstLat), z + radius * Math.cos(firstLat) * Math.sin(secondLong)).tex(0.0D, 1.0D).endVertex();
            }
        }

        tessellator.draw();
    }
    protected void drawStar(BufferBuilder buffer, StellarBody sun, DimensionProperties properties, int solarOrbitalDistance, float sunSize, Vec3d sunColor, float multiplier) {
        if (sun != null && sun.isBlackHole()) {
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01f);
            float f10;
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 30, 0);


            GL11.glDisable(GL11.GL_BLEND);
            GlStateManager.depthMask(true);

            GL11.glPushMatrix();
            GL11.glTranslatef(0, 100, 0);
            f10 = sunSize * 2f * AstronomicalBodyHelper.getBodySizeMultiplier(solarOrbitalDistance);

            mc.renderEngine.bindTexture(TextureResources.locationWhitePng);
            GlStateManager.disableCull();
            GlStateManager.color(skycolor[0], skycolor[1], skycolor[2]); // Set the color
            renderSphere(0, 0, 0, f10, 16, 16); // Draw the sphere
            GlStateManager.enableCull();
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDepthMask(false);
            GL11.glPopMatrix();
/*
            GL11.glPushMatrix();
            mc.renderEngine.bindTexture(TextureResources.locationBlackHole);
            GL11.glTranslatef(0, 100, 0);
            f10 = sunSize * 2f * AstronomicalBodyHelper.getBodySizeMultiplier(solarOrbitalDistance);
            //float scale = 1 ;
            //GL11.glRotatef(phase, 0, 1, 0);
            //GL11.glScaled(scale, scale, scale);
            GlStateManager.color((float) 1, (float) .5, (float) .4, 1f);

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            //multiplier = 2;
            buffer.pos(-f10, 0.0D, -f10).tex(0.0D, 0.0D).endVertex();
            buffer.pos(f10, 0.0D, -f10).tex(1.0D, 0.0D).endVertex();
            buffer.pos(f10, 0.0D, f10).tex(1.0D, 1.0D).endVertex();
            buffer.pos(-f10, 0.0D, f10).tex(0.0D, 1.0D).endVertex();
            Tessellator.getInstance().draw();
            GL11.glPopMatrix();


            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDepthMask(false);


            GL11.glPushMatrix();
            mc.renderEngine.bindTexture(TextureResources.locationBlackHoleBorder);
            GL11.glTranslatef( 0, 99.8F, 0);
            //GL11.glRotatef(phase, 0, 1, 0);
            float scale = 1.1F;
            GL11.glScaled(scale, scale, scale);
            GlStateManager.color((float) 1, (float) .5, (float) .4, 1f);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            //multiplier = 2;
            buffer.pos(-f10, 0.0D, -f10).tex(0.0D, 0.0D).endVertex();
            buffer.pos(f10, 0.0D, -f10).tex(1.0D, 0.0D).endVertex();
            buffer.pos(f10, 0.0D, f10).tex(1.0D, 1.0D).endVertex();
            buffer.pos(-f10, 0.0D, f10).tex(0.0D, 1.0D).endVertex();
            Tessellator.getInstance().draw();
            GL11.glPopMatrix();
*/
            float diskangle = sun.diskAngle;

            float m = -xrotangle;
            while (m > 360)
                m-=360;
            while (m < 0)
                m+=360;
            //Render accretion disk
            mc.renderEngine.bindTexture(TextureResources.locationAccretionDiskDense);
            GlStateManager.depthMask(false);

            float speedMult = 5;
            GlStateManager.disableCull();


            GL11.glPushMatrix();
            GL11.glTranslatef(0, 100, 0);
            GL11.glRotatef(90, 0f, 1f, 0f);
            //GL11.glRotatef(m, 1f, 0f, 0f);
            //GL11.glRotatef(diskangle, 0, 0, 1);
            //GL11.glRotatef(90, 1, 0, 0);
            GL11.glRotatef((System.currentTimeMillis() % (int) (360 * 360 * speedMult)) / (360f * speedMult), 0, 1, 0);

            GlStateManager.color((float) 1, (float) .7, (float) .55, 1f);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            f10 = sunSize * 6.5f * AstronomicalBodyHelper.getBodySizeMultiplier(solarOrbitalDistance);
            buffer.pos(-f10, 0.0D, -f10).tex(0.0D, 0.0D).endVertex();
            buffer.pos(f10, 0.0D, -f10).tex(1.0D, 0.0D).endVertex();
            buffer.pos(f10, 0.0D, f10).tex(1.0D, 1.0D).endVertex();
            buffer.pos(-f10, 0.0D, f10).tex(0.0D, 1.0D).endVertex();
            Tessellator.getInstance().draw();
            GL11.glPopMatrix();


            mc.renderEngine.bindTexture(TextureResources.locationAccretionDisk);

            for (int i = 0; i < 3; i++) {
                speedMult = ((0) * 1.01f + 1)/0.1F;
                GL11.glPushMatrix();
                GL11.glTranslatef(0, 100.01f, 0);
                GL11.glRotatef((float) currentplanetphi, 0f, 1f, 0f);
                GL11.glRotatef(m, 1f, 0f, 0f);
                GL11.glRotatef(diskangle, 0, 0, 1);
                GL11.glRotatef((System.currentTimeMillis() % (int) (speedMult * 36000)) / (100f * speedMult), 0, 1, 0);

                // make every disks angle slightly different
                GL11.glRotatef(120*i, 0, 1, 0);
                GL11.glRotatef(0.5f, 1, 0, 0);

                GlStateManager.color((float) 1, (float) .5, (float) .4, 0.3f);
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
                f10 = sunSize * 40f * AstronomicalBodyHelper.getBodySizeMultiplier(solarOrbitalDistance);
                buffer.pos(-f10, 0.0D, -f10).tex(0.0D, 0.0D).endVertex();
                buffer.pos(f10, 0.0D, -f10).tex(1.0D, 0.0D).endVertex();
                buffer.pos(f10, 0.0D, f10).tex(1.0D, 1.0D).endVertex();
                buffer.pos(-f10, 0.0D, f10).tex(0.0D, 1.0D).endVertex();
                Tessellator.getInstance().draw();
                GL11.glPopMatrix();


                GL11.glPushMatrix();

                GL11.glTranslatef(0, 100f, 0);
                GL11.glRotatef((float) currentplanetphi, 0f, 1f, 0f);
                GL11.glRotatef(m, 1f, 0f, 0f);
                GL11.glRotatef(diskangle, 0, 0, 1);
                GL11.glRotatef((System.currentTimeMillis() % (int) (speedMult * 360 * 50)) / (50f * speedMult), 0, 1, 0);
                // make every disks angle slightly different
                GL11.glRotatef(120*i, 0, 1, 0);
                GL11.glRotatef(0.5f, 1, 0, 0);

                GlStateManager.color((float) 0.8, (float) .7, (float) .4, 0.3f);
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
                f10 = sunSize * 30f * AstronomicalBodyHelper.getBodySizeMultiplier(solarOrbitalDistance);
                //multiplier = 2;
                buffer.pos(-f10, 0.0D, -f10).tex(0.0D, 0.0D).endVertex();
                buffer.pos(f10, 0.0D, -f10).tex(1.0D, 0.0D).endVertex();
                buffer.pos(f10, 0.0D, f10).tex(1.0D, 1.0D).endVertex();
                buffer.pos(-f10, 0.0D, f10).tex(0.0D, 1.0D).endVertex();
                Tessellator.getInstance().draw();
                GL11.glPopMatrix();

                GL11.glPushMatrix();

                GL11.glTranslatef(0, 99.99f, 0);
                GL11.glRotatef((float) currentplanetphi, 0f, 1f, 0f);
                GL11.glRotatef(m, 1f, 0f, 0f);
                GL11.glRotatef(diskangle, 0, 0, 1);
                GL11.glRotatef((System.currentTimeMillis() % (int) (speedMult * 360 * 25)) / (25f * speedMult), 0, 1, 0);
                // make every disks angle slightly different
                GL11.glRotatef(120*i, 0, 1, 0);
                GL11.glRotatef(0.5f, 1, 0, 0);

                GlStateManager.color((float) 0.2, (float) .4, (float) 1, 0.3f);
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
                f10 = sunSize * 15f * AstronomicalBodyHelper.getBodySizeMultiplier(solarOrbitalDistance);
                //multiplier = 2;
                buffer.pos(-f10, 0.0D, -f10).tex(0.0D, 0.0D).endVertex();
                buffer.pos(f10, 0.0D, -f10).tex(1.0D, 0.0D).endVertex();
                buffer.pos(f10, 0.0D, f10).tex(1.0D, 1.0D).endVertex();
                buffer.pos(-f10, 0.0D, f10).tex(0.0D, 1.0D).endVertex();
                Tessellator.getInstance().draw();
                GL11.glPopMatrix();




            }

            GlStateManager.depthMask(true);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            GlStateManager.depthMask(false);
            GL11.glPopMatrix();
            GlStateManager.enableCull();


        } else {
            mc.renderEngine.bindTexture(TextureResources.locationSunPng);
            //Set sun color and distance
            GlStateManager.color((float) sunColor.x, (float) sunColor.y, (float) sunColor.z, Math.min((multiplier) * 2f, 1f));
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            float f10 = sunSize * 15f * AstronomicalBodyHelper.getBodySizeMultiplier(solarOrbitalDistance);
            //multiplier = 2;
            buffer.pos(-f10, 120.0D, -f10).tex(0.0D, 0.0D).endVertex();
            buffer.pos(f10, 120.0D, -f10).tex(1.0D, 0.0D).endVertex();
            buffer.pos(f10, 120.0D, f10).tex(1.0D, 1.0D).endVertex();
            buffer.pos(-f10, 120.0D, f10).tex(0.0D, 1.0D).endVertex();
            Tessellator.getInstance().draw();
        }
    }
}
