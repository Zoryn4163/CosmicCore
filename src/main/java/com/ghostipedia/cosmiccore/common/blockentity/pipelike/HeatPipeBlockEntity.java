package com.ghostipedia.cosmiccore.common.blockentity.pipelike;

import com.ghostipedia.cosmiccore.CosmicUtils;
import com.ghostipedia.cosmiccore.api.capability.CosmicCapabilities;
import com.ghostipedia.cosmiccore.api.capability.recipe.IHeatContainer;
import com.ghostipedia.cosmiccore.api.pipe.HeatPipeProperties;
import com.ghostipedia.cosmiccore.api.registries.CosmicRegistries;
import com.ghostipedia.cosmiccore.common.data.CosmicThermiaDimensions;
import com.ghostipedia.cosmiccore.common.pipelike.heat.HeatPipeNet;
import com.ghostipedia.cosmiccore.common.pipelike.heat.HeatPipeNetHandler;
import com.ghostipedia.cosmiccore.common.pipelike.heat.HeatPipeType;
import com.ghostipedia.cosmiccore.common.pipelike.heat.LevelHeatPipeNet;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDataInfoProvider;
import com.gregtechceu.gtceu.common.item.PortableScannerBehavior;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.EnumMap;
import java.util.List;

public class HeatPipeBlockEntity extends PipeBlockEntity<HeatPipeType, HeatPipeProperties> implements IDataInfoProvider {

    public static final int FREQUENCY = 5;
    //it would take 1242 days of uptime for this to overflow
    private int timer = 0;
    private int lastExec = 0;
    private final int offset = GTValues.RNG.nextInt(20);

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(HeatPipeBlockEntity.class, PipeBlockEntity.MANAGED_FIELD_HOLDER);

    EnumMap<Direction, IHeatContainer> neighbors = new EnumMap<>(Direction.class);
    protected WeakReference<HeatPipeNet> currentHeatNet = new WeakReference<>(null);
    public HeatPipeNetHandler heatContainer;

    private TickableSubscription updateSubs;

    public HeatPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static void onBlockEntityRegister(BlockEntityType<HeatPipeBlockEntity> heatPipeBlockEntityBlockEntityType) {}

    @Override
    public void onLoad() {
        super.onLoad();
        if(updateSubs == null) {
            updateSubs = this.subscribeServerTick(this::update);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if(updateSubs != null) {
            this.unsubscribe(updateSubs);
            updateSubs = null;
        }
    }

    @Override
    public boolean canAttachTo(Direction side) {
        if(level != null) {
            if(level.getBlockEntity(getBlockPos().relative(side)) instanceof HeatPipeBlockEntity) {
                return false;
            }
            return getBlockEntityCapability(CosmicCapabilities.CAPABILITY_HEAT_CONTAINER, level, getBlockPos().relative(side), side.getOpposite()) != null;
        }
        return false;
    }

    @Nullable
    private static <T> T getBlockEntityCapability(Capability<T> capability, Level level, BlockPos pos,
                                                  @Nullable Direction side) {
        if (level.getBlockState(pos).hasBlockEntity()) {
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                return blockEntity.getCapability(capability, side).resolve().orElse(null);
            }
        }
        return null;
    }

    private HeatPipeNet getHeatNet() {
        if(!(level instanceof ServerLevel serverLevel))
            return null;
        HeatPipeNet currentNet = this.currentHeatNet.get();
        if(currentNet != null && currentNet.isValid() && currentNet.containsNode(getBlockPos()))
            return currentNet;
        LevelHeatPipeNet worldNet = LevelHeatPipeNet.getOrCreate(serverLevel);
        currentNet = worldNet.getNetFromPos(getBlockPos());
        if(currentNet != null) {
            this.currentHeatNet = new WeakReference<>(currentNet);
        }
        return currentNet;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CosmicCapabilities.CAPABILITY_HEAT_CONTAINER) {
            return CosmicCapabilities.CAPABILITY_HEAT_CONTAINER.orEmpty(cap, LazyOptional.of(this::getHeatContainer));
        }
        return super.getCapability(cap, side);
    }

    @NotNull
    public HeatPipeNetHandler getHeatContainer() {
        if (heatContainer == null) {
            heatContainer = new HeatPipeNetHandler(this, this.getNodeData());
            heatContainer.setCurrentThermalEnergy((long)(heatContainer.getBaseTemperature() * (1 + Math.random())));
        }
        return heatContainer;
    }

    public CosmicThermiaDimensions.DimThermiaRecord getEnvironmentalThermia() {
        var dimName = level.dimension().location();
        if (CosmicRegistries.DIM_THERMIA.containKey(dimName))
            return CosmicRegistries.DIM_THERMIA.get(dimName);
        else
            return CosmicThermiaDimensions.OVERWORLD;
    }

    public void removeNeighborCache(Direction direction) {
        neighbors.remove(direction);
    }

    public void update() {
        if (level.isClientSide) return;
        timer++;
        if (getOffsetTimer() % FREQUENCY == 0) {
            //ensure we have a valid heatContainer
            getHeatContainer();

            heatContainer.setLastThermalEnergy(heatContainer.getCurrentThermalEnergy());

            for (Direction direction : Direction.values()) {
                if (isConnected(direction)) {
                    if (!neighbors.containsKey(direction)) {
                        BlockEntity neighbor = level.getBlockEntity(getBlockPos().relative(direction));
                        if (neighbor == null) {
                            neighbors.put(direction, null);
                            continue;
                        }
                        LazyOptional<IHeatContainer> opt = neighbor.getCapability(CosmicCapabilities.CAPABILITY_HEAT_CONTAINER);
                        if (!opt.isPresent()) {
                            neighbors.put(direction, null);
                            continue;
                        }
                        neighbors.put(direction, opt.orElse(null));
                    }
                    IHeatContainer neighbor = neighbors.get(direction);
                    if (neighbor == null) continue;
                    long selfTemp = heatContainer.getCurrentThermalEnergy();
                    long neighborTemp = neighbor.getCurrentThermalEnergy();
                    if (neighborTemp > selfTemp) continue;
                    //long transfer = (long)((selfTemp - neighborTemp) * harmonicMean(neighbor.getConductanceRate(), getHeatContainer().getConductanceRate()));
                    long selfTargetTemp = (long)CosmicUtils.DoubleLerp(selfTemp, neighborTemp, (heatContainer.getConductanceRate() + neighbor.getConductanceRate()) / 2f);
                    long transferOut = selfTemp - selfTargetTemp; //this should always be positive since we are only sending thermia to colder pipes
                    long selfLoss = neighbor.acceptHeatFromNetwork(direction, transferOut);
                    heatContainer.changeHeat(selfLoss);
                }
            }

            double current = heatContainer.getCurrentThermalEnergy();
            double max = heatContainer.getOverloadThreshold();
            if (current > max) {
                checkOverload(current, max);
            }

            //equalize with ambient
            heatContainer.iterateThermalEnergyTowardsEnvironment(getEnvironmentalThermia(), lastExec - timer);

            lastExec = timer;
        }
    }

    protected void checkOverload(double currentTemp, double tempLimit) {
        if (currentTemp * 1.2 > tempLimit) {
            //level.setBlock(getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
            this.getHeatContainer().overload();
        }
    }

    @Override
    public void saveCustomPersistedData(CompoundTag tag, boolean forDrop) {
        tag.putLong("Thermal", getHeatContainer().getCurrentThermalEnergy());
        super.saveCustomPersistedData(tag, forDrop);
    }

    @Override
    public void loadCustomPersistedData(CompoundTag tag) {
        getHeatContainer().setCurrentThermalEnergy(tag.getLong("Thermal"));
        super.loadCustomPersistedData(tag);
    }

    /*
    Having pipes minable with pickaxe-type tools by default seems dangerous
    TODO: Discuss the proper tool?
    @Override
    public GTToolType getPipeTuneTool() {
        return GTToolType.MINING_HAMMER;
    }
     */

    @NotNull
    @Override
    public List<Component> getDataInfo(PortableScannerBehavior.DisplayMode mode) {
        //display temperature as Kelvin (millikelvin to kelvin = /1000)
        return List.of(Component.literal("Current Temp: " + FormattingUtil.formatNumber2Places(getHeatContainer().getCurrentThermalEnergy() / 1000d)));
    }
}
