package com.adam8797.electroutilities.content.utilitypole;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;

/**
 * The vanilla wood types the utility pole (and crossarm) are generated for.
 *
 * <p>The pole uses the wood's <em>exterior</em> (bark / log-side) texture; the crossarm uses the
 * <em>interior</em> (stripped) texture, as requested. "Stem" woods (crimson/warped) use their
 * {@code _stem} textures instead of {@code _log}.
 */
public enum WoodSet {
    OAK("oak", MapColor.WOOD, false),
    SPRUCE("spruce", MapColor.PODZOL, false),
    BIRCH("birch", MapColor.SAND, false),
    JUNGLE("jungle", MapColor.DIRT, false),
    ACACIA("acacia", MapColor.COLOR_ORANGE, false),
    DARK_OAK("dark_oak", MapColor.COLOR_BROWN, false),
    MANGROVE("mangrove", MapColor.COLOR_RED, false),
    CHERRY("cherry", MapColor.TERRACOTTA_WHITE, false),
    CRIMSON("crimson", MapColor.CRIMSON_STEM, true),
    WARPED("warped", MapColor.WARPED_STEM, true);

    private final String id;
    private final MapColor mapColor;
    private final boolean stem;

    WoodSet(String id, MapColor mapColor, boolean stem) {
        this.id = id;
        this.mapColor = mapColor;
        this.stem = stem;
    }

    /** e.g. {@code "oak"} — used as the registry-name prefix ({@code oak_utility_pole}). */
    public String id() {
        return id;
    }

    /** Looks up a wood by its {@link #id()}, defaulting to {@link #OAK}. */
    public static WoodSet byId(String id) {
        for (WoodSet wood : values())
            if (wood.id.equals(id))
                return wood;
        return OAK;
    }

    public MapColor mapColor() {
        return mapColor;
    }

    /** e.g. {@code "Oak"}, {@code "Dark Oak"} — for generated lang entries. */
    public String displayName() {
        StringBuilder sb = new StringBuilder();
        for (String word : id.split("_")) {
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    private String logSuffix() {
        return stem ? "_stem" : "_log";
    }

    /** Bark / exterior side texture (pole + bracing). */
    public ResourceLocation exteriorSide() {
        return mc(id + logSuffix());
    }

    /** Exterior end (growth-ring) texture. */
    public ResourceLocation exteriorEnd() {
        return mc(id + logSuffix() + "_top");
    }

    /** Stripped / interior side texture (crossarm). */
    public ResourceLocation interiorSide() {
        return mc("stripped_" + id + logSuffix());
    }

    /** Stripped / interior end texture. */
    public ResourceLocation interiorEnd() {
        return mc("stripped_" + id + logSuffix() + "_top");
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace("block/" + path);
    }
}
