package com.adam8797.electroutilities;

import com.adam8797.electroutilities.content.utilitypole.WoodSet;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Creative tab holding this mod's content.
 */
public class EUCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateElectroUtilities.ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + CreateElectroUtilities.ID))
                    .icon(() -> new ItemStack(EUBlocks.UTILITY_POLES.get(WoodSet.OAK).get()))
                    .displayItems((params, output) -> {
                        for (WoodSet wood : WoodSet.values())
                            output.accept(EUBlocks.UTILITY_POLES.get(wood).get());
                        output.accept(EUItems.CROSSARM.get());
                        output.accept(EUItems.UTILITY_POLE_LABEL.get());
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
