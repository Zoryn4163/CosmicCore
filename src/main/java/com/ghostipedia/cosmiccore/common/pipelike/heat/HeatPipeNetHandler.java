package com.ghostipedia.cosmiccore.common.pipelike.heat;

import com.ghostipedia.cosmiccore.api.capability.recipe.IHeatContainer;
import com.ghostipedia.cosmiccore.api.pipe.HeatPipeProperties;
import com.ghostipedia.cosmiccore.common.blockentity.pipelike.HeatPipeBlockEntity;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;

public class HeatPipeNetHandler implements IHeatContainer {

    private final HeatPipeBlockEntity pipe;
    private final HeatPipeProperties properties;
    @Getter
    @Setter
    private long currentThermalEnergy;
    @Getter
    @Setter
    private long lastThermalEnergy;

    public HeatPipeNetHandler(@NotNull HeatPipeBlockEntity pipe, HeatPipeProperties properties) {
        this.pipe = pipe;
        this.properties = properties;
    }

    @Override
    public long getBaseTemperature() {
        return pipe.getEnvironmentalThermia().ambientThermia();
    }

    @Override
    public long getMinimumThermalEnergy() {
        return properties.getThermalCapacityNegative();
    }

    @Override
    public long getMaximumThermalEnergy() {
        return properties.getThermalCapacity();
    }

    @Override
    public long getUnderloadThreshold() {
        return properties.getUnderloadThreshold();
    }

    @Override
    public long getOverloadThreshold() {
        return properties.getOverloadThreshold();
    }

    @Override
    public float getConductanceRate() {
        return properties.getConductanceRate();
    }

    @Override
    public float getConductanceRateEnvironment() {
        return properties.getConductanceEnvironment();
    }

    @Override
    public long getLastThermalChange() {
        return currentThermalEnergy - lastThermalEnergy;
    }

    @Override
    public void overload() {
        //TODO
    }

    @Override
    public void underload() {
        //TODO
    }

    @Override
    public boolean inputsHeat(Direction side) {
        return pipe.isConnected(side);
    }

    @Override
    public boolean outputsHeat(Direction side) {
        return pipe.isConnected(side);
    }


//    private void update() {
//        int tick = pipe.getLevel().getServer().getTickCount();
//        int update = tick - lastUpdateTick;
//        if (update == 0 || update < 0) {
//            lastUpdateTick = tick;
//            return;
//        }
//        lastUpdateTick = tick;
//        lastThermalEnergy = currentThermalEnergy;
//        currentThermalEnergy = pipe.iterateThermalEnergyTowardsEnvironment(currentThermalEnergy, update);
//        //currentThermalEnergy = pipe.loseEnergy(currentThermalEnergy, pipe.getEnvironmentalConductivity() * properties.getConductanceEnvironment(), update);
//    }
}
