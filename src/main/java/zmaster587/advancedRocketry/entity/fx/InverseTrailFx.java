package zmaster587.advancedRocketry.entity.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import zmaster587.advancedRocketry.client.render.DelayedParticleRenderingEventHandler;
import zmaster587.advancedRocketry.event.RocketEventHandler;

import java.util.List;

public class InverseTrailFx extends Particle {
    protected ResourceLocation icon;


    public InverseTrailFx(World world, double x,
                          double y, double z, double motx, double moty, double motz) {
        super(world, x, y, z, motx, moty, motz);

        DelayedParticleRenderingEventHandler.TrailFxParticles.add(this);

        float chroma = this.rand.nextFloat() * 0.2f;
        this.particleRed = .8F + chroma;
        this.particleGreen = .8F + chroma;
        this.particleBlue = .8F + chroma;
        this.setSize(0.12F, 0.12F);
        this.particleMaxAge = (int) (100.0D);
        this.particleScale = (float) (this.rand.nextFloat() * 0.6F + 6F + Math.pow(1.04f, this.particleMaxAge));
        this.motionX = -motx;
        this.motionY = -moty;
        this.motionZ = -motz;

        this.particleAlpha = 0;
        icon = new ResourceLocation("advancedrocketry:textures/particle/soft.png");


        this.prevPosX = this.posX = x + motx * this.particleMaxAge;
        this.prevPosY = this.posY = y + moty * this.particleMaxAge;
        this.prevPosZ = this.posZ = z + motz * this.particleMaxAge;
    }

    float partialTicks;
    float rotationX;
    float rotationZ;
    float rotationYZ;
    float rotationXY;
    float rotationXZ;


    @Override
    public void renderParticle(BufferBuilder worldRendererIn, Entity entityIn,
                                float partialTicks, float rotationX, float rotationZ,
                                float rotationYZ, float rotationXY, float rotationXZ) {

        this. partialTicks = partialTicks;
        this. rotationX = rotationX;
        this. rotationZ = rotationZ;
        this. rotationYZ = rotationYZ;
        this. rotationXY = rotationXY;
        this. rotationXZ = rotationXZ;

    }

    public void renderParticle2(BufferBuilder worldRendererIn) {



        float f11 = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) partialTicks - interpPosX);
        float f12 = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) partialTicks - interpPosY);
        float f13 = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) partialTicks - interpPosZ);
        float f10 = 0.25F * this.particleScale;

        int i = (240+this.getBrightnessForRender(partialTicks))/2;
        int j = i >> 16 & 65535;
        int k = i & 65535;


        Minecraft.getMinecraft().getTextureManager().bindTexture(icon);


        worldRendererIn.pos(f11 - rotationX * f10 - rotationXY * f10, f12 - rotationZ * f10, f13 - rotationYZ * f10 - rotationXZ * f10).tex(1, 1).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j, k).endVertex();
        worldRendererIn.pos(f11 - rotationX * f10 + rotationXY * f10, f12 + rotationZ * f10, f13 - rotationYZ * f10 + rotationXZ * f10).tex(1, 0).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j, k).endVertex();
        worldRendererIn.pos(f11 + rotationX * f10 + rotationXY * f10, f12 + rotationZ * f10, f13 + rotationYZ * f10 + rotationXZ * f10).tex(0, 0).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j, k).endVertex();
        worldRendererIn.pos(f11 + rotationX * f10 - rotationXY * f10, f12 - rotationZ * f10, f13 + rotationYZ * f10 - rotationXZ * f10).tex(0, 1).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j, k).endVertex();


    }


    @Override
    public int getFXLayer() {
        return 0;
    }

    @Override
    public boolean shouldDisableDepth() {
        return true;
    }

    public static void renderAll(List<InverseTrailFx> TrailFxParticles){
        // Get the BufferBuilder for rendering
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        GlStateManager.disableAlpha(); // Ensure alpha test is disabled

        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        //GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);

        // Render custom particles
        for (InverseTrailFx particle : TrailFxParticles) {
            particle.renderParticle2(buffer);
        }

        Tessellator.getInstance().draw();

        GlStateManager.depthMask(true);
    }

    @Override
    public void onUpdate() {

    }
    public void onUpdate2() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        //Change color and alpha over lifespan
        this.particleAlpha = 0.25f * this.particleAge / (float) this.particleMaxAge;
        this.particleScale /= 1.02f;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
        }

        this.setPosition(posX + this.motionX, posY + this.motionY, posZ + this.motionZ);
    }
}