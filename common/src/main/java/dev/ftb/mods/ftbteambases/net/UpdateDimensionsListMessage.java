package dev.ftb.mods.ftbteambases.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbteambases.FTBTeamBases;
import dev.ftb.mods.ftbteambases.client.FTBTeamBasesClient;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

public record UpdateDimensionsListMessage(List<ResourceKey<Level>> dimensions, boolean add) implements CustomPacketPayload {
	public static final Type<UpdateDimensionsListMessage> TYPE = new Type<>(FTBTeamBases.rl("update_dimensions_list"));
	public static final StreamCodec<FriendlyByteBuf, UpdateDimensionsListMessage> STREAM_CODEC = StreamCodec.composite(
			ResourceKey.streamCodec(Registries.DIMENSION).apply(ByteBufCodecs.list()), UpdateDimensionsListMessage::dimensions,
			ByteBufCodecs.BOOL, UpdateDimensionsListMessage::add,
			UpdateDimensionsListMessage::new
	);

	public static void handle(UpdateDimensionsListMessage message, NetworkManager.PacketContext context) {
		context.queue(() -> {
			Set<ResourceKey<Level>> levels = FTBTeamBasesClient.playerLevels(context.getPlayer());
			if (message.add) {
                levels.addAll(message.dimensions);
            } else {
                message.dimensions.forEach(levels::remove);
            }
		});
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
