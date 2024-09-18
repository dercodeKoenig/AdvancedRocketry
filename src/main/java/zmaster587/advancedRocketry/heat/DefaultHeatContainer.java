package zmaster587.advancedRocketry.heat;

public class DefaultHeatContainer implements IHeatContainer {

    private int heat;
    private int maxHeat;

    @Override
    public int getMaxHeat() {
        return maxHeat;
    }

    @Override
    public int getCurrentHeat() {
        return heat;
    }

    @Override
    public void setHeat(final int amount) {
        heat = Math.min(amount, getMaxHeat());
    }

    @Override
    public void setMaxHeat(final int amount) {
        maxHeat = amount;
    }
}
