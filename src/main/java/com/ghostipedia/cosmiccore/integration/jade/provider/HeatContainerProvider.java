package com.ghostipedia.cosmiccore.integration.jade.provider;

import com.ghostipedia.cosmiccore.CosmicCore;
import com.ghostipedia.cosmiccore.api.capability.recipe.IHeatContainer;
import com.ghostipedia.cosmiccore.common.block.pipelike.HeatPipeBlock;
import com.ghostipedia.cosmiccore.common.blockentity.pipelike.HeatPipeBlockEntity;
import com.gregtechceu.gtceu.GTCEu;

import com.gregtechceu.gtceu.common.blockentity.CableBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class HeatContainerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        BlockEntity be = blockAccessor.getBlockEntity();
        if (be != null) {
            CompoundTag data = blockAccessor.getServerData().getCompound(getUid().toString());
            if (data.contains("heat_data", Tag.TAG_COMPOUND)) {
                var tag = data.getCompound("heat_data");

                //divide by 1,000 for mK -> K conversion
                var currentThermalEnergy = tag.getLong("currentThermalEnergy") / 1000;
                var maximumThermalEnergy = tag.getLong("maximumThermalEnergy") / 1000;
                var minimumThermalEnergy = tag.getLong("minimumThermalEnergy") / 1000;
                var overloadThermalEnergy = tag.getLong("overloadThreshold") / 1000;
                var underloadThermalEnergy = tag.getLong("underloadThreshold") / 1000;
                var conductanceRate = tag.getFloat("conductanceRate") * 100; //show as %
                var conductanceRateAmbient = tag.getFloat("conductanceRateAmbient") * 100; //show as %
                var canBeOverloaded = tag.getBoolean("canBeOverloaded");
                var canBeUnderloaded = tag.getBoolean("canBeUnderloaded");
                var environmentalTemperature = tag.getLong("environmentalTemperature") / 1000;
                var thermalFlow = tag.getLong("thermalFlow");

                iTooltip.add(Component.translatable("cosmiccore.thermia.currentThermalEnergy", currentThermalEnergy));
                iTooltip.add(Component.translatable("cosmiccore.thermia.minimumThermalEnergy", minimumThermalEnergy));
                iTooltip.add(Component.translatable("cosmiccore.thermia.maximumThermalEnergy", maximumThermalEnergy));
                if (canBeUnderloaded)
                    iTooltip.add(Component.translatable("cosmiccore.thermia.underloadThermalEnergy", underloadThermalEnergy));
                if (canBeOverloaded)
                    iTooltip.add(Component.translatable("cosmiccore.thermia.overloadThermalEnergy", overloadThermalEnergy));
                iTooltip.add(Component.translatable("cosmiccore.thermia.conductanceRate", conductanceRate, conductanceRateAmbient));
                iTooltip.add(Component.translatable("cosmiccore.thermia.environmentalTemperature", environmentalTemperature));
                iTooltip.add(Component.translatable("cosmiccore.thermia.thermalFlow", thermalFlow));
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        CompoundTag data = compoundTag.getCompound(getUid().toString());
        if (blockAccessor.getBlock() instanceof HeatPipeBlock hpb) {
            HeatPipeBlockEntity hpbe = (HeatPipeBlockEntity) hpb.getPipeTile(blockAccessor.getLevel(),
                    blockAccessor.getPosition());
            if (hpbe != null) {
                var heatContainer = hpbe.heatContainer;
                if (heatContainer != null) {
                    var cableData = new CompoundTag();
                    cableData.putLong("currentThermalEnergy", heatContainer.getCurrentThermalEnergy());
                    cableData.putLong("maximumThermalEnergy", heatContainer.getMaximumThermalEnergy());
                    cableData.putLong("minimumThermalEnergy", heatContainer.getMinimumThermalEnergy());
                    cableData.putLong("overloadThreshold", heatContainer.getOverloadThreshold());
                    cableData.putLong("underloadThreshold", heatContainer.getUnderloadThreshold());
                    cableData.putFloat("conductanceRate", heatContainer.getConductanceRate());
                    cableData.putFloat("conductanceRateAmbient", hpbe.getEnvironmentalThermia().ambientConductance());
                    cableData.putBoolean("canBeOverloaded", heatContainer.getHeatCanBeOverloaded());
                    cableData.putBoolean("canBeUnderloaded", heatContainer.getHeatCanBeUnderloaded());
                    cableData.putLong("environmentalTemperature", hpbe.getEnvironmentalThermia().ambientThermia());
                    cableData.putLong("thermalFlow", heatContainer.getLastThermalChange());
                    data.put("heat_data", cableData);
                }
            }
        }
        compoundTag.put(getUid().toString(), data);
    }

    @Override
    public ResourceLocation getUid() {
        return CosmicCore.id("heat_info");
    }
}
