package dev.ftb.mods.ftbteambases.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteambases.client.FTBTeamBasesClient;
import dev.ftb.mods.ftbteambases.mixin.LevelAccess;
import dev.ftb.mods.ftbteambases.mixin.PersistentEntitySectionManagerAccess;
import dev.ftb.mods.ftbteambases.mixin.ServerLevelAccess;
import dev.ftb.mods.ftbteambases.util.MiscUtil;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OpenVisitScreenMessage extends BaseS2CMessage {
    private final Map<ResourceLocation, List<BaseData>> dimensionData;

    public OpenVisitScreenMessage(Map<ResourceLocation, List<BaseData>> dimensionData) {
        this.dimensionData = dimensionData;
    }

    public OpenVisitScreenMessage(FriendlyByteBuf buf) {
        dimensionData = buf.readMap(FriendlyByteBuf::readResourceLocation, buf1 -> buf1.readList(BaseData::fromNetwork));
    }

    @Override
    public MessageType getType() {
        return FTBTeamBasesNet.OPEN_VISIT_SCREEN;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeMap(dimensionData, FriendlyByteBuf::writeResourceLocation,
                (buf1, baseData) -> buf1.writeCollection(baseData, (b, d) -> d.toNetwork(b)));
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        FTBTeamBasesClient.openVisitScreen(dimensionData);
    }

    public record BaseData(String teamName, double tickTime, boolean archived, int blockEntities, int entities, int loadedChunks) {
        public static BaseData fromNetwork(FriendlyByteBuf buf) {
            String teamName = buf.readUtf(Short.MAX_VALUE);
            double tickTime = buf.readDouble();
            boolean archived = buf.readBoolean();
            int blockEntities = buf.readInt();
            int entities = buf.readInt();
            int loadedChunks = buf.readVarInt();

            return new BaseData(teamName, tickTime, archived, blockEntities, entities, loadedChunks);
        }

        public static BaseData create(ServerLevel level, String teamName, double tickTime, boolean archived) {
            int beCount = ((LevelAccess) level).getBlockEntityTickers().size();
            PersistentEntitySectionManager<Entity> m = ((ServerLevelAccess) level).getEntityManager();
            int eCount = ((PersistentEntitySectionManagerAccess) m).getKnownUuids().size();
            int lcCount = level.getChunkSource().getLoadedChunksCount();
            return new BaseData(teamName, tickTime, archived, beCount, eCount, lcCount);
        }

        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeUtf(teamName);
            buf.writeDouble(tickTime);
            buf.writeBoolean(archived);
            buf.writeInt(blockEntities);
            buf.writeInt(entities);
            buf.writeVarInt(loadedChunks);
        }
    }
}
