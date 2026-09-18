package com.adam8797.electroutilities.content.substation;

import com.adam8797.electroutilities.content.utilitypole.WoodSet;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;

/**
 * A material a substation pole can be made of: the ten vanilla woods (mined with an axe) plus concrete
 * (mined with a pickaxe). Carries the id, map colour, side/end textures, which tool mines it, and the
 * post model the pole is built from (woods share {@code substation_pole_post}; concrete uses a dedicated
 * model that borrows Electroenergetics' concrete-pole texture).
 */
public record SubstationMaterial(String id, MapColor mapColor, ResourceLocation side, ResourceLocation end,
                                 boolean pickaxe, String postModel) {

    public static SubstationMaterial of(WoodSet wood) {
        return new SubstationMaterial(wood.id(), wood.mapColor(), wood.exteriorSide(), wood.exteriorEnd(),
                false, "substation_pole_post");
    }

    // The pole body borrows Electroenergetics' concrete-pole texture via the dedicated post model; side/end
    // here stay plain gray_concrete because they feed the BER-drawn link nubs (which map a full sprite and
    // so need a uniform tile, not EE's non-tiling atlas) and the break particle.
    public static final SubstationMaterial CONCRETE = new SubstationMaterial(
            "concrete", MapColor.STONE,
            ResourceLocation.withDefaultNamespace("block/gray_concrete"),
            ResourceLocation.withDefaultNamespace("block/gray_concrete"),
            true, "concrete_substation_pole_post");

    /** Registry name for this material's substation pole, e.g. {@code oak_substation_pole}. */
    public String blockName() {
        return id + "_substation_pole";
    }
}
