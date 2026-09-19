package com.adam8797.electroutilities.client.ponder;

import java.util.ArrayList;
import java.util.List;

import com.adam8797.electroutilities.CreateElectroUtilities;
import com.adam8797.electroutilities.EUBlocks;
import com.adam8797.electroutilities.EUItems;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;

import net.minecraft.resources.ResourceLocation;

/**
 * Registers this mod's Ponder scenes. Added to Ponder via
 * {@code PonderIndex.addPlugin(...)} during client setup (see {@code EUClient}).
 */
public class EUPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return CreateElectroUtilities.ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        List<ResourceLocation> utilityPoles = new ArrayList<>();
        EUBlocks.UTILITY_POLES.values().forEach(entry -> utilityPoles.add(entry.getId()));
        // Chapters on the utility pole: the pole itself, the crossarm, then labelling.
        helper.forComponents(utilityPoles)
                .addStoryBoard("utility_pole", EUPonderScenes::utilityPole)
                .addStoryBoard("crossarm", EUPonderScenes::crossarm)
                .addStoryBoard("labels", EUPonderScenes::label);

        List<ResourceLocation> substationPoles = new ArrayList<>();
        EUBlocks.SUBSTATION_POLES.forEach(entry -> substationPoles.add(entry.getId()));
        helper.forComponents(substationPoles)
                .addStoryBoard("substation_pole", EUPonderScenes::substationPole)
                .addStoryBoard("labels", EUPonderScenes::label);

        // The crossarm item also opens the crossarm chapter directly.
        helper.forComponents(EUItems.CROSSARM.getId())
                .addStoryBoard("crossarm", EUPonderScenes::crossarm);

        // The label item opens the labelling chapter directly.
        helper.forComponents(EUItems.UTILITY_POLE_LABEL.getId())
                .addStoryBoard("labels", EUPonderScenes::label);
    }
}
