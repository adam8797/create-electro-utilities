package com.adam8797.electroutilities.net;

import com.adam8797.electroutilities.EUItems;
import com.adam8797.electroutilities.client.EUClientHandlers;
import com.adam8797.electroutilities.content.label.LabelableBlockEntity;
import com.adam8797.electroutilities.content.utilitypole.UtilityPoleBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The mod's custom network payloads and their handlers: a C2S {@link SetLabelPayload} (label editor
 * confirm) and an S2C {@link OpenLabelEditorPayload} (open the editor). Registered on the mod bus from
 * {@link com.adam8797.electroutilities.CreateElectroUtilities}.
 */
public final class EUPackets {

    private EUPackets() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(EUPackets::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SetLabelPayload.TYPE, SetLabelPayload.STREAM_CODEC, EUPackets::handleSetLabel);
        // The client handler references client-only classes only inside the lambda body, so it is never
        // resolved on a dedicated server (where this clientbound payload's handler never runs).
        registrar.playToClient(OpenLabelEditorPayload.TYPE, OpenLabelEditorPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> EUClientHandlers.openEditor(payload)));
    }

    private static void handleSetLabel(SetLabelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player))
                return;
            Level level = player.level();
            BlockPos pos = payload.pos();
            Direction face = payload.face();
            if (face.getAxis().isVertical() || !player.canInteractWithBlock(pos, 2.0))
                return;
            if (!(level.getBlockEntity(pos) instanceof LabelableBlockEntity be))
                return;
            String text = UtilityPoleBlockEntity.truncateLabel(payload.text());
            if (text.isEmpty())
                return; // empty = cancel; labels are removed via sneak, not by clearing here
            // A brand-new label costs one label item; editing an existing one is free.
            if (!be.hasLabel() && !consumeLabel(player))
                return;
            be.setLabel(text, face);
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
        });
    }

    private static boolean consumeLabel(ServerPlayer player) {
        if (player.getAbilities().instabuild)
            return true;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.is(EUItems.UTILITY_POLE_LABEL.get())) {
                held.shrink(1);
                return true;
            }
        }
        return false;
    }
}
