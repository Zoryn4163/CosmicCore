package com.ghostipedia.cosmiccore.api.capability.recipe;

import com.ghostipedia.cosmiccore.CosmicUtils;
import com.ghostipedia.cosmiccore.api.capability.IHeatInfoProvider;

import com.ghostipedia.cosmiccore.api.registries.CosmicRegistries;
import com.ghostipedia.cosmiccore.common.data.CosmicThermiaDimensions;
import net.minecraft.core.Direction;

/**
 * The thermalEnergy is measured in Millikelvin (mK), so 0°c = 273,150 mK
 * //TODO: Determine if this is the measurement to be used. Another consideration is Microkelvin
 */
public interface IHeatContainer extends IHeatInfoProvider {
    /**
     * @return The current amount of Thermal Energy
     */
    long getCurrentThermalEnergy();

    /**
     * @return The minimum amount of Thermal Energy the HeatContainer can have before it is clamped (unless it can Underload)
     */
    long getMinimumThermalEnergy();

    /**
     * @return The maximum amount of Thermal Energy the HeatContainer can have before it is clamped (unless it can Overload)
     */
    long getMaximumThermalEnergy();

    /**
     * Check {@link #getHeatCanBeUnderloaded()} for whether this HeatContainer can underload
     * @return The threshold at which an Underload will occur
     */
    long getUnderloadThreshold();

    /**
     * Check {@link #getHeatCanBeOverloaded()} for whether this HeatContainer can overload
     * @return The threshold at which an Overload will occur
     */
    long getOverloadThreshold();

    void setCurrentThermalEnergy(long thermalEnergy);

    /**
     * @return The rate at which this HeatContainer will adjust heat based on its neighbour(s) during linear interpolation
     */
    float getConductanceRate();

    /**
     * @return The rate at which this HeatContainer will adjust heat to match its environment during linear interpolation
     */
    float getConductanceRateEnvironment();

    long getBaseTemperature();

    long getLastThermalChange();

    /**
     * Called when the HeatContainer {@link #getHeatCanBeOverloaded()} and {@link #getCurrentThermalEnergy()}
     * is greater than {@link #getOverloadThreshold()}
     */
    default void overload() {};

    /**
     * Called when the HeatContainer {@link #getHeatCanBeUnderloaded()} and {@link #getCurrentThermalEnergy()}
     * is less than {@link #getUnderloadThreshold()}
     */
    default void underload() {};

    /**
     * Whether this HeatContainer can Overload
     * @return True when OverloadThermalEnergy is greater than MaximumThermalEnergy
     */
    default boolean getHeatCanBeOverloaded() {
        return getOverloadThreshold() > getMaximumThermalEnergy();
    }

    /**
     * Whether this HeatContainer can Underload
     * @return True when UnderloadThermalEnergy is less than MinimumThermalEnergy
     */
    default boolean getHeatCanBeUnderloaded() {
        return getUnderloadThreshold() < getMinimumThermalEnergy();
    }

    /**
     * @param side The direction we want to check if heat can input from
     * @return if this container can accept heat from this side
     */
    boolean inputsHeat(Direction side);

    /**
     * @param side The direction we want to check if heat can output to
     * @return if this container can eject heat from this side
     */
    default boolean outputsHeat(Direction side) {
        return false;
    };

    /**
     * Adds thermalEnergy to the HeatContainer if the Side {@link #inputsHeat(Direction)}
     * @param side The Direction to input from
     * @param thermalEnergy The amount of thermalEnergy
     * @return The amount of thermalEnergy accepted
     */
    default long acceptHeatFromNetwork(Direction side, long thermalEnergy) {
        if (inputsHeat(side)) {
            return changeHeat(thermalEnergy);
        }
        return 0;
    }

    /**
     * Changes the currentThermalEnergy by the provided thermalEnergy <br><br>
     * The new currentThermalEnergy will be clamped by minimumThermalEnergy or maximumThermalEnergy,
         * UNLESS this HeatContainer can underload / overload <br><br>
     * If the HeatContainer CAN Underload {@link #getHeatCanBeUnderloaded()} or Overload {@link #getHeatCanBeOverloaded()}, then it will do so here
     * @param thermalEnergy The delta thermalEnergy
     * @return The amount of thermalEnergy accepted
     */
    default long changeHeat(long thermalEnergy) {
        long currentEnergy = getCurrentThermalEnergy();
        long delta = currentEnergy;
        currentEnergy += thermalEnergy;
        long fit = getHeatChangeToFitWithinTempLimits();

        //always set the new thermal energy, even if it's under / over the min / max, before performing our underload / overload
        //in case the overload / underload do not destroy the HeatContainer and instead need to e.g. warm up or cool down
        currentEnergy -= fit;
        setCurrentThermalEnergy(currentEnergy);

        //heat (or the absence of it) below absolute zero is voided if it cannot be supported, and we underload
        if (fit == 0 && currentEnergy < 0 && !supportsImpossibleHeatValues()) {
            currentEnergy = 0;
            underload();
        }

        delta = delta - currentEnergy;

        if (fit == 0 && getHeatCanBeUnderloaded() && currentEnergy < getUnderloadThreshold()) {
            //we ka-freeze (*actual implementations may vary)
            underload();
        }
        else if (fit == 0 && getHeatCanBeOverloaded() && currentEnergy > getOverloadThreshold()) {
            //we ka-melt (*actual implementations may vary)
            overload();
        }

        return delta;
    }

    /**
     * @return Value to subtract from the currentThermalEnergy in order to reach the minimumThermalEnergy
     * or maximumThermalEnergy unless the HeatContainer can be Underloaded {@link #getHeatCanBeUnderloaded()}
     * or Overloaded {@link #getHeatCanBeOverloaded()}
     */
    default long getHeatChangeToFitWithinTempLimits() {
        //if heat is less than the minimum
        if (getCurrentThermalEnergy() < getMinimumThermalEnergy()) {
            //and it can be underloaded
            if (getHeatCanBeUnderloaded()) {
                if (getCurrentThermalEnergy() < 0 && !supportsImpossibleHeatValues()) {
                    //return an amount to zero it out (absolute zero) if we don't support it
                    return -getCurrentThermalEnergy();
                }
                else {
                    //otherwise don't clamp (we're below absolute zero meow)
                    return 0;
                }
            }
            else
                //otherwise return the value that will clamp to the minimum
                return -(getMinimumThermalEnergy() - getCurrentThermalEnergy());
        }
        //else if heat is over the maximum
        else if (getCurrentThermalEnergy() > getMaximumThermalEnergy()) {
            //and we can overload
            if (getHeatCanBeOverloaded())
                //don't clamp
                return 0;
            else
                //otherwise return the value that will clamp to the maximum
                return getMaximumThermalEnergy() - getCurrentThermalEnergy();
        }
        //otherwise don't clamp since we're not above the max or below the min
        else {
            return 0;
        }
    }

    @Override
    default HeatInfo getHeatInfo() {
        return new HeatInfo(
                getCurrentThermalEnergy(),
                getMinimumThermalEnergy(),
                getMaximumThermalEnergy(),
                getUnderloadThreshold(),
                getOverloadThreshold(),
                getHeatCanBeOverloaded(),
                getHeatCanBeUnderloaded()
        );
    };

    /**
     * @return Whether this HeatContainer is capable of going below Absolute Zero
     */
    @Override
    default boolean supportsImpossibleHeatValues() {
        return false;
    };

    /**
     * @return Max amount of heat that can be output per tick
     */
    default long getEjectLimit() {
        return getMaximumThermalEnergy() / 10;
    };

    /**
     * @return Max amount of heat that can be accepted per tick
     */
    default long getAcceptLimit() {
        return getMaximumThermalEnergy() / 10;
    }

    /**
     * This method does NOT set the {@code currentThermalEnergy}
     * @param dimThermia The {@link com.ghostipedia.cosmiccore.common.data.CosmicThermiaDimensions.DimThermiaRecord}
     * from {@code CosmicRegistries.DIM_THERMIA}
     * @param ticksPassed Number of ticks passed since last call. The thermal interpolation will occur a
     *                    number of times equal to the amount of ticks that have passed.
     * @return The resulting thermalEnergy from ambient modification.
     */
    default long iterateThermalEnergyTowardsEnvironment(CosmicThermiaDimensions.DimThermiaRecord dimThermia, int ticksPassed) {
        var thermalEnergy = getCurrentThermalEnergy();
        for (int i = 0; i < ticksPassed; i++) {
            //thermalEnergy -= (long)(Math.pow(Math.abs(thermalEnergy), 0.3) * environmentalFactor * Math.signum(thermalEnergy));

            thermalEnergy = (long) CosmicUtils.DoubleLerp(thermalEnergy, dimThermia.ambientThermia(), dimThermia.ambientConductance() * getConductanceRateEnvironment());

        }
        return thermalEnergy;
    }

    // I'm not sure what the purpose of this was supposed to be

//    IHeatContainer DEFAULT = new IHeatContainer() {
//
//        @Override
//        public long acceptHeatFromNetwork(Direction side) {
//            return 0;
//        }
//
//        @Override
//        public boolean inputsHeat(Direction side) {
//            return false;
//        }
//
//        @Override
//        public long changeHeat(long heatDifference) {
//            return 0;
//        }
//
//        @Override
//        public long getOverloadLimit() {
//            return 0;
//        }
//
//        @Override
//        public long getHeatStorage() {
//            return 0;
//        }
//    };
}
