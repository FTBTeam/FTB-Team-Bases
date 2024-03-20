package dev.ftb.mods.ftbteambases.net;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteambases.FTBTeamBasesException;
import dev.ftb.mods.ftbteambases.data.construction.BaseConstructionManager;
import dev.ftb.mods.ftbteambases.data.definition.BaseDefinitionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class CreateBaseMessage extends BaseC2SMessage {
    private final ResourceLocation baseId;

    public CreateBaseMessage(ResourceLocation id) {
        baseId = id;
    }

    public CreateBaseMessage(FriendlyByteBuf buf) {
        this.baseId = buf.readResourceLocation();
    }

    @Override
    public MessageType getType() {
        return FTBTeamBasesNet.CREATE_DIMENSION_FOR_TEAM;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(baseId);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer sp) {
                BaseDefinitionManager.getServerInstance().getBaseDefinition(baseId).ifPresentOrElse(base -> {
                    try {
                        BaseConstructionManager.INSTANCE.begin(sp, base);
                    } catch (CommandSyntaxException | FTBTeamBasesException e) {
                        context.getPlayer().displayClientMessage(Component.literal("Failed: " + e.getMessage()).withStyle(ChatFormatting.RED), false);
                    }
                }, () -> context.getPlayer().displayClientMessage(Component.literal("No base: " + baseId).withStyle(ChatFormatting.RED), false));
            }
        });
    }
}
