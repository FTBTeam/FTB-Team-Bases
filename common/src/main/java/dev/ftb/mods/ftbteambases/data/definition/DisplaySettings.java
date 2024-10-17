package dev.ftb.mods.ftbteambases.data.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record DisplaySettings(String description, String author, Optional<ResourceLocation> previewImage, int displayOrder, boolean devMode) {
    public static final Codec<DisplaySettings> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.STRING.fieldOf("description").forGetter(DisplaySettings::description),
            Codec.STRING.optionalFieldOf("author", "FTB Team").forGetter(DisplaySettings::author),
            ResourceLocation.CODEC.optionalFieldOf("preview_image").forGetter(DisplaySettings::previewImage),
            Codec.INT.optionalFieldOf("display_order", 0).forGetter(DisplaySettings::displayOrder),
            Codec.BOOL.optionalFieldOf("dev_mode", false).forGetter(DisplaySettings::devMode)
    ).apply(builder, DisplaySettings::new));

    public static final StreamCodec<FriendlyByteBuf, DisplaySettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DisplaySettings::description,
            ByteBufCodecs.STRING_UTF8, DisplaySettings::author,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), DisplaySettings::previewImage,
            ByteBufCodecs.VAR_INT, DisplaySettings::displayOrder,
            ByteBufCodecs.BOOL, DisplaySettings::devMode,
            DisplaySettings::new
    );

    public boolean descriptionMatches(String desc) {
        return desc.isEmpty() || description.toLowerCase().contains(desc.toLowerCase());
    }
}
