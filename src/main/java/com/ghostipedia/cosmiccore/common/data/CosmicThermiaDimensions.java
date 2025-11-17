package com.ghostipedia.cosmiccore.common.data;

import com.ghostipedia.cosmiccore.api.registries.CosmicRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class CosmicThermiaDimensions {
    public record DimThermiaRecord(
            ResourceLocation dimension,
            long ambientThermia,
            float ambientConductance
    ) {
        public void register() {
            CosmicRegistries.DIM_THERMIA.register(dimension, this);
        }
    }

    public static final DimThermiaRecord OVERWORLD = new DimThermiaRecord(new ResourceLocation("minecraft:overworld"), (273 + 22) * 1000, 0.01f);
    public static final DimThermiaRecord THE_NETHER = new DimThermiaRecord(new ResourceLocation("minecraft:the_nether"), (273 + 120) * 1000, 0.03f);
    public static final DimThermiaRecord THE_END = new DimThermiaRecord(new ResourceLocation("minecraft:the_end"), (273 - 40) * 1000, 0.001f);

    static {
        CosmicRegistries.DIM_THERMIA.unfreeze();
        OVERWORLD.register();
        THE_NETHER.register();
        THE_END.register();
    }

    public static void init() { }
}
