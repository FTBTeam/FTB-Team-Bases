package dev.ftb.mods.ftbteambases.net;

import dev.architectury.networking.simple.MessageType;
import dev.architectury.networking.simple.SimpleNetworkManager;
import dev.ftb.mods.ftbteambases.FTBTeamBases;

public interface FTBTeamBasesNet {
    SimpleNetworkManager NET = SimpleNetworkManager.create(FTBTeamBases.MOD_ID);

    MessageType SYNC_BASE_TEMPLATES = NET.registerS2C("sync_base_template", SyncBaseTemplatesMessage::new);
    MessageType UPDATE_DIMENSION_LIST = NET.registerS2C("update_dimension_list", UpdateDimensionsListMessage::new);
    MessageType SHOW_SELECTION_GUI = NET.registerS2C("show_selection_gui", ShowSelectionGuiMessage::new);
    MessageType CREATE_DIMENSION_FOR_TEAM = NET.registerC2S("create_dimension", CreateBaseMessage::new);
    MessageType VOID_TEAM_DIMENSION = NET.registerS2C("void_team_dimension", VoidTeamDimensionMessage::new);
    MessageType OPEN_VISIT_SCREEN = NET.registerS2C("open_visit_screen", OpenVisitScreenMessage::new);
    MessageType VISIT_LIVE_BASE = NET.registerC2S("visit_live_base", VisitBaseMessage::new);

    static void init() {
    }
}
