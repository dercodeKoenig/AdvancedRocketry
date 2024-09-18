package zmaster587.advancedRocketry.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.util.AudioRegistry;
import zmaster587.libVulpes.LibVulpes;

public class EntityLaserNode extends Entity {

    //Used to make sure the emitter is still loaded and so we can send blocks back to the emitter
    //Also we don't want the chunk loading with the laser being there without an emitter it will cause a crash
    //private TileSpaceLaser creator;

    // intentionally not saved, flag to determine if the entity controlling the laser is somehow disconnected
    boolean isValid = false;
    private Vec3d poscorrection;

    public EntityLaserNode(World par1World) {
        super(par1World);
        ignoreFrustumCheck = true;
        noClip = true;
        poscorrection = new Vec3d(0,0,0);
    }

    public EntityLaserNode(World world, double x, double y, double z) {
        this(world);
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public void markValid() {
        isValid = true;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        return true;
    }

    @Override
    public void setDead() {
        this.cleanUp();
        super.setDead();
    }

    /**
     * Removes all the light blocks created by the laser
     */
    private void cleanUp() {

    }

    @Override
    public void onUpdate() {

        if (!world.isRemote && !isValid) {
            this.setDead();

            return;
        }

        if (world.isRemote) {
            double ct = 5;
            double cx = poscorrection.x / ct;
            double cy = poscorrection.y / ct;
            double cz = poscorrection.z / ct;
            poscorrection = poscorrection.subtract(cx, cy, cz);

            this.setPosition(posX + cx, posY + cy, posZ + cz);

        }
        super.onUpdate();

        if (this.world.isRemote) {
            if (Minecraft.getMinecraft().gameSettings.particleSetting < 2) {
                final double spread = 3;
                final double initialSpeed = .5;
                for (int i = 0; i < (Minecraft.getMinecraft().gameSettings.particleSetting == 0 ? 20 : 5); i++)
                    AdvancedRocketry.proxy.spawnParticle("fireworksSpark", world, this.posX + (this.rand.nextDouble() * spread) - (spread / 2), this.posY, this.posZ + (this.rand.nextDouble() * spread) - (spread / 2), initialSpeed * this.rand.nextDouble() - (initialSpeed / 2), initialSpeed * this.rand.nextDouble() * 20 + initialSpeed, initialSpeed * this.rand.nextDouble() - (initialSpeed / 2));


                //this.worldObj.spawnParticle("tilecrack_" + this.worldObj.getBlockId((int)this.posX, (int)this.posY - 1, (int)this.posZ) + "_" + 0, this.posX + (this.rand.nextDouble()*spread) - (spread/2), this.posY + 5, this.posZ + (this.rand.nextDouble()*spread) - (spread/2), initialSpeed * this.rand.nextDouble(), initialSpeed * this.rand.nextDouble() * 20 + initialSpeed, initialSpeed * this.rand.nextDouble() - (initialSpeed/2));
                AdvancedRocketry.proxy.spawnParticle("hugeexplosion", world, this.posX + (this.rand.nextDouble() * spread) - (spread / 2), this.posY, this.posZ + (this.rand.nextDouble() * spread) - (spread / 2), initialSpeed * this.rand.nextDouble(), initialSpeed * this.rand.nextDouble() * 4 + initialSpeed, initialSpeed * this.rand.nextDouble() - (initialSpeed / 2));
            }
            //TODO: use sound setting
            LibVulpes.proxy.playSound(world, getPosition(), AudioRegistry.laserDrill, SoundCategory.NEUTRAL, 1.0f, (1.0F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2F) * 0.7F);

        }
    }

    /**
     * Checks if the entity is in range to render by using the past in distance and comparing it to its average edge
     * length * 64 * renderDistanceWeight Args: distance
     */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double par1) {
        //double d1 = this.boundingBox.getAverageEdgeLength();
        //d1 *= 4096.0D * this.renderDistanceWeight;
        return par1 < 16777216D;
    }



    @Override
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {

        Vec3d new_pos = new Vec3d(x, y, z);
        poscorrection = new_pos.subtract(posX, posY, posZ);


        //Vec3d new_pos = new Vec3d(x, y, z);
        //poscorrection = new_pos.subtract(posX, posY, posZ);
    }


    //Dont need to write anything
    @Override
    protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
        // TODO Auto-generated method stub

    }
}
