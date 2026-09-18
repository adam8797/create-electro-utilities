package com.adam8797.electroutilities.net;

import com.adam8797.electroutilities.CreateElectroUtilities;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C: tells the client to open the label editor for the pole at {@code pos} on {@code face}, pre-filled
 * with {@code text} (empty for a new label). Sent when a player applies a label item or right-clicks an
 * existing label empty-handed; see {@link EUPackets}.
 */
public record OpenLabelEditorPayload(BlockPos pos, Direction face, String text) implements CustomPacketPayload {

    public static final Type<OpenLabelEditorPayload> TYPE = new Type<>(CreateElectroUtilities.rl("open_label_editor"));

    public static final StreamCodec<ByteBuf, OpenLabelEditorPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenLabelEditorPayload::pos,
            ByteBufCodecs.BYTE.map(b -> Direction.from3DDataValue(b), d -> (byte) d.get3DDataValue()), OpenLabelEditorPayload::face,
            ByteBufCodecs.stringUtf8(32), OpenLabelEditorPayload::text,
            OpenLabelEditorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
