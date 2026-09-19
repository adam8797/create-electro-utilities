package com.adam8797.electroutilities.client;

import com.adam8797.electroutilities.CreateElectroUtilities;
import com.adam8797.electroutilities.EUItems;
import com.adam8797.electroutilities.client.ponder.EUPonderPlugin;

import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

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

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PonderIndex.addPlugin(new EUPonderPlugin()));
    }

    /**
     * Green cast for treated wood: keeps the green channel and dims red/blue so the log reads as treated.
     * Alpha must be 0xFF — the item tint is multiplied as ARGB, so a 0 alpha renders the icon invisible.
     */
    private static final int TREATED_WOOD_TINT = 0xFFB4FFB4;

    @SubscribeEvent
    static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        EUItems.TREATED_WOOD.values().forEach(entry ->
                event.register((stack, layer) -> layer == 0 ? TREATED_WOOD_TINT : -1, entry.get()));
    }
}
