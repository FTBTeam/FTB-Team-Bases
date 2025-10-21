package dev.ftb.mods.ftbteambases.net;

import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.client.FTBTeamBasesClient;
import dev.ftb.mods.ftbteambases.mixin.LevelAccess;
import dev.ftb.mods.ftbteambases.mixin.PersistentEntitySectionManagerAccess;
import dev.ftb.mods.ftbteambases.mixin.ServerLevelAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record OpenVisitScreenMessage(Map<ResourceLocation, List<BaseData>> dimensionData) implements CustomPacketPayload {
    public static final Type<OpenVisitScreenMessage> TYPE = new Type<>(FTBTeamBases.rl("open_visit_screen"));
    public static final StreamCodec<FriendlyByteBuf, OpenVisitScreenMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::newHashMap, ResourceLocation.STREAM_CODEC, BaseData.STREAM_CODEC.apply(ByteBufCodecs.list())), OpenVisitScreenMessage::dimensionData,
            OpenVisitScreenMessage::new
    );

    public static void handle(OpenVisitScreenMessage message, IPayloadContext ignored) {
        FTBTeamBasesClient.openVisitScreen(message.dimensionData);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record BaseData(String teamName, double tickTime, boolean archived, int blockEntities, int entities, int loadedChunks) {
        public static final StreamCodec<FriendlyByteBuf, BaseData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, BaseData::teamName,
                ByteBufCodecs.DOUBLE, BaseData::tickTime,
                ByteBufCodecs.BOOL, BaseData::archived,
                ByteBufCodecs.INT, BaseData::blockEntities,
                ByteBufCodecs.INT, BaseData::entities,
                ByteBufCodecs.INT, BaseData::loadedChunks,
                BaseData::new
        );

        public static BaseData create(ServerLevel level, String teamName, double tickTime, boolean archived) {
            int beCount = ((LevelAccess) level).getBlockEntityTickers().size();
            PersistentEntitySectionManager<Entity> m = ((ServerLevelAccess) level).getEntityManager();
            int eCount = ((PersistentEntitySectionManagerAccess) m).getKnownUuids().size();
            int lcCount = level.getChunkSource().getLoadedChunksCount();
            return new BaseData(teamName, tickTime, archived, beCount, eCount, lcCount);
        }
    }
}
