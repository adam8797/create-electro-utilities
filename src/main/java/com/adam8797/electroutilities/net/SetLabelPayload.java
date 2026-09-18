package com.adam8797.electroutilities.net;

import com.adam8797.electroutilities.CreateElectroUtilities;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * C2S: the player confirmed a label edit in the pop-up, for the pole at {@code pos} on {@code face}.
 * The server validates reach + item and truncates the text; see {@link EUPackets}.
 */
public record SetLabelPayload(BlockPos pos, Direction face, String text) implements CustomPacketPayload {

    public static final Type<SetLabelPayload> TYPE = new Type<>(CreateElectroUtilities.rl("set_label"));

    public static final StreamCodec<ByteBuf, SetLabelPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetLabelPayload::pos,
            ByteBufCodecs.BYTE.map(b -> Direction.from3DDataValue(b), d -> (byte) d.get3DDataValue()), SetLabelPayload::face,
            ByteBufCodecs.stringUtf8(32), SetLabelPayload::text,
            SetLabelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
