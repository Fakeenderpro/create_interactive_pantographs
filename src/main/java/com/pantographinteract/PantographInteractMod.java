package com.pantographinteract;

import java.util.function.Supplier;

import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;

import net.createmod.catnip.data.Pair;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class PantographInteractMod {

    private static final String NETWORK_VERSION = "create_interactive_pantographs";

    private PantographInteractMod() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(PantographInteractMod::commonSetup);
        modEventBus.addListener(PantographInteractMod::registerPayloads);
        NeoForge.EVENT_BUS.addListener(PantographDirection::onEntityTick);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION).playToClient(
            PantographSyncPayload.TYPE, PantographSyncPayload.CODEC, PantographSyncPayload::handle);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            PantographCompat.register();
            Schedule.INSTRUCTION_TYPES.add(Pair.<ResourceLocation, Supplier<? extends ScheduleInstruction>>of(
                PantographInstruction.ID, PantographInstruction::new));
            PantographTooltip.register();
        });
    }
}
