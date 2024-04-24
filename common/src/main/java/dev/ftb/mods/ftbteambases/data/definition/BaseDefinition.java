package dev.ftb.mods.ftbteambases.data.definition;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.platform.Platform;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.FTBTeamBasesException;
import dev.ftb.mods.ftbteambases.config.ClientConfig;
import dev.ftb.mods.ftbteambases.data.construction.ConstructionWorker;
import dev.ftb.mods.ftbteambases.data.construction.workers.JigsawWorker;
import dev.ftb.mods.ftbteambases.data.construction.workers.PrivateDimensionPregenWorker;
import dev.ftb.mods.ftbteambases.data.construction.workers.RelocatingPregenWorker;
import dev.ftb.mods.ftbteambases.data.construction.workers.SingleStructureWorker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;

import java.io.IOException;
import java.util.Optional;

import static dev.ftb.mods.ftbteambases.FTBTeamBases.rl;

public record BaseDefinition(ResourceLocation id, String description, String author, BlockPos spawnOffset,
                             boolean devMode, Optional<ResourceLocation> previewImage, int displayOrder,
                             DimensionSettings dimensionSettings, ConstructionType constructionType,
                             XZ extents)
{
    public static final ResourceLocation DEFAULT_PREVIEW = rl("default");
    public static final ResourceLocation FALLBACK_IMAGE = rl("textures/fallback.png");
    public static final ResourceLocation DEFAULT_DIMENSION_TYPE = rl("default");
    public static final ResourceLocation DEFAULT_STRUCTURE_SET = rl( "default");

    // TODO get XZ updated in FTB Library for codec support
    private static final Codec<XZ> XZ_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("x").forGetter(XZ::x),
            ExtraCodecs.POSITIVE_INT.fieldOf("z").forGetter(XZ::z)
    ).apply(instance, XZ::of));

    public static final Codec<BaseDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(BaseDefinition::id),
            Codec.STRING.fieldOf("description").forGetter(BaseDefinition::description),
            Codec.STRING.optionalFieldOf("author", "FTB Team").forGetter(BaseDefinition::author),
            BlockPos.CODEC.optionalFieldOf("spawn_offset", BlockPos.ZERO).forGetter(BaseDefinition::spawnOffset),
            Codec.BOOL.optionalFieldOf("dev_mode", false).forGetter(BaseDefinition::devMode),
            ResourceLocation.CODEC.optionalFieldOf("preview_image").forGetter(BaseDefinition::previewImage),
            Codec.INT.optionalFieldOf("display_order", 0).forGetter(BaseDefinition::displayOrder),
            DimensionSettings.CODEC.fieldOf("dimension").forGetter(BaseDefinition::dimensionSettings),
            ConstructionType.CODEC.fieldOf("construction").forGetter(BaseDefinition::constructionType),
            XZ_CODEC.optionalFieldOf("extents", XZ.of(1,1)).forGetter(BaseDefinition::extents)
    ).apply(instance, BaseDefinition::new));

    public static Optional<BaseDefinition> fromJson(JsonElement element) {
        return CODEC.decode(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> FTBTeamBases.LOGGER.error("JSON parse failure: {}", error))
                .map(Pair::getFirst);
    }

    public ConstructionWorker createConstructionWorker(ServerPlayer player) throws IOException {
        if (constructionType.pregen().isPresent()) {
            if (dimensionSettings.privateDimension()) {
                return new PrivateDimensionPregenWorker(player, this, constructionType.pregen().get());
            } else {
                return new RelocatingPregenWorker(player, this, constructionType.pregen().get());
            }
        } else if (constructionType.jigsaw().isPresent()) {
            return new JigsawWorker(player, this, constructionType.jigsaw().get(), dimensionSettings.privateDimension());
        } else if (constructionType.singleStructure().isPresent()) {
            return new SingleStructureWorker(player, this, constructionType.singleStructure().get(), dimensionSettings.privateDimension());
        }

        throw new FTBTeamBasesException("base definition type not supported yet! " + id);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeUtf(description);
        buf.writeUtf(author);
        buf.writeBlockPos(spawnOffset);
        buf.writeBoolean(devMode);
        buf.writeOptional(previewImage, FriendlyByteBuf::writeResourceLocation);
        buf.writeVarInt(displayOrder);
        dimensionSettings.toBytes(buf);
        constructionType.toBytes(buf);
        buf.writeVarInt(extents.x());
        buf.writeVarInt(extents.z());
    }

    public static BaseDefinition fromBytes(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        String desc = buf.readUtf();
        String author = buf.readUtf();
        BlockPos spawnOffset = buf.readBlockPos();
        boolean devMode = buf.readBoolean();
        Optional<ResourceLocation> previewImage = buf.readOptional(FriendlyByteBuf::readResourceLocation);
        int displayOrder = buf.readVarInt();
        DimensionSettings dimensionSettings = DimensionSettings.fromBytes(buf);
        ConstructionType type = ConstructionType.fromBytes(buf);
        XZ extents = XZ.of(buf.readVarInt(), buf.readVarInt());

        return new BaseDefinition(id, desc, author, spawnOffset, devMode, previewImage, displayOrder, dimensionSettings, type, extents);
    }

    public boolean matchesName(String filterStr) {
        return filterStr.isEmpty() || description.toLowerCase().contains(filterStr.toLowerCase());
    }

    public boolean shouldShowInGui() {
        return !devMode || Platform.isDevelopmentEnvironment() || ClientConfig.SHOW_DEV_BASES.get();
    }
}
