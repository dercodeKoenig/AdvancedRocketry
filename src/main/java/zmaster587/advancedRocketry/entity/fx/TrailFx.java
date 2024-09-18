package zmaster587.advancedRocketry.entity.fx;

import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.client.render.DelayedParticleRenderingEventHandler;
import zmaster587.advancedRocketry.event.RocketEventHandler;

import static java.lang.Math.min;
import static java.lang.Math.sqrt;

public class TrailFx extends InverseTrailFx {

    float max_speed_increase = 2f;
    float current_speed_increase = 1.0f;
    int max_engines_for_calculation = 64;

    //increase motion, increase particle size
    public void register_additional_engines(int n){
        float enginepx = min(1,n/(float)max_engines_for_calculation);
        float d = max_speed_increase - current_speed_increase;
        current_speed_increase = current_speed_increase+d*enginepx;
        particleScale*=1+(enginepx*1.5f);
    }

    public TrailFx(World world, double x,
                   double y, double z, double motx, double moty, double motz) {
        super(world, x, y, z, motx, moty, motz);

        //DelayedParticleRenderingEventHandler.fxManager.addParticle(this);

        this.prevPosX = this.posX = x;
        this.prevPosY = this.posY = y;
        this.prevPosZ = this.posZ = z;

        float chroma = this.rand.nextFloat() * 0.2f;
        this.particleRed = .8F + chroma;
        this.particleGreen = .8F + chroma;
        this.particleBlue = .8F + chroma;
        this.particleAlpha = 0.0f;
        this.setSize(0.12F, 0.12F);
        this.particleScale = (this.rand.nextFloat() * 0.6F + 6F)*0.6f;
        this.motionX = motx;
        this.motionY = moty;
        this.motionZ = motz;
        this.particleMaxAge = (int) world.rand.nextInt(400) + 50;

        icon = new ResourceLocation("advancedrocketry:textures/particle/soft1.png");
    }

    @Override
    public int getFXLayer() {
        return 0;
    }

    @Override
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
        this.particleAlpha = min(1 - (this.particleAge / (float) this.particleMaxAge), particleAge/10f)*0.8f;
        double initial_expansion = 1.003;
        double final_expansion = 1.001;
        double current_expansion = initial_expansion - (initial_expansion - final_expansion) * (this.particleAge / (float) this.particleMaxAge);
        this.particleScale *= (float) current_expansion;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
        }
            int ch = world.getHeight((int) this.posX, (int) this.posZ);
            if (this.posY+0.0001 < ch) {
                this.motionY = 0;

                for (int i = 1; i < 3; i++) {
                    if (world.getBlockState(new BlockPos(posX,posY+i,posZ)).equals(Blocks.AIR.getDefaultState())){
                        this.posY = (int)(posY+i);
                        break;
                    }
                }


                this.motionX = (world.rand.nextFloat() - 0.5);
                this.motionZ = (world.rand.nextFloat() - 0.5);
                float speed = world.rand.nextFloat() / 20f+0.01f;
                double l = sqrt(motionX*motionX+motionZ*motionZ);
                motionX *= speed/l;
                motionZ *= speed/l;
                motionY = world.rand.nextFloat() / 60f;


            }
            if (this.motionY < 0) {
                //fast slowdown when near ground AND moving lower
                if (this.posY - ch < 10) {
                    this.motionY *= 0.99;
                }
            }
            this.motionY *= 1-(0.02/(current_speed_increase));
            this.motionY += 0.0005;

        //this.motionX *= 1-(0.02/(current_speed_increase));
        //this.motionZ *= 1-(0.02/(current_speed_increase));

        this.setPosition(posX + this.motionX*current_speed_increase, posY + this.motionY*current_speed_increase, posZ + this.motionZ*current_speed_increase);
    }
}
