package com.adam8797.electroutilities;

import com.adam8797.electroutilities.content.utilitypole.UtilityPoleDevice;
import com.george_vi.electroenergetics.CEERegistries;
import com.george_vi.electroenergetics.devices.device.SimulatedDeviceType;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers this mod's {@link SimulatedDeviceType}s onto EE's public
 * {@link CEERegistries#SIMULATED_DEVICE_TYPE} registry, so our blocks participate in EE's
 * electrical simulation.
 */
public class EUSimulatedDevices {

    private static final DeferredRegister<SimulatedDeviceType<?>> DEVICES =
            DeferredRegister.create(CEERegistries.SIMULATED_DEVICE_TYPE, CreateElectroUtilities.ID);

    /** Passive junction backing every utility pole's crossarm / face connectors. */
    public static final DeferredHolder<SimulatedDeviceType<?>, SimulatedDeviceType<UtilityPoleDevice>> UTILITY_POLE =
            DEVICES.register("utility_pole", () -> new SimulatedDeviceType<>(
                    CreateElectroUtilities.rl("utility_pole"),
                    (type, level, pos, sd) -> new UtilityPoleDevice(level, pos, sd, type)));

    public static void register(IEventBus modEventBus) {
        DEVICES.register(modEventBus);
    }
}
