package zmaster587.advancedRocketry.tile;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import zmaster587.advancedRocketry.util.IBrokenPartBlock;

import javax.annotation.Nonnull;
import java.util.Random;

public class TileBrokenPart extends TileEntitySyncable {

    private int stage;
    private int maxStage;
    private float transitionProb;
    private float[] probs;
    private final Random rand;

    public TileBrokenPart() {
        this(0, 0);
    }

    public TileBrokenPart(int stage, int maxStage, float transitionProb, Random rand) {
        this.stage = stage;
        this.maxStage = maxStage;
        this.rand = rand;
        this.initProb(transitionProb);
    }

    public TileBrokenPart(int maxStage, float transitionProb, Random rand) {
        this(0, maxStage, transitionProb, rand);
    }

    public TileBrokenPart(int maxStage, float transitionProb) {
        this(maxStage, transitionProb, new Random());
    }

    public void setStage(int stage) {
        this.stage = stage;
        this.markDirty();
    }

    public int getStage() {
        return this.stage;
    }

    private void initProb(float transitionProb) {
        this.transitionProb = transitionProb;
        this.probs = new float[maxStage];

        for (int i = 0; i < maxStage; i++) {
            this.probs[i] = transitionProb / (float) Math.sqrt(2 * i + 1);
        }
    }

    public boolean transition() {
        if (stage == maxStage) {
            return true;
        }
        for (int i = maxStage - 1; i >= 0; i--) {
            if (stage == i) {
                return false;
            }
            if (rand.nextFloat() < (stage + 1) * this.probs[i]) {
                stage = i;
                this.markDirty();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canRenderBreaking() {
        return true;
    }

    public ItemStack getDrop() {
        return ((IBrokenPartBlock) this.getBlockType()).getDropItem(world.getBlockState(pos), world, this);
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound compound) {
        compound.setInteger("stage", stage);
        compound.setInteger("maxStage", maxStage);
        compound.setFloat("transitionProb", transitionProb);
        return super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(@Nonnull final NBTTagCompound compound) {
        super.readFromNBT(compound);
        stage = compound.getInteger("stage");
        maxStage = compound.getInteger("maxStage");
        transitionProb = compound.getFloat("transitionProb");

        this.initProb(transitionProb);
    }
}
