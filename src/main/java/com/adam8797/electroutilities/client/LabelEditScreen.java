package com.adam8797.electroutilities.client;

import org.lwjgl.glfw.GLFW;

import com.adam8797.electroutilities.net.SetLabelPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sign-style pop-up for editing a pole label: a single line capped at {@code maxLength} characters
 * (varies by pole type). Confirming sends a {@link SetLabelPayload} to the server; cancelling (Escape)
 * changes nothing.
 */
public class LabelEditScreen extends Screen {

    private final BlockPos pos;
    private final Direction face;
    private final String initial;
    private final int maxLength;
    private EditBox input;

    public LabelEditScreen(BlockPos pos, Direction face, String initial, int maxLength) {
        super(Component.literal("Edit Label"));
        this.pos = pos;
        this.face = face;
        this.initial = initial;
        this.maxLength = maxLength;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        input = new EditBox(font, cx - 60, cy - 10, 120, 20, title);
        input.setMaxLength(maxLength);
        input.setValue(initial);
        addRenderableWidget(input);
        setInitialFocus(input);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> confirm())
                .bounds(cx - 60, cy + 20, 58, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(cx + 2, cy + 20, 58, 20).build());
    }

    private void confirm() {
        String text = input.getValue().strip();
        if (!text.isEmpty())
            PacketDistributor.sendToServer(new SetLabelPayload(pos, face, text));
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 30, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
