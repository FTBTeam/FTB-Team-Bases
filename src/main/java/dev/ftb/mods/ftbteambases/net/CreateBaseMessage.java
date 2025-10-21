package dev.ftb.mods.ftbteambases.net;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.FTBTeamBasesException;
import dev.ftb.mods.ftbteambases.data.construction.BaseConstructionManager;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CreateBaseMessage(ResourceLocation baseId) implements CustomPacketPayload {
    public static final Type<CreateBaseMessage> TYPE = new Type<>(FTBTeamBases.rl("create_base"));
    public static final StreamCodec<FriendlyByteBuf, CreateBaseMessage> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, CreateBaseMessage::baseId,
            CreateBaseMessage::new
    );

    public static void handle(CreateBaseMessage message, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer sp) {
            BaseDefinitionManager.getServerInstance().getBaseDefinition(message.baseId).ifPresentOrElse(base -> {
                try {
                    BaseConstructionManager.INSTANCE.begin(sp, base);
                } catch (CommandSyntaxException | FTBTeamBasesException e) {
                    context.player().displayClientMessage(Component.literal("Failed: " + e.getMessage()).withStyle(ChatFormatting.RED), false);
                }
            }, () -> context.player().displayClientMessage(Component.literal("No base: " + message.baseId).withStyle(ChatFormatting.RED), false));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
