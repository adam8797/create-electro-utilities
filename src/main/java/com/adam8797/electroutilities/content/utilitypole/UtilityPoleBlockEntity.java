package com.adam8797.electroutilities.content.utilitypole;

import com.adam8797.electroutilities.content.label.LabelableBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds all of a utility pole's mutable feature state:
 * <ul>
 *   <li>{@link #mount}: NONE / CROSSARM / CONNECTORS (mutually exclusive).</li>
 *   <li>{@link #rotation}: the whole-pole facing, 0..7 in 45° steps (see {@link PoleRotation}).</li>
 *   <li>crossarm offset (-1/0/+1): where the pole sits within its 3-slot span.</li>
 *   <li>per-slot connectors, stored by their base (rot-0) face N/E/S/W; used when {@code mount == CONNECTORS}.</li>
 *   <li>a single label (text + base face), independent of the mount.</li>
 * </ul>
 * Connectors and the label are stored in the pole's un-rotated frame; {@link #rotation} is applied as a
 * transform for node positions and rendering. Synced to clients for rendering.
 */
public class UtilityPoleBlockEntity extends BlockEntity implements LabelableBlockEntity {

    public static final int MAX_LABEL_LENGTH = 5;

    private PoleMount mount = PoleMount.NONE;
    private int rotation = 0; // 0..7, 45° clockwise steps (PoleRotation)
    private int crossarmOffset = 0; // -1, 0, +1
    // Indexed by Direction.get3DDataValue() (0..5); connectors only ever live on faces perpendicular
    // to the pole axis, but storing all six keeps indexing simple.
    private final PoleConnector[] faces = {
            PoleConnector.NONE, PoleConnector.NONE, PoleConnector.NONE,
            PoleConnector.NONE, PoleConnector.NONE, PoleConnector.NONE };
    private String labelText = "";
    private Direction labelFace = Direction.NORTH;

    public UtilityPoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public PoleMount getMount() {
        return mount;
    }

    public int getRotation() {
        return rotation;
    }

    public int getCrossarmOffset() {
        return crossarmOffset;
    }

    public PoleConnector getConnector(int faceIndex) {
        return faceIndex >= 0 && faceIndex < faces.length ? faces[faceIndex] : PoleConnector.NONE;
    }

    public PoleConnector getConnector(Direction face) {
        return getConnector(PoleConnectorGeometry.faceIndex(face));
    }

    public String getLabelText() {
        return labelText;
    }

    public Direction getLabelFace() {
        return labelFace;
    }

    public boolean hasLabel() {
        return !labelText.isEmpty();
    }

    @Override
    public int maxLabelLength() {
        return MAX_LABEL_LENGTH;
    }

    // ---- mutators (server-side; each re-syncs) ----

    public void setRotation(int rotation) {
        this.rotation = PoleRotation.wrap(rotation);
        sync();
    }

    public void setCrossarm(int offset) {
        this.mount = PoleMount.CROSSARM;
        this.crossarmOffset = Math.max(-1, Math.min(1, offset));
        sync();
    }

    public void clearMount() {
        this.mount = PoleMount.NONE;
        for (int i = 0; i < faces.length; i++)
            faces[i] = PoleConnector.NONE;
        sync();
    }

    /** Sets a face connector, switching the pole into CONNECTORS mode. */
    public boolean setConnector(Direction face, PoleConnector connector) {
        int i = PoleConnectorGeometry.faceIndex(face);
        if (i < 0)
            return false;
        this.mount = PoleMount.CONNECTORS;
        faces[i] = connector;
        sync();
        return true;
    }

    /** Removes a face connector; drops back to NONE if no connectors remain. */
    public void removeConnector(Direction face) {
        int i = PoleConnectorGeometry.faceIndex(face);
        if (i < 0)
            return;
        faces[i] = PoleConnector.NONE;
        boolean any = false;
        for (PoleConnector c : faces)
            any |= c != PoleConnector.NONE;
        if (!any)
            mount = PoleMount.NONE;
        sync();
    }

    public void setLabel(String text, Direction face) {
        this.labelText = truncateLabel(text);
        this.labelFace = face;
        sync();
    }

    public void clearLabel() {
        this.labelText = "";
        sync();
    }

    public static String truncateLabel(String s) {
        if (s == null)
            return "";
        s = s.strip();
        return s.length() > MAX_LABEL_LENGTH ? s.substring(0, MAX_LABEL_LENGTH) : s;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    // ---- persistence ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("Mount", (byte) mount.ordinal());
        tag.putByte("Rot", (byte) rotation);
        tag.putByte("CrossarmOffset", (byte) crossarmOffset);
        byte[] data = new byte[faces.length];
        for (int i = 0; i < faces.length; i++)
            data[i] = (byte) faces[i].ordinal();
        tag.putByteArray("Connectors", data);
        tag.putString("Label", labelText);
        tag.putByte("LabelFace", (byte) labelFace.get3DDataValue());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mount = PoleMount.byId(tag.getByte("Mount"));
        // Back-compat: older poles stored a crossarm axis (CrossarmZ) instead of a full rotation, and
        // only for a crossarm. The old arms extended along positiveDir(axis) — X→EAST, Z→SOUTH — so
        // migrate X to rotation 2 (East) and Z to rotation 4 (South); using North (0) for Z would flip
        // the arm side for any offset≠0 crossarm and orphan its real arm blocks. Non-crossarm poles had
        // no rotation (0), so their connectors/label keep their stored faces.
        rotation = tag.contains("Rot")
                ? PoleRotation.wrap(tag.getByte("Rot"))
                : (mount == PoleMount.CROSSARM ? (tag.getBoolean("CrossarmZ") ? 4 : 2) : 0);
        crossarmOffset = tag.getByte("CrossarmOffset");
        byte[] data = tag.getByteArray("Connectors");
        for (int i = 0; i < faces.length; i++)
            faces[i] = i < data.length ? PoleConnector.byId(data[i]) : PoleConnector.NONE;
        labelText = truncateLabel(tag.getString("Label"));
        labelFace = Direction.from3DDataValue(tag.getByte("LabelFace"));
    }

    // ---- client sync ----

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
