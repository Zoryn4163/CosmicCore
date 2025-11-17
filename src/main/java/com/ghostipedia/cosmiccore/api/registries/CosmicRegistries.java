package com.ghostipedia.cosmiccore.api.registries;

import com.ghostipedia.cosmiccore.CosmicCore;
import com.ghostipedia.cosmiccore.common.data.CosmicThermiaDimensions;
import com.gregtechceu.gtceu.api.registry.GTRegistry;

public class CosmicRegistries {

    //cosmort frontiert registries
    public static final GTRegistry.RL<CosmicThermiaDimensions.DimThermiaRecord> DIM_THERMIA =
            new GTRegistry.RL<>(CosmicCore.id("dim_thermia"));

    public static void init() {}
}
