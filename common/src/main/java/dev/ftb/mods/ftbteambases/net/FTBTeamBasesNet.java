package dev.ftb.mods.ftbteambases.net;

import dev.ftb.mods.ftblibrary.util.NetworkHelper;

public class FTBTeamBasesNet {
    public static void init() {
        NetworkHelper.registerC2S(CreateBaseMessage.TYPE, CreateBaseMessage.STREAM_CODEC, CreateBaseMessage::handle);
        NetworkHelper.registerC2S(VisitBaseMessage.TYPE, VisitBaseMessage.STREAM_CODEC, VisitBaseMessage::handle);

        NetworkHelper.registerS2C(OpenVisitScreenMessage.TYPE, OpenVisitScreenMessage.STREAM_CODEC, OpenVisitScreenMessage::handle);
        NetworkHelper.registerS2C(ShowSelectionGuiMessage.TYPE, ShowSelectionGuiMessage.STREAM_CODEC, ShowSelectionGuiMessage::handle);
        NetworkHelper.registerS2C(SyncBaseTemplatesMessage.TYPE, SyncBaseTemplatesMessage.STREAM_CODEC, SyncBaseTemplatesMessage::handle);
        NetworkHelper.registerS2C(UpdateDimensionsListMessage.TYPE, UpdateDimensionsListMessage.STREAM_CODEC, UpdateDimensionsListMessage::handle);
        NetworkHelper.registerS2C(VoidTeamDimensionMessage.TYPE, VoidTeamDimensionMessage.STREAM_CODEC, VoidTeamDimensionMessage::handle);
    }
}
