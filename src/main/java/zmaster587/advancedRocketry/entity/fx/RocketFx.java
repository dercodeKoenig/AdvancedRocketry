package zmaster587.advancedRocketry.entity.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import zmaster587.advancedRocketry.client.render.DelayedParticleRenderingEventHandler;
import zmaster587.advancedRocketry.event.RocketEventHandler;

import java.util.List;

import static java.lang.Math.min;

public class RocketFx extends Particle {

    public static final ResourceLocation icon = new ResourceLocation("advancedrocketry:textures/particle/soft2.png");

    float alpha = 0.45f;


    float max_lt_increase = 20.0f;
    int max_engines_for_calculation = 32;

    //increase x-z motion
    public void register_additional_engines(int n){
        float enginepx = min(1,n/(float)max_engines_for_calculation);
        this.particleMaxAge += (int) (enginepx * max_lt_increase *(rand.nextFloat()));
        //System.out.println("px:"+enginepx+":"+n);
    }

    public RocketFx(World world, double x,
                    double y, double z, double motx, double moty, double motz, float scale) {
        super(world, x, y, z, motx, moty, motz);

        DelayedParticleRenderingEventHandler.RocketFxParticles.add(this);

        this.prevPosX = this.posX = x;
        this.prevPosY = this.posY = y;
        this.prevPosZ = this.posZ = z;
        this.particleAlpha = alpha;
        this.particleRed = 0.9F + this.rand.nextFloat() / 10f;
        this.particleGreen = 0.8F + this.rand.nextFloat() / 5f;
        this.particleBlue = 0.6F;
        this.setSize(0.12F * scale, 0.12F * scale);
        this.particleScale *= (this.rand.nextFloat() * 0.6F + 6F) * scale;
        this.motionX = motx;
        this.motionY = moty;
        this.motionZ = motz;
        this.particleMaxAge = (int) ((int) (8.0D / (Math.random() * 0.8D + 0.6D))*1.0);
    }

    public RocketFx(World world, double x,
                    double y, double z, double motx, double moty, double motz) {
        this(world, x, y, z, motx, moty, motz, 1.0f);
    }

    @Override
    public int getFXLayer() {
        return 0;
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


        float f;
        float f1;
        float f2;
        float f3;
        float f4 = 0.1F * this.particleScale;

        float f5 = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) partialTicks - interpPosX);
        float f6 = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) partialTicks - interpPosY);
        float f7 = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) partialTicks - interpPosZ);
        int i = (240+this.getBrightnessForRender(partialTicks))/2;
        int j = i >> 16 & 65535;
        int k = i & 65535;
        Vec3d[] avec3d = new Vec3d[]{new Vec3d(-rotationX * f4 - rotationXY * f4, -rotationZ * f4, -rotationYZ * f4 - rotationXZ * f4), new Vec3d(-rotationX * f4 + rotationXY * f4, rotationZ * f4, -rotationYZ * f4 + rotationXZ * f4), new Vec3d(rotationX * f4 + rotationXY * f4, rotationZ * f4, rotationYZ * f4 + rotationXZ * f4), new Vec3d(rotationX * f4 - rotationXY * f4, -rotationZ * f4, rotationYZ * f4 - rotationXZ * f4)};

        if (this.particleAngle != 0.0F) {
            float f8 = this.particleAngle + (this.particleAngle - this.prevParticleAngle) * partialTicks;
            float f9 = MathHelper.cos(f8 * 0.5F);
            float f10 = MathHelper.sin(f8 * 0.5F) * (float) cameraViewDir.x;
            float f11 = MathHelper.sin(f8 * 0.5F) * (float) cameraViewDir.y;
            float f12 = MathHelper.sin(f8 * 0.5F) * (float) cameraViewDir.z;
            Vec3d vec3d = new Vec3d(f10, f11, f12);

            for (int l = 0; l < 4; ++l) {
                avec3d[l] = vec3d.scale(2.0D * avec3d[l].dotProduct(vec3d)).add(avec3d[l].scale((double) (f9 * f9) - vec3d.dotProduct(vec3d))).add(vec3d.crossProduct(avec3d[l]).scale(2.0F * f9));
            }
        }




        Minecraft.getMinecraft().renderEngine.bindTexture(icon);
        f = 0f;
        f1 = 1f;
        f2 = 0f;
        f3 = 1f;

        worldRendererIn.pos((double) f5 + avec3d[0].x, (double) f6 + avec3d[0].y, (double) f7 + avec3d[0].z).tex(f1, f3).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j, k).endVertex();
        worldRendererIn.pos((double) f5 + avec3d[1].x, (double) f6 + avec3d[1].y, (double) f7 + avec3d[1].z).tex(f1, f2).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j, k).endVertex();
        worldRendererIn.pos((double) f5 + avec3d[2].x, (double) f6 + avec3d[2].y, (double) f7 + avec3d[2].z).tex(f, f2).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j, k).endVertex();
        worldRendererIn.pos((double) f5 + avec3d[3].x, (double) f6 + avec3d[3].y, (double) f7 + avec3d[3].z).tex(f, f3).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j, k).endVertex();



    }

    public static void renderAll(List<RocketFx> RocketFxParticles){
        // Get the BufferBuilder for rendering
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        GlStateManager.disableAlpha(); // Ensure alpha test is disabled

        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        //GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);

        // Render custom particles
        for (RocketFx particle : RocketFxParticles) {
            particle.renderParticle2(buffer);
        }

        Tessellator.getInstance().draw();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
    }

    public boolean shouldDisableDepth() {
        return true;
    }

    @Override
    public void onUpdate() {
    }

    public void onUpdate2() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        //Change color and alpha over lifespan
        this.particleAlpha = alpha-alpha*(this.particleAge / (float) this.particleMaxAge); //1 - (this.particleAge / (float) this.particleMaxAge);
        this.particleGreen -= 0.6f / ((float) this.particleMaxAge);
        this.particleBlue -= 0.6f / ((float) this.particleMaxAge);

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
        }
        this.setPosition(posX + this.motionX, posY + this.motionY, posZ + this.motionZ);

        int ch = world.getHeight((int) this.posX, (int) this.posZ);
        if (this.posY < ch  -0.8) {
            this.motionY = 0;
            //this.posY = ch  -0.8 ;

            for (int i = 0; i < 3; i++) {
                BlockPos p = new BlockPos(posX,posY+i,posZ);
                if (world.getBlockState(p).equals(Blocks.AIR.getDefaultState())){
                    this.posY = p.getY()-0.8;
                    break;
                }
            }

            this.motionX = (world.rand.nextFloat() - 0.5) / 2;
            this.motionZ = (world.rand.nextFloat() - 0.5) / 2;
            this.motionY = (world.rand.nextFloat()) / 6;
            this.setPosition(posX + this.motionX, posY + this.motionY, posZ + this.motionZ);


        }

    }
}
