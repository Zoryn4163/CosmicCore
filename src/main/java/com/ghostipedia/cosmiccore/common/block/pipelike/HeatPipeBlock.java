package com.ghostipedia.cosmiccore.common.block.pipelike;

import com.ghostipedia.cosmiccore.api.material.CosmicPropertyKeys;
import com.ghostipedia.cosmiccore.api.pipe.HeatPipeProperties;
import com.ghostipedia.cosmiccore.common.blockentity.pipelike.HeatPipeBlockEntity;
import com.ghostipedia.cosmiccore.common.data.CosmicBlockEntities;
import com.ghostipedia.cosmiccore.common.data.CosmicHeatPipe;
import com.ghostipedia.cosmiccore.common.pipelike.heat.HeatPipeNetHandler;
import com.ghostipedia.cosmiccore.common.pipelike.heat.HeatPipeType;
import com.ghostipedia.cosmiccore.common.pipelike.heat.LevelHeatPipeNet;
import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.block.MaterialPipeBlock;
import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.client.model.PipeModel;
import com.gregtechceu.gtceu.client.renderer.block.PipeBlockRenderer;
import com.gregtechceu.gtceu.common.blockentity.FluidPipeBlockEntity;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;
import com.gregtechceu.gtceu.utils.EntityDamageUtil;
import com.gregtechceu.gtceu.utils.GTUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.ghostipedia.cosmiccore.api.capability.CosmicCapabilities.CAPABILITY_HEAT_CONTAINER;

public class HeatPipeBlock extends MaterialPipeBlock<HeatPipeType, HeatPipeProperties, LevelHeatPipeNet> {
    public PipeBlockRenderer renderer;
    @Getter
    public PipeModel pipeModel;

    public HeatPipeBlock(BlockBehaviour.Properties properties, HeatPipeType type, Material material) {
        super(properties, type, material);
        pipeModel = createPipeModel();
    }

    @Override
    public int tinted(BlockState blockState, @Nullable BlockAndTintGetter blockAndTintGetter, @Nullable BlockPos blockPos, int index) {
        return index == 0 || index == 1 ? material.getMaterialRGB() : -1;
    }

    @Override
    protected HeatPipeProperties createProperties(HeatPipeType heatPipeType, Material material) {
        return heatPipeType.modifyProperties(material.getProperty(CosmicPropertyKeys.HEAT));
    }

    @Override
    protected HeatPipeProperties createMaterialData() {
        return material.getProperty(CosmicPropertyKeys.HEAT);
    }

    @Override
    public LevelHeatPipeNet getWorldPipeNet(ServerLevel level) {
        return LevelHeatPipeNet.getOrCreate(level);
    }

    @Override
    public BlockEntityType<? extends PipeBlockEntity<HeatPipeType, HeatPipeProperties>> getBlockEntityType() {
        return CosmicHeatPipe.HEAT_PIPE.get();
    }

    @Override
    public boolean canPipesConnect(IPipeNode<HeatPipeType, HeatPipeProperties> selfTile, Direction side, IPipeNode<HeatPipeType, HeatPipeProperties> sideTile) {
        return selfTile instanceof HeatPipeBlockEntity && sideTile instanceof HeatPipeBlockEntity;
    }

    @Override
    public boolean canPipeConnectToBlock(IPipeNode<HeatPipeType, HeatPipeProperties> selfTile, Direction side, @Nullable BlockEntity tile) {
        if(tile == null) return false;
        return tile.getCapability(CAPABILITY_HEAT_CONTAINER, side.getOpposite()).isPresent();
    }

    @Override
    protected PipeModel createPipeModel() {
        return pipeType.createPipeModel(material);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // don't apply damage if there is a frame box
        var pipeNode = getPipeTile(level, pos);
        if (pipeNode == null) {
            GTCEu.LOGGER.error("Pipe was null");
            return;
        }
        if (!pipeNode.getFrameMaterial().isNull()) {
            BlockState frameState = GTMaterialBlocks.MATERIAL_BLOCKS.get(TagPrefix.frameGt, pipeNode.getFrameMaterial())
                    .getDefaultState();
            frameState.getBlock().entityInside(frameState, level, pos, entity);
            return;
        }
        if (level.isClientSide) return;
        if (level.getBlockEntity(pos) == null) return;
        HeatPipeBlockEntity pipe = (HeatPipeBlockEntity) level.getBlockEntity(pos);
        HeatPipeNetHandler heatContainer = pipe.getHeatContainer();
        if (heatContainer == null) return;

        if (pipe.getOffsetTimer() % 10 == 0) {
            if (entity instanceof LivingEntity livingEntity) {
                EntityDamageUtil.applyTemperatureDamage(livingEntity,
                        (int)(heatContainer.getCurrentThermalEnergy() / 1000), 1.0F, 20);
            }
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        BlockEntity tile = level.getBlockEntity(pos);
        if (!(tile instanceof HeatPipeBlockEntity h)) return;
        h.removeNeighborCache(GTUtil.getFacingToNeighbor(pos, fromPos));
    }
}
