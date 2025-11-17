package com.ghostipedia.cosmiccore.api.pipe;

import com.gregtechceu.gtceu.api.data.chemical.material.properties.IMaterialProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.MaterialProperties;
import lombok.Getter;

import java.util.Objects;

public class HeatPipeProperties implements IMaterialProperty {

    @Getter
    private final float conductanceRate;
    @Getter
    private final float conductanceEnvironment;
    @Getter
    private final long thermalCapacity;
    @Getter
    private final long thermalCapacityNegative;
    @Getter
    private final long underloadThreshold;
    @Getter
    private final long overloadThreshold;

    private final int hash;

    /**
     * @param minCapacity
     * @param maxCapacity
     * @param conductanceRate The rate at which the thermalCapacity is linearly interpolated to its neighbours and the environment.
     *                        <br><br>A value of 1.0 would cause the source temperature to instantly match the target temperature.
     * @param conductanceRateEnvironment How sensitive the HeatPipe is to the dimension's ambientThermia.
     *                        <br><br>A value of 1.0 means it will use the dimension's ambient temperature.
     * @param underloadThreshold
     * @param overloadThreshold
     */
    public HeatPipeProperties(long minCapacity, long maxCapacity, float conductanceRate, float conductanceRateEnvironment,
                              long underloadThreshold, long overloadThreshold) {
        assert conductanceRate > 0;
        this.thermalCapacity = maxCapacity;
        this.thermalCapacityNegative = minCapacity;
        this.conductanceRate = conductanceRate;
        this.conductanceEnvironment = conductanceRateEnvironment;
        this.overloadThreshold = overloadThreshold;
        this.underloadThreshold = underloadThreshold;
        hash = Objects.hash(this.thermalCapacityNegative, this.thermalCapacity, this.conductanceRate,
                this.conductanceEnvironment, this.underloadThreshold, this.overloadThreshold);
    }

    /**
     * Creates a {@link HeatPipeProperties} that is not capable of Underloading or Overloading
     */
    public static HeatPipeProperties of(long minCapacity, long maxCapacity, float conductanceRate, float conductanceEnvironment) {
        return new HeatPipeProperties(minCapacity, maxCapacity, conductanceRate, conductanceEnvironment, minCapacity, maxCapacity);
    }

    @Override
    public void verifyProperty(MaterialProperties properties) {

    }

    @Override
    public int hashCode() {
        return hash;
    }
}
