package com.adam8797.electroutilities.content.utilitypole;

import com.george_vi.electroenergetics.devices.device.DevicesSavedData;
import com.george_vi.electroenergetics.devices.device.SimulatedDeviceType;
import com.george_vi.electroenergetics.foundation.device.SimpleNonTickingElectricalDevice;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * Passive, non-ticking device backing a utility pole's embedded connectors. Like EE's connector, it
 * simply hosts the pole's electrical nodes (which come from the block's per-face configuration) so
 * wires can attach; it adds nothing to the circuit and stores no state of its own.
 */
public class UtilityPoleDevice extends SimpleNonTickingElectricalDevice {

    public UtilityPoleDevice(Level level, BlockPos pos, DevicesSavedData deviceSD, SimulatedDeviceType<?> type) {
        super(level, pos, deviceSD, type);
    }

    @Override
    public void read(CompoundTag tag) {}

    @Override
    public void write(CompoundTag tag) {}
}
