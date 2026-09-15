package com.adam8797.electroutilities.client;

import com.adam8797.electroutilities.CreateElectroUtilities;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Client-only setup. Registers EE's default connector model as a standalone (additional) model so the
 * {@link com.adam8797.electroutilities.content.utilitypole.UtilityPoleConnectorRenderer} can bake and
 * draw it on pole faces.
 */
@EventBusSubscriber(modid = CreateElectroUtilities.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EUClient {

    public static final ModelResourceLocation CONNECTOR_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("electroenergetics", "block/connector/block"));

    @SubscribeEvent
    static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(CONNECTOR_MODEL);
    }
}
