package dev.ftb.mods.ftbteambases.data.bases;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.utils.GameInstance;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.command.CommandUtils;
import dev.ftb.mods.ftbteambases.config.ServerConfig;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.data.purging.PurgeManager;
import dev.ftb.mods.ftbteambases.util.DimensionUtils;
import dev.ftb.mods.ftbteambases.util.NetherPortalPlacement;
import dev.ftb.mods.ftbteambases.util.RegionCoords;
import dev.ftb.mods.ftbteambases.util.RegionFileUtil;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.data.TeamArgument;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static dev.ftb.mods.ftbteambases.command.CommandUtils.DIM_MISSING;

/**
 * Keeps track of live and archived base instances. Base details (location etc.) are tracked by team UUID.
 */
public class BaseInstanceManager extends SavedData {
    private static final int MAX_REGION_X = 2000;  // allows for x coord = 2000 * 512 -> X = 1,024,000
    private static final String SAVE_NAME = FTBTeamBases.MOD_ID + "_bases";

    // serialization!  using xmap here, so we get mutable hashmaps in the live manager
    private static final Codec<Map<UUID,LiveBaseDetails>> LIVE_BASES_CODEC
            = Codec.unboundedMap(UUIDUtil.STRING_CODEC, LiveBaseDetails.CODEC).xmap(HashMap::new, Map::copyOf);
    private static final Codec<Map<ResourceLocation,RegionCoords>> GEN_POS_CODEC
            = Codec.unboundedMap(ResourceLocation.CODEC, RegionCoords.CODEC).xmap(HashMap::new, Map::copyOf);
    private static final Codec<Map<ResourceLocation,Integer>> Z_OFF_CODEC
            = Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).xmap(HashMap::new, Map::copyOf);
    private static final Codec<Map<String,ArchivedBaseDetails>> ARCHIVED_BASES_CODEC
            = Codec.unboundedMap(Codec.STRING, ArchivedBaseDetails.CODEC).xmap(HashMap::new, Map::copyOf);
    private static final Codec<Map<UUID,BlockPos>> NETHER_PORTAL_POS_CODEC
            = Codec.unboundedMap(UUIDUtil.STRING_CODEC, BlockPos.CODEC).xmap(HashMap::new, Map::copyOf);
    private static final Codec<Set<UUID>> KNOWN_PLAYERS_CODEC
            = UUIDUtil.CODEC.listOf().xmap(HashSet::new, List::copyOf);

    private static final Codec<BaseInstanceManager> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            LIVE_BASES_CODEC.fieldOf("bases").forGetter(mgr -> mgr.liveBases),
            GEN_POS_CODEC.fieldOf("gen_pos").forGetter(mgr -> mgr.storedGenPos),
            Z_OFF_CODEC.fieldOf("z_offset").forGetter(mgr -> mgr.storedZoffset),
            ARCHIVED_BASES_CODEC.fieldOf("archived_bases").forGetter(mgr -> mgr.archivedBases),
            Codec.INT.fieldOf("next_archive_id").forGetter(mgr -> mgr.nextArchiveId),
            Codec.BOOL.fieldOf("is_lobby_created").forGetter(mgr -> mgr.isLobbyCreated),
            BlockPos.CODEC.fieldOf("lobby_spawn_pos").forGetter(mgr -> mgr.lobbySpawnPos),
            NETHER_PORTAL_POS_CODEC.fieldOf("nether_portal_pos").forGetter(mgr -> mgr.playerNetherPortalLocs),
            KNOWN_PLAYERS_CODEC.fieldOf("known_players").forGetter(mgr -> mgr.knownPlayers)
    ).apply(inst, BaseInstanceManager::new));

    // maps team UUID to live base details
    private final Map<UUID, LiveBaseDetails> liveBases;
    // list of all archived bases (which haven't yet been purged)
    private final Map<String, ArchivedBaseDetails> archivedBases;
    // region relocation: maps dimension ID to next available generation pos (as far as we know!) for that dimension
    private final Map<ResourceLocation, RegionCoords> storedGenPos;
    // region relocation: maps dimension ID to Z-axis-increment
    private final Map<ResourceLocation, Integer> storedZoffset;
    // stores player nether portal return positions
    private final Map<UUID,BlockPos> playerNetherPortalLocs;
    // stores uuids of all players who have connected to this server
    private final Set<UUID> knownPlayers;

    private boolean isLobbyCreated;
    private BlockPos lobbySpawnPos;
    private int nextArchiveId;

    private BaseInstanceManager(Map<UUID, LiveBaseDetails> liveBases, Map<ResourceLocation, RegionCoords> genPos,
                                Map<ResourceLocation, Integer> zOffsets, Map<String, ArchivedBaseDetails> archivedBases,
                                int nextArchiveId, boolean isLobbyCreated, BlockPos lobbySpawnPos,
                                Map<UUID,BlockPos> netherPortalPos, Set<UUID> knownPlayers) {
        this.liveBases = liveBases;
        this.storedGenPos = genPos;
        this.storedZoffset = zOffsets;
        this.archivedBases = archivedBases;
        this.nextArchiveId = nextArchiveId;
        this.isLobbyCreated = isLobbyCreated;
        this.lobbySpawnPos = lobbySpawnPos;
        this.playerNetherPortalLocs = netherPortalPos;
        this.knownPlayers = knownPlayers;
    }

    private static BaseInstanceManager createNew() {
        return new BaseInstanceManager(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(),
                0, false, BlockPos.ZERO, new HashMap<>(), new HashSet<>());
    }

    public static BaseInstanceManager get() {
        return get(Objects.requireNonNull(GameInstance.getServer()));
    }

    public static BaseInstanceManager get(MinecraftServer server) {
        DimensionDataStorage dataStorage = Objects.requireNonNull(server.getLevel(Level.OVERWORLD)).getDataStorage();

        return dataStorage.computeIfAbsent(factory(), SAVE_NAME);
    }

    private static SavedData.Factory<BaseInstanceManager> factory() {
        return new SavedData.Factory<>(BaseInstanceManager::createNew, BaseInstanceManager::load, null);
    }

    /**
     * Get the position to generate the next team base in the given dimension, and update the saved pos.
     *
     * @param server the server instance
     * @param baseDefinition the base definition
     * @param dim            the dimension to generate in
     * @param size           the size of the base, in regions
     * @return start region coords to do the generation in
     */
    public RegionCoords nextGenerationPos(MinecraftServer server, BaseDefinition baseDefinition, ResourceLocation dim, XZ size) {
        if (baseDefinition.dimensionSettings().privateDimension()) {
            // simple case: only one base in the dimension
            return new RegionCoords(0, 0);
        } else {
            // find a place where no existing region files are present
            RegionCoords genPos;
            do {
                genPos = getNextRegionCoords(dim, size);
            } while (anyMCAFilesPresent(server, dim, genPos, size));
            return genPos;
        }
    }

    /**
     * Add a new live base to the manager
     *
     * @param ownerId UUID of the owning team
     * @param liveBaseDetails the details to add
     */
    public void addNewBase(UUID ownerId, LiveBaseDetails liveBaseDetails) {
        liveBases.put(ownerId, liveBaseDetails);

        setDirty();
    }

    @NotNull
    private RegionCoords getNextRegionCoords(ResourceLocation dimensionId, XZ baseSize) {
        RegionCoords genPos = storedGenPos.computeIfAbsent(dimensionId, k -> new RegionCoords(0, 0));
        int zOffset = Math.max(storedZoffset.computeIfAbsent(dimensionId, k -> baseSize.z()), baseSize.z());
        storedZoffset.put(dimensionId, zOffset);
        // move east on the X axis
        RegionCoords nextPos = genPos.offsetBy(baseSize.x() + 2, 0);
        if (nextPos.x() > MAX_REGION_X) {
            // return to X=0 and move south on the Z axis
            nextPos = new RegionCoords(0, nextPos.z() + zOffset + 2);
        }
        storedGenPos.put(dimensionId, nextPos);
        setDirty();
        return genPos;
    }

    private boolean anyMCAFilesPresent(MinecraftServer server, ResourceLocation dim, RegionCoords genPos, XZ size) {
        Path path = RegionFileUtil.getPathForDimension(server, ResourceKey.create(Registries.DIMENSION, dim), "region");

        for (int x = 0; x < size.x(); x++) {
            for (int z = 0; z < size.z(); z++) {
                if (Files.exists(path.resolve(genPos.offsetBy(x, z).filename()))) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean teleportToBaseSpawn(ServerPlayer player, UUID baseId) {
        return teleportToBaseSpawn(player, baseId, false);
    }

    public boolean teleportToBaseSpawn(ServerPlayer player, UUID baseId, boolean setRespawnPoint) {
        LiveBaseDetails base = liveBases.get(baseId);
        if (base != null) {
            ServerLevel level = player.getServer().getLevel(base.dimension());
            if (level != null) {
                Vec3 vec = Vec3.atCenterOf(base.spawnPos());
                player.getServer().executeIfPossible(() ->
                        player.teleportTo(level, vec.x, vec.y, vec.z, player.getYRot(), player.getXRot())
                );

                if (setRespawnPoint) {
                    player.setRespawnPosition(base.dimension(), base.spawnPos(), 0, true, false);
                }

                return true;
            }
        }
        return false;
    }

    public boolean teleportToNether(ServerPlayer player, Team team) throws CommandSyntaxException {
        ServerLevel nether = player.getServer().getLevel(Level.NETHER);
        if (nether == null) {
            throw DIM_MISSING.create(Level.NETHER.location().toString());
        }

        PortalInfo portalInfo = NetherPortalPlacement.teamSpecificEntryPoint(nether, player, team);
        BlockPos pos = BlockPos.containing(portalInfo.pos.x(), portalInfo.pos.y(), portalInfo.pos.z());

        ChunkPos chunkpos = new ChunkPos(pos);
        nether.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkpos, 1, player.getId());
        player.stopRiding();
        if (player.isSleeping()) {
            player.stopSleepInBed(true, true);
        }
        player.teleportTo(nether, portalInfo.pos.x(), portalInfo.pos.y() + 0.01, portalInfo.pos.z(), player.getYRot(), player.getXRot());
        player.setPortalCooldown();

        return true;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        return Util.make(new CompoundTag(), tag ->
                tag.put("manager", CODEC.encodeStart(NbtOps.INSTANCE, this)
                        .resultOrPartial(err -> FTBTeamBases.LOGGER.error("failed to serialize base instance data: {}", err))
                        .orElse(new CompoundTag())));
    }

    private static BaseInstanceManager load(CompoundTag tag) {
        BaseInstanceManager res = CODEC.parse(NbtOps.INSTANCE, tag.getCompound("manager"))
                .resultOrPartial(err -> FTBTeamBases.LOGGER.error("failed to deserialize base instance data: {}", err))
                .orElse(BaseInstanceManager.createNew());

        PurgeManager.INSTANCE.cleanUpPurgedArchives(res);

        return res;
    }

    public Optional<LiveBaseDetails> getBaseForPlayer(ServerPlayer player) {
        return FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
                .map(team -> liveBases.get(team.getTeamId()));
    }

    public Optional<LiveBaseDetails> getBaseForTeam(Team team) {
        return Optional.ofNullable(liveBases.get(team.getTeamId()));
    }

    public boolean teleportToLobby(ServerPlayer serverPlayer) {
        ResourceKey<Level> destLevel = ServerConfig.lobbyDimension().orElse(Level.OVERWORLD);
        return DimensionUtils.teleport(serverPlayer, destLevel, lobbySpawnPos);
    }

    public void deleteAndArchive(MinecraftServer server, Team team) {
        LiveBaseDetails base = liveBases.remove(team.getTeamId());

        if (base != null) {
            String name = server.getProfileCache().get(team.getOwner()).map(GameProfile::getName).orElse("unknown");
            name += "-" + nextArchiveId;
            archivedBases.put(name, new ArchivedBaseDetails(name, base.extents(), base.dimension(), base.spawnPos(), team.getOwner(), Util.getEpochMillis()));
            nextArchiveId++;
            setDirty();
        }
    }

    public Map<UUID,LiveBaseDetails> allLiveBases() {
        return Collections.unmodifiableMap(liveBases);
    }

    public Collection<ArchivedBaseDetails> getArchivedBases() {
        return Collections.unmodifiableCollection(archivedBases.values());
    }

    public Collection<ArchivedBaseDetails> getArchivedBasesFor(UUID owner) {
        return archivedBases.values().stream().filter(b -> b.ownerId().equals(owner)).toList();
    }

    public Optional<ArchivedBaseDetails> getArchivedBase(String archiveId) {
        return Optional.ofNullable(archivedBases.get(archiveId));
    }

    public void removeArchivedBase(String archiveId) {
        if (archivedBases.remove(archiveId) != null) {
            setDirty();
        }
    }

    public void unarchiveBase(MinecraftServer server, ArchivedBaseDetails base) throws CommandSyntaxException {
        Team team = FTBTeamsAPI.api().getManager().getTeamForPlayerID(base.ownerId())
                .orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(base.ownerId()));

        if (team.isPlayerTeam()) {
            Team newParty = team.createParty("", null);

            addNewBase(newParty.getId(), base.makeLiveBaseDetails());
            archivedBases.remove(base.archiveId());

            ServerPlayer player = server.getPlayerList().getPlayer(base.ownerId());
            if (player != null) {
                BaseInstanceManager.get(server).teleportToBaseSpawn(player, newParty.getId());
                player.displayClientMessage(Component.translatable("ftbteambases.message.restored_yours"), false);
            }

            // in case it was scheduled for purging
            PurgeManager.INSTANCE.removePending(List.of(base));
        } else {
            String ownerName = server.getProfileCache().get(base.ownerId())
                    .map(GameProfile::getName)
                    .orElse(base.ownerId().toString());
            throw CommandUtils.PLAYER_IN_PARTY.create(ownerName);
        }
    }

    public void teleportToArchivedBase(ServerPlayer player, String archiveName) {
        ArchivedBaseDetails base = archivedBases.get(archiveName);
        if (base != null) {
            ServerLevel level = player.getServer().getLevel(base.dimension());
            if (level != null) {
                Vec3 vec = Vec3.atCenterOf(base.spawnPos());
                player.getServer().executeIfPossible(() ->
                        player.teleportTo(level, vec.x, vec.y, vec.z, player.getYRot(), player.getXRot())
                );
            }
        }
    }

    public boolean isLobbyCreated() {
        return isLobbyCreated;
    }

    public void setLobbyCreated(boolean lobbyCreated) {
        isLobbyCreated = lobbyCreated;
        setDirty();
    }

    public BlockPos getLobbySpawnPos() {
        return lobbySpawnPos;
    }

    public void setLobbySpawnPos(BlockPos lobbySpawnPos) {
        this.lobbySpawnPos = lobbySpawnPos;
        setDirty();
    }

    public void setPlayerNetherPortalLoc(ServerPlayer player, BlockPos portalPos) {
        if (portalPos == null) {
            if (playerNetherPortalLocs.remove(player.getUUID()) != null) {
                setDirty();
            }
        } else {
            playerNetherPortalLocs.put(player.getUUID(), portalPos);
            setDirty();
        }
    }

    public Optional<BlockPos> getPlayerNetherPortalLoc(ServerPlayer player) {
        return Optional.ofNullable(playerNetherPortalLocs.get(player.getUUID()));
    }

    public void addKnownPlayer(ServerPlayer player) {
        if (knownPlayers.add(player.getUUID())) {
            setDirty();
        }
    }

    public boolean isPlayerKnown(ServerPlayer player) {
        return knownPlayers.contains(player.getUUID());
    }
}
