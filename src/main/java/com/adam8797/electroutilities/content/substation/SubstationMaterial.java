package com.adam8797.electroutilities.content.substation;

import com.adam8797.electroutilities.content.utilitypole.WoodSet;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;

/**
 * A material a substation pole can be made of: the ten vanilla woods (mined with an axe) plus concrete
 * (mined with a pickaxe). Carries the id, map colour, side/end textures, and which tool mines it.
 */
public record SubstationMaterial(String id, MapColor mapColor, ResourceLocation side, ResourceLocation end,
                                 boolean pickaxe) {

    public static SubstationMaterial of(WoodSet wood) {
        return new SubstationMaterial(wood.id(), wood.mapColor(), wood.exteriorSide(), wood.exteriorEnd(), false);
    }

    public static final SubstationMaterial CONCRETE = new SubstationMaterial(
            "concrete", MapColor.STONE,
            ResourceLocation.withDefaultNamespace("block/gray_concrete"),
            ResourceLocation.withDefaultNamespace("block/gray_concrete"),
            true);

    /** Registry name for this material's substation pole, e.g. {@code oak_substation_pole}. */
    public String blockName() {
        return id + "_substation_pole";
    }
}
