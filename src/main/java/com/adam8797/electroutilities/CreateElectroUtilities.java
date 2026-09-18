package com.adam8797.electroutilities;

import com.adam8797.electroutilities.content.substation.SubstationPlacementHelper;
import com.adam8797.electroutilities.content.substation.SubstationPoleBlock;
import com.adam8797.electroutilities.content.utilitypole.UtilityPoleBlock;
import com.adam8797.electroutilities.content.utilitypole.UtilityPolePlacementHelper;
import com.adam8797.electroutilities.net.EUPackets;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Create: Electro Utilities — utility poles, crossarms and substation poles that
 * plug into Create: Electro Energetics' wiring system.
 */
@Mod(CreateElectroUtilities.ID)
public class CreateElectroUtilities {

    public static final String ID = "electroutilities";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static CreateRegistrate REGISTRATE;

    public CreateElectroUtilities(IEventBus modEventBus, ModContainer modContainer) {
        REGISTRATE = CreateRegistrate.create(ID);
        REGISTRATE.registerEventListeners(modEventBus);
        REGISTRATE.addRawLang("itemGroup." + ID, "Create: Electro Utilities");
        // Don't let Registrate auto-add our items to the vanilla Search tab; we populate our own tab
        // via EUCreativeTab. Otherwise items land in Search twice (once via aggregation of our tab,
        // once via Registrate) and the Search tab build crashes with "already exists in the tab's list".
        REGISTRATE.defaultCreativeTab((ResourceKey<CreativeModeTab>) null);

        EUBlocks.register();
        EUItems.register();
        EUBlockEntityTypes.register();
        EUSimulatedDevices.register(modEventBus);
        EUCreativeTab.register(modEventBus);
        EUPackets.register(modEventBus);

        SubstationPoleBlock.placementHelperId = PlacementHelpers.register(new SubstationPlacementHelper());
        UtilityPoleBlock.placementHelperId = PlacementHelpers.register(new UtilityPolePlacementHelper());
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.tryBuild(ID, path);
    }
}
