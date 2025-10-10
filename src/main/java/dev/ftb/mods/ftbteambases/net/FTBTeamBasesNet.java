package dev.ftb.mods.ftbteambases.net;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = FTBTeamBases.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class FTBTeamBasesNet {
    private static final String NETWORK_VERSION = "1.0";

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(FTBTeamBases.MOD_ID)
                .versioned(NETWORK_VERSION);

        registrar.playToServer(CreateBaseMessage.TYPE, CreateBaseMessage.STREAM_CODEC, CreateBaseMessage::handle);
        registrar.playToServer(VisitBaseMessage.TYPE, VisitBaseMessage.STREAM_CODEC, VisitBaseMessage::handle);

        registrar.playToClient(OpenVisitScreenMessage.TYPE, OpenVisitScreenMessage.STREAM_CODEC, OpenVisitScreenMessage::handle);
        registrar.playToClient(ShowSelectionGuiMessage.TYPE, ShowSelectionGuiMessage.STREAM_CODEC, ShowSelectionGuiMessage::handle);
        registrar.playToClient(SyncBaseTemplatesMessage.TYPE, SyncBaseTemplatesMessage.STREAM_CODEC, SyncBaseTemplatesMessage::handle);
        registrar.playToClient(UpdateDimensionsListMessage.TYPE, UpdateDimensionsListMessage.STREAM_CODEC, UpdateDimensionsListMessage::handle);
        registrar.playToClient(VoidTeamDimensionMessage.TYPE, VoidTeamDimensionMessage.STREAM_CODEC, VoidTeamDimensionMessage::handle);
    }
}
