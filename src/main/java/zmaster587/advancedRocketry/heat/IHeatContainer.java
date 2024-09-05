package zmaster587.advancedRocketry.heat;

public interface IHeatContainer {

    int getMaxHeat();

    int getCurrentHeat();

    void setHeat(int amount);

    void setMaxHeat(int amount);

    default boolean addHeat(final int amount) {
        if (getCurrentHeat() + amount > getMaxHeat()) {
            return false;
        }
        setHeat(getCurrentHeat() + amount);

        return true;
    }
}
