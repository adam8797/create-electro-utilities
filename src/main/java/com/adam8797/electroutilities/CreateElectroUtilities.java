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

        registerPonderLang();
    }

    /**
     * Ponder scene titles/captions are looked up by translation key ({@code <modid>.ponder.<scene>.header}
     * and {@code .text_N}), so the text passed to the storyboards must be backed by lang entries here.
     * Keep these in sync with the {@code showText(...)} calls in EUPonderScenes (order = text_1, text_2, ...).
     */
    private static void registerPonderLang() {
        ponder("utility_pole", "Routing wires with Utility Poles",
                "Utility poles are crafted from logs treated with Transformer Oil, then placed like shafts.",
                "Aim at a pole's top or bottom and use another pole to extend the column vertically.",
                "Use a connector, crossarm, or label item on the pole to add wiring attachments.",
                "Each face is wired independently — add a connector per face as needed.");
        ponder("crossarm", "The Crossarm",
                "The crossarm is crafted from three connectors over a row of planks.",
                "Use it on a utility pole to span three blocks, giving three independently wireable connectors.",
                "A wrench shifts the crossarm's offset — which slots the arms occupy along the pole.",
                "Stacking another pole on top covers the crossarm's centre connector, leaving the two arm nodes.",
                "Sneak-wrench removes the whole crossarm.");
        ponder("substation_pole", "Substation Poles",
                "Substation poles are thin poles that can be used to decorate a substation.",
                "A pole relays redstone up its length, making for cleaner builds.",
                "Other poles can carry a connector or further attachments for your wiring.");
        ponder("labels", "Labelling Poles",
                "A label marks a pole with a short tag, mounted on the pole itself.",
                "Using a label opens a small editor for one short line — up to five characters.",
                "Thinner substation poles fit up to two characters.",
                "Right-click a label with an empty hand to edit it, or sneak to remove it.");
    }

    private static void ponder(String scene, String header, String... texts) {
        REGISTRATE.addRawLang("electroutilities.ponder." + scene + ".header", header);
        for (int i = 0; i < texts.length; i++)
            REGISTRATE.addRawLang("electroutilities.ponder." + scene + ".text_" + (i + 1), texts[i]);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.tryBuild(ID, path);
    }
}
