package dev.ftb.mods.ftbteambases.config;

import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.snbt.config.*;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.worldgen.chunkgen.ChunkGenerators;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;

public interface ServerConfig {
    NameMap<GameType> GAME_TYPE_NAME_MAP = NameMap.of(GameType.ADVENTURE, GameType.values()).create();

    SNBTConfig CONFIG = SNBTConfig.create(FTBTeamBases.MOD_ID + "-server");

    SNBTConfig GENERAL = CONFIG.addGroup("general");
    BooleanValue CLEAR_PLAYER_INV_ON_JOIN = GENERAL.addBoolean("clear_player_inv_on_join", false)
            .comment("When set to true, the player's inventory will be cleared when joining a team");
    BooleanValue CLEAR_PLAYER_INV_ON_LEAVE = GENERAL.addBoolean("clear_player_inv_on_leave", true)
            .comment("When set to true, the player's inventory will be cleared when leaving a team");

    SNBTConfig LOBBY = CONFIG.addGroup("lobby");
    StringValue LOBBY_STRUCTURE_LOCATION = LOBBY.addString("lobby_structure_location", FTBTeamBases.rl("lobby").toString())
            .comment("Resource location of the structure NBT for the overworld lobby");
    IntValue LOBBY_Y_POS = LOBBY.addInt("lobby_y_pos", 0, -64, 256)
            .comment("Y position at which the lobby structure will be pasted into the overworld. " +
                    "Note: too near world min/max build height may result in parts of the structure being cut off - beware.");
    EnumValue<GameType> LOBBY_GAME_MODE = LOBBY.addEnum("lobby_game_mode", GAME_TYPE_NAME_MAP)
            .comment("The default game mode given to players when in the lobby. Note that admin-mode players are free to change this.");
    IntArrayValue LOBBY_SPAWN = LOBBY.addIntArray("lobby_spawn_pos", new int[]{ 0, 0, 0})
            .comment("Position at which new players spawn. Only used if the lobby structure comes from a pregenerated region!");

    SNBTConfig WORLDGEN = CONFIG.addGroup("worldgen");
    EnumValue<ChunkGenerators> CHUNK_GENERATOR = WORLDGEN.addEnum("chunk_generator", ChunkGenerators.NAME_MAP)
            .comment("The chunk generator to use. SIMPLE_VOID (void dim, one biome), MULTI_BIOME_VOID (void dim, overworld-like biome distribution) and CUSTOM (full worldgen, customisable biome source & noise settings)");
    StringValue SINGLE_BIOME_ID = WORLDGEN.addString("single_biome_id", "")
            .comment("Only used by the CUSTOM and SIMPLE_VOID generators; if non-empty (e.g. 'minecraft:the_void'), the dimension will generate with only this biome. If empty, CUSTOM generator will use an overworld-like biome distribution, and SIMPLE_VOID will use 'minecraft:the_void'");
    EnumValue<FeatureGeneration> FEATURE_GEN = WORLDGEN.addEnum("feature_gen", FeatureGeneration.NAME_MAP)
            .comment("DEFAULT: generate features in non-void worlds, don't generate in void worlds; NEVER: never generate; ALWAYS: always generate");
    StringValue NOISE_SETTINGS = WORLDGEN.addString("noise_settings", "minecraft:overworld")
            .comment("Only used by the CUSTOM generator; resource location for the noise settings to use.");
    BooleanValue ENTITIES_IN_START_STRUCTURE = WORLDGEN.addBoolean("entities_in_start_structure", true)
            .comment("If true, then any entities saved in the starting structure NBT will be included when the structure is generated");

    SNBTConfig NETHER = CONFIG.addGroup("nether");
    BooleanValue ALLOW_NETHER_PORTALS = NETHER.addBoolean("allow_nether_portals", true)
            .comment("When set to true, nether portals may be constructed in team dimensions");
    BooleanValue TEAM_SPECIFIC_NETHER_ENTRY_POINT = NETHER.addBoolean("team_specific_nether_entry_point", true)
            .comment("If true, then players going to the Nether via Nether Portal will be sent to a team-specific position in the Nether");

    static ResourceLocation lobbyLocation() {
        return new ResourceLocation(LOBBY_STRUCTURE_LOCATION.get());
    }

    enum FeatureGeneration {
        DEFAULT,
        NEVER,
        ALWAYS;

        public static final NameMap<FeatureGeneration> NAME_MAP = NameMap.of(DEFAULT, values()).create();

        public boolean shouldGenerate(boolean isVoid) {
            return this == ALWAYS || this == DEFAULT && !isVoid;
        }
    }
}
