package dev.ftb.mods.ftbteambases.data.bases;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.utils.GameInstance;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinition;
import dev.ftb.mods.ftbteambases.util.DimensionUtils;
import dev.ftb.mods.ftbteambases.util.RegionCoords;
import dev.ftb.mods.ftbteambases.util.RegionFileUtil;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Keeps track of live and archived base instances. Base details (location etc.) are tracked by team UUID.
 */
public class BaseInstanceManager extends SavedData {
    private static final int MAX_REGION_X = 2000;  // allows for x coord = 2000 * 512 -> X = 1,024,000
    private static final String SAVE_NAME = FTBTeamBases.MOD_ID + "_bases";

    // serialization!
    private static final Codec<Map<UUID,LiveBaseDetails>> LIVE_BASES_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, LiveBaseDetails.CODEC);
    private static final Codec<Map<ResourceLocation,RegionCoords>> GEN_POS_CODEC = Codec.unboundedMap(ResourceLocation.CODEC, RegionCoords.CODEC);
    private static final Codec<Map<ResourceLocation,Integer>> Z_OFF_CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT);
    private static final Codec<Map<UUID,ArchivedBaseDetails>> ARCHIVED_BASES_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, ArchivedBaseDetails.CODEC);
    private static final Codec<Map<UUID,BlockPos>> NETHER_PORTAL_POS_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, BlockPos.CODEC);
    private static final Codec<BaseInstanceManager> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            LIVE_BASES_CODEC.fieldOf("bases").forGetter(mgr -> mgr.liveBases),
            GEN_POS_CODEC.fieldOf("gen_pos").forGetter(mgr -> mgr.storedGenPos),
            Z_OFF_CODEC.fieldOf("z_offset").forGetter(mgr -> mgr.storedZoffset),
            ARCHIVED_BASES_CODEC.fieldOf("archived_bases").forGetter(mgr -> mgr.archivedBases),
            Codec.BOOL.fieldOf("is_lobby_created").forGetter(mgr -> mgr.isLobbyCreated),
            BlockPos.CODEC.fieldOf("lobby_spawn_pos").forGetter(mgr -> mgr.lobbySpawnPos),
            NETHER_PORTAL_POS_CODEC.fieldOf("nether_portal_pos").forGetter(mgr -> mgr.playerNetherPortalLocs)
    ).apply(inst, BaseInstanceManager::createMutable));

    // maps team UUID to live base details
    private final Map<UUID, LiveBaseDetails> liveBases;
    // maps former base owner (player) UUID to archived base details
    private final Map<UUID, ArchivedBaseDetails> archivedBases;
    // maps dimension ID to next available generation pos (as far as we know) for that dimension
    private final Map<ResourceLocation, RegionCoords> storedGenPos;
    // maps dimension ID to Z-increment
    private final Map<ResourceLocation, Integer> storedZoffset;
    // stores player nether portal return positions
    private final Map<UUID,BlockPos> playerNetherPortalLocs;

    private boolean isLobbyCreated;
    private BlockPos lobbySpawnPos;

    private static BaseInstanceManager createMutable(Map<UUID, LiveBaseDetails> bases, Map<ResourceLocation, RegionCoords> genPos, Map<ResourceLocation, Integer> zOffsets, Map<UUID, ArchivedBaseDetails> archivedBases, boolean isLobbyCreated, BlockPos lobbySpawnPos, Map<UUID,BlockPos> netherPortalPos) {
        // we need to make these mutable; codec deserialization provides immutable maps
        return new BaseInstanceManager(new HashMap<>(bases), new HashMap<>(genPos), new HashMap<>(zOffsets), new HashMap<>(archivedBases),
                isLobbyCreated, lobbySpawnPos, new HashMap<>(netherPortalPos));
    }

    private BaseInstanceManager(Map<UUID, LiveBaseDetails> liveBases, Map<ResourceLocation, RegionCoords> genPos, Map<ResourceLocation, Integer> zOffsets, Map<UUID, ArchivedBaseDetails> archivedBases, boolean isLobbyCreated, BlockPos lobbySpawnPos, Map<UUID,BlockPos> netherPortalPos) {
        this.liveBases = liveBases;
        this.storedGenPos = genPos;
        this.storedZoffset = zOffsets;
        this.archivedBases = archivedBases;
        this.isLobbyCreated = isLobbyCreated;
        this.lobbySpawnPos = lobbySpawnPos;
        this.playerNetherPortalLocs = netherPortalPos;
    }

    private BaseInstanceManager() {
        this.liveBases = new HashMap<>();
        this.storedGenPos = new HashMap<>();
        this.storedZoffset = new HashMap<>();
        this.archivedBases = new HashMap<>();
        this.isLobbyCreated = false;
        this.lobbySpawnPos = null;
        this.playerNetherPortalLocs = new HashMap<>();
    }

    public static BaseInstanceManager get() {
        return get(Objects.requireNonNull(GameInstance.getServer()));
    }

    public static BaseInstanceManager get(MinecraftServer server) {
        DimensionDataStorage dataStorage = Objects.requireNonNull(server.getLevel(Level.OVERWORLD)).getDataStorage();

        return dataStorage.computeIfAbsent(BaseInstanceManager::load, BaseInstanceManager::new, SAVE_NAME);
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
    public void add(UUID ownerId, LiveBaseDetails liveBaseDetails) {
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

    public boolean teleportToSpawn(ServerPlayer player, UUID baseId) {
        return teleportToSpawn(player, baseId, false);
    }

    public boolean teleportToSpawn(ServerPlayer player, UUID baseId, boolean setRespawnPoint) {
        LiveBaseDetails base = liveBases.get(baseId);
        if (base != null) {
            ServerLevel level = player.getServer().getLevel(base.dimension());
            if (level != null) {
                Vec3 vec = Vec3.atBottomCenterOf(base.spawnPos());
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

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        return Util.make(new CompoundTag(), tag ->
                tag.put("manager", CODEC.encodeStart(NbtOps.INSTANCE, this).result()
                        .orElse(new CompoundTag())));
    }

    private static BaseInstanceManager load(CompoundTag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag.getCompound("manager")).result()
                .orElse(new BaseInstanceManager());
    }

    public Optional<LiveBaseDetails> getBaseForPlayer(ServerPlayer player) {
        return FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
                .map(team -> liveBases.get(team.getTeamId()));
    }

    public Optional<LiveBaseDetails> getBaseForTeam(Team team) {
        return Optional.ofNullable(liveBases.get(team.getTeamId()));
    }

    public boolean teleportToLobby(ServerPlayer serverPlayer) {
        return DimensionUtils.teleport(serverPlayer, ServerLevel.OVERWORLD, lobbySpawnPos);
    }

    public void deleteAndArchive(Team team) {
        LiveBaseDetails base = liveBases.remove(team.getTeamId());

        if (base != null) {
            archivedBases.put(team.getOwner(), new ArchivedBaseDetails(base.extents(), base.dimension(), base.spawnPos(), team.getOwner(), Util.getEpochMillis()));
            setDirty();
        }
    }

    public Map<UUID,LiveBaseDetails>  allLiveBases() {
        return Collections.unmodifiableMap(liveBases);
    }

    public Collection<ArchivedBaseDetails> getArchivedBases() {
        return archivedBases.values();
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

}
