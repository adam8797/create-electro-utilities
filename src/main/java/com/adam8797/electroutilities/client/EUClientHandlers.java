package com.adam8797.electroutilities.client;

import com.adam8797.electroutilities.content.label.LabelableBlockEntity;
import com.adam8797.electroutilities.net.OpenLabelEditorPayload;

import net.minecraft.client.Minecraft;

/**
 * Client-only entry points invoked from common network code. Referenced only from inside lambda bodies
 * in {@link com.adam8797.electroutilities.net.EUPackets}, so this class is loaded lazily and never on a
 * dedicated server.
 */
public final class EUClientHandlers {

    private EUClientHandlers() {}

    public static void openEditor(OpenLabelEditorPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        int maxLength = 5;
        if (mc.level != null && mc.level.getBlockEntity(payload.pos()) instanceof LabelableBlockEntity be)
            maxLength = be.maxLabelLength();
        mc.setScreen(new LabelEditScreen(payload.pos(), payload.face(), payload.text(), maxLength));
    }
}
