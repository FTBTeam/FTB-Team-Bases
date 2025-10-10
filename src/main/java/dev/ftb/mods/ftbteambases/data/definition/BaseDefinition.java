package dev.ftb.mods.ftbteambases.data.definition;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLLoader;

import java.io.IOException;
import java.util.Optional;

import static dev.ftb.mods.ftbteambases.FTBTeamBases.rl;

public record BaseDefinition(ResourceLocation id, DisplaySettings displaySettings, BlockPos spawnOffset,
                             DimensionSettings dimensionSettings, ConstructionType constructionType, XZ extents)
{
    public static final ResourceLocation DEFAULT_PREVIEW = rl("default");
    public static final ResourceLocation FALLBACK_IMAGE = rl("textures/fallback.png");
    public static final ResourceLocation DEFAULT_DIMENSION_TYPE = rl("default");
    public static final ResourceLocation DEFAULT_STRUCTURE_SET = rl( "default");

    public static final Codec<BaseDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(BaseDefinition::id),
            DisplaySettings.CODEC.fieldOf("display").forGetter(BaseDefinition::displaySettings),
            BlockPos.CODEC.optionalFieldOf("spawn_offset", BlockPos.ZERO).forGetter(BaseDefinition::spawnOffset),
            DimensionSettings.CODEC.fieldOf("dimension").forGetter(BaseDefinition::dimensionSettings),
            ConstructionType.CODEC.fieldOf("construction").forGetter(BaseDefinition::constructionType),
            XZ.CODEC.optionalFieldOf("extents", XZ.of(1,1)).forGetter(BaseDefinition::extents)
    ).apply(instance, BaseDefinition::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BaseDefinition> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, BaseDefinition::id,
            DisplaySettings.STREAM_CODEC, BaseDefinition::displaySettings,
            BlockPos.STREAM_CODEC, BaseDefinition::spawnOffset,
            DimensionSettings.STREAM_CODEC, BaseDefinition::dimensionSettings,
            ConstructionType.STREAM_CODEC, BaseDefinition::constructionType,
            XZ.STREAM_CODEC, BaseDefinition::extents,
            BaseDefinition::new
    );

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

    public boolean matchesName(String filterStr) {
        return displaySettings.descriptionMatches(filterStr);
    }

    public boolean shouldShowInGui() {
        return !displaySettings.devMode() || !FMLLoader.isProduction() || ClientConfig.SHOW_DEV_BASES.get();
    }

    public int displayOrder() {
        return displaySettings.displayOrder();
    }

    public String description() {
        return displaySettings().description();
    }

    public String author() {
        return displaySettings().author();
    }

    public ResourceLocation previewImage() {
        return displaySettings().previewImage().orElse(DEFAULT_PREVIEW);
    }
}
